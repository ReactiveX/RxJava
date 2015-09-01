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

/**
 * Subject that allows only a single Subscriber to subscribe to it during its lifetime.
 * 
 * <p>This subject buffers notifications and replays them to the Subscriber as requested.
 * 
 * <p>This subject holds an unbounded internal buffer.
 * 
 * <p>If more than one Subscriber attempts to subscribe to this Subject, they
 * will receive an IllegalStateException if this Subject hasn't terminated yet,
 * or the Subscribers receive the terminal event (error or completion) if this
 * Subject has terminated.
 * 
 * @param <T> the value type unicasted
 */
public final class UnicastSubject<T> extends Subject<T, T> {
    
    /**
     * Creates an UnicastSubject with an internal buffer capacity hint 16.
     * @return an UnicastSubject instance
     */
    public static <T> UnicastSubject<T> create() {
        return create(16);
    }
    
    /**
     * Creates an UnicastSubject with the given internal buffer capacity hint.
     * @param capacityHint the hint to size the internal unbounded buffer
     * @return an UnicastSubject instance
     */
    public static <T> UnicastSubject<T> create(int capacityHint) {
        return create(capacityHint, null);
    }

    /**
     * Creates an UnicastSubject with the given internal buffer capacity hint and a callback for
     * the case when the single Subscriber cancels its subscription.
     * 
     * <p>The callback, if not null, is called exactly once and
     * non-overlapped with any active replay.
     * 
     * @param capacityHint the hint to size the internal unbounded buffer
     * @param onCancelled the optional callback
     * @return an UnicastSubject instance
     */
    public static <T> UnicastSubject<T> create(int capacityHint, Runnable onCancelled) {
        State<T> state = new State<>(capacityHint, onCancelled);
        return new UnicastSubject<>(state);
    }

    /** The subject state. */
    final State<T> state;
    /**
     * Constructs the Observable base class.
     * @param state the subject state
     */
    protected UnicastSubject(State<T> state) {
        super(state);
        this.state = state;
    }

    // TODO may need to have a direct WIP field to avoid clashing on the object header
    /** Pads the WIP counter. */
    static abstract class StatePad0 extends AtomicInteger {
        /** */
        private static final long serialVersionUID = 7779228232971173701L;
        /** Cache line padding 1. */
        volatile long p1a, p2a, p3a, p4a, p5a, p6a, p7a;
        /** Cache line padding 2. */
        volatile long p8a, p9a, p10a, p11a, p12a, p13a, p14a, p15a;
    }
    
    /** Contains the requested counter. */
    static abstract class StateRequested extends StatePad0 {
        /** */
        private static final long serialVersionUID = -2744070795149472578L;
        /** Holds the current requested amount. */
        volatile long requested;
        /** Updater to the field requested. */
        static final AtomicLongFieldUpdater<StateRequested> REQUESTED =
                AtomicLongFieldUpdater.newUpdater(StateRequested.class, "requested");
    }
    
    /** Pads away the requested counter. */
    static abstract class StatePad1 extends StateRequested {
        /** */
        private static final long serialVersionUID = -446575186947206398L;
        /** Cache line padding 3. */
        volatile long p1b, p2b, p3b, p4b, p5b, p6b, p7b;
        /** Cache line padding 4. */
        volatile long p8b, p9b, p10b, p11b, p12b, p13b, p14b, p15b;
    }
    
    /** The state of the UnicastSubject. */
    static final class State<T> extends StatePad1 implements Publisher<T>, Subscription, Subscriber<T> {
        /** */
        private static final long serialVersionUID = 5058617037583835632L;

        /** The queue that buffers the source events. */
        final Queue<T> queue;
        
        /** The single subscriber. */
        volatile Subscriber<? super T> subscriber;
        /** Updater to the field subscriber. */
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<State, Subscriber> SUBSCRIBER =
                AtomicReferenceFieldUpdater.newUpdater(State.class, Subscriber.class, "subscriber");
        
        /** Indicates the single subscriber has cancelled. */
        volatile boolean cancelled;
        
        /** Indicates the source has terminated. */
        volatile boolean done;
        /** 
         * The terminal error if not null. 
         * Must be set before writing to done and read after done == true.
         */
        Throwable error;

        /** Set to 1 atomically for the first and only Subscriber. */
        volatile int once;
        /** Updater to field once. */
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<State> ONCE =
                AtomicIntegerFieldUpdater.newUpdater(State.class, "once");
        
        /** 
         * Called when the Subscriber has called cancel.
         * This allows early termination for those who emit into this
         * subject so that they can stop immediately 
         */
        Runnable onCancelled;
        
        /**
         * Constructs the state with the given capacity and optional cancellation callback.
         * @param capacityHint the capacity hint for the internal buffer
         * @param onCancelled the optional cancellation callback
         */
        public State(int capacityHint, Runnable onCancelled) {
            this.onCancelled = onCancelled;
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
        
        void notifyOnCancelled() {
            Runnable r = onCancelled;
            onCancelled = null;
            if (r != null) {
                r.run();
            }
        }
        
        /**
         * Clears the subscriber and the queue.
         * @param q the queue reference (avoid re-reading instance field).
         */
        void clear(Queue<?> q) {
            SUBSCRIBER.lazySet(this, null);
            q.clear();
            notifyOnCancelled();
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
            if (t == null) {
                onError(new NullPointerException());
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
            if (t == null) {
                t = new NullPointerException();
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
                    notifyOnCancelled();
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
                        
                        if (cancelled) {
                            clear(q);
                            notifyOnCancelled();
                            return;
                        }

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
