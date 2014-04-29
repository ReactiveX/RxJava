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
package rx.math.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.math.operators.OperatorMinMax.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;

public class OperatorMinMaxTest {
    @Test
    public void testMin() {
        Observable<Integer> observable = min(Observable.from(2, 3, 1, 4));

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);

        observable.subscribe(observer);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMinWithEmpty() {
        Observable<Integer> observable = min(Observable.<Integer> empty());

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);

        observable.subscribe(observer);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMinWithComparator() {
        Observable<Integer> observable = min(Observable.from(2, 3, 1, 4),
                new Comparator<Integer>() {
                    @Override
                    public int compare(Integer o1, Integer o2) {
                        return o2 - o1;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);

        observable.subscribe(observer);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(4);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMinWithComparatorAndEmpty() {
        Observable<Integer> observable = min(Observable.<Integer> empty(),
                new Comparator<Integer>() {
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
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMinBy() {
        Observable<List<String>> observable = minBy(
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
        inOrder.verify(observer, times(1)).onNext(Arrays.asList("2", "4", "6"));
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMinByWithEmpty() {
        Observable<List<String>> observable = minBy(
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
    public void testMinByWithComparator() {
        Observable<List<String>> observable = minBy(
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
        inOrder.verify(observer, times(1)).onNext(Arrays.asList("1", "3", "5"));
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMinByWithComparatorAndEmpty() {
        Observable<List<String>> observable = minBy(
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

    @Test
    public void testMax() {
        Observable<Integer> observable = max(Observable.from(2, 3, 1, 4));

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
        Observable<Integer> observable = max(Observable.<Integer> empty());

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);

        observable.subscribe(observer);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMaxWithComparator() {
        Observable<Integer> observable = max(Observable.from(2, 3, 1, 4),
                new Comparator<Integer>() {
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
        Observable<Integer> observable = max(Observable.<Integer> empty(),
                new Comparator<Integer>() {
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
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMaxBy() {
        Observable<List<String>> observable = maxBy(
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
        inOrder.verify(observer, times(1)).onNext(Arrays.asList("1", "3", "5"));
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMaxByWithEmpty() {
        Observable<List<String>> observable = maxBy(
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
        Observable<List<String>> observable = maxBy(
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
        inOrder.verify(observer, times(1)).onNext(Arrays.asList("2", "4", "6"));
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMaxByWithComparatorAndEmpty() {
        Observable<List<String>> observable = maxBy(
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
