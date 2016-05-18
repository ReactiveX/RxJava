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

import java.util.concurrent.atomic.AtomicLong;

import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Producer;
import rx.Subscriber;
import rx.Subscription;
import rx.annotations.Beta;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func2;
import rx.internal.operators.BackpressureUtils;
import rx.plugins.RxJavaPlugins;

/**
 * A utility class to create {@code OnSubscribe<T>} functions that respond correctly to back
 * pressure requests from subscribers. This is an improvement over
 * {@link rx.Observable#create(OnSubscribe) Observable.create(OnSubscribe)} which does not provide
 * any means of managing back pressure requests out-of-the-box.
 *
 * @param <S>
 *            the type of the user-define state used in {@link #generateState() generateState(S)} ,
 *            {@link #next(Object, Observer) next(S, Subscriber)}, and
 *            {@link #onUnsubscribe(Object) onUnsubscribe(S)}.
 * @param <T>
 *            the type of {@code Subscribers} that will be compatible with {@code this}.
 */
@Beta
public abstract class SyncOnSubscribe<S, T> implements OnSubscribe<T> {
    
    /* (non-Javadoc)
     * @see rx.functions.Action1#call(java.lang.Object)
     */
    @Override
    public final void call(final Subscriber<? super T> subscriber) {
        S state;
        
        try {
            state = generateState();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            subscriber.onError(e);
            return;
        }
        
        SubscriptionProducer<S, T> p = new SubscriptionProducer<S, T>(subscriber, this, state);
        subscriber.add(p);
        subscriber.setProducer(p);
    }

    /**
     * Executed once when subscribed to by a subscriber (via {@link #call(Subscriber)})
     * to produce a state value. This value is passed into {@link #next(Object, Observer) next(S
     * state, Observer <T> observer)} on the first iteration. Subsequent iterations of {@code next}
     * will receive the state returned by the previous invocation of {@code next}.
     * 
     * @return the initial state value
     */
    protected abstract S generateState();

    /**
     * Called to produce data to the downstream subscribers. To emit data to a downstream subscriber
     * call {@code observer.onNext(t)}. To signal an error condition call
     * {@code observer.onError(throwable)} or throw an Exception. To signal the end of a data stream
     * call {@code 
     * observer.onCompleted()}. Implementations of this method must follow the following rules.
     * 
     * <ul>
     * <li>Must not call {@code observer.onNext(t)} more than 1 time per invocation.</li>
     * <li>Must not call {@code observer.onNext(t)} concurrently.</li>
     * </ul>
     * 
     * The value returned from an invocation of this method will be passed in as the {@code state}
     * argument of the next invocation of this method.
     * 
     * @param state
     *            the state value (from {@link #generateState()} on the first invocation or the
     *            previous invocation of this method.
     * @param observer
     *            the observer of data emitted by
     * @return the next iteration's state value
     */
    protected abstract S next(S state, Observer<? super T> observer);

    /**
     * Clean up behavior that is executed after the downstream subscriber's subscription is
     * unsubscribed. This method will be invoked exactly once.
     * 
     * @param state
     *            the last state value prior from {@link #generateState()} or
     *            {@link #next(Object, Observer) next(S, Observer&lt;T&gt;)} before unsubscribe.
     */
    protected void onUnsubscribe(S state) {

    }

    /**
     * Generates a synchronous {@link SyncOnSubscribe} that calls the provided {@code next} function
     * to generate data to downstream subscribers.
     * 
     * @param <T> the type of the generated values
     * @param <S> the type of the associated state with each Subscriber
     * @param generator
     *            generates the initial state value (see {@link #generateState()})
     * @param next
     *            produces data to the downstream subscriber (see {@link #next(Object, Observer)
     *            next(S, Subscriber)})
     * @return a SyncOnSubscribe that emits data in a protocol compatible with back-pressure.
     */
    @Beta
    public static <S, T> SyncOnSubscribe<S, T> createSingleState(Func0<? extends S> generator, 
            final Action2<? super S, ? super Observer<? super T>> next) {
        Func2<S, ? super Observer<? super T>, S> nextFunc = new Func2<S, Observer<? super T>, S>() {
            @Override
            public S call(S state, Observer<? super T> subscriber) {
                next.call(state, subscriber);
                return state;
            }
        };
        return new SyncOnSubscribeImpl<S, T>(generator, nextFunc);
    }

    /**
     * Generates a synchronous {@link SyncOnSubscribe} that calls the provided {@code next} function
     * to generate data to downstream subscribers.
     * 
     * This overload creates a SyncOnSubscribe without an explicit clean up step.
     * 
     * @param <T> the type of the generated values
     * @param <S> the type of the associated state with each Subscriber
     * @param generator
     *            generates the initial state value (see {@link #generateState()})
     * @param next
     *            produces data to the downstream subscriber (see {@link #next(Object, Observer)
     *            next(S, Subscriber)})
     * @param onUnsubscribe
     *            clean up behavior (see {@link #onUnsubscribe(Object) onUnsubscribe(S)})
     * @return a SyncOnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Beta
    public static <S, T> SyncOnSubscribe<S, T> createSingleState(Func0<? extends S> generator, 
            final Action2<? super S, ? super Observer<? super T>> next, 
            final Action1<? super S> onUnsubscribe) {
        Func2<S, Observer<? super T>, S> nextFunc = new Func2<S, Observer<? super T>, S>() {
            @Override
            public S call(S state, Observer<? super T> subscriber) {
                next.call(state, subscriber);
                return state;
            }
        };
        return new SyncOnSubscribeImpl<S, T>(generator, nextFunc, onUnsubscribe);
    }

    /**
     * Generates a synchronous {@link SyncOnSubscribe} that calls the provided {@code next} function
     * to generate data to downstream subscribers.
     * 
     * @param <T> the type of the generated values
     * @param <S> the type of the associated state with each Subscriber
     * @param generator
     *            generates the initial state value (see {@link #generateState()})
     * @param next
     *            produces data to the downstream subscriber (see {@link #next(Object, Observer)
     *            next(S, Subscriber)})
     * @param onUnsubscribe
     *            clean up behavior (see {@link #onUnsubscribe(Object) onUnsubscribe(S)})
     * @return a SyncOnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Beta
    public static <S, T> SyncOnSubscribe<S, T> createStateful(Func0<? extends S> generator, 
            Func2<? super S, ? super Observer<? super T>, ? extends S> next, 
            Action1<? super S> onUnsubscribe) {
        return new SyncOnSubscribeImpl<S, T>(generator, next, onUnsubscribe);
    }
    
    /**
     * Generates a synchronous {@link SyncOnSubscribe} that calls the provided {@code next} function
     * to generate data to downstream subscribers.
     * 
     * @param <T> the type of the generated values
     * @param <S> the type of the associated state with each Subscriber
     * @param generator
     *            generates the initial state value (see {@link #generateState()})
     * @param next
     *            produces data to the downstream subscriber (see {@link #next(Object, Observer)
     *            next(S, Subscriber)})
     * @return a SyncOnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Beta
    public static <S, T> SyncOnSubscribe<S, T> createStateful(Func0<? extends S> generator, 
            Func2<? super S, ? super Observer<? super T>, ? extends S> next) {
        return new SyncOnSubscribeImpl<S, T>(generator, next);
    }

    /**
     * Generates a synchronous {@link SyncOnSubscribe} that calls the provided {@code next} function
     * to generate data to downstream subscribers.
     * 
     * This overload creates a "state-less" SyncOnSubscribe which does not have an explicit state
     * value. This should be used when the {@code next} function closes over it's state.
     * 
     * @param <T> the type of the generated values
     * @param next
     *            produces data to the downstream subscriber (see {@link #next(Object, Observer)
     *            next(S, Subscriber)})
     * @return a SyncOnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Beta
    public static <T> SyncOnSubscribe<Void, T> createStateless(final Action1<? super Observer<? super T>> next) {
        Func2<Void, Observer<? super T>, Void> nextFunc = new Func2<Void, Observer<? super T>, Void>() {
            @Override
            public Void call(Void state, Observer<? super T> subscriber) {
                next.call(subscriber);
                return state;
            }
        };
        return new SyncOnSubscribeImpl<Void, T>(nextFunc);
    }

    /**
     * Generates a synchronous {@link SyncOnSubscribe} that calls the provided {@code next} function
     * to generate data to downstream subscribers.
     * 
     * This overload creates a "state-less" SyncOnSubscribe which does not have an explicit state
     * value. This should be used when the {@code next} function closes over it's state.
     * 
     * @param <T> the type of the generated values
     * @param next
     *            produces data to the downstream subscriber (see {@link #next(Object, Observer)
     *            next(S, Subscriber)})
     * @param onUnsubscribe
     *            clean up behavior (see {@link #onUnsubscribe(Object) onUnsubscribe(S)})
     * @return a SyncOnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Beta
    public static <T> SyncOnSubscribe<Void, T> createStateless(final Action1<? super Observer<? super T>> next, 
            final Action0 onUnsubscribe) {
        Func2<Void, Observer<? super T>, Void> nextFunc = new Func2<Void, Observer<? super T>, Void>() {
            @Override
            public Void call(Void state, Observer<? super T> subscriber) {
                next.call(subscriber);
                return null;
            }
        };
        Action1<? super Void> wrappedOnUnsubscribe = new Action1<Void>(){
            @Override
            public void call(Void t) {
                onUnsubscribe.call();
            }};
        return new SyncOnSubscribeImpl<Void, T>(nextFunc, wrappedOnUnsubscribe);
    }

    /**
     * An implementation of SyncOnSubscribe that delegates
     * {@link SyncOnSubscribe#next(Object, Subscriber)}, {@link SyncOnSubscribe#generateState()},
     * and {@link SyncOnSubscribe#onUnsubscribe(Object)} to provided functions/closures.
     *
     * @param <S>
     *            the type of the user-defined state
     * @param <T>
     *            the type of compatible Subscribers
     */
    private static final class SyncOnSubscribeImpl<S, T> extends SyncOnSubscribe<S, T> {
        private final Func0<? extends S> generator;
        private final Func2<? super S, ? super Observer<? super T>, ? extends S> next;
        private final Action1<? super S> onUnsubscribe;

        SyncOnSubscribeImpl(Func0<? extends S> generator, Func2<? super S, ? super Observer<? super T>, ? extends S> next, Action1<? super S> onUnsubscribe) {
            this.generator = generator;
            this.next = next;
            this.onUnsubscribe = onUnsubscribe;
        }

        public SyncOnSubscribeImpl(Func0<? extends S> generator, Func2<? super S, ? super Observer<? super T>, ? extends S> next) {
            this(generator, next, null);
        }

        public SyncOnSubscribeImpl(Func2<S, Observer<? super T>, S> next, Action1<? super S> onUnsubscribe) {
            this(null, next, onUnsubscribe);
        }

        public SyncOnSubscribeImpl(Func2<S, Observer<? super T>, S> nextFunc) {
            this(null, nextFunc, null);
        }

        @Override
        protected S generateState() {
            return generator == null ? null : generator.call();
        }

        @Override
        protected S next(S state, Observer<? super T> observer) {
            return next.call(state, observer);
        }

        @Override
        protected void onUnsubscribe(S state) {
            if (onUnsubscribe != null)
                onUnsubscribe.call(state);
        }
    }

    /**
     * Contains the producer loop that reacts to downstream requests of work.
     *
     * @param <T>
     *            the type of compatible Subscribers
     */
    private static class SubscriptionProducer<S, T> 
    extends AtomicLong implements Producer, Subscription, Observer<T> {
        /** */
        private static final long serialVersionUID = -3736864024352728072L;
        private final Subscriber<? super T> actualSubscriber;
        private final SyncOnSubscribe<S, T> parent;
        private boolean onNextCalled;
        private boolean hasTerminated;
        
        private S state;

        SubscriptionProducer(final Subscriber<? super T> subscriber, SyncOnSubscribe<S, T> parent, S state) {
            this.actualSubscriber = subscriber;
            this.parent = parent;
            this.state = state;
        }

        @Override
        public boolean isUnsubscribed() {
            return get() < 0L;
        }
        
        @Override
        public void unsubscribe() {
            while(true) {
                long requestCount = get();
                if (compareAndSet(0L, -1L)) {
                    doUnsubscribe();
                    return;
                }
                else if (compareAndSet(requestCount, -2L))
                    // the loop is iterating concurrently
                    // need to check if requestCount == -1
                    // and unsub if so after loop iteration
                    return;
            }
        }
        
        private boolean tryUnsubscribe() {
            // only one thread at a time can iterate over request count
            // therefore the requestCount atomic cannot be decrement concurrently here
            // safe to set to -1 atomically (since this check can only be done by 1 thread)
            if (hasTerminated || get() < -1) {
                set(-1);
                doUnsubscribe();
                return true;
            }
            return false;
        }

        private void doUnsubscribe() {
            try {
                parent.onUnsubscribe(state);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
            }
        }

        @Override
        public void request(long n) {
            if (n > 0 && BackpressureUtils.getAndAddRequest(this, n) == 0L) {
                if (n == Long.MAX_VALUE) {
                    fastpath();
                } else {
                    slowPath(n);
                }
            }
        }

        private void fastpath() {
            final SyncOnSubscribe<S, T> p = parent;
            Subscriber<? super T> a = actualSubscriber;
            
            for (;;) {
                try {
                    onNextCalled = false;
                    nextIteration(p);
                } catch (Throwable ex) {
                    handleThrownError(a, ex);
                    return;
                }
                if (tryUnsubscribe()) {
                    return;
                }
            }
        }

        private void handleThrownError(Subscriber<? super T> a, Throwable ex) {
            if (hasTerminated) {
                RxJavaPlugins.getInstance().getErrorHandler().handleError(ex);
            } else {
                hasTerminated = true;
                a.onError(ex);
                unsubscribe();
            }
        }

        private void slowPath(long n) {
            final SyncOnSubscribe<S, T> p = parent;
            Subscriber<? super T> a = actualSubscriber;
            long numRequested = n;
            for (;;) {
                long numRemaining = numRequested;
                do {
                    try {
                        onNextCalled = false;
                        nextIteration(p);
                    } catch (Throwable ex) {
                        handleThrownError(a, ex);
                        return;
                    }
                    if (tryUnsubscribe()) {
                        return;
                    }
                    if (onNextCalled)
                        numRemaining--;
                } while (numRemaining != 0L);
                numRequested = addAndGet(-numRequested);
                if (numRequested <= 0L)
                    break;
            }
            // catches cases where unsubscribe is called before decrementing atomic request count
            tryUnsubscribe();
        }

        private void nextIteration(final SyncOnSubscribe<S, T> parent) {
            state = parent.next(state, this);
        }
        
        @Override
        public void onCompleted() {
            if (hasTerminated) {
                throw new IllegalStateException("Terminal event already emitted.");
            }
            hasTerminated = true;
            if (!actualSubscriber.isUnsubscribed()) {
                actualSubscriber.onCompleted();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (hasTerminated) {
                throw new IllegalStateException("Terminal event already emitted.");
            }
            hasTerminated = true;
            if (!actualSubscriber.isUnsubscribed()) {
                actualSubscriber.onError(e);
            }
        }

        @Override
        public void onNext(T value) {
            if (onNextCalled) {
                throw new IllegalStateException("onNext called multiple times!");
            }
            onNextCalled = true;
            actualSubscriber.onNext(value);
        }
    }

    
}