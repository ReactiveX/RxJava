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
package rx.internal.operators;

import rx.Observable.Operator;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * Unsubscribes on the specified Scheduler.
 * <p>
 * @param <T> the value type
 */
public class OperatorUnsubscribeOn<T> implements Operator<T, T> {

    final Scheduler scheduler;

    public OperatorUnsubscribeOn(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        final Subscriber<T> parent = new Subscriber<T>() {

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
        
        subscriber.add(Subscriptions.create(new Action0() {

            @Override
            public void call() {
                final Scheduler.Worker inner = scheduler.createWorker();
                inner.schedule(new Action0() {

                    @Override
                    public void call() {
                        parent.unsubscribe();
                        inner.unsubscribe();
                    }
                });
            }

        }));
        
        return parent;


    }
}
