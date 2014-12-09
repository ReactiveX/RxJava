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
import rx.functions.*;

/**
 * Abstract base class for the {@link OnSubscribe} interface that helps you build Observable sources one
 * {@code onNext} at a time, and automatically supports unsubscription and backpressure.
 * <p>
 * <h1>Usage rules</h1>
 * When you implement the {@code next()} method, you
 * <ul>
 *  <li>should either
 *   <ul>
 *   <li>create the next value and signal it via {@link SubscriptionState#onNext state.onNext()},</li>
 *   <li>signal a terminal condition via {@link SubscriptionState#onError state.onError()}, or
 *       {@link SubscriptionState#onCompleted state.onCompleted()}, or</li>
 *   <li>signal a stop condition via {@link SubscriptionState#stop state.stop()} indicating no further values
 *       will be sent.</li>
 *   </ul>
 *  </li>
 *  <li>may
 *   <ul>
 *   <li>call {@link SubscriptionState#onNext state.onNext()} and either
 *       {@link SubscriptionState#onError state.onError()} or
 *       {@link SubscriptionState#onCompleted state.onCompleted()} together, and
 *   <li>block or sleep.
 *   </ul>
 *  </li>
 *  <li>should not
 *   <ul>
 *   <li>do nothing or do async work and not produce any event or request stopping. If neither of 
 *       the methods are called, an {@link IllegalStateException} is forwarded to the {@code Subscriber} and
 *       the Observable is terminated;</li>
 *   <li>call the {@code state.on}<i>foo</i>() methods more than once (yields
 *       {@link IllegalStateException}).</li>
 *   </ul>
 *  </li>
 * </ul>
 * 
 * The {@link SubscriptionState} object features counters that may help implement a state machine:
 * <ul>
 * <li>A call counter, accessible via {@link SubscriptionState#calls state.calls()} tells how many times the
 *     {@code next()} was run (zero based).</li>
 * <li>You can use a phase counter, accessible via {@link SubscriptionState#phase state.phase}, that helps track
 *     the current emission phase, in a {@code switch()} statement to implement the state machine. (It is named
 *     {@code phase} to avoid confusion with the per-subscriber state.)</li>
 * <li>You can arbitrarily change the current phase with
 *     {@link SubscriptionState#advancePhase state.advancePhase()}, 
 *     {@link SubscriptionState#advancePhaseBy(int) state.advancedPhaseBy(int)} and
 *     {@link SubscriptionState#phase(int) state.phase(int)}.</li>
 * </ul>
 * <p>
 * When you implement {@code AbstractOnSubscribe}, you may override {@link AbstractOnSubscribe#onSubscribe} to
 * perform special actions (such as registering {@code Subscription}s with {@code Subscriber.add()}) and return
 * additional state for each subscriber subscribing. You can access this custom state with the
 * {@link SubscriptionState#state state.state()} method. If you need to do some cleanup, you can override the
 * {@link #onTerminated} method.
 * <p>
 * For convenience, a lambda-accepting static factory method, {@link #create}, is available.
 * Another convenience is {@link #toObservable} which turns an {@code AbstractOnSubscribe}
 * instance into an {@code Observable} fluently.
 * 
 * <h1>Examples</h1>
 * Note: these examples use the lambda-helper factories to avoid boilerplane.
 * 
 * <h3>Implement: just</h3>
 * <pre><code>
 * AbstractOnSubscribe.create(s -> {
 *   s.onNext(1);
 *   s.onCompleted();
 * }).toObservable().subscribe(System.out::println);
 * </code></pre>

 * <h3>Implement: from Iterable</h3>
 * <pre><code>
 * Iterable<T> iterable = ...;
 * AbstractOnSubscribe.create(s -> {
 *   Iterator<T> it = s.state();
 *   if (it.hasNext()) {
 *     s.onNext(it.next());
 *   }
 *   if (!it.hasNext()) {
 *     s.onCompleted();
 *   }
 * }, u -> iterable.iterator()).subscribe(System.out::println);
 * </code></pre>
 *
 * <h3>Implement source that fails a number of times before succeeding</h3>
 * <pre><code>
 * AtomicInteger fails = new AtomicInteger();
 * int numFails = 50;
 * AbstractOnSubscribe.create(s -> {
 *   long c = s.calls();
 *   switch (s.phase()) {
 *   case 0:
 *     s.onNext("Beginning");
 *     s.onError(new RuntimeException("Oh, failure.");
 *     if (c == numFails.getAndIncrement()) {
 *       s.advancePhase();
 *     }
 *     break;
 *   case 1:
 *     s.onNext("Beginning");
 *     s.advancePhase();
 *   case 2:
 *     s.onNext("Finally working");
 *     s.onCompleted();
 *     s.advancePhase();
 *   default:
 *     throw new IllegalStateException("How did we get here?");
 *   }
 * }).subscribe(System.out::println);
 * </code></pre>

 * <h3>Implement: never</h3>
 * <pre><code>
 * AbstractOnSubscribe.create(s -> {
 *   s.stop();
 * }).toObservable()
 * .timeout(1, TimeUnit.SECONDS)
 * .subscribe(System.out::println, Throwable::printStacktrace, () -> System.out.println("Done"));
 * </code></pre>
 *
 * @param <T> the value type
 * @param <S> the per-subscriber user-defined state type
 * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
 * @Experimental
 */
@Experimental
public abstract class AbstractOnSubscribe<T, S> implements OnSubscribe<T> {
    /**
     * Called when a Subscriber subscribes and lets the implementor create a per-subscriber custom state.
     * <p>
     * Override this method to have custom state per-subscriber. The default implementation returns
     * {@code null}.
     *
     * @param subscriber the subscriber who is subscribing
     * @return the custom state
     */
    protected S onSubscribe(Subscriber<? super T> subscriber) {
        return null;
    }

    /**
     * Called after the terminal emission or when the downstream unsubscribes.
     * <p>
     * This is called only once and no {@code onNext} call will run concurrently with it. The default
     * implementation does nothing.
     *
     * @param state the user-provided state
     */
    protected void onTerminated(S state) {
        
    }

    /**
     * Override this method to create an emission state-machine.
     *
     * @param state the per-subscriber subscription state
     */
    protected abstract void next(SubscriptionState<T, S> state);

    @Override
    public final void call(final Subscriber<? super T> subscriber) {
        final S custom = onSubscribe(subscriber);
        final SubscriptionState<T, S> state = new SubscriptionState<T, S>(this, subscriber, custom);
        subscriber.add(new SubscriptionCompleter<T, S>(state));
        subscriber.setProducer(new SubscriptionProducer<T, S>(state));
    }
    
    /**
     * Convenience method to create an Observable from this implemented instance.
     *
     * @return the created observable
     */
    public final Observable<T> toObservable() {
        return Observable.create(this);
    }

    /** Function that returns null. */
    private static final Func1<Object, Object> NULL_FUNC1 = new Func1<Object, Object>() {
        @Override
        public Object call(Object t1) {
            return null;
        }
    };
    
    /**
     * Creates an {@code AbstractOnSubscribe} instance which calls the provided {@code next} action.
     * <p>
     * This is a convenience method to help create {@code AbstractOnSubscribe} instances with the help of
     * lambdas.
     *
     * @param <T> the value type
     * @param <S> the per-subscriber user-defined state type
     * @param next the next action to call
     * @return an {@code AbstractOnSubscribe} instance
     */
    public static <T, S> AbstractOnSubscribe<T, S> create(Action1<SubscriptionState<T, S>> next) {
        @SuppressWarnings("unchecked")
        Func1<? super Subscriber<? super T>, ? extends S> nullFunc =
                (Func1<? super Subscriber<? super T>, ? extends S>)NULL_FUNC1;
        return create(next, nullFunc, Actions.empty());
    }

    /**
     * Creates an {@code AbstractOnSubscribe} instance which creates a custom state with the {@code onSubscribe}
     * function and calls the provided {@code next} action.
     * <p>
     * This is a convenience method to help create {@code AbstractOnSubscribe} instances with the help of
     * lambdas.
     *
     * @param <T> the value type
     * @param <S> the per-subscriber user-defined state type
     * @param next the next action to call
     * @param onSubscribe the function that returns a per-subscriber state to be used by {@code next}
     * @return an {@code AbstractOnSubscribe} instance
     */
    public static <T, S> AbstractOnSubscribe<T, S> create(Action1<SubscriptionState<T, S>> next,
            Func1<? super Subscriber<? super T>, ? extends S> onSubscribe) {
        return create(next, onSubscribe, Actions.empty());
    }

    /**
     * Creates an {@code AbstractOnSubscribe} instance which creates a custom state with the {@code onSubscribe}
     * function, calls the provided {@code next} action and calls the {@code onTerminated} action to release the
     * state when its no longer needed.
     * <p>
     * This is a convenience method to help create {@code AbstractOnSubscribe} instances with the help of
     * lambdas.
     *
     * @param <T> the value type
     * @param <S> the per-subscriber user-defined state type
     * @param next the next action to call
     * @param onSubscribe the function that returns a per-subscriber state to be used by {@code next}
     * @param onTerminated the action to call to release the state created by the {@code onSubscribe} function
     * @return an {@code AbstractOnSubscribe} instance
     */
    public static <T, S> AbstractOnSubscribe<T, S> create(Action1<SubscriptionState<T, S>> next,
            Func1<? super Subscriber<? super T>, ? extends S> onSubscribe, Action1<? super S> onTerminated) {
        return new LambdaOnSubscribe<T, S>(next, onSubscribe, onTerminated);
    }

    /**
     * An implementation that forwards the three main methods ({@code next}, {@code onSubscribe}, and
     * {@code onTermianted}) to functional callbacks.
     *
     * @param <T> the value type
     * @param <S> the per-subscriber user-defined state type
     */
    private static final class LambdaOnSubscribe<T, S> extends AbstractOnSubscribe<T, S> {
        final Action1<SubscriptionState<T, S>> next;
        final Func1<? super Subscriber<? super T>, ? extends S> onSubscribe;
        final Action1<? super S> onTerminated;
        private LambdaOnSubscribe(Action1<SubscriptionState<T, S>> next,
                Func1<? super Subscriber<? super T>, ? extends S> onSubscribe, Action1<? super S> onTerminated) {
            this.next = next;
            this.onSubscribe = onSubscribe;
            this.onTerminated = onTerminated;
        }
        @Override
        protected S onSubscribe(Subscriber<? super T> subscriber) {
            return onSubscribe.call(subscriber);
        }
        @Override
        protected void onTerminated(S state) {
            onTerminated.call(state);
        }
        @Override
        protected void next(SubscriptionState<T, S> state) {
            next.call(state);
        }
    }

    /**
     * Manages unsubscription of the state.
     *
     * @param <T> the value type
     * @param <S> the per-subscriber user-defined state type
     */
    private static final class SubscriptionCompleter<T, S> extends AtomicBoolean implements Subscription {
        private static final long serialVersionUID = 7993888274897325004L;
        private final SubscriptionState<T, S> state;
        private SubscriptionCompleter(SubscriptionState<T, S> state) {
            this.state = state;
        }
        @Override
        public boolean isUnsubscribed() {
            return get();
        }
        @Override
        public void unsubscribe() {
            if (compareAndSet(false, true)) {
                state.free();
            }
        }

    }
    /**
     * Contains the producer loop that reacts to downstream requests of work.
     *
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
                    if (!doNext()) {
                        break;
                    }
                }
            } else 
            if (n > 0 && state.requestCount.getAndAdd(n) == 0) {
                if (!state.subscriber.isUnsubscribed()) {
                    do {
                        if (!doNext()) {
                            break;
                        }
                    } while (state.requestCount.decrementAndGet() > 0 && !state.subscriber.isUnsubscribed());
                }
            }
        }

        /**
         * Executes the user-overridden next() method and performs state bookkeeping and
         * verification.
         *
         * @return true if the outer loop may continue
         */
        protected boolean doNext() {
            if (state.use()) {
                try {
                    int p = state.phase();
                    state.parent.next(state);
                    if (!state.verify()) {
                        throw new IllegalStateException("No event produced or stop called @ Phase: " + p + " -> " + state.phase() + ", Calls: " + state.calls());
                    }
                    if (state.accept() || state.stopRequested()) {
                        state.terminate();
                        return false;
                    }
                    state.calls++;
                } catch (Throwable t) {
                    state.terminate();
                    state.subscriber.onError(t);
                    return false;
                } finally {
                    state.free();
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Represents a per-subscription state for the {@code AbstractOnSubscribe} operation. It supports phasing
     * and counts the number of times a value was requested by the downstream.
     *
     * @param <T> the value type 
     * @param <S> the per-subscriber user-defined state type
     * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
     * @Experimental
     */
    public static final class SubscriptionState<T, S> {
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
        private boolean stopRequested;
        private Throwable theException;
        private SubscriptionState(AbstractOnSubscribe<T, S> parent, Subscriber<? super T> subscriber, S state) {
            this.parent = parent;
            this.subscriber = subscriber;
            this.state = state;
            this.requestCount = new AtomicLong();
            this.inUse = new AtomicInteger(1);
        }

        /**
         * @return the per-subscriber specific user-defined state created via
         *         {@link AbstractOnSubscribe#onSubscribe}
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
         *
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
         *
         * @param amount the amount to advance the phase
         */
        public void advancePhaseBy(int amount) {
            phase += amount;
        }

        /**
         * @return the number of times {@link AbstractOnSubscribe#next} was called so far, starting at 0 for the
         *         very first call
         */
        public long calls() {
            return calls;
        }

        /**
         * Call this method to offer the next {@code onNext} value for the subscriber.
         * 
         * @param value the value to {@code onNext}
         * @throws IllegalStateException if there is a value already offered but not taken or a terminal state
         *         is reached
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
         * Call this method to send an {@code onError} to the subscriber and terminate all further activities.
         * If there is a pending {@code onNext}, that value is emitted to the subscriber followed by this
         * exception.
         * 
         * @param e the exception to deliver to the client
         * @throws IllegalStateException if the terminal state has been reached already
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
         * Call this method to send an {@code onCompleted} to the subscriber and terminate all further
         * activities. If there is a pending {@code onNext}, that value is emitted to the subscriber followed by
         * this exception.
         * 
         * @throws IllegalStateException if the terminal state has been reached already
         */
        public void onCompleted() {
            if (hasCompleted) {
                throw new IllegalStateException("Already terminated", theException);
            }
            hasCompleted = true;
        }

        /**
         * Signals that there won't be any further events.
         */
        public void stop() {
            stopRequested = true;
        }

        /**
         * Emits the {@code onNext} and/or the terminal value to the actual subscriber.
         *
         * @return {@code true} if the event was a terminal event
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
         * Verify if the {@code next()} generated an event or requested a stop.
         *
         * @return true if either event was generated or stop was requested
         */
        protected boolean verify() {
            return hasOnNext || hasCompleted || stopRequested;
        }

        /** @return true if the {@code next()} requested a stop */
        protected boolean stopRequested() {
            return stopRequested;
        }

        /**
         * Request the state to be used by {@code onNext} or returns {@code false} if the downstream has
         * unsubscribed.
         *
         * @return {@code true} if the state can be used exclusively
         * @throws IllegalStateEception
         * @warn "throws" section incomplete
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
         * Release the state if there are no more interest in it and it is not in use.
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
         * Terminates the state immediately and calls {@link AbstractOnSubscribe#onTerminated} with the custom
         * state.
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
    }
}
