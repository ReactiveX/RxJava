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
import rx.exceptions.Exceptions;
import rx.functions.*;
import rx.internal.util.atomic.SpscAtomicArrayQueue;
import rx.internal.util.unsafe.*;
import rx.subscriptions.Subscriptions;

public final class OperatorEagerConcatMap<T, R> implements Operator<R, T> {
    final Func1<? super T, ? extends Observable<? extends R>> mapper;
    final int bufferSize;
    private final int maxConcurrent;
    public OperatorEagerConcatMap(Func1<? super T, ? extends Observable<? extends R>> mapper, int bufferSize, int maxConcurrent) {
        this.mapper = mapper;
        this.bufferSize = bufferSize;
        this.maxConcurrent = maxConcurrent;
    }
    
    @Override
    public Subscriber<? super T> call(Subscriber<? super R> t) {
        EagerOuterSubscriber<T, R> outer = new EagerOuterSubscriber<T, R>(mapper, bufferSize, maxConcurrent, t);
        outer.init();
        return outer;
    }
    
    static final class EagerOuterProducer extends AtomicLong implements Producer {
        /** */
        private static final long serialVersionUID = -657299606803478389L;
        
        final EagerOuterSubscriber<?, ?> parent;
        
        public EagerOuterProducer(EagerOuterSubscriber<?, ?> parent) {
            this.parent = parent;
        }

        @Override
        public void request(long n) {
            if (n < 0) {
                throw new IllegalStateException("n >= 0 required but it was " + n);
            }
            
            if (n > 0) {
                BackpressureUtils.getAndAddRequest(this, n);
                parent.drain();
            }
        }
    }
    
    static final class EagerOuterSubscriber<T, R> extends Subscriber<T> {
        final Func1<? super T, ? extends Observable<? extends R>> mapper;
        final int bufferSize;
        final Subscriber<? super R> actual;
        
        final LinkedList<EagerInnerSubscriber<R>> subscribers;
        
        volatile boolean done;
        Throwable error;
        
        volatile boolean cancelled;
        
        final AtomicInteger wip;
        private EagerOuterProducer sharedProducer;
        
        public EagerOuterSubscriber(Func1<? super T, ? extends Observable<? extends R>> mapper, int bufferSize,
                int maxConcurrent, Subscriber<? super R> actual) {
            this.mapper = mapper;
            this.bufferSize = bufferSize;
            this.actual = actual;
            this.subscribers = new LinkedList<EagerInnerSubscriber<R>>();
            this.wip = new AtomicInteger();
            request(maxConcurrent == Integer.MAX_VALUE ? Long.MAX_VALUE : maxConcurrent);
        }
        
        void init() {
            sharedProducer = new EagerOuterProducer(this);
            add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    cancelled = true;
                    if (wip.getAndIncrement() == 0) {
                        cleanup();
                    }
                }
            }));
            actual.add(this);
            actual.setProducer(sharedProducer);
        }
        
        void cleanup() {
            List<Subscription> list;
            synchronized (subscribers) {
                list = new ArrayList<Subscription>(subscribers);
                subscribers.clear();
            }
            
            for (Subscription s : list) {
                s.unsubscribe();
            }
        }
        
        @Override
        public void onNext(T t) {
            Observable<? extends R> observable;
            
            try {
                observable = mapper.call(t);
            } catch (Throwable e) {
                Exceptions.throwOrReport(e, actual, t);
                return;
            }
            
            EagerInnerSubscriber<R> inner = new EagerInnerSubscriber<R>(this, bufferSize);
            if (cancelled) {
                return;
            }
            synchronized (subscribers) {
                if (cancelled) {
                    return;
                }
                subscribers.add(inner);
            }
            if (cancelled) {
                return;
            }
            observable.unsafeSubscribe(inner);
            drain();
        }
        
        @Override
        public void onError(Throwable e) {
            error = e;
            done = true;
            drain();
        }
        
        @Override
        public void onCompleted() {
            done = true;
            drain();
        }
        
        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            int missed = 1;
            
            final AtomicLong requested = sharedProducer;
            final Subscriber<? super R> actualSubscriber = this.actual;
            final NotificationLite<R> nl = NotificationLite.instance();
            
            for (;;) {
                
                if (cancelled) {
                    cleanup();
                    return;
                }
                
                EagerInnerSubscriber<R> innerSubscriber;
                
                boolean outerDone = done;
                synchronized (subscribers) {
                    innerSubscriber = subscribers.peek();
                }
                boolean empty = innerSubscriber == null;
                
                if (outerDone) {
                    Throwable error = this.error;
                    if (error != null) {
                        cleanup();
                        actualSubscriber.onError(error);
                        return;
                    } else
                    if (empty) {
                        actualSubscriber.onCompleted();
                        return;
                    }
                }

                if (!empty) {
                    long requestedAmount = requested.get();
                    long emittedAmount = 0L;
                    boolean unbounded = requestedAmount == Long.MAX_VALUE;
                    
                    Queue<Object> innerQueue = innerSubscriber.queue;
                    boolean innerDone = false;
                    
                    
                    for (;;) {
                        outerDone = innerSubscriber.done;
                        Object v = innerQueue.peek();
                        empty = v == null;
                        
                        if (outerDone) {
                            Throwable innerError = innerSubscriber.error;
                            if (innerError != null) {
                                cleanup();
                                actualSubscriber.onError(innerError);
                                return;
                            } else
                            if (empty) {
                                synchronized (subscribers) {
                                    subscribers.poll();
                                }
                                innerSubscriber.unsubscribe();
                                innerDone = true;
                                request(1);
                                break;
                            }
                        }
                        
                        if (empty) {
                            break;
                        }
                        
                        if (requestedAmount == 0L) {
                            break;
                        }
                        
                        innerQueue.poll();
                        
                        try {
                            actualSubscriber.onNext(nl.getValue(v));
                        } catch (Throwable ex) {
                            Exceptions.throwOrReport(ex, actualSubscriber, v);
                            return;
                        }
                        
                        requestedAmount--;
                        emittedAmount--;
                    }
                    
                    if (emittedAmount != 0L) {
                        if (!unbounded) {
                            requested.addAndGet(emittedAmount);
                        }
                        if (!innerDone) {
                            innerSubscriber.requestMore(-emittedAmount);
                        }
                    }
                    
                    if (innerDone) {
                        continue;
                    }
                }
                
                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    return;
                }
            }
        }
    }
    
    static final class EagerInnerSubscriber<T> extends Subscriber<T> {
        final EagerOuterSubscriber<?, T> parent;
        final Queue<Object> queue;
        final NotificationLite<T> nl;
        
        volatile boolean done;
        Throwable error;
        
        public EagerInnerSubscriber(EagerOuterSubscriber<?, T> parent, int bufferSize) {
            super();
            this.parent = parent;
            Queue<Object> q;
            if (UnsafeAccess.isUnsafeAvailable()) {
                q = new SpscArrayQueue<Object>(bufferSize);
            } else {
                q = new SpscAtomicArrayQueue<Object>(bufferSize);
            }
            this.queue = q;
            this.nl = NotificationLite.instance();
            request(bufferSize);
        }
        
        @Override
        public void onNext(T t) {
            queue.offer(nl.next(t));
            parent.drain();
        }
        
        @Override
        public void onError(Throwable e) {
            error = e;
            done = true;
            parent.drain();
        }
        
        @Override
        public void onCompleted() {
            done = true;
            parent.drain();
        }
        
        void requestMore(long n) {
            request(n);
        }
    }
}
