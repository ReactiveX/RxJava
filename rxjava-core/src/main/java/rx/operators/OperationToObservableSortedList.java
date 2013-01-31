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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Similar to toList in that it converts a sequence<T> into a List<T> except that it accepts a Function that will provide an implementation of Comparator.
 * 
 * @param <T>
 */
public final class OperationToObservableSortedList<T> {

    /**
     * Sort T objects by their natural order (object must implement Comparable).
     * 
     * @param sequence
     * @throws ClassCastException
     *             if T objects do not implement Comparable
     * @return
     */
    public static <T> Func1<Observer<List<T>>, Subscription> toSortedList(Observable<T> sequence) {
        return new ToObservableSortedList<T>(sequence);
    }

    /**
     * Sort T objects using the defined sort function.
     * 
     * @param sequence
     * @param sortFunction
     * @return
     */
    public static <T> Func1<Observer<List<T>>, Subscription> toSortedList(Observable<T> sequence, Func2<T, T, Integer> sortFunction) {
        return new ToObservableSortedList<T>(sequence, sortFunction);
    }

    private static class ToObservableSortedList<T> implements Func1<Observer<List<T>>, Subscription> {

        private final Observable<T> that;
        private final ConcurrentLinkedQueue<T> list = new ConcurrentLinkedQueue<T>();
        private final Func2<T, T, Integer> sortFunction;

        // unchecked as we're support Object for the default
        @SuppressWarnings("unchecked")
        private ToObservableSortedList(Observable<T> that) {
            this(that, defaultSortFunction);
        }

        private ToObservableSortedList(Observable<T> that, Func2<T, T, Integer> sortFunction) {
            this.that = that;
            this.sortFunction = sortFunction;
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

                        // sort the list before delivery
                        Collections.sort(l, new Comparator<T>() {

                            @Override
                            public int compare(T o1, T o2) {
                                return sortFunction.call(o1, o2);
                            }

                        });

                        observer.onNext(Collections.unmodifiableList(l));
                        observer.onCompleted();
                    } catch (Exception e) {
                        onError(e);
                    }

                }
            });
        }

        // raw because we want to support Object for this default
        @SuppressWarnings("rawtypes")
        private static Func2 defaultSortFunction = new DefaultComparableFunction();

        private static class DefaultComparableFunction implements Func2<Object, Object, Integer> {

            // unchecked because we want to support Object for this default
            @SuppressWarnings("unchecked")
            @Override
            public Integer call(Object t1, Object t2) {
                Comparable<Object> c1 = (Comparable<Object>) t1;
                Comparable<Object> c2 = (Comparable<Object>) t2;
                return c1.compareTo(c2);
            }

        }

    }

    public static class UnitTest {

        @Test
        public void testSortedList() {
            Observable<Integer> w = Observable.toObservable(1, 3, 2, 5, 4);
            Observable<List<Integer>> observable = Observable.create(toSortedList(w));

            @SuppressWarnings("unchecked")
            Observer<List<Integer>> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, times(1)).onNext(Arrays.asList(1, 2, 3, 4, 5));
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testSortedListWithCustomFunction() {
            Observable<Integer> w = Observable.toObservable(1, 3, 2, 5, 4);
            Observable<List<Integer>> observable = Observable.create(toSortedList(w, new Func2<Integer, Integer, Integer>() {

                @Override
                public Integer call(Integer t1, Integer t2) {
                    return t2 - t1;
                }

            }));

            @SuppressWarnings("unchecked")
            Observer<List<Integer>> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, times(1)).onNext(Arrays.asList(5, 4, 3, 2, 1));
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

    }
}