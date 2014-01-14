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
 * Returns the elements of the specified sequence or the specified default value
 * in a singleton sequence if the sequence is empty.
 */
public class OperationDefaultIfEmpty {

    /**
     * Returns the elements of the specified sequence or the specified default
     * value in a singleton sequence if the sequence is empty.
     * 
     * @param source
     *            The sequence to return the specified value for if it is empty.
     * @param defaultValue
     *            The value to return if the sequence is empty.
     * @return An observable sequence that contains the specified default value
     *         if the source is empty; otherwise, the elements of the source
     *         itself.
     */
    public static <T> OnSubscribeFunc<T> defaultIfEmpty(
            Observable<? extends T> source, T defaultValue) {
        return new DefaultIfEmpty<T>(source, defaultValue);
    }

    private static class DefaultIfEmpty<T> implements OnSubscribeFunc<T> {

        private final Observable<? extends T> source;
        private final T defaultValue;

        private DefaultIfEmpty(Observable<? extends T> source, T defaultValue) {
            this.source = source;
            this.defaultValue = defaultValue;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            final SafeObservableSubscription subscription = new SafeObservableSubscription();
            return subscription.wrap(source.subscribe(new Observer<T>() {

                private volatile boolean hasEmitted = false;

                @Override
                public void onNext(T value) {
                    try {
                        hasEmitted = true;
                        observer.onNext(value);
                    } catch (Throwable ex) {
                        observer.onError(ex);
                        // this will work if the sequence is asynchronous, it
                        // will have no effect on a synchronous observable
                        subscription.unsubscribe();
                    }
                }

                @Override
                public void onError(Throwable ex) {
                    observer.onError(ex);
                }

                @Override
                public void onCompleted() {
                    if (hasEmitted) {
                        observer.onCompleted();
                    } else {
                        observer.onNext(defaultValue);
                        observer.onCompleted();
                    }
                }
            }));
        }
    }
}
