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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiPredicate;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.subscriptions.*;

public final class FlowableSequenceEqual<T> extends Flowable<Boolean> {
    final Publisher<? extends T> first;
    final Publisher<? extends T> second;
    final BiPredicate<? super T, ? super T> comparer;
    final int bufferSize;
    
    public FlowableSequenceEqual(Publisher<? extends T> first, Publisher<? extends T> second,
            BiPredicate<? super T, ? super T> comparer, int bufferSize) {
        this.first = first;
        this.second = second;
        this.comparer = comparer;
        this.bufferSize = bufferSize;
    }
    
    @Override
    public void subscribeActual(Subscriber<? super Boolean> s) {
        EqualCoordinator<T> ec = new EqualCoordinator<T>(s, bufferSize, first, second, comparer);
        ec.subscribe();
    }
    
    static final class EqualCoordinator<T> extends AtomicInteger implements Subscription {
        /** */
        private static final long serialVersionUID = -6178010334400373240L;
        final Subscriber<? super Boolean> actual;
        final BiPredicate<? super T, ? super T> comparer;
        final ArrayCompositeSubscription resources;
        final Publisher<? extends T> first;
        final Publisher<? extends T> second;
        final EqualSubscriber<T>[] subscribers;
        
        volatile boolean cancelled;
        
        final AtomicBoolean once = new AtomicBoolean();
        
        T v1;
        
        T v2;
        
        public EqualCoordinator(Subscriber<? super Boolean> actual, int bufferSize,
                Publisher<? extends T> first, Publisher<? extends T> second,
                BiPredicate<? super T, ? super T> comparer) {
            this.actual = actual;
            this.first = first;
            this.second = second;
            this.comparer = comparer;
            @SuppressWarnings("unchecked")
            EqualSubscriber<T>[] as = new EqualSubscriber[2];
            this.subscribers = as;
            as[0] = new EqualSubscriber<T>(this, 0, bufferSize);
            as[1] = new EqualSubscriber<T>(this, 1, bufferSize);
            this.resources = new ArrayCompositeSubscription(2);
        }
        
        boolean setSubscription(Subscription s, int index) {
            return resources.setResource(index, s);
        }
        
        void subscribe() {
            EqualSubscriber<T>[] as = subscribers;
            first.subscribe(as[0]);
            second.subscribe(as[1]);
        }

        @Override
        public void request(long n) {
            if (!SubscriptionHelper.validate(n)) {
                return;
            }
            if (once.compareAndSet(false, true)) {
                EqualSubscriber<T>[] as = subscribers;
                first.subscribe(as[0]);
                second.subscribe(as[1]);
            }
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                resources.dispose();
                
                if (getAndIncrement() == 0) {
                    EqualSubscriber<T>[] as = subscribers;
                    as[0].queue.clear();
                    as[1].queue.clear();
                }
            }
        }
        
        void cancel(SpscArrayQueue<T> q1, SpscArrayQueue<T> q2) {
            cancelled = true;
            q1.clear();
            q2.clear();
        }
        
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            
            int missed = 1;
            EqualSubscriber<T>[] as = subscribers;
            
            final EqualSubscriber<T> s1 = as[0];
            final SpscArrayQueue<T> q1 = s1.queue;
            final EqualSubscriber<T> s2 = as[1];
            final SpscArrayQueue<T> q2 = s2.queue;
            
            for (;;) {
                
                long r = 0L;
                for (;;) {
                    if (cancelled) {
                        q1.clear();
                        q2.clear();
                        return;
                    }
                    
                    boolean d1 = s1.done;
                    
                    if (d1) {
                        Throwable e = s1.error;
                        if (e != null) {
                            cancel(q1, q2);
                            
                            actual.onError(e);
                            return;
                        }
                    }
                    
                    boolean d2 = s2.done;

                    if (d2) {
                        Throwable e = s2.error;
                        if (e != null) {
                            cancel(q1, q2);
                            
                            actual.onError(e);
                            return;
                        }
                    }

                    if (v1 == null) {
                        v1 = q1.poll();
                    }
                    boolean e1 = v1 == null;
                    
                    if (v2 == null) {
                        v2 = q2.poll();
                    }
                    boolean e2 = v2 == null;

                    if (d1 && d2 && e1 && e2) {
                        actual.onNext(true);
                        actual.onComplete();
                        return;
                    }
                    if ((d1 && d2) && (e1 != e2)) {
                        cancel(q1, q2);
                        
                        actual.onNext(false);
                        actual.onComplete();
                        return;
                    }
                    
                    if (!e1 && !e2) {
                        boolean c;
                        
                        try {
                            c = comparer.test(v1, v2);
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            cancel(q1, q2);
                            
                            actual.onError(ex);
                            return;
                        }
                        
                        if (!c) {
                            cancel(q1, q2);
                            
                            actual.onNext(false);
                            actual.onComplete();
                            return;
                        }
                        r++;
                        
                        v1 = null;
                        v2 = null;
                    }
                    
                    if (e1 || e2) {
                        break;
                    }
                }
                
                if (r != 0L) {
                    s1.s.request(r);
                    s2.s.request(r);
                }
                
                
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }
    
    static final class EqualSubscriber<T> implements Subscriber<T> {
        final EqualCoordinator<T> parent;
        final SpscArrayQueue<T> queue;
        final int index;
        final int bufferSize;
        
        volatile boolean done;
        Throwable error;
        
        Subscription s;
        
        public EqualSubscriber(EqualCoordinator<T> parent, int index, int bufferSize) {
            this.parent = parent;
            this.bufferSize = bufferSize;
            this.index = index;
            this.queue = new SpscArrayQueue<T>(bufferSize);
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (parent.setSubscription(s, index)) {
                this.s = s;
                s.request(bufferSize);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (!queue.offer(t)) {
                onError(new IllegalStateException("Queue full?!"));
                return;
            }
            parent.drain();
        }
        
        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            parent.drain();
        }
        
        @Override
        public void onComplete() {
            done = true;
            parent.drain();
        }
    }
}
