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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

public final class OperationToObservableList<T> {

    public static <T> Func1<Observer<List<T>>, Subscription> toObservableList(Observable<T> that) {
        return new ToObservableList<T>(that);
    }

    private static class ToObservableList<T> implements Func1<Observer<List<T>>, Subscription> {

        private final Observable<T> that;
        final ConcurrentLinkedQueue<T> list = new ConcurrentLinkedQueue<T>();

        public ToObservableList(Observable<T> that) {
            this.that = that;
        }

        public Subscription call(final Observer<List<T>> observer) {

            return that.subscribe(new Observer<T>() {
                public void onNext(T value) {
                    // onNext can be concurrently executed so list must be thread-safe
                    list.add(value);
                }

                public void onError(Exception ex) {
                    observer.onError(ex);
                }

                public void onCompleted() {
                    try {
                        // copy from LinkedQueue to List since ConcurrentLinkedQueue does not implement the List interface
                        ArrayList<T> l = new ArrayList<T>(list.size());
                        for (T t : list) {
                            l.add(t);
                        }

                        // benjchristensen => I want to make this list immutable but some clients are sorting this
                        // instead of using toSortedList() and this change breaks them until we migrate their code.
                        // observer.onNext(Collections.unmodifiableList(l));
                        observer.onNext(l);
                        observer.onCompleted();
                    } catch (Exception e) {
                        onError(e);
                    }

                }
            });
        }
    }

    public static class UnitTest {

        @Test
        public void testList() {
            Observable<String> w = Observable.toObservable("one", "two", "three");
            Observable<List<String>> observable = Observable.create(toObservableList(w));

            @SuppressWarnings("unchecked")
            Observer<List<String>> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, times(1)).onNext(Arrays.asList("one", "two", "three"));
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }
    }
}