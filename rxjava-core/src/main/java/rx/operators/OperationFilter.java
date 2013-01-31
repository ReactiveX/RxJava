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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.AtomicObservableSubscription;
import rx.util.functions.Func1;

public final class OperationFilter<T> {

    public static <T> Func1<Observer<T>, Subscription> filter(Observable<T> that, Func1<T, Boolean> predicate) {
        return new Filter<T>(that, predicate);
    }

    private static class Filter<T> implements Func1<Observer<T>, Subscription> {

        private final Observable<T> that;
        private final Func1<T, Boolean> predicate;
        private final AtomicObservableSubscription subscription = new AtomicObservableSubscription();

        public Filter(Observable<T> that, Func1<T, Boolean> predicate) {
            this.that = that;
            this.predicate = predicate;
        }

        public Subscription call(final Observer<T> observer) {
            return subscription.wrap(that.subscribe(new Observer<T>() {
                public void onNext(T value) {
                    try {
                        if ((boolean) predicate.call(value)) {
                            observer.onNext(value);
                        }
                    } catch (Exception ex) {
                        observer.onError(ex);
                        // this will work if the sequence is asynchronous, it will have no effect on a synchronous observable
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
        }

    }

    public static class UnitTest {

        @Test
        public void testFilter() {
            Observable<String> w = Observable.toObservable("one", "two", "three");
            Observable<String> observable = Observable.create(filter(w, new Func1<String, Boolean>() {

                @Override
                public Boolean call(String t1) {
                    if (t1.equals("two"))
                        return true;
                    else
                        return false;
                }
            }));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, Mockito.never()).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, Mockito.never()).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }
    }
}