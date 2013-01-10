package org.rx.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.rx.functions.Func1;
import org.rx.reactive.Observable;
import org.rx.reactive.Observer;
import org.rx.reactive.Subscription;

/* package */final class OperationFilter<T> extends Observable<T> {
    private final Observable<T> that;
    private final Func1<Boolean, T> predicate;

    OperationFilter(Observable<T> that, Func1<Boolean, T> predicate) {
        this.that = that;
        this.predicate = predicate;
    }

    public Subscription subscribe(Observer<T> Observer) {
        final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
        final Observer<T> observer = new AtomicObserver<T>(Observer, subscription);

        subscription.setActual(that.subscribe(new Observer<T>() {
            public void onNext(T value) {
                try {
                    if ((boolean) predicate.call(value)) {
                        observer.onNext(value);
                    }
                } catch (Exception ex) {
                    observer.onError(ex);
                    subscription.unsubscribe();
                }
            }

            public void onError(Exception ex) {
                observer.onError(ex);
            }

            public void onCompleted() {
                observer.onCompleted();
            }
        }));

        return subscription;
    }

    public static class UnitTest {

        @Test
        public void testFilter() {
            Observable<String> w = ObservableExtensions.toObservable("one", "two", "three");
            Observable<String> Observable = new OperationFilter<String>(w, new Func1<Boolean, String>() {

                @Override
                public Boolean call(String t1) {
                    if (t1.equals("two"))
                        return true;
                    else
                        return false;
                }
            });

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            Observable.subscribe(aObserver);
            verify(aObserver, never()).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, never()).onNext("three");
            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }
    }
}