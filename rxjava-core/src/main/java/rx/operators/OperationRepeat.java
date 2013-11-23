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

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.subscriptions.SerialSubscription;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

public class OperationRepeat<T> implements Observable.OnSubscribeFunc<T> {

    private final Observable<T> source;

    public static <T> Observable.OnSubscribeFunc<T> repeat(Observable<T> seed) {
        return new OperationRepeat(seed);
    }

    private OperationRepeat(Observable<T> source) {
        this.source = source;
    }

    @Override
    public Subscription onSubscribe(final Observer<? super T> observer) {
        final SerialSubscription subscription = new SerialSubscription();
        subscription.setSubscription(Schedulers.currentThread().schedule(new Action1<Action0>() {
            @Override
            public void call(final Action0 self) {
                subscription.setSubscription(source.subscribe(new Observer<T>() {

                    @Override
                    public void onCompleted() {
                        subscription.getSubscription().unsubscribe();
                        self.call();
                    }

                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }

                    @Override
                    public void onNext(T value) {
                        observer.onNext(value);
                    }
                }));
            }
        }));
        return subscription;
    }
}