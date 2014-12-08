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
 * Abstract base class for the OnSubscribe interface that helps building
 * observable sources one onNext at a time and automatically supports
 * unsubscription and backpressure.
 * <p>
 * <h1>Usage rules</h1>
 * Implementors of the {@code next()} method
 * <ul>
 * <li>should either
 *   <ul>
 *   <li>create the next value and signal it via {@code state.onNext()},</li>
 *   <li>signal a terminal condition via {@code state.onError()} or {@code state.onCompleted()} or</li>
 *   <li>signal a stop condition via {@code state.stop()} indicating no further values will be sent.</li>
 *   </ul>
 * </li>
 * <li>may
 *   <ul>
 *   <li>call {@code state.onNext()} and either {@code state.onError()} or {@code state.onCompleted()} together and
 *   <li>block or sleep.
 *   </ul>
 * </li>
 * <li>should not
 *   <ul>
 *   <li>do nothing or do async work and not produce any event or request stopping. If neither of 
 *   the methods are called, an {@code IllegalStateException} is forwarded to the {@code Subscriber} and
 *   the Observable is terminated;</li>
 *   <li>call the {@code state.onXYZ} methods more than once (yields {@code IllegalStateException}).</li>
 *   </ul>
 * </li>
 * </ul>
 * 
 * The {@code SubscriptionState} object features counters that may help implement a state machine:
 * <ul>
 * <li>A call counter, accessible via {@code state.calls()} that tells how many times 
 * the {@code next()} was run (zero based).
 * <li>A phase counter, accessible via {@code state.phase()} that helps track the current emission 
 * phase and may be used in a {@code switch ()} statement to implement the state machine. 
 * (It was named phase to avoid confusion with the per-subscriber state.)</li>
 * <li>The current phase can be arbitrarily changed via {@code state.advancePhase()}, 
 * {@code state.advancePhaseBy(int)} and {@code state.phase(int)}.</li>
 * 
 * </ul>
 * <p>
 * The implementors of the {@code AbstractOnSubscribe} may override the {@code onSubscribe} to perform
 * special actions (such as registering {@code Subscription}s with {@code Subscriber.add()}) and return additional state for each subscriber subscribing. This custom state is
 * accessible through the {@code state.state()} method. If the custom state requires some form of cleanup,
 * the {@code onTerminated} method can be overridden.
 * <p>
 * For convenience, lambda-accepting static factory methods, named {@code create()}, are available. Another
 * convenience is the {@code toObservable} which turns an {@code AbstractOnSubscribe} instance into an {@code Observable} fluently.
 * 
 * <h1>Examples</h1>
 * Note: the examples use the lambda-helper factories to avoid boilerplane.
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

 * @param <T> the value type
 * @param <S> the per-subscriber user-defined state type
 */
@Experimental
public abstract class AbstractOnSubscribe<T, S> implements OnSubscribe<T> {
    /**
     * Called when a Subscriber subscribes and let's the implementor
     * create a per-subscriber custom state.
     * <p>
     * Override this method to have custom state per-subscriber.
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
        subscriber.add(new SubscriptionCompleter<T, S>(state));
        subscriber.setProducer(new SubscriptionProducer<T, S>(state));
    }
    
    /**
     * Convenience method to create an observable from the implemented instance
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
     * Creates an AbstractOnSubscribe instance which calls the provided {@code next} action.
     * <p>
     * This is a convenience method to help create AbstractOnSubscribe instances with the
     * help of lambdas.
     * @param <T> the value type
     * @param <S> the per-subscriber user-defined state type
     * @param next the next action to call
     * @return an AbstractOnSubscribe instance
     */
    public static <T, S> AbstractOnSubscribe<T, S> create(Action1<SubscriptionState<T, S>> next) {
        @SuppressWarnings("unchecked")
        Func1<? super Subscriber<? super T>, ? extends S> nullFunc =
                (Func1<? super Subscriber<? super T>, ? extends S>)NULL_FUNC1;
        return create(next, nullFunc, Actions.empty());
    }
    /**
     * Creates an AbstractOnSubscribe instance which creates a custom state with the
     * {@code onSubscribe} function and calls the provided {@code next} action.
     * <p>
     * This is a convenience method to help create AbstractOnSubscribe instances with the
     * help of lambdas.
     * @param <T> the value type
     * @param <S> the per-subscriber user-defined state type
     * @param next the next action to call
     * @param onSubscribe the function that returns a per-subscriber state to be used by next
     * @return an AbstractOnSubscribe instance
     */
    public static <T, S> AbstractOnSubscribe<T, S> create(Action1<SubscriptionState<T, S>> next,
            Func1<? super Subscriber<? super T>, ? extends S> onSubscribe) {
        return create(next, onSubscribe, Actions.empty());
    }
    /**
     * Creates an AbstractOnSubscribe instance which creates a custom state with the
     * {@code onSubscribe} function, calls the provided {@code next} action and
     * calls the {@code onTerminated} action to release the state when its no longer needed.
     * <p>
     * This is a convenience method to help create AbstractOnSubscribe instances with the
     * help of lambdas.
     * @param <T> the value type
     * @param <S> the per-subscriber user-defined state type
     * @param next the next action to call
     * @param onSubscribe the function that returns a per-subscriber state to be used by next
     * @param onTerminated the action to call to release the state created by the onSubscribe function
     * @return an AbstractOnSubscribe instance
     */
    public static <T, S> AbstractOnSubscribe<T, S> create(Action1<SubscriptionState<T, S>> next,
            Func1<? super Subscriber<? super T>, ? extends S> onSubscribe, Action1<? super S> onTerminated) {
        return new LambdaOnSubscribe<T, S>(next, onSubscribe, onTerminated);
    }
    /**
     * An implementation that forwards the 3 main methods to functional callbacks.
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
     * @param <T> the value type
     * @param <S> the per-subscriber user-defined state type
     */
    private static final class SubscriptionCompleter<T, S> extends AtomicBoolean implements Subscription {
        /** */
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
     * Represents a per-subscription state for the AbstractOnSubscribe operation.
     * It supports phasing and counts the number of times a value was requested
     * by the downstream.
     * @param <T> the value type 
     * @param <S> the per-subscriber user-defined state type
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
         * Signals that there won't be any further events.
         */
        public void stop() {
            stopRequested = true;
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
         * Verify if the next() generated an event or requested a stop.
         * @return true if either event was generated or stop was requested
         */
        protected boolean verify() {
            return hasOnNext || hasCompleted || stopRequested;
        }
        /** @returns true if the next() requested a stop. */
        protected boolean stopRequested() {
            return stopRequested;
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
    }
}
