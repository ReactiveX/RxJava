/*
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

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.Objects;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableMap.MapSubscriber;
import io.reactivex.rxjava3.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

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
        Publisher<? extends T>[] sources = array;
        int count;
        if (sources == null) {
            count = 0;
            sources = new Publisher[8];

            try {
                for (Publisher<? extends T> p : iterable) {
                    if (count == sources.length) {
                        Publisher<? extends T>[] b = new Publisher[count + (count >> 2)];
                        System.arraycopy(sources, 0, b, 0, count);
                        sources = b;
                    }
                    sources[count++] = Objects.requireNonNull(p, "The Iterator returned a null Publisher");
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptySubscription.error(ex, s);
                return;
            }

        } else {
            count = sources.length;
        }

        if (count == 0) {
            EmptySubscription.complete(s);
            return;
        }
        if (count == 1) {
            sources[0].subscribe(new MapSubscriber<>(s, new SingletonArrayFunc()));
            return;
        }

        CombineLatestCoordinator<T, R> coordinator =
                new CombineLatestCoordinator<>(s, combiner, count, bufferSize, delayErrors);

        s.onSubscribe(coordinator);

        coordinator.subscribe(sources, count);
    }

    static final class CombineLatestCoordinator<T, R>
    extends BasicIntQueueSubscription<R> {

        private static final long serialVersionUID = -5082275438355852221L;

        final Subscriber<? super R> downstream;

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

        final AtomicThrowable error;

        CombineLatestCoordinator(Subscriber<? super R> actual,
                Function<? super Object[], ? extends R> combiner, int n,
                int bufferSize, boolean delayErrors) {
            this.downstream = actual;
            this.combiner = combiner;
            @SuppressWarnings("unchecked")
            CombineLatestInnerSubscriber<T>[] a = new CombineLatestInnerSubscriber[n];
            for (int i = 0; i < n; i++) {
                a[i] = new CombineLatestInnerSubscriber<>(this, i, bufferSize);
            }
            this.subscribers = a;
            this.latest = new Object[n];
            this.queue = new SpscLinkedArrayQueue<>(bufferSize);
            this.requested = new AtomicLong();
            this.error = new  AtomicThrowable();
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
            drain();
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
            final Subscriber<? super R> a = downstream;
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
            final Subscriber<? super R> a = downstream;
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
                        w = Objects.requireNonNull(combiner.apply(va), "The combiner returned a null value");
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
                error.tryTerminateAndReport();
                return true;
            }

            if (d) {
                if (delayErrors) {
                    if (empty) {
                        cancelAll();
                        error.tryTerminateConsumer(a);
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
        public R poll() throws Throwable {
            Object e = queue.poll();
            if (e == null) {
                return null;
            }
            T[] a = (T[])queue.poll();
            R r = Objects.requireNonNull(combiner.apply(a), "The combiner returned a null value");
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
        public R apply(T t) throws Throwable {
            return combiner.apply(new Object[] { t });
        }
    }
}
