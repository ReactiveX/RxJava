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
package rx.observables.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.Mockito;

import rx.observables.Observable;
import rx.observables.Observer;
import rx.observables.Subscription;
import rx.util.Func1;

public final class OperationFilter<T> {

    public static <T> Observable<T> filter(Observable<T> that, Func1<T, Boolean> predicate) {
        return new Filter<T>(that, predicate);
    }

    private static class Filter<T> extends Observable<T> {

        private final Observable<T> that;
        private final Func1<T, Boolean> predicate;

        public Filter(Observable<T> that, Func1<T, Boolean> predicate) {
            this.that = that;
            this.predicate = predicate;
        }

        public Subscription subscribe(Observer<T> Observer) {
            final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
            final Observer<T> observer = new AtomicObserver<T>(Observer, subscription);

            subscription.setActual(that.subscribe(new Observer<T>() {
                public void onNext(T value) {
                    try {
                        if ((boolean) predicate.call(value)) {
                            observer.onNext(value);
                        }
                    } catch (Exception ex) {
                        observer.onError(ex);
                        subscription.unsubscribe();
                    }
                }

                public void onError(Exception ex) {
                    observer.onError(ex);
                }

                public void onCompleted() {
                    observer.onCompleted();
                }
            }));

            return subscription;
        }
    }

    public static class UnitTest {

        @Test
        public void testFilter() {
            Observable<String> w = Observable.toObservable("one", "two", "three");
            Observable<String> Observable = filter(w, new Func1<String, Boolean>() {

                @Override
                public Boolean call(String t1) {
                    if (t1.equals("two"))
                        return true;
                    else
                        return false;
                }
            });

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            Observable.subscribe(aObserver);
            verify(aObserver, Mockito.never()).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, Mockito.never()).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }
    }
}