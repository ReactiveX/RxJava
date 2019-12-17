/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.Objects;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Multicasts a Flowable over a selector function.
 *
 * @param <T> the input value type
 * @param <R> the output value type
 */
public final class FlowablePublishMulticast<T, R> extends AbstractFlowableWithUpstream<T, R> {

    final Function<? super Flowable<T>, ? extends Publisher<? extends R>> selector;

    final int prefetch;

    final boolean delayError;

    public FlowablePublishMulticast(Flowable<T> source,
            Function<? super Flowable<T>, ? extends Publisher<? extends R>> selector, int prefetch,
            boolean delayError) {
        super(source);
        this.selector = selector;
        this.prefetch = prefetch;
        this.delayError = delayError;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        MulticastProcessor<T> mp = new MulticastProcessor<T>(prefetch, delayError);

        Publisher<? extends R> other;

        try {
            other = Objects.requireNonNull(selector.apply(mp), "selector returned a null Publisher");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        OutputCanceller<R> out = new OutputCanceller<R>(s, mp);

        other.subscribe(out);

        source.subscribe(mp);
    }

    static final class OutputCanceller<R> implements FlowableSubscriber<R>, Subscription {
        final Subscriber<? super R> downstream;

        final MulticastProcessor<?> processor;

        Subscription upstream;

        OutputCanceller(Subscriber<? super R> actual, MulticastProcessor<?> processor) {
            this.downstream = actual;
            this.processor = processor;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(R t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
            processor.dispose();
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
            processor.dispose();
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
            processor.dispose();
        }
    }

    static final class MulticastProcessor<T> extends Flowable<T> implements FlowableSubscriber<T>, Disposable {

        @SuppressWarnings("rawtypes")
        static final MulticastSubscription[] EMPTY = new MulticastSubscription[0];

        @SuppressWarnings("rawtypes")
        static final MulticastSubscription[] TERMINATED = new MulticastSubscription[0];

        final AtomicInteger wip;

        final AtomicReference<MulticastSubscription<T>[]> subscribers;

        final int prefetch;

        final int limit;

        final boolean delayError;

        final AtomicReference<Subscription> upstream;

        volatile SimpleQueue<T> queue;

        int sourceMode;

        volatile boolean done;
        Throwable error;

        int consumed;

        @SuppressWarnings("unchecked")
        MulticastProcessor(int prefetch, boolean delayError) {
            this.prefetch = prefetch;
            this.limit = prefetch - (prefetch >> 2); // request after 75% consumption
            this.delayError = delayError;
            this.wip = new AtomicInteger();
            this.upstream = new AtomicReference<Subscription>();
            this.subscribers = new AtomicReference<MulticastSubscription<T>[]>(EMPTY);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this.upstream, s)) {
                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<T> qs = (QueueSubscription<T>) s;

                    int m = qs.requestFusion(QueueSubscription.ANY);
                    if (m == QueueSubscription.SYNC) {
                        sourceMode = m;
                        queue = qs;
                        done = true;
                        drain();
                        return;
                    }
                    if (m == QueueSubscription.ASYNC) {
                        sourceMode = m;
                        queue = qs;
                        QueueDrainHelper.request(s, prefetch);
                        return;
                    }
                }

                queue = QueueDrainHelper.createQueue(prefetch);

                QueueDrainHelper.request(s, prefetch);
            }
        }

        @Override
        public void dispose() {
            SubscriptionHelper.cancel(upstream);
            if (wip.getAndIncrement() == 0) {
                SimpleQueue<T> q = queue;
                if (q != null) {
                    q.clear();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return upstream.get() == SubscriptionHelper.CANCELLED;
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            if (sourceMode == QueueSubscription.NONE && !queue.offer(t)) {
                upstream.get().cancel();
                onError(new MissingBackpressureException());
                return;
            }
            drain();
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            error = t;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                drain();
            }
        }

        boolean add(MulticastSubscription<T> s) {
            for (;;) {
                MulticastSubscription<T>[] current = subscribers.get();
                if (current == TERMINATED) {
                    return false;
                }
                int n = current.length;
                @SuppressWarnings("unchecked")
                MulticastSubscription<T>[] next = new MulticastSubscription[n + 1];
                System.arraycopy(current, 0, next, 0, n);
                next[n] = s;
                if (subscribers.compareAndSet(current, next)) {
                    return true;
                }
            }
        }

        @SuppressWarnings("unchecked")
        void remove(MulticastSubscription<T> s) {
            for (;;) {
                MulticastSubscription<T>[] current = subscribers.get();
                int n = current.length;
                if (n == 0) {
                    return;
                }
                int j = -1;

                for (int i = 0; i < n; i++) {
                    if (current[i] == s) {
                        j = i;
                        break;
                    }
                }

                if (j < 0) {
                    return;
                }
                MulticastSubscription<T>[] next;
                if (n == 1) {
                    next = EMPTY;
                } else {
                    next = new MulticastSubscription[n - 1];
                    System.arraycopy(current, 0, next, 0, j);
                    System.arraycopy(current, j + 1, next, j, n - j - 1);
                }
                if (subscribers.compareAndSet(current, next)) {
                    return;
                }
            }
        }

        @Override
        protected void subscribeActual(Subscriber<? super T> s) {
            MulticastSubscription<T> ms = new MulticastSubscription<T>(s, this);
            s.onSubscribe(ms);
            if (add(ms)) {
                if (ms.isCancelled()) {
                    remove(ms);
                    return;
                }
                drain();
            } else {
                Throwable ex = error;
                if (ex != null) {
                    s.onError(ex);
                } else {
                    s.onComplete();
                }
            }
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;

            SimpleQueue<T> q = queue;

            int upstreamConsumed = consumed;
            int localLimit = limit;
            boolean canRequest = sourceMode != QueueSubscription.SYNC;
            AtomicReference<MulticastSubscription<T>[]> subs = subscribers;

            MulticastSubscription<T>[] array = subs.get();

            outer:
            for (;;) {

                int n = array.length;

                if (q != null && n != 0) {
                    long r = Long.MAX_VALUE;

                    for (MulticastSubscription<T> ms : array) {
                        long u = ms.get() - ms.emitted;
                        if (u != Long.MIN_VALUE) {
                            if (r > u) {
                                r = u;
                            }
                        } else {
                            n--;
                        }
                    }

                    if (n == 0) {
                        r = 0;
                    }

                    while (r != 0) {
                        if (isDisposed()) {
                            q.clear();
                            return;
                        }

                        boolean d = done;

                        if (d && !delayError) {
                            Throwable ex = error;
                            if (ex != null) {
                                errorAll(ex);
                                return;
                            }
                        }

                        T v;

                        try {
                            v = q.poll();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            SubscriptionHelper.cancel(upstream);
                            errorAll(ex);
                            return;
                        }

                        boolean empty = v == null;

                        if (d && empty) {
                            Throwable ex = error;
                            if (ex != null) {
                                errorAll(ex);
                            } else {
                                completeAll();
                            }
                            return;
                        }

                        if (empty) {
                            break;
                        }

                        boolean subscribersChange = false;

                        for (MulticastSubscription<T> ms : array) {
                            long msr = ms.get();
                            if (msr != Long.MIN_VALUE) {
                                if (msr != Long.MAX_VALUE) {
                                    ms.emitted++;
                                }
                                ms.downstream.onNext(v);
                            } else {
                                subscribersChange = true;
                            }
                        }

                        r--;

                        if (canRequest && ++upstreamConsumed == localLimit) {
                            upstreamConsumed = 0;
                            upstream.get().request(localLimit);
                        }

                        MulticastSubscription<T>[] freshArray = subs.get();
                        if (subscribersChange || freshArray != array) {
                            array = freshArray;
                            continue outer;
                        }
                    }

                    if (r == 0) {
                        if (isDisposed()) {
                            q.clear();
                            return;
                        }

                        boolean d = done;

                        if (d && !delayError) {
                            Throwable ex = error;
                            if (ex != null) {
                                errorAll(ex);
                                return;
                            }
                        }

                        if (d && q.isEmpty()) {
                            Throwable ex = error;
                            if (ex != null) {
                                errorAll(ex);
                            } else {
                                completeAll();
                            }
                            return;
                        }
                    }
                }

                consumed = upstreamConsumed;
                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
                if (q == null) {
                    q = queue;
                }
                array = subs.get();
            }
        }

        @SuppressWarnings("unchecked")
        void errorAll(Throwable ex) {
            for (MulticastSubscription<T> ms : subscribers.getAndSet(TERMINATED)) {
                if (ms.get() != Long.MIN_VALUE) {
                    ms.downstream.onError(ex);
                }
            }
        }

        @SuppressWarnings("unchecked")
        void completeAll() {
            for (MulticastSubscription<T> ms : subscribers.getAndSet(TERMINATED)) {
                if (ms.get() != Long.MIN_VALUE) {
                    ms.downstream.onComplete();
                }
            }
        }
    }

    static final class MulticastSubscription<T>
    extends AtomicLong
    implements Subscription {

        private static final long serialVersionUID = 8664815189257569791L;

        final Subscriber<? super T> downstream;

        final MulticastProcessor<T> parent;

        long emitted;

        MulticastSubscription(Subscriber<? super T> actual, MulticastProcessor<T> parent) {
            this.downstream = actual;
            this.parent = parent;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.addCancel(this, n);
                parent.drain();
            }
        }

        @Override
        public void cancel() {
            if (getAndSet(Long.MIN_VALUE) != Long.MIN_VALUE) {
                parent.remove(this);
                parent.drain(); // unblock the others
            }
        }

        public boolean isCancelled() {
            return get() == Long.MIN_VALUE;
        }
    }
}
