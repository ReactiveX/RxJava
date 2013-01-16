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
package rx.observables.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Test;
import org.mockito.Mockito;

import rx.observables.Observable;
import rx.observables.Observer;
import rx.observables.Subscription;

public final class OperationToObservableList<T> {

    public static <T> Observable<List<T>> toObservableList(Observable<T> that) {
        return new ToObservableList<T>(that);
    }

    private static class ToObservableList<T> extends Observable<List<T>> {

        private final Observable<T> that;
        final ConcurrentLinkedQueue<T> list = new ConcurrentLinkedQueue<T>();

        public ToObservableList(Observable<T> that) {
            this.that = that;
        }

        public Subscription subscribe(Observer<List<T>> listObserver) {
            final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
            final Observer<List<T>> Observer = new AtomicObserver<List<T>>(listObserver, subscription);

            subscription.setActual(that.subscribe(new Observer<T>() {
                public void onNext(T value) {
                    // onNext can be concurrently executed so list must be thread-safe
                    list.add(value);
                }

                public void onError(Exception ex) {
                    Observer.onError(ex);
                }

                public void onCompleted() {
                    try {
                        // copy from LinkedQueue to List since ConcurrentLinkedQueue does not implement the List interface
                        ArrayList<T> l = new ArrayList<T>(list.size());
                        for (T t : list) {
                            l.add(t);
                        }

                        // benjchristensen => I want to make this immutable but some clients are sorting this
                        // instead of using toSortedList() and this change breaks them until we migrate their code.
                        // Observer.onNext(Collections.unmodifiableList(l));
                        Observer.onNext(l);
                        Observer.onCompleted();
                    } catch (Exception e) {
                        onError(e);
                    }

                }
            }));
            return subscription;
        }
    }

    public static class UnitTest {

        @Test
        public void testList() {
            Observable<String> w = Observable.toObservable("one", "two", "three");
            Observable<List<String>> Observable = toObservableList(w);

            @SuppressWarnings("unchecked")
            Observer<List<String>> aObserver = mock(Observer.class);
            Observable.subscribe(aObserver);
            verify(aObserver, times(1)).onNext(Arrays.asList("one", "two", "three"));
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }
    }
}