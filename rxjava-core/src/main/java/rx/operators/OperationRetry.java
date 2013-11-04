package rx.operators;

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

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.util.functions.Func2;

import java.util.concurrent.atomic.AtomicInteger;

public class OperationRetry {

    private static final int INFINITE_RETRY = -1;

    public static <T> OnSubscribeFunc<T> retry(final Observable<T> observable, final int retryCount) {
        return new Retry<T>(observable, retryCount);
    }

    public static <T> OnSubscribeFunc<T> retry(final Observable<T> observable) {
        return new Retry<T>(observable, INFINITE_RETRY);
    }

    private static class Retry<T> implements OnSubscribeFunc<T> {

        private final Observable<T> source;
        private final int retryCount;
        private final AtomicInteger attempts = new AtomicInteger(0);
        private final CompositeSubscription subscription = new CompositeSubscription();

        public Retry(Observable<T> source, int retryCount) {
            this.source = source;
            this.retryCount = retryCount;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            MultipleAssignmentSubscription rescursiveSubscription = new MultipleAssignmentSubscription();
            subscription.add(Schedulers.currentThread().schedule(rescursiveSubscription, attemptSubscription(observer)));
            subscription.add(rescursiveSubscription);
            return subscription;
        }

        private Func2<Scheduler, MultipleAssignmentSubscription, Subscription> attemptSubscription(final Observer<? super T> observer) {
            return new Func2<Scheduler, MultipleAssignmentSubscription, Subscription>() {

                @Override
                public Subscription call(final Scheduler scheduler, final MultipleAssignmentSubscription rescursiveSubscription) {
                    attempts.incrementAndGet();
                    return source.subscribe(new Observer<T>() {

                        @Override
                        public void onCompleted() {
                            observer.onCompleted();
                        }

                        @Override
                        public void onError(Throwable e) {
                            if ((retryCount == INFINITE_RETRY || attempts.get() <= retryCount) && !subscription.isUnsubscribed()) {
                                // retry again
                                // add the new subscription and schedule a retry recursively
                                rescursiveSubscription.setSubscription(scheduler.schedule(rescursiveSubscription, attemptSubscription(observer)));
                            } else {
                                // give up and pass the failure
                                observer.onError(e);
                            }
                        }

                        @Override
                        public void onNext(T v) {
                            observer.onNext(v);
                        }
                    });

                }

            };
        }

    }
}
