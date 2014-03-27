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
package rx.operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.functions.Func5;
import rx.functions.Func6;
import rx.functions.Func7;
import rx.functions.Func8;
import rx.functions.Func9;
import rx.functions.FuncN;
import rx.functions.Functions;
import rx.subscriptions.CompositeSubscription;

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
    @SuppressWarnings("unchecked")
    public static <T0, T1, R> OnSubscribeFunc<R> combineLatest(Observable<? extends T0> w0, Observable<T1> w1, Func2<? super T0, ? super T1, ? extends R> combineLatestFunction) {
        return new CombineLatest<Object, R>(Arrays.asList(w0, w1), Functions.fromFunc(combineLatestFunction));
    }

    /**
     * @see #combineLatest(Observable w0, Observable w1, Func2 combineLatestFunction)
     */
    @SuppressWarnings("unchecked")
    public static <T0, T1, T2, R> OnSubscribeFunc<R> combineLatest(Observable<? extends T0> w0, Observable<? extends T1> w1, Observable<? extends T2> w2,
            Func3<? super T0, ? super T1, ? super T2, ? extends R> combineLatestFunction) {
        return new CombineLatest<Object, R>(Arrays.asList(w0, w1, w2), Functions.fromFunc(combineLatestFunction));
    }

    /**
     * @see #combineLatest(Observable w0, Observable w1, Func2 combineLatestFunction)
     */
    @SuppressWarnings("unchecked")
    public static <T0, T1, T2, T3, R> OnSubscribeFunc<R> combineLatest(Observable<? extends T0> w0, Observable<? extends T1> w1, Observable<? extends T2> w2, Observable<? extends T3> w3,
            Func4<? super T0, ? super T1, ? super T2, ? super T3, ? extends R> combineLatestFunction) {
        return new CombineLatest<Object, R>(Arrays.asList(w0, w1, w2, w3), Functions.fromFunc(combineLatestFunction));
    }

    /**
     * @see #combineLatest(Observable w0, Observable w1, Func2 combineLatestFunction)
     */
    @SuppressWarnings("unchecked")
    public static <T0, T1, T2, T3, T4, R> OnSubscribeFunc<R> combineLatest(Observable<? extends T0> w0, Observable<? extends T1> w1, Observable<? extends T2> w2, Observable<? extends T3> w3, Observable<? extends T4> w4,
            Func5<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? extends R> combineLatestFunction) {
        return new CombineLatest<Object, R>(Arrays.asList(w0, w1, w2, w3, w4), Functions.fromFunc(combineLatestFunction));
    }

    /**
     * @see #combineLatest(Observable w0, Observable w1, Func2 combineLatestFunction)
     */
    @SuppressWarnings("unchecked")
    public static <T0, T1, T2, T3, T4, T5, R> OnSubscribeFunc<R> combineLatest(Observable<? extends T0> w0, Observable<? extends T1> w1, Observable<? extends T2> w2, Observable<? extends T3> w3, Observable<? extends T4> w4, Observable<? extends T5> w5,
            Func6<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> combineLatestFunction) {
        return new CombineLatest<Object, R>(Arrays.asList(w0, w1, w2, w3, w4, w5), Functions.fromFunc(combineLatestFunction));
    }

    /**
     * @see #combineLatest(Observable w0, Observable w1, Func2 combineLatestFunction)
     */
    @SuppressWarnings("unchecked")
    public static <T0, T1, T2, T3, T4, T5, T6, R> OnSubscribeFunc<R> combineLatest(Observable<? extends T0> w0, Observable<? extends T1> w1, Observable<? extends T2> w2, Observable<? extends T3> w3, Observable<? extends T4> w4, Observable<? extends T5> w5, Observable<? extends T6> w6,
            Func7<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> combineLatestFunction) {
        return new CombineLatest<Object, R>(Arrays.asList(w0, w1, w2, w3, w4, w5, w6), Functions.fromFunc(combineLatestFunction));
    }

    /**
     * @see #combineLatest(Observable w0, Observable w1, Func2 combineLatestFunction)
     */
    @SuppressWarnings("unchecked")
    public static <T0, T1, T2, T3, T4, T5, T6, T7, R> OnSubscribeFunc<R> combineLatest(Observable<? extends T0> w0, Observable<? extends T1> w1, Observable<? extends T2> w2, Observable<? extends T3> w3, Observable<? extends T4> w4, Observable<? extends T5> w5, Observable<? extends T6> w6, Observable<? extends T7> w7,
            Func8<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> combineLatestFunction) {
        return new CombineLatest<Object, R>(Arrays.asList(w0, w1, w2, w3, w4, w5, w6, w7), Functions.fromFunc(combineLatestFunction));
    }

    /**
     * @see #combineLatest(Observable w0, Observable w1, Func2 combineLatestFunction)
     */
    @SuppressWarnings("unchecked")
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, R> OnSubscribeFunc<R> combineLatest(Observable<? extends T0> w0, Observable<? extends T1> w1, Observable<? extends T2> w2, Observable<? extends T3> w3, Observable<? extends T4> w4, Observable<? extends T5> w5, Observable<? extends T6> w6, Observable<? extends T7> w7,
            Observable<? extends T8> w8,
            Func9<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> combineLatestFunction) {
        return new CombineLatest<Object, R>(Arrays.asList(w0, w1, w2, w3, w4, w5, w6, w7, w8), Functions.fromFunc(combineLatestFunction));
    }

    static final class CombineLatest<T, R> implements OnSubscribeFunc<R> {
        final List<Observable<? extends T>> sources;
        final FuncN<? extends R> combiner;

        public CombineLatest(Iterable<? extends Observable<? extends T>> sources, FuncN<? extends R> combiner) {
            this.sources = new ArrayList<Observable<? extends T>>();
            this.combiner = combiner;
            for (Observable<? extends T> source : sources) {
                this.sources.add(source);
            }
        }

        @Override
        public Subscription onSubscribe(Observer<? super R> t1) {
            CompositeSubscription csub = new CompositeSubscription();

            Collector collector = new Collector(t1, csub, sources.size());

            int index = 0;
            List<SourceObserver> observers = new ArrayList<SourceObserver>(sources.size() + 1);
            for (Observable<? extends T> source : sources) {
                SafeObservableSubscription sas = new SafeObservableSubscription();
                csub.add(sas);
                observers.add(new SourceObserver(collector, sas, index, source));
                index++;
            }

            for (SourceObserver so : observers) {
                // if we run to completion, don't bother any further
                if (!csub.isUnsubscribed()) {
                    so.connect();
                }
            }

            return csub;
        }

        /**
         * The collector that combines the latest values from many sources.
         */
        final class Collector {
            final Observer<? super R> observer;
            final Subscription cancel;
            final Lock lock;
            final Object[] values;
            /** Bitmap to keep track who produced a value already. */
            final BitSet hasValue;
            /** Bitmap to keep track who has completed. */
            final BitSet completed;
            /** Number of source observers who have produced a value. */
            int hasCount;
            /** Number of completed source observers. */
            int completedCount;

            public Collector(Observer<? super R> observer, Subscription cancel, int count) {
                this.observer = observer;
                this.cancel = cancel;
                this.values = new Object[count];
                this.hasValue = new BitSet(count);
                this.completed = new BitSet(count);
                this.lock = new ReentrantLock();
            }

            public void next(int index, T value) {
                Throwable err = null;
                lock.lock();
                try {
                    if (!isTerminated()) {
                        values[index] = value;
                        if (!hasValue.get(index)) {
                            hasValue.set(index);
                            hasCount++;
                        }
                        if (hasCount == values.length) {
                            // clone: defensive copy due to varargs
                            try {
                                observer.onNext(combiner.call(values.clone()));
                            } catch (Throwable t) {
                                terminate();
                                err = t;
                            }
                        }
                    }
                } finally {
                    lock.unlock();
                }
                if (err != null) {
                    // no need to lock here
                    observer.onError(err);
                    cancel.unsubscribe();
                }
            }

            public void error(int index, Throwable e) {
                boolean unsub = false;
                lock.lock();
                try {
                    if (!isTerminated()) {
                        terminate();
                        unsub = true;
                    }
                } finally {
                    lock.unlock();
                }
                if (unsub) {
                    observer.onError(e);
                    cancel.unsubscribe();
                }
            }

            boolean isTerminated() {
                return completedCount == values.length + 1;
            }

            void terminate() {
                completedCount = values.length + 1;
                Arrays.fill(values, null);
            }

            public void completed(int index) {
                boolean unsub = false;
                lock.lock();
                try {
                    if (!completed.get(index)) {
                        completed.set(index);
                        completedCount++;
                    }
                    if ((!hasValue.get(index) || completedCount == values.length)
                            && !isTerminated()) {
                        terminate();
                        unsub = true;
                    }
                } finally {
                    lock.unlock();
                }
                if (unsub) {
                    // no need to hold a lock at this point
                    observer.onCompleted();
                    cancel.unsubscribe();
                }
            }
        }

        /**
         * Observes a specific source and communicates with the collector.
         */
        final class SourceObserver implements Observer<T> {
            final SafeObservableSubscription self;
            final Collector collector;
            final int index;
            Observable<? extends T> source;

            public SourceObserver(Collector collector,
                    SafeObservableSubscription self, int index,
                    Observable<? extends T> source) {
                this.self = self;
                this.collector = collector;
                this.index = index;
                this.source = source;
            }

            @Override
            public void onNext(T args) {
                collector.next(index, args);
            }

            @Override
            public void onError(Throwable e) {
                collector.error(index, e);
            }

            @Override
            public void onCompleted() {
                collector.completed(index);
                self.unsubscribe();
            }

            /** Connect to the source. */
            void connect() {
                self.wrap(source.subscribe(this));
                source = null;
            }
        }
    }
}
