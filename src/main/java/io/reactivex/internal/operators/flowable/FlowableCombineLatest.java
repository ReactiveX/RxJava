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

package io.reactivex.internal.operators.flowable;

import java.util.Iterator;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.annotations.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.operators.flowable.FlowableMap.MapSubscriber;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Combines the latest values from multiple sources through a function.
 *
 * @param <T> the value type of the sources
 * @param <R> the result type
 */
public final class FlowableCombineLatest<T, R>
extends Flowable<R> {

    @Nullable
    final Publisher<? extends T>[] array;

    @Nullable
    final Iterable<? extends Publisher<? extends T>> iterable;

    final Function<? super Object[], ? extends R> combiner;

    final int bufferSize;

    final boolean delayErrors;

    public FlowableCombineLatest(@NonNull Publisher<? extends T>[] array,
                    @NonNull Function<? super Object[], ? extends R> combiner,
                    int bufferSize, boolean delayErrors) {
        this.array = array;
        this.iterable = null;
        this.combiner = combiner;
        this.bufferSize = bufferSize;
        this.delayErrors = delayErrors;
    }

    public FlowableCombineLatest(@NonNull Iterable<? extends Publisher<? extends T>> iterable,
                    @NonNull Function<? super Object[], ? extends R> combiner,
                    int bufferSize, boolean delayErrors) {
        this.array = null;
        this.iterable = iterable;
        this.combiner = combiner;
        this.bufferSize = bufferSize;
        this.delayErrors = delayErrors;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void subscribeActual(Subscriber<? super R> s) {
        Publisher<? extends T>[] a = array;
        int n;
        if (a == null) {
            n = 0;
            a = new Publisher[8];

            Iterator<? extends Publisher<? extends T>> it;

            try {
                it = ObjectHelper.requireNonNull(iterable.iterator(), "The iterator returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                EmptySubscription.error(e, s);
                return;
            }

            for (;;) {

                boolean b;

                try {
                    b = it.hasNext();
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    EmptySubscription.error(e, s);
                    return;
                }

                if (!b) {
                    break;
                }

                Publisher<? extends T> p;

                try {
                    p = ObjectHelper.requireNonNull(it.next(), "The publisher returned by the iterator is null");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    EmptySubscription.error(e, s);
                    return;
                }

                if (n == a.length) {
                    Publisher<? extends T>[] c = new Publisher[n + (n >> 2)];
                    System.arraycopy(a, 0, c, 0, n);
                    a = c;
                }
                a[n++] = p;
            }

        } else {
            n = a.length;
        }

        if (n == 0) {
            EmptySubscription.complete(s);
            return;
        }
        if (n == 1) {
            ((Publisher<T>)a[0]).subscribe(new MapSubscriber<T, R>(s, new SingletonArrayFunc()));
            return;
        }


        CombineLatestCoordinator<T, R> coordinator =
                new CombineLatestCoordinator<T, R>(s, combiner, n, bufferSize, delayErrors);

        s.onSubscribe(coordinator);

        coordinator.subscribe(a, n);
    }

    static final class CombineLatestCoordinator<T, R>
    extends BasicIntQueueSubscription<R> {


        private static final long serialVersionUID = -5082275438355852221L;

        final Subscriber<? super R> actual;

        final Function<? super Object[], ? extends R> combiner;

        final CombineLatestInnerSubscriber<T>[] subscribers;

        final SpscLinkedArrayQueue<Object> queue;

        final Object[] latest;

        final boolean delayErrors;

        boolean outputFused;

        int nonEmptySources;

        int completedSources;

        volatile boolean cancelled;

        final AtomicLong requested;

        volatile boolean done;

        final AtomicReference<Throwable> error;

        CombineLatestCoordinator(Subscriber<? super R> actual,
                Function<? super Object[], ? extends R> combiner, int n,
                int bufferSize, boolean delayErrors) {
            this.actual = actual;
            this.combiner = combiner;
            @SuppressWarnings("unchecked")
            CombineLatestInnerSubscriber<T>[] a = new CombineLatestInnerSubscriber[n];
            for (int i = 0; i < n; i++) {
                a[i] = new CombineLatestInnerSubscriber<T>(this, i, bufferSize);
            }
            this.subscribers = a;
            this.latest = new Object[n];
            this.queue = new SpscLinkedArrayQueue<Object>(bufferSize);
            this.requested = new AtomicLong();
            this.error = new  AtomicReference<Throwable>();
            this.delayErrors = delayErrors;
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
            cancelled = true;
            cancelAll();
        }

        void subscribe(Publisher<? extends T>[] sources, int n) {
            CombineLatestInnerSubscriber<T>[] a = subscribers;

            for (int i = 0; i < n; i++) {
                if (done || cancelled) {
                    return;
                }
                sources[i].subscribe(a[i]);
            }
        }

        void innerValue(int index, T value) {

            boolean replenishInsteadOfDrain;

            synchronized (this) {
                Object[] os = latest;

                int localNonEmptySources = nonEmptySources;

                if (os[index] == null) {
                    localNonEmptySources++;
                    nonEmptySources = localNonEmptySources;
                }

                os[index] = value;

                if (os.length == localNonEmptySources) {

                    queue.offer(subscribers[index], os.clone());

                    replenishInsteadOfDrain = false;
                } else {
                    replenishInsteadOfDrain = true;
                }
            }

            if (replenishInsteadOfDrain) {
                subscribers[index].requestOne();
            } else {
                drain();
            }
        }

        void innerComplete(int index) {
            synchronized (this) {
                Object[] os = latest;

                if (os[index] != null) {
                    int localCompletedSources = completedSources + 1;

                    if (localCompletedSources == os.length) {
                        done = true;
                    } else {
                        completedSources = localCompletedSources;
                        return;
                    }
                } else {
                    done = true;
                }
            }
            drain();
        }

        void innerError(int index, Throwable e) {

            if (ExceptionHelper.addThrowable(error, e)) {
                if (!delayErrors) {
                    cancelAll();
                    done = true;
                    drain();
                } else {
                    innerComplete(index);
                }
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        void drainOutput() {
            final Subscriber<? super R> a = actual;
            final SpscLinkedArrayQueue<Object> q = queue;

            int missed = 1;

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

                boolean d = done;

                boolean empty = q.isEmpty();

                if (!empty) {
                    a.onNext(null);
                }

                if (d && empty) {
                    a.onComplete();
                    return;
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @SuppressWarnings("unchecked")
        void drainAsync() {
            final Subscriber<? super R> a = actual;
            final SpscLinkedArrayQueue<Object> q = queue;

            int missed = 1;

            for (;;) {

                long r = requested.get();
                long e = 0L;

                while (e != r) {
                    boolean d = done;

                    Object v = q.poll();

                    boolean empty = v == null;

                    if (checkTerminated(d, empty, a, q)) {
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    T[] va = (T[])q.poll();

                    R w;

                    try {
                        w = ObjectHelper.requireNonNull(combiner.apply(va), "The combiner returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);

                        cancelAll();
                        ExceptionHelper.addThrowable(error, ex);
                        ex = ExceptionHelper.terminate(error);

                        a.onError(ex);
                        return;
                    }

                    a.onNext(w);

                    ((CombineLatestInnerSubscriber<T>)v).requestOne();

                    e++;
                }

                if (e == r) {
                    if (checkTerminated(done, q.isEmpty(), a, q)) {
                        return;
                    }
                }

                if (e != 0L && r != Long.MAX_VALUE) {
                    requested.addAndGet(-e);
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
                drainOutput();
            } else {
                drainAsync();
            }
        }

        boolean checkTerminated(boolean d, boolean empty, Subscriber<?> a, SpscLinkedArrayQueue<?> q) {
            if (cancelled) {
                cancelAll();
                q.clear();
                return true;
            }

            if (d) {
                if (delayErrors) {
                    if (empty) {
                        cancelAll();
                        Throwable e = ExceptionHelper.terminate(error);

                        if (e != null && e != ExceptionHelper.TERMINATED) {
                            a.onError(e);
                        } else {
                            a.onComplete();
                        }
                        return true;
                    }
                } else {
                    Throwable e = ExceptionHelper.terminate(error);

                    if (e != null && e != ExceptionHelper.TERMINATED) {
                        cancelAll();
                        q.clear();
                        a.onError(e);
                        return true;
                    } else
                    if (empty) {
                        cancelAll();

                        a.onComplete();
                        return true;
                    }
                }
            }
            return false;
        }

        void cancelAll() {
            for (CombineLatestInnerSubscriber<T> inner : subscribers) {
                inner.cancel();
            }
        }

        @Override
        public int requestFusion(int requestedMode) {
            if ((requestedMode & BOUNDARY) != 0) {
                return NONE;
            }
            int m = requestedMode & ASYNC;
            outputFused = m != 0;
            return m;
        }

        @Nullable
        @SuppressWarnings("unchecked")
        @Override
        public R poll() throws Exception {
            Object e = queue.poll();
            if (e == null) {
                return null;
            }
            T[] a = (T[])queue.poll();
            R r = ObjectHelper.requireNonNull(combiner.apply(a), "The combiner returned a null value");
            ((CombineLatestInnerSubscriber<T>)e).requestOne();
            return r;
        }

        @Override
        public void clear() {
            queue.clear();
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    static final class CombineLatestInnerSubscriber<T>
    extends AtomicReference<Subscription>
            implements FlowableSubscriber<T> {


        private static final long serialVersionUID = -8730235182291002949L;

        final CombineLatestCoordinator<T, ?> parent;

        final int index;

        final int prefetch;

        final int limit;

        int produced;

        CombineLatestInnerSubscriber(CombineLatestCoordinator<T, ?> parent, int index, int prefetch) {
            this.parent = parent;
            this.index = index;
            this.prefetch = prefetch;
            this.limit = prefetch - (prefetch >> 2);
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.setOnce(this, s, prefetch);
        }

        @Override
        public void onNext(T t) {
            parent.innerValue(index, t);
        }

        @Override
        public void onError(Throwable t) {
            parent.innerError(index, t);
        }

        @Override
        public void onComplete() {
            parent.innerComplete(index);
        }

        public void cancel() {
            SubscriptionHelper.cancel(this);
        }

        public void requestOne() {

            int p = produced + 1;
            if (p == limit) {
                produced = 0;
                get().request(p);
            } else {
                produced = p;
            }

        }
    }

    final class SingletonArrayFunc implements Function<T, R> {
        @Override
        public R apply(T t) throws Exception {
            return combiner.apply(new Object[] { t });
        }
    }
}
