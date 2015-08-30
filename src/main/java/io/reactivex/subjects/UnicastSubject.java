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

package io.reactivex.subjects;

import java.util.Queue;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;

public final class UnicastSubject<T> extends Subject<T, T> {
    
    public static <T> UnicastSubject<T> create() {
        return create(16);
    }
    
    public static <T> UnicastSubject<T> create(int capacityHint) {
        State<T> state = new State<>(capacityHint);
        return new UnicastSubject<>(state);
    }
    
    final State<T> state;
    protected UnicastSubject(State<T> state) {
        super(state);
        this.state = state;
    }

    static abstract class StatePad0 extends AtomicInteger {
        /** */
        private static final long serialVersionUID = 7779228232971173701L;
        volatile long p1, p2, p3, p4, p5, p6, p7;
    }
    
    static abstract class StateRequested extends StatePad0 {
        /** */
        private static final long serialVersionUID = -2744070795149472578L;
        
        volatile long requested;
        static final AtomicLongFieldUpdater<StateRequested> REQUESTED =
                AtomicLongFieldUpdater.newUpdater(StateRequested.class, "requested");
    }
    
    static abstract class StatePad1 extends StateRequested {
        /** */
        private static final long serialVersionUID = -446575186947206398L;
        volatile long p0, p1, p2, p3, p4, p5, p6, p7;
    }
    
    static final class State<T> extends StatePad1 implements Publisher<T>, Subscription, Subscriber<T> {
        /** */
        private static final long serialVersionUID = 5058617037583835632L;

        final Queue<T> queue;
        
        volatile Subscriber<? super T> subscriber;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<State, Subscriber> SUBSCRIBER =
                AtomicReferenceFieldUpdater.newUpdater(State.class, Subscriber.class, "subscriber");
        
        volatile boolean cancelled;
        
        volatile boolean done;
        Throwable error;

        volatile int once;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<State> ONCE =
                AtomicIntegerFieldUpdater.newUpdater(State.class, "once");
        
        public State(int capacityHint) {
            queue = new SpscLinkedArrayQueue<>(capacityHint);
        }
        
        @Override
        public void subscribe(Subscriber<? super T> s) {
            if (once == 0 && ONCE.compareAndSet(this, 0, 1)) {
                SUBSCRIBER.lazySet(this, s);
                s.onSubscribe(this);
            } else {
                if (done) {
                    Throwable e = error;
                    if (e != null) {
                        EmptySubscription.error(e, s);
                    } else {
                        EmptySubscription.complete(s);
                    }
                } else {
                    EmptySubscription.error(new IllegalStateException("Only a single subscriber allowed."), s);
                }
            }
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            BackpressureHelper.add(REQUESTED, this, n);
            drain();
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                
                if (getAndIncrement() == 0) {
                    clear(queue);
                }
            }
        }
        
        void clear(Queue<?> q) {
            SUBSCRIBER.lazySet(this, null);
            q.clear();
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (done || cancelled) {
                s.cancel();
                return;
            }
            s.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(T t) {
            if (done || cancelled) {
                return;
            }
            queue.offer(t);
            drain();
        }
        
        @Override
        public void onError(Throwable t) {
            if (done || cancelled) {
                return;
            }
            error = t;
            done = true;
            drain();
        }
        
        @Override
        public void onComplete() {
            if (done || cancelled) {
                return;
            }
            done = true;
            drain();
        }
        
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            
            final Queue<T> q = queue;
            Subscriber<? super T> a = subscriber;
            int missed = 1;
            
            for (;;) {
                
                if (cancelled) {
                    clear(q);
                    return;
                }
                if (a != null) {
                    
                    boolean d = done;
                    boolean empty = q.isEmpty();
                    if (d && empty) {
                        SUBSCRIBER.lazySet(this, null);
                        Throwable ex = error;
                        if (ex != null) {
                            a.onError(ex);
                        } else {
                            a.onComplete();
                        }
                        return;
                    }
                    
                    long r = requested;
                    boolean unbounded = r == Long.MAX_VALUE;
                    long e = 0L;
                    
                    while (r != 0L) {
                        d = done;
                        T v = queue.poll();
                        empty = v == null;
                        
                        if (d && empty) {
                            SUBSCRIBER.lazySet(this, null);
                            Throwable ex = error;
                            if (ex != null) {
                                a.onError(ex);
                            } else {
                                a.onComplete();
                            }
                            return;
                        }
                        
                        if (empty) {
                            break;
                        }
                        
                        a.onNext(v);
                        
                        r--;
                        e--;
                    }
                    
                    if (e != 0 && !unbounded) {
                        REQUESTED.getAndAdd(this, e);
                    }
                    
                }
                
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
                
                if (a == null) {
                    a = subscriber;
                }
            }
        }
    }
    
    @Override
    public void onSubscribe(Subscription s) {
        state.onSubscribe(s);
    }
    
    @Override
    public void onNext(T t) {
        state.onNext(t);
    }
    
    @Override
    public void onError(Throwable t) {
        state.onError(t);
    }
    
    @Override
    public void onComplete() {
        state.onComplete();
    }
    
    @Override
    public boolean hasSubscribers() {
        return state.subscriber != null;
    }
    
    @Override
    public Throwable getThrowable() {
        State<T> s = state;
        if (s.done) {
            return s.error;
        }
        return null;
    }
    
    @Override
    public boolean hasThrowable() {
        State<T> s = state;
        return s.done && s.error != null;
    }
    
    @Override
    public boolean hasComplete() {
        State<T> s = state;
        return s.done && s.error == null;
    }
    
    @Override
    public boolean hasValue() {
        return false;
    }
    
    @Override
    public T getValue() {
        return null;
    }
    
    @Override
    public T[] getValues(T[] array) {
        if (array.length != 0) {
            array[0] = null;
        }
        return array;
    }
}
