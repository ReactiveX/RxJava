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
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;

/**
 * If the Observable completes after emitting a single item that matches a
 * predicate, return an Observable containing that item. If it emits more than
 * one such item or no item, throw an IllegalArgumentException.
 */
public class OperationSingle {

    public static <T> OnSubscribeFunc<T> single(
            final Observable<? extends T> source) {
        return single(source, false, null);
    }

    public static <T> OnSubscribeFunc<T> singleOrDefault(
            final Observable<? extends T> source, final T defaultValue) {
        return single(source, true, defaultValue);
    }

    private static <T> OnSubscribeFunc<T> single(
            final Observable<? extends T> source,
            final boolean hasDefaultValue, final T defaultValue) {
        return new OnSubscribeFunc<T>() {

            @Override
            public Subscription onSubscribe(final Observer<? super T> observer) {
                final SafeObservableSubscription subscription = new SafeObservableSubscription();
                subscription.wrap(source.subscribe(new Observer<T>() {

                    private T value;
                    private boolean isEmpty = true;
                    private boolean hasTooManyElemenets;

                    @Override
                    public void onCompleted() {
                        if (hasTooManyElemenets) {
                            // We has already sent an onError message
                        } else {
                            if (isEmpty) {
                                if (hasDefaultValue) {
                                    observer.onNext(defaultValue);
                                    observer.onCompleted();
                                } else {
                                    observer.onError(new IllegalArgumentException(
                                            "Sequence contains no elements"));
                                }
                            } else {
                                observer.onNext(value);
                                observer.onCompleted();
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        observer.onError(e);
                    }

                    @Override
                    public void onNext(T value) {
                        if (isEmpty) {
                            this.value = value;
                            isEmpty = false;
                        } else {
                            hasTooManyElemenets = true;
                            observer.onError(new IllegalArgumentException(
                                    "Sequence contains too many elements"));
                            subscription.unsubscribe();
                        }
                    }
                }));
                return subscription;
            }
        };
    }
}
