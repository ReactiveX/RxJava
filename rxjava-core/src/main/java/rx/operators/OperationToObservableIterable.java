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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable.OnGetSubscriptionFunc;
import rx.Observable.OnPartialSubscribeFunc;
import rx.Observable.OnPartialUnsubscribeFunc;
import rx.Observable.PartialSubscription;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

/**
 * Converts an Iterable sequence into an Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/toObservable.png">
 * <p>
 * You can convert any object that supports the Iterable interface into an Observable that emits
 * each item in the object, with the toObservable operation.
 */
public final class OperationToObservableIterable<T> {

    public static <T> OnGetSubscriptionFunc<T> toObservableIterable(Iterable<? extends T> list) {
        return new ToObservableIterable<T>(list);
    }

    private static class ToObservableIterable<T> implements OnGetSubscriptionFunc<T> {
        public ToObservableIterable(Iterable<? extends T> list) {
            this.iterable = list;
        }

        public Iterable<? extends T> iterable;

        @Override
        public PartialSubscription<T> onGetSubscription() {
            final AtomicReference<PartialSubscription<T>> subscription = new AtomicReference<PartialSubscription<T>>();
            
            subscription.set(PartialSubscription.create(new OnPartialSubscribeFunc<T>() {
                @Override
                public void onSubscribe(Observer<? super T> observer) {
                    for (T item : iterable) {
                        if (!subscription.get().isUnsubscribed())
                            observer.onNext(item);
                    }
                    if (!subscription.get().isUnsubscribed())
                        observer.onCompleted();
                }
            }, new OnPartialUnsubscribeFunc() {
                @Override
                public void onUnsubscribe() {
                }
            }));
            
            return subscription.get();
        }
    }
}
