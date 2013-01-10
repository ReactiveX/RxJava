package org.rx.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.rx.functions.Func1;
import org.rx.reactive.Observable;
import org.rx.reactive.Observer;
import org.rx.reactive.Subscription;

/**
 * Accepts a Function and makes it into a Observable.
 * <p>
 * This is equivalent to Rx Observable.Create
 * 
 * @see http://msdn.microsoft.com/en-us/library/hh229114(v=vs.103).aspx
 * @see ObservableExtensions.toObservable
 * @see ObservableExtensions.create
 */
/* package */class OperationToObservableFunction<T> extends Observable<T> {
    private final Func1<Subscription, Observer<T>> func;

    OperationToObservableFunction(Func1<Subscription, Observer<T>> func) {
        this.func = func;
    }

    @Override
    public Subscription subscribe(Observer<T> Observer) {
        final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
        // We specifically use the SingleThreaded AtomicObserver since we can't ensure the implementation is thread-safe
        // so will not allow it to use the MultiThreaded version even when other operators are doing so
        final Observer<T> atomicObserver = new AtomicObserverSingleThreaded<T>(Observer, subscription);
        // if func.call is synchronous, then the subscription won't matter as it can't ever be called
        // if func.call is asynchronous, then the subscription will get set and can be unsubscribed from
        subscription.setActual(func.call(atomicObserver));

        return subscription;
    }

    public static class UnitTest {

        @Test
        public void testCreate() {

            Observable<String> Observable = new OperationToObservableFunction<String>(new Func1<Subscription, Observer<String>>() {

                @Override
                public Subscription call(Observer<String> Observer) {
                    Observer.onNext("one");
                    Observer.onNext("two");
                    Observer.onNext("three");
                    Observer.onCompleted();
                    return ObservableExtensions.noOpSubscription();
                }

            });

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