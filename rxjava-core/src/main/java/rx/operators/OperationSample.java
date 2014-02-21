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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Returns an Observable that emits the results of sampling the items emitted by the source
 * Observable at a specified time interval.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/sample.png">
 */
public final class OperationSample {

    /**
     * Samples the observable sequence at each interval.
     */
    public static <T> OnSubscribeFunc<T> sample(final Observable<? extends T> source, long period, TimeUnit unit) {
        return new Sample<T>(source, period, unit, Schedulers.computation());
    }

    /**
     * Samples the observable sequence at each interval.
     */
    public static <T> OnSubscribeFunc<T> sample(final Observable<? extends T> source, long period, TimeUnit unit, Scheduler scheduler) {
        return new Sample<T>(source, period, unit, scheduler);
    }

    private static class Sample<T> implements OnSubscribeFunc<T> {
        private final Observable<? extends T> source;
        private final long period;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        private final AtomicBoolean hasValue = new AtomicBoolean();
        private final AtomicReference<T> latestValue = new AtomicReference<T>();

        private Sample(Observable<? extends T> source, long interval, TimeUnit unit, Scheduler scheduler) {
            this.source = source;
            this.period = interval;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            Observable<Long> clock = Observable.create(OperationInterval.interval(period, unit, scheduler));
            final Subscription clockSubscription = clock.subscribe(new Observer<Long>() {
                @Override
                public void onCompleted() { /* the clock never completes */
                }

                @Override
                public void onError(Throwable e) { /* the clock has no errors */
                }

                @Override
                public void onNext(Long tick) {
                    if (hasValue.get()) {
                        observer.onNext(latestValue.get());
                    }
                }
            });

            final Subscription sourceSubscription = source.subscribe(new Observer<T>() {
                @Override
                public void onCompleted() {
                    clockSubscription.unsubscribe();
                    observer.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    clockSubscription.unsubscribe();
                    observer.onError(e);
                }

                @Override
                public void onNext(T value) {
                    latestValue.set(value);
                    hasValue.set(true);
                }
            });

            return Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    clockSubscription.unsubscribe();
                    sourceSubscription.unsubscribe();
                }
            });
        }
    }

    /**
     * Sample with the help of another observable.
     * 
     * @see <a href='http://msdn.microsoft.com/en-us/library/hh229742.aspx'>MSDN: Observable.Sample</a>
     */
    public static class SampleWithObservable<T, U> implements OnSubscribeFunc<T> {
        final Observable<T> source;
        final Observable<U> sampler;

        public SampleWithObservable(Observable<T> source, Observable<U> sampler) {
            this.source = source;
            this.sampler = sampler;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {
            return new ResultManager(t1).init();
        }

        /** Observe source values. */
        class ResultManager implements Observer<T> {
            final Observer<? super T> observer;
            final CompositeSubscription cancel;
            T value;
            boolean valueTaken = true;
            boolean done;
            final Object guard;

            public ResultManager(Observer<? super T> observer) {
                this.observer = observer;
                cancel = new CompositeSubscription();
                guard = new Object();
            }

            public Subscription init() {
                cancel.add(source.subscribe(this));
                cancel.add(sampler.subscribe(new Sampler()));

                return cancel;
            }

            @Override
            public void onNext(T args) {
                synchronized (guard) {
                    valueTaken = false;
                    value = args;
                }
            }

            @Override
            public void onError(Throwable e) {
                synchronized (guard) {
                    if (!done) {
                        done = true;
                        observer.onError(e);
                        cancel.unsubscribe();
                    }
                }
            }

            @Override
            public void onCompleted() {
                synchronized (guard) {
                    if (!done) {
                        done = true;
                        observer.onCompleted();
                        cancel.unsubscribe();
                    }
                }
            }

            /** Take the latest value, but only once. */
            class Sampler implements Observer<U> {
                @Override
                public void onNext(U args) {
                    synchronized (guard) {
                        if (!valueTaken && !done) {
                            valueTaken = true;
                            observer.onNext(value);
                        }
                    }
                }

                @Override
                public void onError(Throwable e) {
                    ResultManager.this.onError(e);
                }

                @Override
                public void onCompleted() {
                    ResultManager.this.onCompleted();
                }
            }
        }
    }
}
