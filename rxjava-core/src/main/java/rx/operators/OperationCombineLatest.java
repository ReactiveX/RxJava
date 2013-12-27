/**
 * Copyright 2013 Netflix, Inc.
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
package rx.operators;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import rx.IObservable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Func4;
import rx.util.functions.Func5;
import rx.util.functions.Func6;
import rx.util.functions.Func7;
import rx.util.functions.Func8;
import rx.util.functions.Func9;
import rx.util.functions.FuncN;
import rx.util.functions.Functions;

/**
 * Returns an Observable that combines the emissions of multiple source observables. Once each
 * source Observable has emitted at least one item, combineLatest emits an item whenever any of
 * the source Observables emits an item, by combining the latest emissions from each source
 * Observable with a specified function.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/combineLatest.png">
 */
public class OperationCombineLatest {

    /**
     * Combines the two given observables, emitting an event containing an aggregation of the latest values of each of the source observables
     * each time an event is received from one of the source observables, where the aggregation is defined by the given function.
     * 
     * @param w0
     *            The first source observable.
     * @param w1
     *            The second source observable.
     * @param combineLatestFunction
     *            The aggregation function used to combine the source observable values.
     * @return A function from an observer to a subscription. This can be used to create an observable from.
     */
    public static <T0, T1, R> OnSubscribeFunc<R> combineLatest(IObservable<? extends T0> w0, IObservable<T1> w1, Func2<? super T0, ? super T1, ? extends R> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addObserver(new CombineObserver<R, T0>(a, w0));
        a.addObserver(new CombineObserver<R, T1>(a, w1));
        return a;
    }

    /**
     * @see #combineLatest(IObservable w0, IObservable w1, Func2 combineLatestFunction)
     */
    public static <T0, T1, T2, R> OnSubscribeFunc<R> combineLatest(IObservable<? extends T0> w0, IObservable<? extends T1> w1, IObservable<? extends T2> w2,
            Func3<? super T0, ? super T1, ? super T2, ? extends R> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addObserver(new CombineObserver<R, T0>(a, w0));
        a.addObserver(new CombineObserver<R, T1>(a, w1));
        a.addObserver(new CombineObserver<R, T2>(a, w2));
        return a;
    }

    /**
     * @see #combineLatest(IObservable w0, IObservable w1, Func2 combineLatestFunction)
     */
    public static <T0, T1, T2, T3, R> OnSubscribeFunc<R> combineLatest(IObservable<? extends T0> w0, IObservable<? extends T1> w1, IObservable<? extends T2> w2, IObservable<? extends T3> w3,
            Func4<? super T0, ? super T1, ? super T2, ? super T3, ? extends R> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addObserver(new CombineObserver<R, T0>(a, w0));
        a.addObserver(new CombineObserver<R, T1>(a, w1));
        a.addObserver(new CombineObserver<R, T2>(a, w2));
        a.addObserver(new CombineObserver<R, T3>(a, w3));
        return a;
    }

    /**
     * @see #combineLatest(IObservable w0, IObservable w1, Func2 combineLatestFunction)
     */
    public static <T0, T1, T2, T3, T4, R> OnSubscribeFunc<R> combineLatest(IObservable<? extends T0> w0, IObservable<? extends T1> w1, IObservable<? extends T2> w2, IObservable<? extends T3> w3, IObservable<? extends T4> w4,
            Func5<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? extends R> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addObserver(new CombineObserver<R, T0>(a, w0));
        a.addObserver(new CombineObserver<R, T1>(a, w1));
        a.addObserver(new CombineObserver<R, T2>(a, w2));
        a.addObserver(new CombineObserver<R, T3>(a, w3));
        a.addObserver(new CombineObserver<R, T4>(a, w4));
        return a;
    }

    /**
     * @see #combineLatest(IObservable w0, IObservable w1, Func2 combineLatestFunction)
     */
    public static <T0, T1, T2, T3, T4, T5, R> OnSubscribeFunc<R> combineLatest(IObservable<? extends T0> w0, IObservable<? extends T1> w1, IObservable<? extends T2> w2, IObservable<? extends T3> w3, IObservable<? extends T4> w4, IObservable<? extends T5> w5,
            Func6<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addObserver(new CombineObserver<R, T0>(a, w0));
        a.addObserver(new CombineObserver<R, T1>(a, w1));
        a.addObserver(new CombineObserver<R, T2>(a, w2));
        a.addObserver(new CombineObserver<R, T3>(a, w3));
        a.addObserver(new CombineObserver<R, T4>(a, w4));
        a.addObserver(new CombineObserver<R, T5>(a, w5));
        return a;
    }

    /**
     * @see #combineLatest(IObservable w0, IObservable w1, Func2 combineLatestFunction)
     */
    public static <T0, T1, T2, T3, T4, T5, T6, R> OnSubscribeFunc<R> combineLatest(IObservable<? extends T0> w0, IObservable<? extends T1> w1, IObservable<? extends T2> w2, IObservable<? extends T3> w3, IObservable<? extends T4> w4, IObservable<? extends T5> w5, IObservable<? extends T6> w6,
            Func7<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addObserver(new CombineObserver<R, T0>(a, w0));
        a.addObserver(new CombineObserver<R, T1>(a, w1));
        a.addObserver(new CombineObserver<R, T2>(a, w2));
        a.addObserver(new CombineObserver<R, T3>(a, w3));
        a.addObserver(new CombineObserver<R, T4>(a, w4));
        a.addObserver(new CombineObserver<R, T5>(a, w5));
        a.addObserver(new CombineObserver<R, T6>(a, w6));
        return a;
    }

    /**
     * @see #combineLatest(IObservable w0, IObservable w1, Func2 combineLatestFunction)
     */
    public static <T0, T1, T2, T3, T4, T5, T6, T7, R> OnSubscribeFunc<R> combineLatest(IObservable<? extends T0> w0, IObservable<? extends T1> w1, IObservable<? extends T2> w2, IObservable<? extends T3> w3, IObservable<? extends T4> w4, IObservable<? extends T5> w5, IObservable<? extends T6> w6, IObservable<? extends T7> w7,
            Func8<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addObserver(new CombineObserver<R, T0>(a, w0));
        a.addObserver(new CombineObserver<R, T1>(a, w1));
        a.addObserver(new CombineObserver<R, T2>(a, w2));
        a.addObserver(new CombineObserver<R, T3>(a, w3));
        a.addObserver(new CombineObserver<R, T4>(a, w4));
        a.addObserver(new CombineObserver<R, T5>(a, w5));
        a.addObserver(new CombineObserver<R, T6>(a, w6));
        a.addObserver(new CombineObserver<R, T7>(a, w7));
        return a;
    }

    /**
     * @see #combineLatest(IObservable w0, IObservable w1, Func2 combineLatestFunction)
     */
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, R> OnSubscribeFunc<R> combineLatest(IObservable<? extends T0> w0, IObservable<? extends T1> w1, IObservable<? extends T2> w2, IObservable<? extends T3> w3, IObservable<? extends T4> w4, IObservable<? extends T5> w5, IObservable<? extends T6> w6, IObservable<? extends T7> w7,
            IObservable<? extends T8> w8,
            Func9<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> combineLatestFunction) {
        Aggregator<R> a = new Aggregator<R>(Functions.fromFunc(combineLatestFunction));
        a.addObserver(new CombineObserver<R, T0>(a, w0));
        a.addObserver(new CombineObserver<R, T1>(a, w1));
        a.addObserver(new CombineObserver<R, T2>(a, w2));
        a.addObserver(new CombineObserver<R, T3>(a, w3));
        a.addObserver(new CombineObserver<R, T4>(a, w4));
        a.addObserver(new CombineObserver<R, T5>(a, w5));
        a.addObserver(new CombineObserver<R, T6>(a, w6));
        a.addObserver(new CombineObserver<R, T7>(a, w7));
        a.addObserver(new CombineObserver<R, T8>(a, w8));
        return a;
    }

    /* package accessible for unit tests */static class CombineObserver<R, T> implements Observer<T> {
        final IObservable<? extends T> w;
        final Aggregator<R> a;
        private Subscription subscription;

        public CombineObserver(Aggregator<R> a, IObservable<? extends T> w) {
            this.a = a;
            this.w = w;
        }

        private void startWatching() {
            if (subscription != null) {
                throw new RuntimeException("This should only be called once.");
            }
            subscription = w.subscribe(this);
        }

        @Override
        public void onCompleted() {
            a.complete(this);
        }

        @Override
        public void onError(Throwable e) {
            a.error(e);
        }

        @Override
        public void onNext(T args) {
            a.next(this, args);
        }
    }

    /**
     * Receive notifications from each of the observables we are reducing and execute the combineLatestFunction
     * whenever we have received an event from one of the observables, as soon as each Observable has received
     * at least one event.
     */
    /* package accessible for unit tests */static class Aggregator<R> implements OnSubscribeFunc<R> {

        private volatile Observer<R> observer;

        private final FuncN<? extends R> combineLatestFunction;
        private final AtomicBoolean running = new AtomicBoolean(true);

        // Stores how many observers have already completed
        private final AtomicInteger numCompleted = new AtomicInteger(0);

        /**
         * The latest value from each observer.
         */
        private final Map<CombineObserver<? extends R, ?>, Object> latestValue = new ConcurrentHashMap<CombineObserver<? extends R, ?>, Object>();

        /**
         * Ordered list of observers to combine.
         * No synchronization is necessary as these can not be added or changed asynchronously.
         */
        private final List<CombineObserver<R, ?>> observers = new LinkedList<CombineObserver<R, ?>>();

        public Aggregator(FuncN<? extends R> combineLatestFunction) {
            this.combineLatestFunction = combineLatestFunction;
        }

        /**
         * Receive notification of a Observer starting (meaning we should require it for aggregation)
         * 
         * @param w
         *            The observer to add.
         */
        <T> void addObserver(CombineObserver<R, T> w) {
            observers.add(w);
        }

        /**
         * Receive notification of a Observer completing its iterations.
         * 
         * @param w
         *            The observer that has completed.
         */
        <T> void complete(CombineObserver<? extends R, T> w) {
            int completed = numCompleted.incrementAndGet();
            // if all CombineObservers are completed, we mark the whole thing as completed
            if (completed == observers.size()) {
                if (running.get()) {
                    // mark ourselves as done
                    observer.onCompleted();
                    // just to ensure we stop processing in case we receive more onNext/complete/error calls after this
                    running.set(false);
                }
            }
        }

        /**
         * Receive error for a Observer. Throw the error up the chain and stop processing.
         */
        void error(Throwable e) {
            observer.onError(e);
            /* tell all observers to unsubscribe since we had an error */
            stop();
        }

        /**
         * Receive the next value from an observer.
         * <p>
         * If we have received values from all observers, trigger the combineLatest function, otherwise store the value and keep waiting.
         * 
         * @param w
         * @param arg
         */
        <T> void next(CombineObserver<? extends R, T> w, T arg) {
            if (observer == null) {
                throw new RuntimeException("This shouldn't be running if an Observer isn't registered");
            }

            /* if we've been 'unsubscribed' don't process anything further even if the things we're watching keep sending (likely because they are not responding to the unsubscribe call) */
            if (!running.get()) {
                return;
            }

            // remember this as the latest value for this observer
            latestValue.put(w, arg);

            if (latestValue.size() < observers.size()) {
                // we don't have a value yet for each observer to combine, so we don't have a combined value yet either
                return;
            }

            Object[] argsToCombineLatest = new Object[observers.size()];
            int i = 0;
            for (CombineObserver<R, ?> _w : observers) {
                argsToCombineLatest[i++] = latestValue.get(_w);
            }

            try {
                R combinedValue = combineLatestFunction.call(argsToCombineLatest);
                observer.onNext(combinedValue);
            } catch (Throwable ex) {
                observer.onError(ex);
            }
        }

        @Override
        public Subscription onSubscribe(Observer<? super R> observer) {
            if (this.observer != null) {
                throw new IllegalStateException("Only one Observer can subscribe to this Observable.");
            }

            SafeObservableSubscription subscription = new SafeObservableSubscription(new Subscription() {
                @Override
                public void unsubscribe() {
                    stop();
                }
            });
            this.observer = new SynchronizedObserver<R>(observer, subscription);

            /* start the observers */
            for (CombineObserver<R, ?> rw : observers) {
                rw.startWatching();
            }

            return subscription;
        }

        private void stop() {
            /* tell ourselves to stop processing onNext events */
            running.set(false);
            /* propogate to all observers to unsubscribe */
            for (CombineObserver<R, ?> rw : observers) {
                if (rw.subscription != null) {
                    rw.subscription.unsubscribe();
                }
            }
        }
    }
}
