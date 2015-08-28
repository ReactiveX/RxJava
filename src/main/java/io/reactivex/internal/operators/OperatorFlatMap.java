/*
 * Copyright 2011-2015 David Karnok
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.internal.operators;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Function;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.queue.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * 
 */
public final class OperatorFlatMap<T, U> implements Operator<U, T> {
    final Function<? super T, ? extends Publisher<? extends U>> mapper;
    final boolean delayErrors;
    final int maxConcurrency;
    final int bufferSize;
    
    public OperatorFlatMap( 
            Function<? super T, ? extends Publisher<? extends U>> mapper,
            boolean delayErrors, int maxConcurrency, int bufferSize) {
        this.mapper = mapper;
        this.delayErrors = delayErrors;
        this.maxConcurrency = maxConcurrency;
        this.bufferSize = bufferSize;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super U> t) {
        return new MergeSubscriber<>(t, mapper, delayErrors, maxConcurrency, bufferSize);
    }
    
    static final class MergeSubscriber<T, U> extends AtomicInteger implements Subscription, Subscriber<T> {
        /** */
        private static final long serialVersionUID = -2117620485640801370L;
        
        final Subscriber<? super U> actual;
        final Function<? super T, ? extends Publisher<? extends U>> mapper;
        final boolean delayErrors;
        final int maxConcurrency;
        final int bufferSize;
        
        volatile Queue<U> queue;
        
        volatile boolean done;
        
        volatile Queue<Throwable> errors;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<MergeSubscriber, Queue> ERRORS =
                AtomicReferenceFieldUpdater.newUpdater(MergeSubscriber.class, Queue.class, "errors");
        
        static final Queue<Throwable> ERRORS_CLOSED = new RejectingQueue<>();
        
        volatile boolean cancelled;
        
        volatile InnerSubscriber<?, ?>[] subscribers;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<MergeSubscriber, InnerSubscriber[]> SUBSCRIBERS =
                AtomicReferenceFieldUpdater.newUpdater(MergeSubscriber.class, InnerSubscriber[].class, "subscribers");
        
        static final InnerSubscriber<?, ?>[] EMPTY = new InnerSubscriber<?, ?>[0];
        
        static final InnerSubscriber<?, ?>[] CANCELLED = new InnerSubscriber<?, ?>[0];
        
        volatile long requested;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<MergeSubscriber> REQUESTED =
                AtomicLongFieldUpdater.newUpdater(MergeSubscriber.class, "requested");
        
        Subscription s;
        
        long uniqueId;
        long lastId;
        int lastIndex;
        
        public MergeSubscriber(Subscriber<? super U> actual, Function<? super T, ? extends Publisher<? extends U>> mapper,
                boolean delayErrors, int maxConcurrency, int bufferSize) {
            this.actual = actual;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.maxConcurrency = maxConcurrency;
            this.bufferSize = bufferSize;
            SUBSCRIBERS.lazySet(this, EMPTY);
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (this.s != null) {
                s.cancel();
                RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
                return;
            }
            this.s = s;
            actual.onSubscribe(this);
            if (maxConcurrency == Integer.MAX_VALUE) {
                s.request(Long.MAX_VALUE);
            } else {
                s.request(maxConcurrency);
            }
        }
        
        @Override
        public void onNext(T t) {
            // safeguard against misbehaving sources
            if (done) {
                return;
            }
            Publisher<? extends U> p;
            try {
                p = mapper.apply(t);
            } catch (Throwable e) {
                onError(e);
                return;
            }
            if (p instanceof PublisherScalarSource) {
                tryEmitScalar(((PublisherScalarSource<? extends U>)p).value());
            } else {
                InnerSubscriber<T, U> inner = new InnerSubscriber<>(this, uniqueId++);
                p.subscribe(inner);
            }
        }
        
        void addInner(InnerSubscriber<T, U> inner) {
            for (;;) {
                InnerSubscriber<?, ?>[] a = subscribers;
                if (a == CANCELLED) {
                    inner.dispose();
                    return;
                }
                int n = a.length;
                InnerSubscriber<?, ?>[] b = new InnerSubscriber[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = inner;
                if (SUBSCRIBERS.compareAndSet(this, a, b)) {
                    return;
                }
            }
        }
        
        void removeInner(InnerSubscriber<T, U> inner) {
            for (;;) {
                InnerSubscriber<?, ?>[] a = subscribers;
                if (a == CANCELLED || a == EMPTY) {
                    return;
                }
                int n = a.length;
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
                if (SUBSCRIBERS.compareAndSet(this, a, b)) {
                    return;
                }
            }
        }
        
        void tryEmitScalar(U value) {
            if (get() == 0 && compareAndSet(0, 1)) {
                long r = requested;
                if (r != 0L) {
                    actual.onNext(value);
                    if (r != Long.MAX_VALUE) {
                        REQUESTED.decrementAndGet(this);
                    }
                    if (maxConcurrency != Integer.MAX_VALUE) {
                        s.request(1);
                    }
                }
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                Queue<U> q = queue;
                if (q == null) {
                    if (maxConcurrency == Integer.MAX_VALUE) {
                        q = new SpscLinkedArrayQueue<>(bufferSize);
                    } else {
                        if (Pow2.isPowerOfTwo(maxConcurrency)) {
                            q = new SpscArrayQueue<>(maxConcurrency);
                        } else {
                            q = new SpscExactArrayQueue<>(maxConcurrency);
                        }
                    }
                    queue = q;
                }
                if (!q.offer(value)) {
                    onError(new IllegalStateException("Scalar queue full?!"));
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
                long r = requested;
                if (r != 0L) {
                    actual.onNext(value);
                    if (r != Long.MAX_VALUE) {
                        REQUESTED.decrementAndGet(this);
                    }
                    inner.requestMore(1);
                }
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                Queue<U> q = inner.queue;
                if (q == null) {
                    q = new SpscArrayQueue<>(bufferSize);
                    inner.queue = q;
                }
                if (!q.offer(value)) {
                    onError(new IllegalStateException("Inner queue full?!"));
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
                return;
            }
            getErrorQueue().offer(t);
            done = true;
            drain();
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
            if (n <= 0) {
                RxJavaPlugins.onError(new IllegalArgumentException("n > 0 required"));
                return;
            }
            BackpressureHelper.add(REQUESTED, this, n);
            drain();
        }
        
        @Override
        public void cancel() {
            cancelled = true;
            s.cancel();
            unsubscribe();
        }
        
        void drain() {
            if (getAndIncrement() == 0) {
                drainLoop();
            }
        }
        
        void drainLoop() {
            final Subscriber<? super U> child = this.actual;
            for (;;) {
                if (checkTerminate()) {
                    return;
                }
                Queue<U> svq = queue;
                
                long r = requested;
                boolean unbounded = r == Long.MAX_VALUE;
                
                long replenishMain = 0;

                if (svq != null) {
                    for (;;) {
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
                                r = REQUESTED.addAndGet(this, -scalarEmission);
                            }
                        }
                        if (r == 0L || o == null) {
                            break;
                        }
                    }
                }

                boolean d = done;
                svq = queue;
                InnerSubscriber<?, ?>[] inner = subscribers;
                int n = inner.length;
                
                if (d && (svq == null || svq.isEmpty()) && n == 0) {
                    Queue<Throwable> e = errors;
                    if (e == null || e.isEmpty()) {
                        child.onComplete();
                    } else {
                        reportError(e);
                    }
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
                    for (int i = 0; i < n; i++) {
                        if (checkTerminate()) {
                            return;
                        }
                        @SuppressWarnings("unchecked")
                        InnerSubscriber<T, U> is = (InnerSubscriber<T, U>)inner[j];
                        
                        U o = null;
                        for (;;) {
                            long produced = 0;
                            while (r != 0L) {
                                if (checkTerminate()) {
                                    return;
                                }
                                Queue<U> q = is.queue;
                                if (q == null) {
                                    break;
                                }
                                o = q.poll();
                                if (o == null) {
                                    break;
                                }

                                child.onNext(o);
                                
                                r--;
                                produced++;
                            }
                            if (produced != 0L) {
                                if (!unbounded) {
                                    r = REQUESTED.addAndGet(this, -produced);
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
                        Queue<U> innerQueue = is.queue;
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
                
                if (replenishMain != 0L) {
                    s.request(replenishMain);
                }
                if (innerCompleted) {
                    continue;
                }
                if (decrementAndGet() == 0) {
                    return;
                }
            }
        }
        
        boolean checkTerminate() {
            if (cancelled) {
                return true;
            }
            Queue<Throwable> e = errors;
            if (!delayErrors && (e != null && !e.isEmpty())) {
                try {
                    reportError(e);
                } finally {
                    unsubscribe();
                }
                return true;
            }
            return false;
        }
        
        void reportError(Queue<Throwable> q) {
            Throwable ex = null;
            
            Throwable t;
            int count = 0;
            while ((t = q.poll()) != null) {
                if (count == 0) {
                    ex = t;
                } else
                if (count == 1) {
                    Throwable e = ex;
                    ex = new RuntimeException("Multiple exceptions");
                    ex.addSuppressed(e);
                    ex.addSuppressed(t);
                } else {
                    ex.addSuppressed(t);
                }
                
                count++;
            }
            if (count > 1) {
                actual.onError(ex);
            } else {
                actual.onError(ex.getSuppressed()[0]);
            }
        }
        
        void unsubscribe() {
            InnerSubscriber<?, ?>[] a = subscribers;
            if (a != CANCELLED) {
                a = SUBSCRIBERS.getAndSet(this, CANCELLED);
                if (a != CANCELLED) {
                    ERRORS.getAndSet(this, ERRORS_CLOSED);
                    for (InnerSubscriber<?, ?> inner : a) {
                        inner.dispose();
                    }
                }
            }
        }
        
        Queue<Throwable> getErrorQueue() {
            for (;;) {
                Queue<Throwable> q = errors;
                if (q != null) {
                    return q;
                }
                q = new MpscLinkedQueue<>();
                if (ERRORS.compareAndSet(this, null, q)) {
                    return q;
                }
            }
        }
    }
    
    static final class InnerSubscriber<T, U> implements Subscriber<U>, Disposable {
        final long id;
        final MergeSubscriber<T, U> parent;
        Subscription s;
        volatile boolean done;
        volatile Queue<U> queue;
        int outstanding;
        final int limit;
        final int bufferSize;
        
        public InnerSubscriber(MergeSubscriber<T, U> parent, long id) {
            this.id = id;
            this.parent = parent;
            this.bufferSize = parent.bufferSize;
            this.limit = bufferSize >> 2;
        }
        @Override
        public void onSubscribe(Subscription s) {
            if (this.s != null) {
                s.cancel();
                RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
                return;
            }
            this.s = s;
            parent.addInner(this);
            
            outstanding = bufferSize;
            s.request(outstanding);
        }
        @Override
        public void onNext(U t) {
            parent.tryEmit(t, this);
        }
        @Override
        public void onError(Throwable t) {
            parent.getErrorQueue().offer(t);
            done = true;
            parent.drain();
        }
        @Override
        public void onComplete() {
            done = true;
            parent.drain();
        }
        
        void requestMore(long n) {
            int r = outstanding - (int)n;
            if (r > limit) {
                outstanding = r;
                return;
            }
            outstanding = bufferSize;
            int k = bufferSize - r;
            if (k > 0) {
                s.request(k);
            }
        }
        
        @Override
        public void dispose() {
            s.cancel();
        }
    }
    
    static final class RejectingQueue<T> extends AbstractQueue<T> {
        @Override
        public boolean offer(T e) {
            return false;
        }

        @Override
        public T poll() {
            return null;
        }

        @Override
        public T peek() {
            return null;
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
        
    }
}
