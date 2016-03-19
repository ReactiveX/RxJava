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

package io.reactivex.internal.operators.observable;

import java.util.*;
import java.util.concurrent.atomic.*;

import io.reactivex.Observer;
import io.reactivex.Observable;
import io.reactivex.Observable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.queue.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.Pow2;

public final class NbpOperatorFlatMap<T, U> implements NbpOperator<U, T> {
    final Function<? super T, ? extends Observable<? extends U>> mapper;
    final boolean delayErrors;
    final int maxConcurrency;
    final int bufferSize;
    
    public NbpOperatorFlatMap( 
            Function<? super T, ? extends Observable<? extends U>> mapper,
            boolean delayErrors, int maxConcurrency, int bufferSize) {
        this.mapper = mapper;
        this.delayErrors = delayErrors;
        this.maxConcurrency = maxConcurrency;
        this.bufferSize = bufferSize;
    }
    
    @Override
    public Observer<? super T> apply(Observer<? super U> t) {
        return new MergeSubscriber<T, U>(t, mapper, delayErrors, maxConcurrency, bufferSize);
    }
    
    static final class MergeSubscriber<T, U> extends AtomicInteger implements Disposable, Observer<T> {
        /** */
        private static final long serialVersionUID = -2117620485640801370L;
        
        final Observer<? super U> actual;
        final Function<? super T, ? extends Observable<? extends U>> mapper;
        final boolean delayErrors;
        final int maxConcurrency;
        final int bufferSize;
        
        volatile Queue<U> queue;
        
        volatile boolean done;
        
        final AtomicReference<Queue<Throwable>> errors = new AtomicReference<Queue<Throwable>>();
        
        static final Queue<Throwable> ERRORS_CLOSED = new RejectingQueue<Throwable>();
        
        volatile boolean cancelled;
        
        final AtomicReference<InnerSubscriber<?, ?>[]> subscribers;
        
        static final InnerSubscriber<?, ?>[] EMPTY = new InnerSubscriber<?, ?>[0];
        
        static final InnerSubscriber<?, ?>[] CANCELLED = new InnerSubscriber<?, ?>[0];
        
        Disposable s;
        
        long uniqueId;
        long lastId;
        int lastIndex;
        
        Queue<Observable<? extends U>> sources;
        
        int wip;
        
        public MergeSubscriber(Observer<? super U> actual, Function<? super T, ? extends Observable<? extends U>> mapper,
                boolean delayErrors, int maxConcurrency, int bufferSize) {
            this.actual = actual;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.maxConcurrency = maxConcurrency;
            this.bufferSize = bufferSize;
            if (maxConcurrency != Integer.MAX_VALUE) {
                sources = new ArrayDeque<Observable<? extends U>>(maxConcurrency);
            }
            this.subscribers = new AtomicReference<InnerSubscriber<?, ?>[]>(EMPTY);
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(this);
        }
        
        @Override
        public void onNext(T t) {
            // safeguard against misbehaving sources
            if (done) {
                return;
            }
            Observable<? extends U> p;
            try {
                p = mapper.apply(t);
            } catch (Throwable e) {
                onError(e);
                return;
            }
            if (p instanceof NbpObservableScalarSource) {
                tryEmitScalar(((NbpObservableScalarSource<? extends U>)p).value());
            } else {
                if (maxConcurrency == Integer.MAX_VALUE) {
                    subscribeInner(p);
                } else {
                    synchronized (this) {
                        if (wip == maxConcurrency) {
                            sources.offer(p);
                            return;
                        }
                        wip++;
                    }
                    subscribeInner(p);
                }
            }
        }
        
        void subscribeInner(Observable<? extends U> p) {
            InnerSubscriber<T, U> inner = new InnerSubscriber<T, U>(this, uniqueId++);
            addInner(inner);
            p.subscribe(inner);
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
                actual.onNext(value);
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
                actual.onNext(value);
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                Queue<U> q = inner.queue;
                if (q == null) {
                    q = new SpscLinkedArrayQueue<U>(bufferSize);
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
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                if (getAndIncrement() == 0) {
                    s.dispose();
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
            final Observer<? super U> child = this.actual;
            int missed = 1;
            for (;;) {
                if (checkTerminate()) {
                    return;
                }
                Queue<U> svq = queue;
                
                if (svq != null) {
                    for (;;) {
                        U o = null;
                        for (;;) {
                            o = svq.poll();
                            if (checkTerminate()) {
                                return;
                            }
                            if (o == null) {
                                break;
                            }
                            
                            child.onNext(o);
                        }
                        if (o == null) {
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
                            for (;;) {
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
                            }
                            if (o == null) {
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
                            innerCompleted = true;
                        }
                        
                        j++;
                        if (j == n) {
                            j = 0;
                        }
                    }
                    lastIndex = j;
                    lastId = inner[j].id;
                }
                
                if (innerCompleted) {
                    if (maxConcurrency != Integer.MAX_VALUE) {
                        Observable<? extends U> p;
                        synchronized (this) {
                            p = sources.poll();
                            if (p == null) {
                                wip--;
                                continue;
                            }
                        }
                        subscribeInner(p);
                    }
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
                s.dispose();
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
    
    static final class InnerSubscriber<T, U> extends AtomicReference<Disposable> 
    implements Observer<U>, Disposable {
        /** */
        private static final long serialVersionUID = -4606175640614850599L;
        final long id;
        final MergeSubscriber<T, U> parent;
        
        volatile boolean done;
        volatile Queue<U> queue;
        
        static final Disposable CANCELLED = new Disposable() {
            @Override
            public void dispose() { }
        };
        
        public InnerSubscriber(MergeSubscriber<T, U> parent, long id) {
            this.id = id;
            this.parent = parent;
        }
        @Override
        public void onSubscribe(Disposable s) {
            if (!compareAndSet(null, s)) {
                s.dispose();
                if (get() != CANCELLED) {
                    SubscriptionHelper.reportDisposableSet();
                }
                return;
            }
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
        
        @Override
        public void dispose() {
            Disposable s = get();
            if (s != CANCELLED) {
                s = getAndSet(CANCELLED);
                if (s != CANCELLED && s != null) {
                    s.dispose();
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
