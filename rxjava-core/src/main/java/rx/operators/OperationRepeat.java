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

import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.observables.ConnectableObservable;
import rx.subscriptions.SerialSubscription;

/**
 * Generates an observable sequence that repeats the given source sequence of
 * the specified number of times.
 * 
 */
public class OperationRepeat {

    public static <T> OnSubscribeFunc<T> repeat(Observable<T> source) {
        return repeat(source, Schedulers.immediate());
    }

    public static <T> OnSubscribeFunc<T> repeat(Observable<T> source,
            int repeatCount) {
        return repeat(source, repeatCount, Schedulers.immediate());
    }

    public static <T> OnSubscribeFunc<T> repeat(Observable<T> source,
            Scheduler scheduler) {
        return repeat(source, INFINITE, scheduler);
    }

    public static <T> OnSubscribeFunc<T> repeat(final Observable<T> source,
            final int repeatCount, final Scheduler scheduler) {
        if (repeatCount != INFINITE && repeatCount < 0) {
            throw new IllegalArgumentException(
                    "repeatCount should not be less than 0");
        }

        return new OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(final Observer<? super T> observer) {
                if (repeatCount == 0) {
                    return source.ignoreElements().observeOn(scheduler)
                            .subscribe(observer);
                }

                final SerialSubscription subscription = new SerialSubscription();
                ConnectableObservable<T> replayObservable = source.replay();
                subscription.setSubscription(replayObservable.observeOn(
                        scheduler).subscribe(new Observer<T>() {

                    private AtomicInteger remainCount = new AtomicInteger(
                            repeatCount);

                    @Override
                    public void onCompleted() {
                        if (repeatCount != INFINITE
                                && remainCount.decrementAndGet() == 0) {
                            observer.onCompleted();
                        } else {
                            ConnectableObservable<T> replayObservable = source
                                    .replay();
                            subscription.setSubscription(replayObservable
                                    .observeOn(scheduler).subscribe(this));
                            replayObservable.connect();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        observer.onError(e);
                    }

                    @Override
                    public void onNext(T value) {
                        observer.onNext(value);
                    }

                }));
                replayObservable.connect();
                return subscription;
            }
        };
    }

    private static final int INFINITE = Integer.MIN_VALUE;
}
