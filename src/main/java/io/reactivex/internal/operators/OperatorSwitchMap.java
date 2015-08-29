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
        
        long requested;

        volatile long missedRequested;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<SwitchMapSubscriber> MISSED_REQUESTED =
                AtomicLongFieldUpdater.newUpdater(SwitchMapSubscriber.class, "missedRequested");

        
        volatile boolean done;
        Throwable error;
        
        volatile boolean cancelled;
        
        Subscription s;
        
        volatile Publisher<? extends R> publisher;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<SwitchMapSubscriber, Publisher> PUBLISHER =
                AtomicReferenceFieldUpdater.newUpdater(SwitchMapSubscriber.class, Publisher.class, "publisher");
        
        SwitchMapInnerSubscriber<T, R> active;
        
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
            Publisher<? extends R> p;
            try {
                p = mapper.apply(t);
            } catch (Throwable e) {
                s.cancel();
                onError(e);
                return;
            }
            
            PUBLISHER.lazySet(this, p);
            drain();
        }
        
        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            drain();
        }
        
        @Override
        public void onComplete() {
            done = true;
            drain();
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            BackpressureHelper.add(MISSED_REQUESTED, this, n);
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

                if (getAndIncrement() == 0) {
                    clear(active);
                }
            }
        }
        
        void clear(SwitchMapInnerSubscriber<T, R> inner) {
            if (inner != null) {
                inner.cancel();
            }
            publisher = null;
            s.cancel();
        }
        
        @SuppressWarnings("unchecked")
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            final Subscriber<? super R> a = actual;
            
            int missing = 1;
            
            for (;;) {
                boolean d = done;
                Publisher<? extends R> p = publisher;
                SwitchMapInnerSubscriber<T, R> inner = active;
                boolean empty = p == null && inner == null;
                
                if (checkTerminated(d, empty, a, inner)) {
                    return;
                }

                // get the latest publisher
                if (p != null) {
                    p = PUBLISHER.getAndSet(this, null);
                }
                
                long c = unique;
                if (p != null) {
                    if (inner != null) {
                        inner.cancel();
                        inner = null;
                    }
                    unique = ++c;
                    inner = new SwitchMapInnerSubscriber<>(this, c, bufferSize);
                    active = inner;
                    
                    p.subscribe(inner);
                }

                if (inner != null) {
                    long r = requested;
                    
                    long mr = MISSED_REQUESTED.getAndSet(this, 0L);
                    if (mr != 0L) {
                        r = BackpressureHelper.addCap(r, mr);
                        requested = r;
                    }
                    
                    boolean unbounded = r == Long.MAX_VALUE;
    
                    Queue<R> q = inner.queue;
                    long e = 0;
                    
                    while (r != 0L) {
                        d = inner.done;
                        R v = q.poll();
                        empty = v == null;

                        if (cancelled) {
                            clear(active);
                            return;
                        }
                        if (d) {
                            Throwable err = inner.error;
                            if (err != null) {
                                clear(active);
                                a.onError(err);
                                return;
                            } else
                            if (empty) {
                                active = null;
                                break;
                            }
                        }
                        
                        if (publisher != null) {
                            break;
                        }
                        
                        if (empty) {
                            break;
                        }
                        
                        a.onNext(v);
                        
                        r--;
                        e++;
                    }
                    
                    if (e != 0L) {
                        if (!unbounded) {
                            requested -= e;
                            inner.get().request(e);
                        }
                        
                    }
                }

                missing = addAndGet(-missing);
                if (missing == 0) {
                    break;
                }
            }
        }
        
        boolean checkTerminated(boolean d, boolean empty, Subscriber<? super R> a, SwitchMapInnerSubscriber<T, R> inner) {
            if (cancelled) {
                clear(inner);
                return true;
            }
            if (d) {
                Throwable e = error;
                if (e != null) {
                    cancelled = true;
                    clear(inner);
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
