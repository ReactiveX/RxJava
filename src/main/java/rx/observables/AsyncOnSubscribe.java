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

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.*;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.annotations.Experimental;
import rx.functions.*;
import rx.internal.operators.BufferUntilSubscriber;
import rx.observers.SerializedObserver;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.CompositeSubscription;

/**
 * A utility class to create {@code OnSubscribe<T>} functions that respond correctly to back
 * pressure requests from subscribers. This is an improvement over
 * {@link rx.Observable#create(OnSubscribe) Observable.create(OnSubscribe)} which does not provide
 * any means of managing back pressure requests out-of-the-box. This variant of an OnSubscribe
 * function allows for the asynchronous processing of requests.
 *
 * @param <S>
 *            the type of the user-define state used in {@link #generateState() generateState(S)} ,
 *            {@link #next(Object, long, Observer) next(S, Long, Observer)}, and
 *            {@link #onUnsubscribe(Object) onUnsubscribe(S)}.
 * @param <T>
 *            the type of {@code Subscribers} that will be compatible with {@code this}.
 */
@Experimental
public abstract class AsyncOnSubscribe<S, T> implements OnSubscribe<T> {

    /**
     * Executed once when subscribed to by a subscriber (via {@link #call(Subscriber)})
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
     * @param <T> the type of the generated values
     * @param <S> the type of the associated state with each Subscriber
     * @param generator
     *            generates the initial state value (see {@link #generateState()})
     * @param next
     *            produces data to the downstream subscriber (see
     *            {@link #next(Object, long, Observer) next(S, long, Observer)})
     * @return an AsyncOnSubscribe that emits data in a protocol compatible with back-pressure.
     */
    @Experimental
    public static <S, T> AsyncOnSubscribe<S, T> createSingleState(Func0<? extends S> generator, 
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
     * @param <T> the type of the generated values
     * @param <S> the type of the associated state with each Subscriber
     * @param generator
     *            generates the initial state value (see {@link #generateState()})
     * @param next
     *            produces data to the downstream subscriber (see
     *            {@link #next(Object, long, Observer) next(S, long, Observer)})
     * @param onUnsubscribe
     *            clean up behavior (see {@link #onUnsubscribe(Object) onUnsubscribe(S)})
     * @return an AsyncOnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Experimental
    public static <S, T> AsyncOnSubscribe<S, T> createSingleState(Func0<? extends S> generator, 
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
     * @param <T> the type of the generated values
     * @param <S> the type of the associated state with each Subscriber
     * @param generator
     *            generates the initial state value (see {@link #generateState()})
     * @param next
     *            produces data to the downstream subscriber (see
     *            {@link #next(Object, long, Observer) next(S, long, Observer)})
     * @param onUnsubscribe
     *            clean up behavior (see {@link #onUnsubscribe(Object) onUnsubscribe(S)})
     * @return an AsyncOnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Experimental
    public static <S, T> AsyncOnSubscribe<S, T> createStateful(Func0<? extends S> generator, 
            Func3<? super S, Long, ? super Observer<Observable<? extends T>>, ? extends S> next, 
            Action1<? super S> onUnsubscribe) {
        return new AsyncOnSubscribeImpl<S, T>(generator, next, onUnsubscribe);
    }

    /**
     * Generates a synchronous {@link AsyncOnSubscribe} that calls the provided {@code next}
     * function to generate data to downstream subscribers.
     * 
     * @param <T> the type of the generated values
     * @param <S> the type of the associated state with each Subscriber
     * @param generator
     *            generates the initial state value (see {@link #generateState()})
     * @param next
     *            produces data to the downstream subscriber (see
     *            {@link #next(Object, long, Observer) next(S, long, Observer)})
     * @return an AsyncOnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Experimental
    public static <S, T> AsyncOnSubscribe<S, T> createStateful(Func0<? extends S> generator, 
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
     * @param <T> the type of the generated values
     * @param next
     *            produces data to the downstream subscriber (see
     *            {@link #next(Object, long, Observer) next(S, long, Observer)})
     * @return an AsyncOnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Experimental
    public static <T> AsyncOnSubscribe<Void, T> createStateless(final Action2<Long, ? super Observer<Observable<? extends T>>> next) {
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
     * @param <T> the type of the generated values
     * @param next
     *            produces data to the downstream subscriber (see
     *            {@link #next(Object, long, Observer) next(S, long, Observer)})
     * @param onUnsubscribe
     *            clean up behavior (see {@link #onUnsubscribe(Object) onUnsubscribe(S)})
     * @return an AsyncOnSubscribe that emits data downstream in a protocol compatible with
     *         back-pressure.
     */
    @Experimental
    public static <T> AsyncOnSubscribe<Void, T> createStateless(final Action2<Long, ? super Observer<Observable<? extends T>>> next, 
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

        AsyncOnSubscribeImpl(Func0<? extends S> generator, Func3<? super S, Long, ? super Observer<Observable<? extends T>>, ? extends S> next, Action1<? super S> onUnsubscribe) {
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
    public final void call(final Subscriber<? super T> actualSubscriber) {
        S state;
        try {
            state = generateState();
        } catch (Throwable ex) {
            actualSubscriber.onError(ex);
            return;
        }
        UnicastSubject<Observable<T>> subject = UnicastSubject.<Observable<T>> create();
        
        final AsyncOuterManager<S, T> outerProducer = new AsyncOuterManager<S, T>(this, state, subject);
        
        Subscriber<T> concatSubscriber = new Subscriber<T>() {
            @Override
            public void onNext(T t) {
                actualSubscriber.onNext(t);
            }
            
            @Override
            public void onError(Throwable e) {
                actualSubscriber.onError(e);
            }
            
            @Override
            public void onCompleted() {
                actualSubscriber.onCompleted();
            }
            
            @Override
            public void setProducer(Producer p) {
                outerProducer.setConcatProducer(p);
            }
        };
        
        subject.onBackpressureBuffer().concatMap(new Func1<Observable<T>, Observable<T>>() {
            @Override
            public Observable<T> call(Observable<T> v) {
                return v.onBackpressureBuffer();
            }
        }).unsafeSubscribe(concatSubscriber);
        
        actualSubscriber.add(concatSubscriber);
        actualSubscriber.add(outerProducer);
        actualSubscriber.setProducer(outerProducer);

    }

    static final class AsyncOuterManager<S, T> implements Producer, Subscription, Observer<Observable<? extends T>> {

        final AtomicBoolean isUnsubscribed;

        private final AsyncOnSubscribe<S, T> parent;
        private final SerializedObserver<Observable<? extends T>> serializedSubscriber;
        final CompositeSubscription subscriptions = new CompositeSubscription();

        private boolean hasTerminated;
        private boolean onNextCalled;

        private S state;

        private final UnicastSubject<Observable<T>> merger;
        
        boolean emitting;
        List<Long> requests;
        Producer concatProducer;
        
        long expectedDelivery;

        public AsyncOuterManager(AsyncOnSubscribe<S, T> parent, S initialState, UnicastSubject<Observable<T>> merger) {
            this.parent = parent;
            this.serializedSubscriber = new SerializedObserver<Observable<? extends T>>(this);
            this.state = initialState;
            this.merger = merger;
            this.isUnsubscribed = new AtomicBoolean();
        }

        @Override
        public void unsubscribe() {
            if (isUnsubscribed.compareAndSet(false, true)) {
                synchronized (this) {
                    if (emitting) {
                        requests = new ArrayList<Long>();
                        requests.add(0L);
                        return;
                    }
                    emitting = true;
                }
                cleanup();
            }
        }

        void setConcatProducer(Producer p) {
            if (concatProducer != null) {
                throw new IllegalStateException("setConcatProducer may be called at most once!");
            }
            concatProducer = p;
        }
        
        @Override
        public boolean isUnsubscribed() {
            return isUnsubscribed.get();
        }

        public void nextIteration(long requestCount) {
            state = parent.next(state, requestCount, serializedSubscriber);
        }
        
        void cleanup() {
            subscriptions.unsubscribe();
            try {
                parent.onUnsubscribe(state);
            } catch (Throwable ex) {
                handleThrownError(ex);
            }
        }

        @Override
        public void request(long n) {
            if (n == 0) {
                return;
            }
            if (n < 0) {
                throw new IllegalStateException("Request can't be negative! " + n);
            }
            boolean quit = false;
            synchronized (this) {
                if (emitting) {
                    List<Long> q = requests;
                    if (q == null) {
                        q = new ArrayList<Long>();
                        requests = q;
                    }
                    q.add(n);
                    
                    quit = true; 
                } else {
                    emitting = true;
                }
            }
            
            concatProducer.request(n);
            
            if (quit) {
                return;
            }
            
            if (tryEmit(n)) {
                return;
            }
            for (;;) {
                List<Long> q;
                synchronized (this) {
                    q = requests;
                    if (q == null) {
                        emitting = false;
                        return;
                    }
                    requests = null;
                }
                
                for (long r : q) {
                    if (tryEmit(r)) {
                        return;
                    }
                }
            }
        }

        /**
         * Called when a source has produced less than its provision (completed prematurely); this will trigger the generation of another
         * source that will hopefully emit the missing amount.
         * @param n the missing amount to produce via a new source.
         */
        public void requestRemaining(long n) {
            if (n == 0) {
                return;
            }
            if (n < 0) {
                throw new IllegalStateException("Request can't be negative! " + n);
            }
            synchronized (this) {
                if (emitting) {
                    List<Long> q = requests;
                    if (q == null) {
                        q = new ArrayList<Long>();
                        requests = q;
                    }
                    q.add(n);
                    
                    return;
                }
                emitting = true;
            }
            
            if (tryEmit(n)) {
                return;
            }
            for (;;) {
                List<Long> q;
                synchronized (this) {
                    q = requests;
                    if (q == null) {
                        emitting = false;
                        return;
                    }
                    requests = null;
                }
                
                for (long r : q) {
                    if (tryEmit(r)) {
                        return;
                    }
                }
            }
        }

        boolean tryEmit(long n) {
            if (isUnsubscribed()) {
                cleanup();
                return true;
            }
            
            try {
                onNextCalled = false;
                expectedDelivery = n;
                nextIteration(n);
                
                if (hasTerminated || isUnsubscribed()) {
                    cleanup();
                    return true;
                }
                if (!onNextCalled) {
                    handleThrownError(new IllegalStateException("No events emitted!"));
                    return true;
                }
            } catch (Throwable ex) {
                handleThrownError(ex);
                return true;
            }
            return false;
        }

        private void handleThrownError(Throwable ex) {
            if (hasTerminated) {
                RxJavaPlugins.getInstance().getErrorHandler().handleError(ex);
            } else {
                hasTerminated = true;
                merger.onError(ex);
                cleanup();
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

        @SuppressWarnings("unchecked")
        private void subscribeBufferToObservable(final Observable<? extends T> t) {
            final BufferUntilSubscriber<T> buffer = BufferUntilSubscriber.<T> create();

            final long expected = expectedDelivery;
            final Subscriber<T> s = new Subscriber<T>() {
                long remaining = expected;
                @Override
                public void onNext(T t) {
                    remaining--;
                    buffer.onNext(t);
                }
                @Override
                public void onError(Throwable e) {
                    buffer.onError(e);
                }
                @Override
                public void onCompleted() {
                    buffer.onCompleted();
                    long r = remaining;
                    if (r > 0) {
                        requestRemaining(r);
                    }
                }
            };
            subscriptions.add(s);

            Observable<? extends T> doOnTerminate = t.doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        subscriptions.remove(s);
                    }});
            
            ((Observable<T>)doOnTerminate).subscribe(s);

            merger.onNext(buffer);
        }
    }

    static final class UnicastSubject<T> extends Observable<T>implements Observer<T> {
        public static <T> UnicastSubject<T> create() {
            return new UnicastSubject<T>(new State<T>());
        }

        private State<T> state;

        protected UnicastSubject(final State<T> state) {
            super(state);
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

        static final class State<T> implements OnSubscribe<T> {
            Subscriber<? super T> subscriber;
            @Override
            public void call(Subscriber<? super T> s) {
                synchronized (this) {
                    if (subscriber == null) {
                        subscriber = s;
                        return;
                    }
                }
                s.onError(new IllegalStateException("There can be only one subscriber"));
            }
        }
    }
}
