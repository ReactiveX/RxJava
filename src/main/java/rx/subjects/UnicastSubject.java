/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.subjects;

import java.util.Queue;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.annotations.Experimental;
import rx.exceptions.*;
import rx.functions.Action0;
import rx.internal.operators.*;
import rx.internal.util.atomic.*;
import rx.internal.util.unsafe.*;

/**
 * A Subject variant which buffers events until a single Subscriber arrives and replays them to it
 * and potentially switches to direct delivery once the Subscriber caught up and requested an unlimited
 * amount. In this case, the buffered values are no longer retained. If the Subscriber
 * requests a limited amount, queueing is involved and only those values are retained which
 * weren't requested by the Subscriber at that time.
 * 
 * @param <T> the input and output value type
 */
@Experimental
public final class UnicastSubject<T> extends Subject<T, T> {

    /**
     * Constructs an empty UnicastSubject instance with the default capacity hint of 16 elements.
     * 
     * @param <T> the input and output value type
     * @return the created UnicastSubject instance
     */
    public static <T> UnicastSubject<T> create() {
        return create(16);
    }
    /**
     * Constructs an empty UnicastSubject instance with a capacity hint.
     * <p>The capacity hint determines the internal queue's island size: the larger
     * it is the less frequent allocation will happen if there is no subscriber
     * or the subscriber hasn't caught up.
     * @param <T> the input and output value type
     * @param capacityHint the capacity hint for the internal queue
     * @return the created BufferUntilSubscriber instance
     */
    public static <T> UnicastSubject<T> create(int capacityHint) {
        State<T> state = new State<T>(capacityHint, null);
        return new UnicastSubject<T>(state);
    }

    /**
     * Constructs an empty UnicastSubject instance with a capacity hint and
     * an Action0 instance to call if the subject reaches its terminal state
     * or the single Subscriber unsubscribes mid-sequence.
     * <p>The capacity hint determines the internal queue's island size: the larger
     * it is the less frequent allocation will happen if there is no subscriber
     * or the subscriber hasn't caught up.
     * @param <T> the input and output value type
     * @param capacityHint the capacity hint for the internal queue
     * @param onTerminated the optional callback to call when subject reaches its terminal state
     *                      or the single Subscriber unsubscribes mid-sequence. It will be called
     *                      at most once.
     * @return the created BufferUntilSubscriber instance
     */
    public static <T> UnicastSubject<T> create(int capacityHint, Action0 onTerminated) {
        State<T> state = new State<T>(capacityHint, onTerminated);
        return new UnicastSubject<T>(state);
    }

    final State<T> state;

    private UnicastSubject(State<T> state) {
        super(state);
        this.state = state;
    }
    
    @Override
    public void onNext(T t) {
        state.onNext(t);
    }
    
    @Override
    public void onError(Throwable e) {
        state.onError(e);
    }
    
    @Override
    public void onCompleted() {
        state.onCompleted();
    }
    
    @Override
    public boolean hasObservers() {
        return state.subscriber.get() != null;
    }
    
    /**
     * The single-consumption replaying state.
     *
     * @param <T> the value type
     */
    static final class State<T> extends AtomicLong implements Producer, Observer<T>, OnSubscribe<T>, Subscription {
        /** */
        private static final long serialVersionUID = -9044104859202255786L;
        /** The single subscriber. */
        final AtomicReference<Subscriber<? super T>> subscriber;
        /** The queue holding values until the subscriber arrives and catches up. */
        final Queue<Object> queue;
        /** JCTools queues don't accept nulls. */
        final NotificationLite<T> nl;
        /** Atomically set to true on terminal condition. */
        final AtomicReference<Action0> terminateOnce;
        /** In case the source emitted an error. */
        Throwable error;
        /** Indicates the source has terminated. */
        volatile boolean done;
        /** Emitter loop: emitting indicator. Guarded by this. */
        boolean emitting;
        /** Emitter loop: missed emission indicator. Guarded by this. */
        boolean missed;
        /** Indicates the queue can be bypassed because the child has caught up with the replay. */
        volatile boolean caughtUp;
        /**
         * Constructor.
         * @param capacityHint indicates how large each island in the Spsc queue should be to
         * reduce allocation frequency
         * @param onTerminated the action to call when the subject reaches its terminal state or
         * the single subscriber unsubscribes.
         */
        public State(int capacityHint, Action0 onTerminated) {
            this.nl = NotificationLite.instance();
            this.subscriber = new AtomicReference<Subscriber<? super T>>();
            this.terminateOnce = onTerminated != null ? new AtomicReference<Action0>(onTerminated) : null;
            
            Queue<Object> q;
            if (capacityHint > 1) {
                q = UnsafeAccess.isUnsafeAvailable()
                        ? new SpscUnboundedArrayQueue<Object>(capacityHint)
                        : new SpscUnboundedAtomicArrayQueue<Object>(capacityHint);
            } else {
                q = UnsafeAccess.isUnsafeAvailable()
                        ? new SpscLinkedQueue<Object>()
                        : new SpscLinkedAtomicQueue<Object>();
            }
            this.queue = q;
        }
        
        @Override
        public void onNext(T t) {
            if (!done) {
                if (!caughtUp) {
                    boolean stillReplay = false;
                    /*
                     * We need to offer while holding the lock because
                     * we have to atomically switch caughtUp to true
                     * that can only happen if there isn't any concurrent
                     * offer() happening while the emission is in replayLoop().
                     */
                    synchronized (this) {
                        if (!caughtUp) {
                            queue.offer(nl.next(t));
                            stillReplay = true;
                        }
                    }
                    if (stillReplay) {
                        replay();
                        return;
                    }
                }
                Subscriber<? super T> s = subscriber.get();
                try {
                    s.onNext(t);
                } catch (Throwable ex) {
                    Exceptions.throwOrReport(ex, s, t);
                }
            }
        }
        @Override
        public void onError(Throwable e) {
            if (!done) {
                
                doTerminate();
                
                error = e;
                done = true;
                if (!caughtUp) {
                    boolean stillReplay = false;
                    synchronized (this) {
                        stillReplay = !caughtUp;
                    }
                    if (stillReplay) {
                        replay();
                        return;
                    }
                }
                subscriber.get().onError(e);
            }
        }
        @Override
        public void onCompleted() {
            if (!done) {

                doTerminate();

                done = true;
                if (!caughtUp) {
                    boolean stillReplay = false;
                    synchronized (this) {
                        stillReplay = !caughtUp;
                    }
                    if (stillReplay) {
                        replay();
                        return;
                    }
                }
                subscriber.get().onCompleted();
            }
        }
        
        @Override
        public void request(long n) {
            if (n < 0L) {
                throw new IllegalArgumentException("n >= 0 required");
            } else
            if (n > 0L) {
                BackpressureUtils.getAndAddRequest(this, n);
                replay();
            } else
            if (done) { // terminal events can be delivered for zero requests
                replay();
            }
        }
        /**
         * Tries to set the given subscriber if not already set, sending an
         * IllegalStateException to the subscriber otherwise.
         * @param subscriber
         */
        @Override
        public void call(Subscriber<? super T> subscriber) {
            if (this.subscriber.compareAndSet(null, subscriber)) {
                subscriber.add(this);
                subscriber.setProducer(this);
            } else {
                subscriber.onError(new IllegalStateException("Only a single subscriber is allowed"));
            }
        }
        /**
         * Tries to replay the contents of the queue.
         */
        void replay() {
            synchronized (this) {
                if (emitting) {
                    missed = true;
                    return;
                }
                emitting = true;
            }
            Queue<Object> q = queue;
            for (;;) {
                Subscriber<? super T> s = subscriber.get();
                boolean unlimited = false;
                if (s != null) {
                    boolean d = done;
                    boolean empty = q.isEmpty();
                    
                    if (checkTerminated(d, empty, s)) {
                        return;
                    }
                    long r = get();
                    unlimited = r == Long.MAX_VALUE;
                    long e = 0L;
                    
                    while (r != 0) {
                        d = done;
                        Object v = q.poll();
                        empty = v == null;
                        if (checkTerminated(d, empty, s)) {
                            return;
                        }
                        if (empty) {
                            break;
                        }
                        T value = nl.getValue(v);
                        try {
                            s.onNext(value);
                        } catch (Throwable ex) {
                            q.clear();
                            Exceptions.throwIfFatal(ex);
                            s.onError(OnErrorThrowable.addValueAsLastCause(ex, value));
                            return;
                        }
                        r--;
                        e++;
                    }
                    if (!unlimited && e != 0L) {
                        addAndGet(-e);
                    }
                }
                
                synchronized (this) {
                    if (!missed) {
                        if (unlimited && q.isEmpty()) {
                            caughtUp = true;
                        }
                        emitting = false;
                        return;
                    }
                    missed = false;
                }
            }
        }
        /**
         * Terminates the state by setting the done flag and tries to clear the queue.
         * Should be called only when the child unsubscribes
         */
        @Override
        public void unsubscribe() {

            doTerminate();

            done = true;
            synchronized (this) {
                if (emitting) {
                    return;
                }
                emitting = true;
            }
            queue.clear();
        }
        
        @Override
        public boolean isUnsubscribed() {
            return done;
        }
        
        /**
         * Checks if one of the terminal conditions have been met: child unsubscribed,
         * an error happened or the source terminated and the queue is empty
         * @param done
         * @param empty
         * @param s
         * @return
         */
        boolean checkTerminated(boolean done, boolean empty, Subscriber<? super T> s) {
            if (s.isUnsubscribed()) {
                queue.clear();
                return true;
            }
            if (done) {
                Throwable e = error;
                if (e != null) {
                    queue.clear();
                    s.onError(e);
                    return true;
                } else
                if (empty) {
                    s.onCompleted();
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Call the optional termination action at most once.
         */
        void doTerminate() {
            AtomicReference<Action0> ref = this.terminateOnce;
            if (ref != null) {
                Action0 a = ref.get();
                if (a != null && ref.compareAndSet(a, null)) {
                    a.call();
                }
            }
        }
    }
}