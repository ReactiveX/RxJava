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

import rx.Observable;
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscriber;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

/**
 * Asynchronously subscribes and unsubscribes Observers on the specified Scheduler.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/subscribeOn.png">
 */
public class OperatorSubscribeOn<T> implements Operator<T, Observable<T>> {

    private final Scheduler scheduler;

    public OperatorSubscribeOn(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super Observable<T>> call(
            final Subscriber<? super T> subscriber) {
        return new Subscriber<Observable<T>>() {

            @Override
            public void onCompleted() {
                // ignore
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @Override
            public void onNext(final Observable<T> o) {
                scheduler.schedule(new Action1<Inner>() {

                    @Override
                    public void call(final Inner inner) {
                        if (!inner.isUnsubscribed()) {
                            final CompositeSubscription cs = new CompositeSubscription();
                            subscriber.add(Subscriptions.create(new Action0() {

                                @Override
                                public void call() {
                                    scheduler.schedule(new Action1<Inner>() {

                                        @Override
                                        public void call(final Inner inner) {
                                            cs.unsubscribe();
                                            inner.unsubscribe();
                                        }

                                    });
                                }

                            }));
                            cs.add(subscriber);
                            o.subscribe(new Subscriber<T>(cs) {

                                @Override
                                public void onCompleted() {
                                    subscriber.onCompleted();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    subscriber.onError(e);
                                }

                                @Override
                                public void onNext(T t) {
                                    subscriber.onNext(t);
                                }
                            });
                        }
                    }

                });
            }

        };
    }
}
