/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.*;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Flowable.Operator;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.queue.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;

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
        return new MergeSubscriber<T, U>(t, mapper, delayErrors, maxConcurrency, bufferSize);
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
        
        final AtomicReference<Queue<Throwable>> errors = new AtomicReference<Queue<Throwable>>();
        
        static final Queue<Throwable> ERRORS_CLOSED = new RejectingQueue<Throwable>();
        
        volatile boolean cancelled;
        
        final AtomicReference<InnerSubscriber<?, ?>[]> subscribers = new AtomicReference<InnerSubscriber<?, ?>[]>();
        
        static final InnerSubscriber<?, ?>[] EMPTY = new InnerSubscriber<?, ?>[0];
        
        static final InnerSubscriber<?, ?>[] CANCELLED = new InnerSubscriber<?, ?>[0];
        
        final AtomicLong requested = new AtomicLong();
        
        Subscription s;
        
        long uniqueId;
        long lastId;
        int lastIndex;
        
        int scalarEmitted;
        final int scalarLimit;
        
        public MergeSubscriber(Subscriber<? super U> actual, Function<? super T, ? extends Publisher<? extends U>> mapper,
                boolean delayErrors, int maxConcurrency, int bufferSize) {
            this.actual = actual;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.maxConcurrency = maxConcurrency;
            this.bufferSize = bufferSize;
            this.scalarLimit = Math.max(1, maxConcurrency >> 1);
            subscribers.lazySet(EMPTY);
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(this);
            if (!cancelled) {
                if (maxConcurrency == Integer.MAX_VALUE) {
                    s.request(Long.MAX_VALUE);
                } else {
                    s.request(maxConcurrency);
                }
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
            if (p instanceof ObservableScalarSource) {
                tryEmitScalar(((ObservableScalarSource<? extends U>)p).value());
            } else {
                InnerSubscriber<T, U> inner = new InnerSubscriber<T, U>(this, uniqueId++);
                addInner(inner);
                p.subscribe(inner);
            }
        }
        
        void addInner(InnerSubscriber<T, U> inner) {
            for (;;) {
                InnerSubscriber<?, ?>[] a = subscribers.get();
                if (a == CANCELLED) {
                    inner.dispose();
                    return;
                }
                int n = a.length;
                InnerSubscriber<?, ?>[] b = new InnerSubscriber[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = inner;
                if (subscribers.compareAndSet(a, b)) {
                    return;
                }
            }
        }
        
        void removeInner(InnerSubscriber<T, U> inner) {
            for (;;) {
                InnerSubscriber<?, ?>[] a = subscribers.get();
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
                if (subscribers.compareAndSet(a, b)) {
                    return;
                }
            }
        }
        
        Queue<U> getMainQueue() {
            Queue<U> q = queue;
            if (q == null) {
                if (maxConcurrency == Integer.MAX_VALUE) {
                    q = new SpscLinkedArrayQueue<U>(bufferSize);
                } else {
                    if (Pow2.isPowerOfTwo(maxConcurrency)) {
                        q = new SpscArrayQueue<U>(maxConcurrency);
                    } else {
                        q = new SpscExactArrayQueue<U>(maxConcurrency);
                    }
                }
                queue = q;
            }
            return q;
        }
        
        void tryEmitScalar(U value) {
            if (get() == 0 && compareAndSet(0, 1)) {
                long r = requested.get();
                if (r != 0L) {
                    actual.onNext(value);
                    if (r != Long.MAX_VALUE) {
                        requested.decrementAndGet();
                    }
                    if (maxConcurrency != Integer.MAX_VALUE && !cancelled
                            && ++scalarEmitted == scalarLimit) {
                        scalarEmitted = 0;
                        s.request(scalarLimit);
                    }
                } else {
                    Queue<U> q = getMainQueue();
                    if (!q.offer(value)) {
                        onError(new IllegalStateException("Scalar queue full?!"));
                        return;
                    }
                }
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                Queue<U> q = getMainQueue();
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
        
        Queue<U> getInnerQueue(InnerSubscriber<T, U> inner) {
            Queue<U> q = inner.queue;
            if (q == null) {
                q = new SpscArrayQueue<U>(bufferSize);
                inner.queue = q;
            }
            return q;
        }
        
        void tryEmit(U value, InnerSubscriber<T, U> inner) {
            if (get() == 0 && compareAndSet(0, 1)) {
                long r = requested.get();
                if (r != 0L) {
                    actual.onNext(value);
                    if (r != Long.MAX_VALUE) {
                        requested.decrementAndGet();
                    }
                    inner.requestMore(1);
                } else {
                    Queue<U> q = getInnerQueue(inner);
                    if (!q.offer(value)) {
                        onError(new MissingBackpressureException("Inner queue full?!"));
                        return;
                    }
                }
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                Queue<U> q = inner.queue;
                if (q == null) {
                    q = new SpscArrayQueue<U>(bufferSize);
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
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            BackpressureHelper.add(requested, n);
            drain();
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                if (getAndIncrement() == 0) {
                    s.cancel();
                    unsubscribe();
                }
            }
        }
        
        void drain() {
            if (getAndIncrement() == 0) {
                drainLoop();
            }
        }
        
        void drainLoop() {
            final Subscriber<? super U> child = this.actual;
            int missed = 1;
            for (;;) {
                if (checkTerminate()) {
                    return;
                }
                Queue<U> svq = queue;
                
                long r = requested.get();
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
                                r = requested.addAndGet(-scalarEmission);
                            }
                        }
                        if (r == 0L || o == null) {
                            break;
                        }
                    }
                }

                boolean d = done;
                svq = queue;
                InnerSubscriber<?, ?>[] inner = subscribers.get();
                int n = inner.length;
                
                if (d && (svq == null || svq.isEmpty()) && n == 0) {
                    Queue<Throwable> e = errors.get();
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
                
                if (replenishMain != 0L && !cancelled) {
                    s.request(replenishMain);
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
                s.cancel();
                unsubscribe();
                return true;
            }
            Queue<Throwable> e = errors.get();
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
            CompositeException composite = null;
            Throwable ex = null;
            
            Throwable t;
            int count = 0;
            while ((t = q.poll()) != null) {
                if (count == 0) {
                    ex = t;
                } else {
                    if (composite == null) {
                        composite = new CompositeException(ex);
                    }
                    composite.suppress(t);
                }
                
                count++;
            }
            if (composite != null) {
                actual.onError(composite);
            } else {
                actual.onError(ex);
            }
        }
        
        void unsubscribe() {
            InnerSubscriber<?, ?>[] a = subscribers.get();
            if (a != CANCELLED) {
                a = subscribers.getAndSet(CANCELLED);
                if (a != CANCELLED) {
                    errors.getAndSet(ERRORS_CLOSED);
                    for (InnerSubscriber<?, ?> inner : a) {
                        inner.dispose();
                    }
                }
            }
        }
        
        Queue<Throwable> getErrorQueue() {
            for (;;) {
                Queue<Throwable> q = errors.get();
                if (q != null) {
                    return q;
                }
                q = new MpscLinkedQueue<Throwable>();
                if (errors.compareAndSet(null, q)) {
                    return q;
                }
            }
        }
    }
    
    static final class InnerSubscriber<T, U> extends AtomicReference<Subscription> 
    implements Subscriber<U>, Disposable {
        /** */
        private static final long serialVersionUID = -4606175640614850599L;
        final long id;
        final MergeSubscriber<T, U> parent;
        final int limit;
        final int bufferSize;
        
        volatile boolean done;
        volatile Queue<U> queue;
        int outstanding;
        
        static final Subscription CANCELLED = new Subscription() {
            @Override
            public void request(long n) {
                
            }
            
            @Override
            public void cancel() {
                
            }
        };
        
        public InnerSubscriber(MergeSubscriber<T, U> parent, long id) {
            this.id = id;
            this.parent = parent;
            this.bufferSize = parent.bufferSize;
            this.limit = bufferSize >> 2;
        }
        @Override
        public void onSubscribe(Subscription s) {
            if (!compareAndSet(null, s)) {
                s.cancel();
                if (get() != CANCELLED) {
                    SubscriptionHelper.reportSubscriptionSet();
                }
                return;
            }
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
                get().request(k);
            }
        }
        
        @Override
        public void dispose() {
            Subscription s = get();
            if (s != CANCELLED) {
                s = getAndSet(CANCELLED);
                if (s != CANCELLED && s != null) {
                    s.cancel();
                }
            }
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
            return Collections.<T>emptyList().iterator();
        }

        @Override
        public int size() {
            return 0;
        }
        
    }

}
