/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.maybe;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.*;

import io.reactivex.annotations.Nullable;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.SimpleQueue;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Run all MaybeSources of an array at once and signal their values as they become available.
 *
 * @param <T> the value type
 */
public final class MaybeMergeArray<T> extends Flowable<T> {

    final MaybeSource<? extends T>[] sources;

    public MaybeMergeArray(MaybeSource<? extends T>[] sources) {
        this.sources = sources;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        MaybeSource<? extends T>[] maybes = sources;
        int n = maybes.length;

        SimpleQueueWithConsumerIndex<Object> queue;

        if (n <= bufferSize()) {
            queue = new MpscFillOnceSimpleQueue<Object>(n);
        } else {
            queue = new ClqSimpleQueue<Object>();
        }
        MergeMaybeObserver<T> parent = new MergeMaybeObserver<T>(s, n, queue);

        s.onSubscribe(parent);

        AtomicThrowable e = parent.error;

        for (MaybeSource<? extends T> source : maybes) {
            if (parent.isCancelled() || e.get() != null) {
                return;
            }

            source.subscribe(parent);
        }
    }

    static final class MergeMaybeObserver<T>
    extends BasicIntQueueSubscription<T> implements MaybeObserver<T> {

        private static final long serialVersionUID = -660395290758764731L;

        final Subscriber<? super T> actual;

        final CompositeDisposable set;

        final AtomicLong requested;

        final SimpleQueueWithConsumerIndex<Object> queue;

        final AtomicThrowable error;

        final int sourceCount;

        volatile boolean cancelled;

        boolean outputFused;

        long consumed;

        MergeMaybeObserver(Subscriber<? super T> actual, int sourceCount, SimpleQueueWithConsumerIndex<Object> queue) {
            this.actual = actual;
            this.sourceCount = sourceCount;
            this.set = new CompositeDisposable();
            this.requested = new AtomicLong();
            this.error = new AtomicThrowable();
            this.queue = queue;
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & ASYNC) != 0) {
                outputFused = true;
                return ASYNC;
            }
            return NONE;
        }

        @Nullable
        @SuppressWarnings("unchecked")
        @Override
        public T poll() throws Exception {
            for (;;) {
                Object o = queue.poll();
                if (o != NotificationLite.COMPLETE) {
                    return (T)o;
                }
            }
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }

        @Override
        public void clear() {
            queue.clear();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                set.dispose();
                if (getAndIncrement() == 0) {
                    queue.clear();
                }
            }
        }

        @Override
        public void onSubscribe(Disposable d) {
            set.add(d);
        }

        @Override
        public void onSuccess(T value) {
            queue.offer(value);
            drain();
        }

        @Override
        public void onError(Throwable e) {
            if (error.addThrowable(e)) {
                set.dispose();
                queue.offer(NotificationLite.COMPLETE);
                drain();
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            queue.offer(NotificationLite.COMPLETE);
            drain();
        }

        boolean isCancelled() {
            return cancelled;
        }

        @SuppressWarnings("unchecked")
        void drainNormal() {
            int missed = 1;
            Subscriber<? super T> a = actual;
            SimpleQueueWithConsumerIndex<Object> q = queue;
            long e = consumed;

            for (;;) {

                long r = requested.get();

                while (e != r) {
                    if (cancelled) {
                        q.clear();
                        return;
                    }

                    Throwable ex = error.get();
                    if (ex != null) {
                        q.clear();
                        a.onError(error.terminate());
                        return;
                    }

                    if (q.consumerIndex() == sourceCount) {
                        a.onComplete();
                        return;
                    }

                    Object v = q.poll();

                    if (v == null) {
                        break;
                    }

                    if (v != NotificationLite.COMPLETE) {
                        a.onNext((T)v);

                        e++;
                    }
                }

                if (e == r) {
                    Throwable ex = error.get();
                    if (ex != null) {
                        q.clear();
                        a.onError(error.terminate());
                        return;
                    }

                    while (q.peek() == NotificationLite.COMPLETE) {
                        q.drop();
                    }

                    if (q.consumerIndex() == sourceCount) {
                        a.onComplete();
                        return;
                    }
                }

                consumed = e;
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }

        }

        void drainFused() {
            int missed = 1;
            Subscriber<? super T> a = actual;
            SimpleQueueWithConsumerIndex<Object> q = queue;

            for (;;) {
                if (cancelled) {
                    q.clear();
                    return;
                }
                Throwable ex = error.get();
                if (ex != null) {
                    q.clear();
                    a.onError(ex);
                    return;
                }

                boolean d = q.producerIndex() == sourceCount;

                if (!q.isEmpty()) {
                    a.onNext(null);
                }

                if (d) {
                    a.onComplete();
                    return;
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }

        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            if (outputFused) {
                drainFused();
            } else {
                drainNormal();
            }
        }
    }

    interface SimpleQueueWithConsumerIndex<T> extends SimpleQueue<T> {

        @Nullable
        @Override
        T poll();

        T peek();

        void drop();

        int consumerIndex();

        int producerIndex();
    }

    static final class MpscFillOnceSimpleQueue<T>
    extends AtomicReferenceArray<T>
    implements SimpleQueueWithConsumerIndex<T> {


        private static final long serialVersionUID = -7969063454040569579L;
        final AtomicInteger producerIndex;

        int consumerIndex;

        MpscFillOnceSimpleQueue(int length) {
            super(length);
            this.producerIndex = new AtomicInteger();
        }

        @Override
        public boolean offer(T value) {
            ObjectHelper.requireNonNull(value, "value is null");
            int idx = producerIndex.getAndIncrement();
            if (idx < length()) {
                lazySet(idx, value);
                return true;
            }
            return false;
        }

        @Override
        public boolean offer(T v1, T v2) {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public T poll() {
            int ci = consumerIndex;
            if (ci == length()) {
                return null;
            }
            AtomicInteger pi = producerIndex;
            for (;;) {
                T v = get(ci);
                if (v != null) {
                    consumerIndex = ci + 1;
                    lazySet(ci, null);
                    return v;
                }
                if (pi.get() == ci) {
                    return null;
                }
            }
        }

        @Override
        public T peek() {
            int ci = consumerIndex;
            if (ci == length()) {
                return null;
            }
            return get(ci);
        }

        @Override
        public void drop() {
            int ci = consumerIndex;
            lazySet(ci, null);
            consumerIndex = ci + 1;
        }

        @Override
        public boolean isEmpty() {
            return consumerIndex == producerIndex();
        }

        @Override
        public void clear() {
            while (poll() != null && !isEmpty()) { }
        }

        @Override
        public int consumerIndex() {
            return consumerIndex;
        }

        @Override
        public int producerIndex() {
            return producerIndex.get();
        }
    }

    static final class ClqSimpleQueue<T> extends ConcurrentLinkedQueue<T> implements SimpleQueueWithConsumerIndex<T> {

        private static final long serialVersionUID = -4025173261791142821L;

        int consumerIndex;

        final AtomicInteger producerIndex;

        ClqSimpleQueue() {
            this.producerIndex = new AtomicInteger();
        }

        @Override
        public boolean offer(T v1, T v2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean offer(T e) {
            producerIndex.getAndIncrement();
            return super.offer(e);
        }

        @Nullable
        @Override
        public T poll() {
            T v = super.poll();
            if (v != null) {
                consumerIndex++;
            }
            return v;
        }

        @Override
        public int consumerIndex() {
            return consumerIndex;
        }

        @Override
        public int producerIndex() {
            return producerIndex.get();
        }

        @Override
        public void drop() {
            poll();
        }
    }
}
