/**
 * Copyright 2015 Netflix, Inc.
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Producer;
import rx.Subscriber;
import rx.Subscription;
import rx.annotations.Experimental;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action3;
import rx.functions.Func0;
import rx.functions.Func3;
import rx.internal.operators.BufferUntilSubscriber;
import rx.observers.SerializedObserver;
import rx.observers.Subscribers;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.BooleanSubscription;
;
/**
 * A utility class to create {@code OnSubscribe<T>} functions that respond correctly to back
 * pressure requests from subscribers. This is an improvement over
 * {@link rx.Observable#create(OnSubscribe) Observable.create(OnSubscribe)} which does not provide
 * any means of managing back pressure requests out-of-the-box. This variant of an OnSubscribe
 * function allows for the asynchronous processing of requests.
 *
 * @param <S>
 *            the type of the user-define state used in {@link #generateState() generateState(S)} ,
 *            {@link #next(Object, Long, Subscriber) next(S, Long, Subscriber)}, and
 *            {@link #onUnsubscribe(Object) onUnsubscribe(S)}.
 * @param <T>
 *            the type of {@code Subscribers} that will be compatible with {@code this}.
 */
@Experimental
public abstract class AsyncOnSubscribe<S, T> implements OnSubscribe<T> {

    /**
     * Executed once when subscribed to by a subscriber (via {@link OnSubscribe#call(Subscriber)})
     * to produce a state value. This value is passed into {@link #next(Object, long, Observer)
     * next(S state, Observer <T> observer)} on the first iteration. Subsequent iterations of
     * {@code next} will receive the state returned by the previous invocation of {@code next}.
     * 
     * @return the initial state value
     */
    protected abstract S generateState();

    /**
     * Called to produce data to the downstream subscribers. To emit data to a downstream subscriber
     * call {@code observer.onNext(t)}. To signal an error condition call
     * {@code observer.onError(throwable)} or throw an Exception. To signal the end of a data stream
     * call {@code observer.onCompleted()}. Implementations of this method must follow the following
     * rules.
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
     * @param requested
     *            the amount of data requested. An observable emitted to the observer should not
     *            exceed this amount.
     * @param observer
     *            the observer of data emitted by
     * @return the next iteration's state value
     */
    protected abstract S next(S state, long requested, Observer<Observable<? extends T>> observer);

    /**
     * Clean up behavior that is executed after the downstream subscriber's subscription is
     * unsubscribed. This method will be invoked exactly once.
     * 
     * @param state
     *            the last state value returned from {@code next(S, Long, Observer)} or
     *            {@code generateState()} at the time when a terminal event is emitted from
     *            {@link #next(Object, long, Observer)} or unsubscribing.
     */
    protected void onUnsubscribe(S state) {

    }

    /**
     * Generates a synchronous {@link AsyncOnSubscribe} that calls the provided {@code next}
     * function to generate data to downstream subscribers.
     * 
     * @param generator
     *            generates the initial state value (see {@link #generateState()})
     * @param next
     *            produces data to the downstream subscriber (see
     *            {@link #next(Object, long, Observer) next(S, long, Observer)})
     * @return an OnSubscribe that emits data in a protocol compatible with back-pressure.
     */
    @Experimental
    public static <S, T> OnSubscribe<T> createSingleState(Func0<? extends S> generator, 
            final Action3<? super S, Long, ? super Observer<Observable<? extends T>>> next) {
        Func3<S, Long, ? super Observer<Observable<? extends T>>, S> nextFunc = 
                new Func3<S, Long, Observer<Observable<? extends T>>, S>() {
                    @Override
                    public S call(S state, Long requested, Observer<Observable<? extends T>> subscriber) {
                        next.call(state, requested, subscriber);
                        return state;
                    }};
        return new AsyncOnSubscribeImpl<S, T>(generator, nextFunc);
    }

    /**
     * Generates a synchronous {@link AsyncOnSubscribe} that calls the provided {@code next}
     * function to generate data to downstream subscribers.
     * 
     * This overload creates a AsyncOnSubscribe without an explicit clean up step.
     * 
     * @param generator
     *            generates the initial state value (see {@link #generateState()})
     * @param next
     *            produces data to the downstream subscriber (see
     *            {@link #next(Object, long, Observer) next(S, long, Observer)})
     * @param onUnsubscribe
     *            clean up behavior (see {@link #onUnsubscribe(Object) onUnsubscribe(S)})
     * @return an OnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Experimental
    public static <S, T> OnSubscribe<T> createSingleState(Func0<? extends S> generator, 
            final Action3<? super S, Long, ? super Observer<Observable<? extends T>>> next, 
            final Action1<? super S> onUnsubscribe) {
        Func3<S, Long, Observer<Observable<? extends T>>, S> nextFunc = 
                new Func3<S, Long, Observer<Observable<? extends T>>, S>() {
                    @Override
                    public S call(S state, Long requested, Observer<Observable<? extends T>> subscriber) {
                        next.call(state, requested, subscriber);
                        return state;
                    }};
        return new AsyncOnSubscribeImpl<S, T>(generator, nextFunc, onUnsubscribe);
    }

    /**
     * Generates a synchronous {@link AsyncOnSubscribe} that calls the provided {@code next}
     * function to generate data to downstream subscribers.
     * 
     * @param generator
     *            generates the initial state value (see {@link #generateState()})
     * @param next
     *            produces data to the downstream subscriber (see
     *            {@link #next(Object, long, Observer) next(S, long, Observer)})
     * @param onUnsubscribe
     *            clean up behavior (see {@link #onUnsubscribe(Object) onUnsubscribe(S)})
     * @return an OnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Experimental
    public static <S, T> OnSubscribe<T> createStateful(Func0<? extends S> generator, 
            Func3<? super S, Long, ? super Observer<Observable<? extends T>>, ? extends S> next, 
            Action1<? super S> onUnsubscribe) {
        return new AsyncOnSubscribeImpl<S, T>(generator, next, onUnsubscribe);
    }

    /**
     * Generates a synchronous {@link AsyncOnSubscribe} that calls the provided {@code next}
     * function to generate data to downstream subscribers.
     * 
     * @param generator
     *            generates the initial state value (see {@link #generateState()})
     * @param next
     *            produces data to the downstream subscriber (see
     *            {@link #next(Object, long, Observer) next(S, long, Observer)})
     * @return an OnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Experimental
    public static <S, T> OnSubscribe<T> createStateful(Func0<? extends S> generator, 
            Func3<? super S, Long, ? super Observer<Observable<? extends T>>, ? extends S> next) {
        return new AsyncOnSubscribeImpl<S, T>(generator, next);
    }

    /**
     * Generates a synchronous {@link AsyncOnSubscribe} that calls the provided {@code next}
     * function to generate data to downstream subscribers.
     * 
     * This overload creates a "state-less" AsyncOnSubscribe which does not have an explicit state
     * value. This should be used when the {@code next} function closes over it's state.
     * 
     * @param next
     *            produces data to the downstream subscriber (see
     *            {@link #next(Object, long, Observer) next(S, long, Observer)})
     * @return an OnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Experimental
    public static <T> OnSubscribe<T> createStateless(final Action2<Long, ? super Observer<Observable<? extends T>>> next) {
        Func3<Void, Long, Observer<Observable<? extends T>>, Void> nextFunc = 
                new Func3<Void, Long, Observer<Observable<? extends T>>, Void>() {
                    @Override
                    public Void call(Void state, Long requested, Observer<Observable<? extends T>> subscriber) {
                        next.call(requested, subscriber);
                        return state;
                    }};
        return new AsyncOnSubscribeImpl<Void, T>(nextFunc);
    }

    /**
     * Generates a synchronous {@link AsyncOnSubscribe} that calls the provided {@code next}
     * function to generate data to downstream subscribers.
     * 
     * This overload creates a "state-less" AsyncOnSubscribe which does not have an explicit state
     * value. This should be used when the {@code next} function closes over it's state.
     * 
     * @param next
     *            produces data to the downstream subscriber (see
     *            {@link #next(Object, long, Observer) next(S, long, Observer)})
     * @param onUnsubscribe
     *            clean up behavior (see {@link #onUnsubscribe(Object) onUnsubscribe(S)})
     * @return an OnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Experimental
    public static <T> OnSubscribe<T> createStateless(final Action2<Long, ? super Observer<Observable<? extends T>>> next, 
            final Action0 onUnsubscribe) {
        Func3<Void, Long, Observer<Observable<? extends T>>, Void> nextFunc = 
                new Func3<Void, Long, Observer<Observable<? extends T>>, Void>() {
                    @Override
                    public Void call(Void state, Long requested, Observer<Observable<? extends T>> subscriber) {
                        next.call(requested, subscriber);
                        return null;
                    }};
        Action1<? super Void> wrappedOnUnsubscribe = new Action1<Void>() {
            @Override
            public void call(Void t) {
                onUnsubscribe.call();
            }};
        return new AsyncOnSubscribeImpl<Void, T>(nextFunc, wrappedOnUnsubscribe);
    }

    /**
     * An implementation of AsyncOnSubscribe that delegates
     * {@link AsyncOnSubscribe#next(Object, long, Observer)},
     * {@link AsyncOnSubscribe#generateState()}, and {@link AsyncOnSubscribe#onUnsubscribe(Object)}
     * to provided functions/closures.
     *
     * @param <S>
     *            the type of the user-defined state
     * @param <T>
     *            the type of compatible Subscribers
     */
    private static final class AsyncOnSubscribeImpl<S, T> extends AsyncOnSubscribe<S, T> {
        private final Func0<? extends S> generator;
        private final Func3<? super S, Long, ? super Observer<Observable<? extends T>>, ? extends S> next;
        private final Action1<? super S> onUnsubscribe;

        private AsyncOnSubscribeImpl(Func0<? extends S> generator, Func3<? super S, Long, ? super Observer<Observable<? extends T>>, ? extends S> next, Action1<? super S> onUnsubscribe) {
            this.generator = generator;
            this.next = next;
            this.onUnsubscribe = onUnsubscribe;
        }

        public AsyncOnSubscribeImpl(Func0<? extends S> generator, Func3<? super S, Long, ? super Observer<Observable<? extends T>>, ? extends S> next) {
            this(generator, next, null);
        }

        public AsyncOnSubscribeImpl(Func3<S, Long, Observer<Observable<? extends T>>, S> next, Action1<? super S> onUnsubscribe) {
            this(null, next, onUnsubscribe);
        }

        public AsyncOnSubscribeImpl(Func3<S, Long, Observer<Observable<? extends T>>, S> nextFunc) {
            this(null, nextFunc, null);
        }

        @Override
        protected S generateState() {
            return generator == null ? null : generator.call();
        }

        @Override
        protected S next(S state, long requested, Observer<Observable<? extends T>> observer) {
            return next.call(state, requested, observer);
        }

        @Override
        protected void onUnsubscribe(S state) {
            if (onUnsubscribe != null)
                onUnsubscribe.call(state);
        }
    }

    @Override
    public final void call(Subscriber<? super T> actualSubscriber) {
        S state = generateState();
        UnicastSubject<Observable<T>> subject = UnicastSubject.<Observable<T>> create();
        AsyncOuterSubscriber<S, T> outerSubscriberProducer = new AsyncOuterSubscriber<S, T>(this, state, subject);
        actualSubscriber.add(outerSubscriberProducer);
        Observable.concat(subject).unsafeSubscribe(Subscribers.wrap(actualSubscriber));
        actualSubscriber.setProducer(outerSubscriberProducer);
    }

    private static class AsyncOuterSubscriber<S, T> extends ConcurrentLinkedQueue<Long>implements Producer, Subscription, Observer<Observable<? extends T>> {
        /** */
        private static final long serialVersionUID = -7884904861928856832L;

        private volatile int isUnsubscribed;
        @SuppressWarnings("rawtypes")
        private static final AtomicIntegerFieldUpdater<AsyncOuterSubscriber> IS_UNSUBSCRIBED = AtomicIntegerFieldUpdater.newUpdater(AsyncOuterSubscriber.class, "isUnsubscribed");

        private final AsyncOnSubscribe<S, T> parent;
        private final SerializedObserver<Observable<? extends T>> serializedSubscriber;
        private final Set<Subscription> subscriptions = new HashSet<Subscription>();

        private boolean hasTerminated = false;
        private boolean onNextCalled = false;

        private S state;

        private final UnicastSubject<Observable<T>> merger;

        public AsyncOuterSubscriber(AsyncOnSubscribe<S, T> parent, S initialState, UnicastSubject<Observable<T>> merger) {
            this.parent = parent;
            this.serializedSubscriber = new SerializedObserver<Observable<? extends T>>(this);
            this.state = initialState;
            this.merger = merger;
        }

        @Override
        public void unsubscribe() {
            if (IS_UNSUBSCRIBED.compareAndSet(this, 0, 1)) {
                // it's safe to process terminal behavior
                if (isEmpty()) {
                    parent.onUnsubscribe(state);
                }
                for (Subscription s : subscriptions) {
                    if (!s.isUnsubscribed()) {
                        s.unsubscribe();
                    }
                }
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return isUnsubscribed != 0;
        }

        public void nextIteration(long requestCount) {
            state = parent.next(state, requestCount, serializedSubscriber);
        }

        @Override
        public void request(long n) {
            int size = 0;
            Long r;
            synchronized (this) {
                size = size();
                add(n);
                r = n;
            }
            if (size == 0) {
                do {
                    // check if unsubscribed before doing any work
                    if (isUnsubscribed()) {
                        unsubscribe();
                        return;
                    }
                    // otherwise try one iteration for a request of `numRequested` elements
                    try {
                        onNextCalled = false;
                        nextIteration(r);
                        if (onNextCalled)
                            r = poll();
                        if (hasTerminated || isUnsubscribed()) {
                            parent.onUnsubscribe(state);
                        }
                    } catch (Throwable ex) {
                        handleThrownError(parent, state, ex);
                        return;
                    }
                } while (r != null && !hasTerminated);
            }
        }

        private void handleThrownError(final AsyncOnSubscribe<S, T> p, S st, Throwable ex) {
            if (hasTerminated) {
                RxJavaPlugins.getInstance().getErrorHandler().handleError(ex);
            } else {
                hasTerminated = true;
                merger.onError(ex);
                unsubscribe();
            }
        }

        @Override
        public void onCompleted() {
            if (hasTerminated) {
                throw new IllegalStateException("Terminal event already emitted.");
            }
            hasTerminated = true;
            merger.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            if (hasTerminated) {
                throw new IllegalStateException("Terminal event already emitted.");
            }
            hasTerminated = true;
            merger.onError(e);
        }

        // This exists simply to check if the subscription has already been
        // terminated before getting access to the subscription
        private static Subscription SUBSCRIPTION_SENTINEL = new BooleanSubscription();

        @Override
        public void onNext(final Observable<? extends T> t) {
            if (onNextCalled) {
                throw new IllegalStateException("onNext called multiple times!");
            }
            onNextCalled = true;
            if (hasTerminated)
                return;
            subscribeBufferToObservable(t);
        }

        private void subscribeBufferToObservable(final Observable<? extends T> t) {
            BufferUntilSubscriber<T> buffer = BufferUntilSubscriber.<T> create();
            final AtomicReference<Subscription> holder = new AtomicReference<Subscription>(null);
            Subscription innerSubscription = t
                .doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        if (!holder.compareAndSet(null, SUBSCRIPTION_SENTINEL)) {
                            Subscription h = holder.get();
                            subscriptions.remove(h);
                        }
                    }})
                .subscribe(buffer);

            if (holder.compareAndSet(null, innerSubscription)) {
                subscriptions.add(innerSubscription);
            }
            merger.onNext(buffer);
        }
    }

    private static final class UnicastSubject<T> extends Observable<T>implements Observer<T> {
        public static <T> UnicastSubject<T> create() {
            return new UnicastSubject<T>(new State<T>());
        }

        private State<T> state;

        protected UnicastSubject(final State<T> state) {
            super(new OnSubscribe<T>() {
                @Override
                public void call(Subscriber<? super T> s) {
                    if (state.subscriber != null) {
                        s.onError(new IllegalStateException("There can be only one subscriber"));
                    } else {
                        state.subscriber = s;
                    }
                }
            });
            this.state = state;
        }

        @Override
        public void onCompleted() {
            state.subscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            state.subscriber.onError(e);
        }

        @Override
        public void onNext(T t) {
            state.subscriber.onNext(t);
        }

        private static class State<T> {
            private Subscriber<? super T> subscriber;
        }
    }
}
