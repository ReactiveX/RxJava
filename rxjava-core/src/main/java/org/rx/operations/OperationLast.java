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
package org.rx.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;
import org.rx.reactive.Observable;
import org.rx.reactive.Observer;
import org.rx.reactive.Subscription;

/**
 * Returns the last element of an observable sequence.
 * 
 * @param <T>
 */
public final class OperationLast<T> {

    public static <T> Observable<T> last(Observable<T> observable) {
        return new Last<T>(observable);
    }

    private static class Last<T> extends Observable<T> {

        private final AtomicReference<T> lastValue = new AtomicReference<T>();
        private final Observable<T> that;
        private final AtomicBoolean onNextCalled = new AtomicBoolean(false);

        public Last(Observable<T> that) {
            this.that = that;
        }

        public Subscription subscribe(final Observer<T> Observer) {
            final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
            final Observer<T> observer = new AtomicObserver<T>(Observer, subscription);

            subscription.setActual(that.subscribe(new Observer<T>() {
                public void onNext(T value) {
                    onNextCalled.set(true);
                    lastValue.set(value);
                }

                public void onError(Exception ex) {
                    observer.onError(ex);
                }

                public void onCompleted() {
                    if (onNextCalled.get()) {
                        observer.onNext(lastValue.get());
                    }
                    observer.onCompleted();
                }
            }));

            return subscription;
        }
    }

    public static class UnitTest {

        @Test
        public void testLast() {
            Observable<String> w = Observable.toObservable("one", "two", "three");
            Observable<String> Observable = last(w);

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            Observable.subscribe(aObserver);
            verify(aObserver, Mockito.never()).onNext("one");
            verify(aObserver, Mockito.never()).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }
    }
}