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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

public class OperationMax {

    public static <T extends Comparable<T>> Observable<T> max(
            Observable<T> source) {
        return source.reduce(new Func2<T, T, T>() {
            @Override
            public T call(T acc, T value) {
                if (acc.compareTo(value) > 0) {
                    return acc;
                }
                return value;
            }
        });
    }

    public static <T> Observable<T> max(Observable<T> source,
            final Comparator<T> comparator) {
        return source.reduce(new Func2<T, T, T>() {
            @Override
            public T call(T acc, T value) {
                if (comparator.compare(acc, value) > 0) {
                    return acc;
                }
                return value;
            }
        });
    }

    public static <T, R extends Comparable<R>> Observable<List<T>> maxBy(
            Observable<T> source, final Func1<T, R> selector) {
        return source.reduce(new ArrayList<T>(),
                new Func2<List<T>, T, List<T>>() {

                    @Override
                    public List<T> call(List<T> acc, T value) {
                        if (acc.isEmpty()) {
                            acc.add(value);
                        } else {
                            int flag = selector.call(acc.get(0)).compareTo(
                                    selector.call(value));
                            if (flag < 0) {
                                acc.clear();
                                acc.add(value);
                            } else if (flag == 0) {
                                acc.add(value);
                            }
                        }
                        return acc;
                    }
                });
    }

    public static <T, R> Observable<List<T>> maxBy(Observable<T> source,
            final Func1<T, R> selector, final Comparator<R> comparator) {
        return source.reduce(new ArrayList<T>(),
                new Func2<List<T>, T, List<T>>() {

                    @Override
                    public List<T> call(List<T> acc, T value) {
                        if (acc.isEmpty()) {
                            acc.add(value);
                        } else {
                            int flag = comparator.compare(
                                    selector.call(acc.get(0)),
                                    selector.call(value));
                            if (flag < 0) {
                                acc.clear();
                                acc.add(value);
                            } else if (flag == 0) {
                                acc.add(value);
                            }
                        }
                        return acc;
                    }
                });
    }

    public static class UnitTest {

        @Test
        public void testMax() {
            Observable<Integer> observable = OperationMax.max(Observable.from(
                    2, 3, 1, 4));

            @SuppressWarnings("unchecked")
            Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);

            observable.subscribe(observer);
            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext(4);
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testMaxWithEmpty() {
            Observable<Integer> observable = OperationMax.max(Observable
                    .<Integer> empty());

            @SuppressWarnings("unchecked")
            Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);

            observable.subscribe(observer);
            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onError(
                    any(UnsupportedOperationException.class));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testMaxWithComparator() {
            Observable<Integer> observable = OperationMax.max(
                    Observable.from(2, 3, 1, 4), new Comparator<Integer>() {
                        @Override
                        public int compare(Integer o1, Integer o2) {
                            return o2 - o1;
                        }
                    });

            @SuppressWarnings("unchecked")
            Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);

            observable.subscribe(observer);
            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext(1);
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testMaxWithComparatorAndEmpty() {
            Observable<Integer> observable = OperationMax.max(
                    Observable.<Integer> empty(), new Comparator<Integer>() {
                        @Override
                        public int compare(Integer o1, Integer o2) {
                            return o2 - o1;
                        }
                    });

            @SuppressWarnings("unchecked")
            Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);

            observable.subscribe(observer);
            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onError(
                    any(UnsupportedOperationException.class));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testMaxBy() {
            Observable<List<String>> observable = OperationMax.maxBy(
                    Observable.from("1", "2", "3", "4", "5", "6"),
                    new Func1<String, Integer>() {
                        @Override
                        public Integer call(String t1) {
                            return Integer.parseInt(t1) % 2;
                        }
                    });

            @SuppressWarnings("unchecked")
            Observer<List<String>> observer = (Observer<List<String>>) mock(Observer.class);

            observable.subscribe(observer);
            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext(
                    Arrays.asList("1", "3", "5"));
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testMaxByWithEmpty() {
            Observable<List<String>> observable = OperationMax.maxBy(
                    Observable.<String> empty(), new Func1<String, Integer>() {
                        @Override
                        public Integer call(String t1) {
                            return Integer.parseInt(t1) % 2;
                        }
                    });

            @SuppressWarnings("unchecked")
            Observer<List<String>> observer = (Observer<List<String>>) mock(Observer.class);

            observable.subscribe(observer);
            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext(new ArrayList<String>());
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testMaxByWithComparator() {
            Observable<List<String>> observable = OperationMax.maxBy(
                    Observable.from("1", "2", "3", "4", "5", "6"),
                    new Func1<String, Integer>() {
                        @Override
                        public Integer call(String t1) {
                            return Integer.parseInt(t1) % 2;
                        }
                    }, new Comparator<Integer>() {
                        @Override
                        public int compare(Integer o1, Integer o2) {
                            return o2 - o1;
                        }
                    });

            @SuppressWarnings("unchecked")
            Observer<List<String>> observer = (Observer<List<String>>) mock(Observer.class);

            observable.subscribe(observer);
            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext(
                    Arrays.asList("2", "4", "6"));
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testMaxByWithComparatorAndEmpty() {
            Observable<List<String>> observable = OperationMax.maxBy(
                    Observable.<String> empty(), new Func1<String, Integer>() {
                        @Override
                        public Integer call(String t1) {
                            return Integer.parseInt(t1) % 2;
                        }
                    }, new Comparator<Integer>() {
                        @Override
                        public int compare(Integer o1, Integer o2) {
                            return o2 - o1;
                        }
                    });

            @SuppressWarnings("unchecked")
            Observer<List<String>> observer = (Observer<List<String>>) mock(Observer.class);

            observable.subscribe(observer);
            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext(new ArrayList<String>());
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }
    }

}
