package org.rx.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Test;
import org.rx.reactive.Observable;
import org.rx.reactive.Observer;
import org.rx.reactive.Subscription;

/**
 * Accepts an Iterable object and exposes it as an Observable.
 * 
 * @param <T>
 *            The type of the Iterable sequence.
 */
/* package */class OperationToObservableIterable<T> extends Observable<T> {
    public OperationToObservableIterable(Iterable<T> list) {
        this.iterable = list;
    }

    public Iterable<T> iterable;

    public Subscription subscribe(Observer<T> Observer) {
        final AtomicObservableSubscription subscription = new AtomicObservableSubscription(ObservableExtensions.noOpSubscription());
        final Observer<T> observer = new AtomicObserver<T>(Observer, subscription);

        for (T item : iterable) {
            observer.onNext(item);
        }
        observer.onCompleted();

        return subscription;
    }

    public static class UnitTest {

        @Test
        public void testIterable() {
            Observable<String> Observable = new OperationToObservableIterable<String>(Arrays.<String> asList("one", "two", "three"));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            Observable.subscribe(aObserver);
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }
    }
}