package org.rx.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.rx.reactive.Observable;
import org.rx.reactive.Observer;
import org.rx.reactive.Subscription;

/**
 * Returns the last element of an observable sequence.
 * 
 * @param <T>
 */
/* package */final class OperationLast<T> extends Observable<T> {
    private final AtomicReference<T> lastValue = new AtomicReference<T>();
    private final Observable<T> that;
    private final AtomicBoolean onNextCalled = new AtomicBoolean(false);

    OperationLast(Observable<T> that) {
        this.that = that;
    }

    public Subscription subscribe(final Observer<T> Observer) {
        final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
        final Observer<T> observer = new AtomicObserver<T>(Observer, subscription);

        subscription.setActual(that.subscribe(new Observer<T>() {
            public void onNext(T value) {
                onNextCalled.set(true);
                lastValue.set(value);
            }

            public void onError(Exception ex) {
                observer.onError(ex);
            }

            public void onCompleted() {
                if (onNextCalled.get()) {
                    observer.onNext(lastValue.get());
                }
                observer.onCompleted();
            }
        }));

        return subscription;
    }

    public static class UnitTest {

        @Test
        public void testLast() {
            Observable<String> w = ObservableExtensions.toObservable("one", "two", "three");
            Observable<String> Observable = new OperationLast<String>(w);

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            Observable.subscribe(aObserver);
            verify(aObserver, never()).onNext("one");
            verify(aObserver, never()).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }
    }
}