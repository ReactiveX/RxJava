package rx.operators;

import static org.mockito.Matchers.isA;
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

public class OperationMinTest {
    @Test
    public void testMin() {
        Observable<Integer> observable = OperationMin.min(Observable.from(2, 3,
                1, 4));

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
        Observable<Integer> observable = OperationMin.min(Observable
                .<Integer> empty());

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);

        observable.subscribe(observer);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMinWithComparator() {
        Observable<Integer> observable = OperationMin.min(
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
        inOrder.verify(observer, times(1)).onNext(4);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMinWithComparatorAndEmpty() {
        Observable<Integer> observable = OperationMin.min(
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
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMinBy() {
        Observable<List<String>> observable = OperationMin.minBy(
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
        Observable<List<String>> observable = OperationMin.minBy(
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
        Observable<List<String>> observable = OperationMin.minBy(
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
        Observable<List<String>> observable = OperationMin.minBy(
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
