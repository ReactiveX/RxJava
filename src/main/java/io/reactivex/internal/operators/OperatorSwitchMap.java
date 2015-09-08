/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators;

import java.util.Queue;
import java.util.concurrent.atomic.*;
import java.util.function.Function;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.internal.queue.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class OperatorSwitchMap<T, R> implements Operator<R, T> {
    final Function<? super T, ? extends Publisher<? extends R>> mapper;
    final int bufferSize;

    public OperatorSwitchMap(Function<? super T, ? extends Publisher<? extends R>> mapper, int bufferSize) {
        this.mapper = mapper;
        this.bufferSize = bufferSize;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super R> t) {
        return new SwitchMapSubscriber<>(t, mapper, bufferSize);
    }
    
    static final class SwitchMapSubscriber<T, R> extends AtomicInteger implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -3491074160481096299L;
        final Subscriber<? super R> actual;
        final Function<? super T, ? extends Publisher<? extends R>> mapper;
        final int bufferSize;
        
        
        volatile boolean done;
        Throwable error;
        
        volatile boolean cancelled;
        
        Subscription s;
        
        volatile SwitchMapInnerSubscriber<T, R> active;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<SwitchMapSubscriber, SwitchMapInnerSubscriber> ACTIVE =
                AtomicReferenceFieldUpdater.newUpdater(SwitchMapSubscriber.class, SwitchMapInnerSubscriber.class, "active");
        
        volatile long requested;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<SwitchMapSubscriber> REQUESTED =
                AtomicLongFieldUpdater.newUpdater(SwitchMapSubscriber.class, "requested");
        
        static final SwitchMapInnerSubscriber<Object, Object> CANCELLED;
        static {
            CANCELLED = new SwitchMapInnerSubscriber<>(null, -1L, 1);
            CANCELLED.cancel();
        }
        
        volatile long unique;
        
        public SwitchMapSubscriber(Subscriber<? super R> actual, Function<? super T, ? extends Publisher<? extends R>> mapper, int bufferSize) {
            this.actual = actual;
            this.mapper = mapper;
            this.bufferSize = bufferSize;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(this);
        }
        
        @Override
        public void onNext(T t) {
            long c = unique + 1;
            unique = c;
            
            SwitchMapInnerSubscriber<T, R> inner = active;
            if (inner != null) {
                inner.cancel();
            }
            
            Publisher<? extends R> p;
            try {
                p = mapper.apply(t);
            } catch (Throwable e) {
                s.cancel();
                onError(e);
                return;
            }

            if (p == null) {
                s.cancel();
                onError(new NullPointerException("The publisher returned is null"));
                return;
            }
            
            SwitchMapInnerSubscriber<T, R> nextInner = new SwitchMapInnerSubscriber<>(this, c, bufferSize);
            
            for (;;) {
                inner = active;
                if (inner == CANCELLED) {
                    break;
                }
                if (ACTIVE.compareAndSet(this, inner, nextInner)) {
                    p.subscribe(nextInner);
                    break;
                }
            }
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
            BackpressureHelper.add(REQUESTED, this, n);
            if (unique == 0L) {
                s.request(Long.MAX_VALUE);
            } else {
                drain();
            }
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                
                disposeInner();
            }
        }
        
        @SuppressWarnings("unchecked")
        void disposeInner() {
            SwitchMapInnerSubscriber<T, R> a = active;
            if (a != CANCELLED) {
                a = ACTIVE.getAndSet(this, CANCELLED);
                if (a != CANCELLED && a != null) {
                    s.cancel();
                }
            }
        }
        
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            final Subscriber<? super R> a = actual;
            
            int missing = 1;

            for (;;) {

                if (cancelled) {
                    return;
                }
                
                if (done) {
                    Throwable err = error;
                    if (err != null) {
                        disposeInner();
                        s.cancel();
                        a.onError(err);
                        return;
                    } else
                    if (active == null) {
                        a.onComplete();
                        return;
                    }
                }
                
                SwitchMapInnerSubscriber<T, R> inner = active;

                if (inner != null) {
                    Queue<R> q = inner.queue;

                    if (inner.done) {
                        Throwable err = inner.error;
                        if (err != null) {
                            s.cancel();
                            disposeInner();
                            a.onError(err);
                            return;
                        } else
                        if (q.isEmpty()) {
                            ACTIVE.compareAndSet(this, inner, null);
                            continue;
                        }
                    }
                    
                    long r = requested;
                    boolean unbounded = r == Long.MAX_VALUE;
                    long e = 0L;
                    boolean retry = false;
                    
                    while (r != 0L) {
                        boolean d = inner.done;
                        R v = q.poll();
                        boolean empty = v == null;

                        if (cancelled) {
                            return;
                        }
                        if (inner != active) {
                            retry = true;
                            break;
                        }
                        
                        if (d) {
                            Throwable err = inner.error;
                            if (err != null) {
                                s.cancel();
                                a.onError(err);
                                return;
                            } else
                            if (empty) {
                                ACTIVE.compareAndSet(this, inner, null);
                                retry = true;
                                break;
                            }
                        }
                        
                        if (empty) {
                            break;
                        }
                        
                        a.onNext(v);
                        
                        r--;
                        e--;
                    }
                    
                    if (e != 0L) {
                        if (!cancelled) {
                            if (!unbounded) {
                                REQUESTED.addAndGet(this, e);
                            }
                            inner.get().request(-e);
                        }
                    }
                    
                    if (retry) {
                        continue;
                    }
                }
                
                missing = addAndGet(-missing);
                if (missing == 0) {
                    break;
                }
            }
        }
        
        boolean checkTerminated(boolean d, boolean empty, Subscriber<? super R> a) {
            if (cancelled) {
                s.cancel();
                return true;
            }
            if (d) {
                Throwable e = error;
                if (e != null) {
                    cancelled = true;
                    s.cancel();
                    a.onError(e);
                    return true;
                } else
                if (empty) {
                    a.onComplete();
                    return true;
                }
            }
            
            return false;
        }
    }
    
    static final class SwitchMapInnerSubscriber<T, R> extends AtomicReference<Subscription> implements Subscriber<R> {
        /** */
        private static final long serialVersionUID = 3837284832786408377L;
        final SwitchMapSubscriber<T, R> parent;
        final long index;
        final int bufferSize;
        final Queue<R> queue;
        
        volatile boolean done;
        Throwable error;
        
        static final Subscription CANCELLED = new Subscription() {
            @Override
            public void request(long n) {
                
            }
            
            @Override
            public void cancel() {
                
            }
        };
        
        public SwitchMapInnerSubscriber(SwitchMapSubscriber<T, R> parent, long index, int bufferSize) {
            this.parent = parent;
            this.index = index;
            this.bufferSize = bufferSize;
            Queue<R> q;
            if (Pow2.isPowerOfTwo(bufferSize)) {
                q = new SpscArrayQueue<>(bufferSize);
            } else {
                q = new SpscExactArrayQueue<>(bufferSize);
            }
            this.queue = q;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (index == parent.unique) {
                if (!compareAndSet(null, s)) {
                    s.cancel();
                    if (get() != CANCELLED) {
                        SubscriptionHelper.reportSubscriptionSet();
                    }
                    return;
                }
                s.request(bufferSize);
            } else {
                s.cancel();
            }
        }
        
        @Override
        public void onNext(R t) {
            if (index == parent.unique) {
                if (!queue.offer(t)) {
                    onError(new IllegalStateException("Queue full?!"));
                    return;
                }
                parent.drain();
            }
        }

        @Override
        public void onError(Throwable t) {
            if (index == parent.unique) {
                error = t;
                done = true;
                parent.drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }
        
        @Override
        public void onComplete() {
            if (index == parent.unique) {
                done = true;
                parent.drain();
            }
        }
        
        public void cancel() {
            Subscription s = get();
            if (s != CANCELLED) {
                s = getAndSet(CANCELLED);
                if (s != CANCELLED && s != null) {
                    s.cancel();
                }
            }
        }
    }
}
