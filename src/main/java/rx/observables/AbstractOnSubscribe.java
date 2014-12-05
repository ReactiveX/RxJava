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

package rx.observables;

import java.util.Arrays;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.annotations.Experimental;
import rx.exceptions.CompositeException;

/**
 * Abstract base class for the OnSubscribe interface that supports building
 * observable sources one onNext at a time and automatically supports
 * unsubscription and backpressure.
 * <p>
 * 
 * @param <T> the value type
 * @param <S> the per-subscriber user-defined state type
 */
@Experimental
public abstract class AbstractOnSubscribe<T, S> implements OnSubscribe<T> {
    /**
     * Called when a Subscriber subscribes and let's the implementor
     * create a per-subscriber custom state.
     * <p>
     * Override this method to have custom state per subscriber.
     * The default implementation returns {@code null}.
     * @param subscriber the subscriber who is subscribing
     * @return the custom state
     */
    protected S onSubscribe(Subscriber<? super T> subscriber) {
        return null;
    }
    /**
     * Called after the terminal emission or when the downstream unsubscribes.
     * <p>
     * This is called only once and it is made sure no onNext call runs concurrently with it.
     * The default implementation does nothing.
     * @param state the user-provided state
     */
    protected void onTerminated(S state) {
        
    }
    /**
     * Override this method and create an emission state-machine.
     * @param state the per-subscriber subscription state.
     */
    protected abstract void next(SubscriptionState<T, S> state);

    @Override
    public final void call(final Subscriber<? super T> subscriber) {
        final S custom = onSubscribe(subscriber);
        final SubscriptionState<T, S> state = new SubscriptionState<T, S>(this, subscriber, custom);
        subscriber.add(state);
        subscriber.setProducer(new SubscriptionProducer<T, S>(state));
    }
    
    /**
     * Convenience method to create an observable from the implemented instance
     * @return the created observable
     */
    public final Observable<T> toObservable() {
        return Observable.create(this);
    }
    /**
     * Contains the producer loop that reacts to downstream requests of work.
     * @param <T> the value type
     * @param <S> the per-subscriber user-defined state type
     */
    private static final class SubscriptionProducer<T, S> implements Producer {
        final SubscriptionState<T, S> state;
        private SubscriptionProducer(SubscriptionState<T, S> state) {
            this.state = state;
        }
        @Override
        public void request(long n) {
            if (n == Long.MAX_VALUE) {
                for (; !state.subscriber.isUnsubscribed(); ) {
                    if (state.use()) {
                        try {
                            state.parent.next(state);
                            state.calls++;
                            if (state.accept()) {
                                state.terminate();
                                break;
                            }
                        } catch (Throwable t) {
                            state.terminate();
                            state.subscriber.onError(t);
                            break;
                        } finally {
                            state.free();
                        }
                    } else {
                        break;
                    }
                }
            } else 
            if (n > 0 && state.requestCount.getAndAdd(n) == 0) {
                if (!state.subscriber.isUnsubscribed()) {
                    do {
                        if (state.use()) {
                            try {
                                state.parent.next(state);
                                state.calls++;
                                if (state.accept()) {
                                    state.terminate();
                                    break;
                                }
                            } catch (Throwable t) {
                                state.terminate();
                                state.subscriber.onError(t);
                                break;
                            } finally {
                                state.free();
                            }
                        } else {
                            break;
                        }
                    } while (state.requestCount.decrementAndGet() > 0 && !state.subscriber.isUnsubscribed());
                }
            }
        }
    }
    /**
     * Represents a per-subscription state for the AbstractOnSubscribe operation.
     * It supports phasing and counts the number of times a value was requested
     * by the downstream.
     * @param <T> the value type 
     * @param <S> the per-subscriber user-defined state type
     */
    public static final class SubscriptionState<T, S> implements Subscription {
        private final AbstractOnSubscribe<T, S> parent;
        private final Subscriber<? super T> subscriber;
        private final S state;
        private final AtomicLong requestCount;
        private final AtomicInteger inUse;
        private int phase;
        private long calls;
        private T theValue;
        private boolean hasOnNext;
        private boolean hasCompleted;
        private Throwable theException;
        private SubscriptionState(AbstractOnSubscribe<T, S> parent, Subscriber<? super T> subscriber, S state) {
            this.parent = parent;
            this.subscriber = subscriber;
            this.state = state;
            this.requestCount = new AtomicLong();
            this.inUse = new AtomicInteger(1);
        }
        /**
         * @return the per-subscriber specific user-defined state created via AbstractOnSubscribe.onSubscribe.
         */
        public S state() {
            return state;
        }
        /**
         * @return the current phase value
         */
        public int phase() {
            return phase;
        }
        /**
         * Sets a new phase value.
         * @param newPhase
         */
        public void phase(int newPhase) {
            phase = newPhase;
        }
        /**
         * Advance the current phase by 1.
         */
        public void advancePhase() {
            advancePhaseBy(1);
        }
        /**
         * Advance the current phase by the given amount (can be negative).
         * @param amount the amount to advance the phase
         */
        public void advancePhaseBy(int amount) {
            phase += amount;
        }
        /**
         * @return the number of times AbstractOnSubscribe.next was called so far, starting at 0
         * for the very first call.
         */
        public long calls() {
            return calls;
        }
        /**
         * Call this method to offer the next onNext value for the subscriber.
         * <p>
         * Throws IllegalStateException if there is a value already offered but not taken or
         * a terminal state is reached.
         * @param value the value to onNext
         */
        public void onNext(T value) {
            if (hasOnNext) {
                throw new IllegalStateException("onNext not consumed yet!");
            } else
            if (hasCompleted) {
                throw new IllegalStateException("Already terminated", theException);
            }
            theValue = value;
            hasOnNext = true;
        }
        /**
         * Call this method to send an onError to the subscriber and terminate
         * all further activities. If there is an onNext even not taken, that
         * value is emitted to the subscriber followed by this exception.
         * <p>
         * Throws IllegalStateException if the terminal state has been reached already.
         * @param e the exception to deliver to the client
         */
        public void onError(Throwable e) {
            if (e == null) {
                throw new NullPointerException("e != null required");
            }
            if (hasCompleted) {
                throw new IllegalStateException("Already terminated", theException);
            }
            theException = e;
            hasCompleted = true;
        }
        /**
         * Call this method to send an onCompleted to the subscriber and terminate
         * all further activities. If there is an onNext even not taken, that
         * value is emitted to the subscriber followed by this exception.
         * <p>
         * Throws IllegalStateException if the terminal state has been reached already.
         * @param e the exception to deliver to the client
         */
        public void onCompleted() {
            if (hasCompleted) {
                throw new IllegalStateException("Already terminated", theException);
            }
            hasCompleted = true;
        }
        /**
         * Emits the onNextValue and/or the terminal value to the actual subscriber.
         * @return true if the event was a terminal event
         */
        protected boolean accept() {
            if (hasOnNext) {
                T value = theValue;
                theValue = null;
                hasOnNext = false;
                
                try {
                    subscriber.onNext(value);
                } catch (Throwable t) {
                    hasCompleted = true;
                    Throwable e = theException;
                    theException = null;
                    if (e == null) {
                        subscriber.onError(t);
                    } else {
                        subscriber.onError(new CompositeException(Arrays.asList(t, e)));
                    }
                    return true;
                }
            }
            if (hasCompleted) {
                Throwable e = theException;
                theException = null;
                
                if (e != null) {
                    subscriber.onError(e);
                } else {
                    subscriber.onCompleted();
                }
                return true;
            }
            return false;
        }
        /**
         * Request the state to be used by onNext or returns false if
         * the downstream has unsubscribed.
         * @return true if the state can be used exclusively
         */
        protected boolean use() {
            int i = inUse.get();
            if (i == 0) {
                return false;
            } else
            if (i == 1 && inUse.compareAndSet(1, 2)) {
                return true;
            }
            throw new IllegalStateException("This is not reentrant nor threadsafe!");
        }
        /**
         * Release the state if there are no more interest in it and
         * is not in use.
         */
        protected void free() {
            int i = inUse.get();
            if (i <= 0) {
                return;
            } else
            if (inUse.decrementAndGet() == 0) {
                parent.onTerminated(state);
            }
        }
        /**
         * Terminates the state immediately and calls
         * onTerminated with the custom state.
         */
        protected void terminate() {
            for (;;) {
                int i = inUse.get();
                if (i <= 0) {
                    return;
                }
                if (inUse.compareAndSet(i, 0)) {
                    parent.onTerminated(state);
                    break;
                }
            }
        }
        @Override
        public boolean isUnsubscribed() {
            return inUse.get() <= 0;
        }
        @Override
        public void unsubscribe() {
            free();
        }
    }
}
