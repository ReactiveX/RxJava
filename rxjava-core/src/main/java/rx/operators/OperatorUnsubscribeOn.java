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

import rx.Observable.Operator;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Unsubscribes on the specified Scheduler.
 * <p>
 */
public class OperatorUnsubscribeOn<T> implements Operator<T, T> {

    private final Scheduler scheduler;

    public OperatorUnsubscribeOn(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        final CompositeSubscription parentSubscription = new CompositeSubscription();
        subscriber.add(Subscriptions.create(new Action0() {

            @Override
            public void call() {
                final Scheduler.Worker inner = scheduler.createWorker();
                inner.schedule(new Action0() {

                    @Override
                    public void call() {
                        parentSubscription.unsubscribe();
                        inner.unsubscribe();
                    }
                });
            }

        }));

        return new Subscriber<T>(parentSubscription) {

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

        };
    }
}
