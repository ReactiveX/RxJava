package org.rx.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.Mockito;
import org.rx.functions.Func1;
import org.rx.reactive.Observable;
import org.rx.reactive.Observer;
import org.rx.reactive.Subscription;

public final class OperationFilter<T> {

    public static <T> Observable<T> filter(Observable<T> that, Func1<Boolean, T> predicate) {
        return new Filter<T>(that, predicate);
    }

    private static class Filter<T> extends Observable<T> {

        private final Observable<T> that;
        private final Func1<Boolean, T> predicate;

        public Filter(Observable<T> that, Func1<Boolean, T> predicate) {
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
    }

    public static class UnitTest {

        @Test
        public void testFilter() {
            Observable<String> w = Observable.toObservable("one", "two", "three");
            Observable<String> Observable = filter(w, new Func1<Boolean, String>() {

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
            verify(aObserver, Mockito.never()).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, Mockito.never()).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }
    }
}