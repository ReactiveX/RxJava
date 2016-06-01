/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import java.util.*;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable;
import rx.Observable.Operator;
import rx.functions.Action0;
import rx.internal.util.atomic.SpscLinkedArrayQueue;
import rx.subjects.*;
import rx.subscriptions.Subscriptions;

/**
 * Creates windows of values into the source sequence with skip frequency and size bounds.
 * 
 * If skip == size then the windows are non-overlapping, otherwise, windows may overlap
 * or can be discontinuous. The returned Observable sequence is cold and need to be
 * consumed while the window operation is in progress.
 * 
 * <p>Note that this conforms the Rx.NET behavior, but does not match former RxJava
 * behavior, which operated as a regular buffer and mapped its lists to Observables.</p>
 * 
 * @param <T> the value type
 */
public final class OperatorWindowWithSize<T> implements Operator<Observable<T>, T> {
    final int size;
    final int skip;

    public OperatorWindowWithSize(int size, int skip) {
        this.size = size;
        this.skip = skip;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super Observable<T>> child) {
        if (skip == size) {
            WindowExact<T> parent = new WindowExact<T>(child, size);
            
            child.add(parent.cancel);
            child.setProducer(parent.createProducer());
            
            return parent;
        } else
        if (skip > size) {
            WindowSkip<T> parent = new WindowSkip<T>(child, size, skip);
            
            child.add(parent.cancel);
            child.setProducer(parent.createProducer());
            
            return parent;
        }

        WindowOverlap<T> parent = new WindowOverlap<T>(child, size, skip);
        
        child.add(parent.cancel);
        child.setProducer(parent.createProducer());
        
        return parent;
        
    }
    
    static final class WindowExact<T> extends Subscriber<T> implements Action0 {
        final Subscriber<? super Observable<T>> actual;
        
        final int size;
        
        final AtomicInteger wip;
        
        final Subscription cancel;
        
        int index;
        
        Subject<T, T> window;
        
        public WindowExact(Subscriber<? super Observable<T>> actual, int size) {
            this.actual = actual;
            this.size = size;
            this.wip = new AtomicInteger(1);
            this.cancel = Subscriptions.create(this);
            this.add(cancel);
            this.request(0);
        }
        
        @Override
        public void onNext(T t) {
            int i = index;
            
            Subject<T, T> w = window;
            if (i == 0) {
                wip.getAndIncrement();
                
                w = UnicastSubject.create(size, this);
                window = w;
                
                actual.onNext(w);
            }
            i++;
            
            w.onNext(t);
            
            if (i == size) {
                index = 0;
                window = null;
                w.onCompleted();
            } else {
                index = i;
            }
        }
        
        @Override
        public void onError(Throwable e) {
            Subject<T, T> w = window;
            
            if (w != null) {
                window = null;
                w.onError(e);
            }
            actual.onError(e);
        }
        
        @Override
        public void onCompleted() {
            Subject<T, T> w = window;
            
            if (w != null) {
                window = null;
                w.onCompleted();
            }
            actual.onCompleted();
        }
        
        Producer createProducer() {
            return new Producer() {
                @Override
                public void request(long n) {
                    if (n < 0L) {
                        throw new IllegalArgumentException("n >= 0 required but it was " + n);
                    }
                    if (n != 0L) {
                        long u = BackpressureUtils.multiplyCap(size, n);
                        WindowExact.this.request(u);
                    }
                }
            };
        }
        
        @Override
        public void call() {
            if (wip.decrementAndGet() == 0) {
                unsubscribe();
            }
        }
    }
    
    static final class WindowSkip<T> extends Subscriber<T> implements Action0 {
        final Subscriber<? super Observable<T>> actual;
        
        final int size;
        
        final int skip;
        
        final AtomicInteger wip;
        
        final Subscription cancel;
        
        int index;
        
        Subject<T, T> window;
        
        public WindowSkip(Subscriber<? super Observable<T>> actual, int size, int skip) {
            this.actual = actual;
            this.size = size;
            this.skip = skip;
            this.wip = new AtomicInteger(1);
            this.cancel = Subscriptions.create(this);
            this.add(cancel);
            this.request(0);
        }
        
        @Override
        public void onNext(T t) {
            int i = index;
            
            Subject<T, T> w = window;
            if (i == 0) {
                wip.getAndIncrement();
                
                w = UnicastSubject.create(size, this);
                window = w;
                
                actual.onNext(w);
            }
            i++;
            
            if (w != null) {
                w.onNext(t);
            }
            
            if (i == size) {
                index = i;
                window = null;
                w.onCompleted();
            } else
            if (i == skip) {
                index = 0;
            } else {
                index = i;
            }
            
        }
        
        @Override
        public void onError(Throwable e) {
            Subject<T, T> w = window;
            
            if (w != null) {
                window = null;
                w.onError(e);
            }
            actual.onError(e);
        }
        
        @Override
        public void onCompleted() {
            Subject<T, T> w = window;
            
            if (w != null) {
                window = null;
                w.onCompleted();
            }
            actual.onCompleted();
        }
        
        Producer createProducer() {
            return new WindowSkipProducer();
        }
        
        @Override
        public void call() {
            if (wip.decrementAndGet() == 0) {
                unsubscribe();
            }
        }
        
        final class WindowSkipProducer extends AtomicBoolean implements Producer {
            /** */
            private static final long serialVersionUID = 4625807964358024108L;

            @Override
            public void request(long n) {
                if (n < 0L) {
                    throw new IllegalArgumentException("n >= 0 required but it was " + n);
                }
                if (n != 0L) {
                    WindowSkip<T> parent = WindowSkip.this;
                    if (!get() && compareAndSet(false, true)) {
                        long u = BackpressureUtils.multiplyCap(n, parent.size);
                        long v = BackpressureUtils.multiplyCap(parent.skip - parent.size, n - 1);
                        long w = BackpressureUtils.addCap(u, v);
                        parent.request(w);
                    } else {
                        long u = BackpressureUtils.multiplyCap(n, parent.skip);
                        parent.request(u);
                    }
                }
            }
        }
    }
    
    static final class WindowOverlap<T> extends Subscriber<T> implements Action0 {
        final Subscriber<? super Observable<T>> actual;
        
        final int size;
        
        final int skip;
        
        final AtomicInteger wip;
        
        final Subscription cancel;

        final ArrayDeque<Subject<T, T>> windows;

        final AtomicLong requested;
        
        final AtomicInteger drainWip;
        
        final Queue<Subject<T, T>> queue;
        
        Throwable error;
        
        volatile boolean done;
        
        int index;
        
        int produced;
        
        public WindowOverlap(Subscriber<? super Observable<T>> actual, int size, int skip) {
            this.actual = actual;
            this.size = size;
            this.skip = skip;
            this.wip = new AtomicInteger(1);
            this.windows = new ArrayDeque<Subject<T, T>>();
            this.drainWip = new AtomicInteger();
            this.requested = new AtomicLong();
            this.cancel = Subscriptions.create(this);
            this.add(cancel);
            this.request(0);
            int maxWindows = (size + (skip - 1)) / skip;
            this.queue = new SpscLinkedArrayQueue<Subject<T, T>>(maxWindows);
        }
        
        @Override
        public void onNext(T t) {
            int i = index;
            
            ArrayDeque<Subject<T, T>> q = windows;
            
            if (i == 0 && !actual.isUnsubscribed()) {
                wip.getAndIncrement();
                
                Subject<T, T> w = UnicastSubject.create(16, this);
                q.offer(w);
                
                queue.offer(w);
                drain();
            }

            for (Subject<T, T> w : windows) {
                w.onNext(t);
            }

            int p = produced + 1;
            
            if (p == size) {
                produced = p - skip;
                
                Subject<T, T> w = q.poll();
                if (w != null) {
                    w.onCompleted();
                }
            } else {
                produced = p;
            }
            
            i++;
            if (i == skip) {
                index = 0;
            } else {
                index = i;
            }
        }
        
        @Override
        public void onError(Throwable e) {
            for (Subject<T, T> w : windows) {
                w.onError(e);
            }
            windows.clear();
            
            error = e;
            done = true;
            drain();
        }
        
        @Override
        public void onCompleted() {
            for (Subject<T, T> w : windows) {
                w.onCompleted();
            }
            windows.clear();
            
            done = true;
            drain();
        }
        
        Producer createProducer() {
            return new WindowOverlapProducer();
        }
        
        @Override
        public void call() {
            if (wip.decrementAndGet() == 0) {
                unsubscribe();
            }
        }
        
        void drain() {
            AtomicInteger dw = drainWip;
            if (dw.getAndIncrement() != 0) {
                return;
            }

            final Subscriber<? super Subject<T, T>> a = actual;
            final Queue<Subject<T, T>> q = queue;
            
            int missed = 1;
            
            for (;;) {
                
                long r = requested.get();
                long e = 0L;
                
                while (e != r) {
                    boolean d = done;
                    Subject<T, T> v = q.poll();
                    boolean empty = v == null;
                    
                    if (checkTerminated(d, empty, a, q)) {
                        return;
                    }
                    
                    if (empty) {
                        break;
                    }
                    
                    a.onNext(v);
                    
                    e++;
                }
                
                if (e == r) {
                    if (checkTerminated(done, q.isEmpty(), a, q)) {
                        return;
                    }
                }
                
                if (e != 0 && r != Long.MAX_VALUE) {
                    requested.addAndGet(-e);
                }
                
                missed = dw.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        boolean checkTerminated(boolean d, boolean empty, Subscriber<? super Subject<T, T>> a, Queue<Subject<T, T>> q) {
            if (a.isUnsubscribed()) {
                q.clear();
                return true;
            }
            if (d) {
                Throwable e = error;
                if (e != null) {
                    q.clear();
                    a.onError(e);
                    return true;
                } else
                if (empty) {
                    a.onCompleted();
                    return true;
                }
            }
            return false;
        }
        
        final class WindowOverlapProducer extends AtomicBoolean implements Producer {
            /** */
            private static final long serialVersionUID = 4625807964358024108L;

            @Override
            public void request(long n) {
                if (n < 0L) {
                    throw new IllegalArgumentException("n >= 0 required but it was " + n);
                }
                if (n != 0L) {
                    
                    WindowOverlap<T> parent = WindowOverlap.this;
                    
                    if (!get() && compareAndSet(false, true)) {
                        long u = BackpressureUtils.multiplyCap(parent.skip, n - 1);
                        long v = BackpressureUtils.addCap(u, parent.size);
                        
                        parent.request(v);
                    } else {
                        long u = BackpressureUtils.multiplyCap(parent.skip, n);
                        WindowOverlap.this.request(u);
                    }
                    
                    BackpressureUtils.getAndAddRequest(parent.requested, n);
                    parent.drain();
                }
            }
        }
    }

}
