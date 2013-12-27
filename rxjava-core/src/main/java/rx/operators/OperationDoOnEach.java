/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import rx.IObservable;
import rx.Observer;
import rx.Observable.OnSubscribeFunc;
import rx.Subscription;

/**
 * Converts the elements of an observable sequence to the specified type.
 */
public class OperationDoOnEach {
    public static <T> OnSubscribeFunc<T> doOnEach(IObservable<? extends T> sequence, Observer<? super T> observer) {
        return new DoOnEachObservable<T>(sequence, observer);
    }

    private static class DoOnEachObservable<T> implements OnSubscribeFunc<T> {

        private final IObservable<? extends T> sequence;
        private final Observer<? super T> doOnEachObserver;

        public DoOnEachObservable(IObservable<? extends T> sequence, Observer<? super T> doOnEachObserver) {
            this.sequence = sequence;
            this.doOnEachObserver = doOnEachObserver;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            final SafeObservableSubscription subscription = new SafeObservableSubscription();
            return subscription.wrap(sequence.subscribe(new SafeObserver<T>(subscription, new Observer<T>() {
                @Override
                public void onCompleted() {
                    doOnEachObserver.onCompleted();
                    observer.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    doOnEachObserver.onError(e);
                    observer.onError(e);
                }

                @Override
                public void onNext(T value) {
                    doOnEachObserver.onNext(value);
                    observer.onNext(value);
                }
            })));
        }

    }
}