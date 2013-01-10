package org.rx.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Test;
import org.rx.functions.Func2;
import org.rx.reactive.Observable;
import org.rx.reactive.Observer;
import org.rx.reactive.Subscription;

/**
 * Similar to toList in that it converts a sequence<T> into a List<T> except that it accepts a Function that will provide an implementation of Comparator.
 * 
 * @param <T>
 */
final class OperationToObservableSortedList<T> extends Observable<List<T>> {

    /**
     * Sort T objects by their natural order (object must implement Comparable).
     * 
     * @param sequence
     * @throws ClassCastException
     *             if T objects do not implement Comparable
     * @return
     */
    public static <T> Observable<List<T>> toSortedList(Observable<T> sequence) {
        return new OperationToObservableSortedList<T>(sequence);
    }

    /**
     * Sort T objects using the defined sort function.
     * 
     * @param sequence
     * @param sortFunction
     * @return
     */
    public static <T> Observable<List<T>> toSortedList(Observable<T> sequence, Func2<Integer, T, T> sortFunction) {
        return new OperationToObservableSortedList<T>(sequence, sortFunction);
    }

    private final Observable<T> that;
    private final ConcurrentLinkedQueue<T> list = new ConcurrentLinkedQueue<T>();
    private final Func2<Integer, T, T> sortFunction;

    // unchecked as we're support Object for the default
    @SuppressWarnings("unchecked")
    private OperationToObservableSortedList(Observable<T> that) {
        this(that, defaultSortFunction);
    }

    private OperationToObservableSortedList(Observable<T> that, Func2<Integer, T, T> sortFunction) {
        this.that = that;
        this.sortFunction = sortFunction;
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

                    // sort the list before delivery
                    Collections.sort(l, new Comparator<T>() {

                        @Override
                        public int compare(T o1, T o2) {
                            return sortFunction.call(o1, o2);
                        }

                    });

                    Observer.onNext(Collections.unmodifiableList(l));
                    Observer.onCompleted();
                } catch (Exception e) {
                    onError(e);
                }

            }
        }));
        return subscription;
    }

    // raw because we want to support Object for this default
    @SuppressWarnings("rawtypes")
    private static Func2 defaultSortFunction = new DefaultComparableFunction();

    private static class DefaultComparableFunction implements Func2<Integer, Object, Object> {

        // unchecked because we want to support Object for this default
        @SuppressWarnings("unchecked")
        @Override
        public Integer call(Object t1, Object t2) {
            Comparable<Object> c1 = (Comparable<Object>) t1;
            Comparable<Object> c2 = (Comparable<Object>) t2;
            return c1.compareTo(c2);
        }

    }

    public static class UnitTest {

        @Test
        public void testSortedList() {
            Observable<Integer> w = ObservableExtensions.toObservable(1, 3, 2, 5, 4);
            Observable<List<Integer>> Observable = toSortedList(w);

            @SuppressWarnings("unchecked")
            Observer<List<Integer>> aObserver = mock(Observer.class);
            Observable.subscribe(aObserver);
            verify(aObserver, times(1)).onNext(Arrays.asList(1, 2, 3, 4, 5));
            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testSortedListWithCustomFunction() {
            Observable<Integer> w = ObservableExtensions.toObservable(1, 3, 2, 5, 4);
            Observable<List<Integer>> Observable = toSortedList(w, new Func2<Integer, Integer, Integer>() {

                @Override
                public Integer call(Integer t1, Integer t2) {
                    return t2 - t1;
                }

            });
            ;

            @SuppressWarnings("unchecked")
            Observer<List<Integer>> aObserver = mock(Observer.class);
            Observable.subscribe(aObserver);
            verify(aObserver, times(1)).onNext(Arrays.asList(5, 4, 3, 2, 1));
            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

    }
}