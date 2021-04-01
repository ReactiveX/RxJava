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

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.queue.*;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class FlowableFlatMap<T, U> extends AbstractFlowableWithUpstream<T, U> {
    final Function<? super T, ? extends Publisher<? extends U>> mapper;
    final boolean delayErrors;
    final int maxConcurrency;
    final int bufferSize;

    public FlowableFlatMap(Flowable<T> source,
            Function<? super T, ? extends Publisher<? extends U>> mapper,
            boolean delayErrors, int maxConcurrency, int bufferSize) {
        super(source);
        this.mapper = mapper;
        this.delayErrors = delayErrors;
        this.maxConcurrency = maxConcurrency;
        this.bufferSize = bufferSize;
    }

    @Override
    protected void subscribeActual(Subscriber<? super U> s) {
        if (FlowableScalarXMap.tryScalarXMapSubscribe(source, s, mapper)) {
            return;
        }
        source.subscribe(subscribe(s, mapper, delayErrors, maxConcurrency, bufferSize));
    }

    public static <T, U> FlowableSubscriber<T> subscribe(Subscriber<? super U> s,
            Function<? super T, ? extends Publisher<? extends U>> mapper,
            boolean delayErrors, int maxConcurrency, int bufferSize) {
        return new MergeSubscriber<>(s, mapper, delayErrors, maxConcurrency, bufferSize);
    }

    static final class MergeSubscriber<T, U> extends AtomicInteger implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -2117620485640801370L;

        final Subscriber<? super U> downstream;
        final Function<? super T, ? extends Publisher<? extends U>> mapper;
        final boolean delayErrors;
        final int maxConcurrency;
        final int bufferSize;

        volatile SimplePlainQueue<U> queue;

        volatile boolean done;

        final AtomicThrowable errors = new AtomicThrowable();

        volatile boolean cancelled;

        final AtomicReference<InnerSubscriber<?, ?>[]> subscribers = new AtomicReference<>();

        static final InnerSubscriber<?, ?>[] EMPTY = new InnerSubscriber<?, ?>[0];

        static final InnerSubscriber<?, ?>[] CANCELLED = new InnerSubscriber<?, ?>[0];

        final AtomicLong requested = new AtomicLong();

        Subscription upstream;

        long uniqueId;
        long lastId;
        int lastIndex;

        int scalarEmitted;
        final int scalarLimit;

        MergeSubscriber(Subscriber<? super U> actual, Function<? super T, ? extends Publisher<? extends U>> mapper,
                boolean delayErrors, int maxConcurrency, int bufferSize) {
            this.downstream = actual;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.maxConcurrency = maxConcurrency;
            this.bufferSize = bufferSize;
            this.scalarLimit = Math.max(1, maxConcurrency >> 1);
            subscribers.lazySet(EMPTY);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
                if (!cancelled) {
                    if (maxConcurrency == Integer.MAX_VALUE) {
                        s.request(Long.MAX_VALUE);
                    } else {
                        s.request(maxConcurrency);
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onNext(T t) {
            // safeguard against misbehaving sources
            if (done) {
                return;
            }
            Publisher<? extends U> p;
            try {
                p = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Publisher");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                upstream.cancel();
                onError(e);
                return;
            }
            if (p instanceof Supplier) {
                U u;

                try {
                    u  = ((Supplier<U>)p).get();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    errors.tryAddThrowableOrReport(ex);
                    drain();
                    return;
                }

                if (u != null) {
                    tryEmitScalar(u);
                } else {
                    if (maxConcurrency != Integer.MAX_VALUE
                            && !cancelled && ++scalarEmitted == scalarLimit) {
                        scalarEmitted = 0;
                        upstream.request(scalarLimit);
                    }
                }
            } else {
                InnerSubscriber<T, U> inner = new InnerSubscriber<>(this, bufferSize, uniqueId++);
                if (addInner(inner)) {
                    p.subscribe(inner);
                }
            }
        }

        boolean addInner(InnerSubscriber<T, U> inner) {
            for (;;) {
                InnerSubscriber<?, ?>[] a = subscribers.get();
                if (a == CANCELLED) {
                    inner.dispose();
                    return false;
                }
                int n = a.length;
                InnerSubscriber<?, ?>[] b = new InnerSubscriber[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = inner;
                if (subscribers.compareAndSet(a, b)) {
                    return true;
                }
            }
        }

        void removeInner(InnerSubscriber<T, U> inner) {
            for (;;) {
                InnerSubscriber<?, ?>[] a = subscribers.get();
                int n = a.length;
                if (n == 0) {
                    return;
                }
                int j = -1;
                for (int i = 0; i < n; i++) {
                    if (a[i] == inner) {
                        j = i;
                        break;
                    }
                }
                if (j < 0) {
                    return;
                }
                InnerSubscriber<?, ?>[] b;
                if (n == 1) {
                    b = EMPTY;
                } else {
                    b = new InnerSubscriber<?, ?>[n - 1];
                    System.arraycopy(a, 0, b, 0, j);
                    System.arraycopy(a, j + 1, b, j, n - j - 1);
                }
                if (subscribers.compareAndSet(a, b)) {
                    return;
                }
            }
        }

        SimpleQueue<U> getMainQueue() {
            SimplePlainQueue<U> q = queue;
            if (q == null) {
                if (maxConcurrency == Integer.MAX_VALUE) {
                    q = new SpscLinkedArrayQueue<>(bufferSize);
                } else {
                    q = new SpscArrayQueue<>(maxConcurrency);
                }
                queue = q;
            }
            return q;
        }

        void tryEmitScalar(U value) {
            if (get() == 0 && compareAndSet(0, 1)) {
                long r = requested.get();
                SimpleQueue<U> q = queue;
                if (r != 0L && (q == null || q.isEmpty())) {
                    downstream.onNext(value);
                    if (r != Long.MAX_VALUE) {
                        requested.decrementAndGet();
                    }
                    if (maxConcurrency != Integer.MAX_VALUE
                            && !cancelled && ++scalarEmitted == scalarLimit) {
                        scalarEmitted = 0;
                        upstream.request(scalarLimit);
                    }
                } else {
                    if (q == null) {
                        q = getMainQueue();
                    }
                    if (!q.offer(value)) {
                        onError(new MissingBackpressureException("Scalar queue full?!"));
                    }
                }
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SimpleQueue<U> q = getMainQueue();
                if (!q.offer(value)) {
                    onError(new MissingBackpressureException("Scalar queue full?!"));
                    return;
                }
                if (getAndIncrement() != 0) {
                    return;
                }
            }
            drainLoop();
        }

        void tryEmit(U value, InnerSubscriber<T, U> inner) {
            if (get() == 0 && compareAndSet(0, 1)) {
                long r = requested.get();
                SimpleQueue<U> q = inner.queue;
                if (r != 0L && (q == null || q.isEmpty())) {
                    downstream.onNext(value);
                    if (r != Long.MAX_VALUE) {
                        requested.decrementAndGet();
                    }
                    inner.requestMore(1);
                } else {
                    if (q == null) {
                        q = new SpscArrayQueue<>(bufferSize);
                        inner.queue = q;
                    }
                    if (!q.offer(value)) {
                        onError(new MissingBackpressureException("Inner queue full?!"));
                    }
                }
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SimpleQueue<U> q = inner.queue;
                if (q == null) {
                    q = new SpscArrayQueue<>(bufferSize);
                    inner.queue = q;
                }
                if (!q.offer(value)) {
                    onError(new MissingBackpressureException("Inner queue full?!"));
                    return;
                }
                if (getAndIncrement() != 0) {
                    return;
                }
            }
            drainLoop();
        }

        @Override
        public void onError(Throwable t) {
            // safeguard against misbehaving sources
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            if (errors.tryAddThrowableOrReport(t)) {
                done = true;
                if (!delayErrors) {
                    for (InnerSubscriber<?, ?> a : subscribers.getAndSet(CANCELLED)) {
                        a.dispose();
                    }
                }
                drain();
            }
        }

        @Override
        public void onComplete() {
            // safeguard against misbehaving sources
            if (done) {
                return;
            }
            done = true;
            drain();
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
                upstream.cancel();
                disposeAll();
                if (getAndIncrement() == 0) {
                    SimpleQueue<U> q = queue;
                    if (q != null) {
                        q.clear();
                    }
                }
            }
        }

        void drain() {
            if (getAndIncrement() == 0) {
                drainLoop();
            }
        }

        void drainLoop() {
            final Subscriber<? super U> child = this.downstream;
            int missed = 1;
            for (;;) {
                if (checkTerminate()) {
                    return;
                }
                SimplePlainQueue<U> svq = queue;

                long r = requested.get();
                boolean unbounded = r == Long.MAX_VALUE;

                long replenishMain = 0;

                if (svq != null) {
                    long scalarEmission = 0;
                    U o = null;
                    while (r != 0L) {
                        o = svq.poll();

                        if (checkTerminate()) {
                            return;
                        }
                        if (o == null) {
                            break;
                        }

                        child.onNext(o);

                        replenishMain++;
                        scalarEmission++;
                        r--;
                    }
                    if (scalarEmission != 0L) {
                        if (unbounded) {
                            r = Long.MAX_VALUE;
                        } else {
                            r = requested.addAndGet(-scalarEmission);
                        }
                    }
                }

                boolean d = done;
                svq = queue;
                InnerSubscriber<?, ?>[] inner = subscribers.get();
                int n = inner.length;

                if (d && (svq == null || svq.isEmpty()) && n == 0) {
                    errors.tryTerminateConsumer(downstream);
                    return;
                }

                boolean innerCompleted = false;
                if (n != 0) {
                    long startId = lastId;
                    int index = lastIndex;

                    if (n <= index || inner[index].id != startId) {
                        if (n <= index) {
                            index = 0;
                        }
                        int j = index;
                        for (int i = 0; i < n; i++) {
                            if (inner[j].id == startId) {
                                break;
                            }
                            j++;
                            if (j == n) {
                                j = 0;
                            }
                        }
                        index = j;
                        lastIndex = j;
                        lastId = inner[j].id;
                    }

                    int j = index;
                    sourceLoop:
                    for (int i = 0; i < n; i++) {
                        if (checkTerminate()) {
                            return;
                        }

                        @SuppressWarnings("unchecked")
                        InnerSubscriber<T, U> is = (InnerSubscriber<T, U>)inner[j];

                        U o = null;
                        for (;;) {
                            SimpleQueue<U> q = is.queue;
                            if (q == null) {
                                break;
                            }
                            long produced = 0;
                            while (r != 0L) {
                                if (checkTerminate()) {
                                    return;
                                }

                                try {
                                    o = q.poll();
                                } catch (Throwable ex) {
                                    Exceptions.throwIfFatal(ex);
                                    is.dispose();
                                    errors.tryAddThrowableOrReport(ex);
                                    if (!delayErrors) {
                                        upstream.cancel();
                                    }
                                    if (checkTerminate()) {
                                        return;
                                    }
                                    removeInner(is);
                                    innerCompleted = true;
                                    i++;
                                    continue sourceLoop;
                                }
                                if (o == null) {
                                    break;
                                }

                                child.onNext(o);

                                r--;
                                produced++;
                            }
                            if (produced != 0L) {
                                if (!unbounded) {
                                    r = requested.addAndGet(-produced);
                                } else {
                                    r = Long.MAX_VALUE;
                                }
                                is.requestMore(produced);
                            }
                            if (r == 0 || o == null) {
                                break;
                            }
                        }
                        boolean innerDone = is.done;
                        SimpleQueue<U> innerQueue = is.queue;
                        if (innerDone && (innerQueue == null || innerQueue.isEmpty())) {
                            removeInner(is);
                            if (checkTerminate()) {
                                return;
                            }
                            replenishMain++;
                            innerCompleted = true;
                        }
                        if (r == 0L) {
                            break;
                        }

                        j++;
                        if (j == n) {
                            j = 0;
                        }
                    }
                    lastIndex = j;
                    lastId = inner[j].id;
                }

                if (replenishMain != 0L && !cancelled) {
                    upstream.request(replenishMain);
                }
                if (innerCompleted) {
                    continue;
                }
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        boolean checkTerminate() {
            if (cancelled) {
                clearScalarQueue();
                return true;
            }
            if (!delayErrors && errors.get() != null) {
                clearScalarQueue();
                errors.tryTerminateConsumer(downstream);
                return true;
            }
            return false;
        }

        void clearScalarQueue() {
            SimpleQueue<U> q = queue;
            if (q != null) {
                q.clear();
            }
        }

        void disposeAll() {
            InnerSubscriber<?, ?>[] a = subscribers.getAndSet(CANCELLED);
            if (a != CANCELLED) {
                for (InnerSubscriber<?, ?> inner : a) {
                    inner.dispose();
                }
                errors.tryTerminateAndReport();
            }
        }

        void innerError(InnerSubscriber<T, U> inner, Throwable t) {
            if (errors.tryAddThrowableOrReport(t)) {
                inner.done = true;
                if (!delayErrors) {
                    upstream.cancel();
                    for (InnerSubscriber<?, ?> a : subscribers.getAndSet(CANCELLED)) {
                        a.dispose();
                    }
                }
                drain();
            }
        }
    }

    static final class InnerSubscriber<T, U> extends AtomicReference<Subscription>
    implements FlowableSubscriber<U>, Disposable {

        private static final long serialVersionUID = -4606175640614850599L;
        final long id;
        final MergeSubscriber<T, U> parent;
        final int limit;
        final int bufferSize;

        volatile boolean done;
        volatile SimpleQueue<U> queue;
        long produced;
        int fusionMode;

        InnerSubscriber(MergeSubscriber<T, U> parent, int bufferSize, long id) {
            this.id = id;
            this.parent = parent;
            this.bufferSize = bufferSize;
            this.limit = bufferSize >> 2;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {

                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<U> qs = (QueueSubscription<U>) s;
                    int m = qs.requestFusion(QueueSubscription.ANY | QueueSubscription.BOUNDARY);
                    if (m == QueueSubscription.SYNC) {
                        fusionMode = m;
                        queue = qs;
                        done = true;
                        parent.drain();
                        return;
                    }
                    if (m == QueueSubscription.ASYNC) {
                        fusionMode = m;
                        queue = qs;
                    }

                }

                s.request(bufferSize);
            }
        }

        @Override
        public void onNext(U t) {
            if (fusionMode != QueueSubscription.ASYNC) {
                parent.tryEmit(t, this);
            } else {
                parent.drain();
            }
        }

        @Override
        public void onError(Throwable t) {
            lazySet(SubscriptionHelper.CANCELLED);
            parent.innerError(this, t);
        }

        @Override
        public void onComplete() {
            done = true;
            parent.drain();
        }

        void requestMore(long n) {
            if (fusionMode != QueueSubscription.SYNC) {
                long p = produced + n;
                if (p >= limit) {
                    produced = 0;
                    get().request(p);
                } else {
                    produced = p;
                }
            }
        }

        @Override
        public void dispose() {
            SubscriptionHelper.cancel(this);
        }

        @Override
        public boolean isDisposed() {
            return get() == SubscriptionHelper.CANCELLED;
        }
    }
}
