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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import rx.IObservable;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;

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
    public static <T> OnSubscribeFunc<T> sample(final IObservable<? extends T> source, long period, TimeUnit unit) {
        return new Sample<T>(source, period, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * Samples the observable sequence at each interval.
     */
    public static <T> OnSubscribeFunc<T> sample(final IObservable<? extends T> source, long period, TimeUnit unit, Scheduler scheduler) {
        return new Sample<T>(source, period, unit, scheduler);
    }

    private static class Sample<T> implements OnSubscribeFunc<T> {
        private final IObservable<? extends T> source;
        private final long period;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        private final AtomicBoolean hasValue = new AtomicBoolean();
        private final AtomicReference<T> latestValue = new AtomicReference<T>();

        private Sample(IObservable<? extends T> source, long interval, TimeUnit unit, Scheduler scheduler) {
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
}
