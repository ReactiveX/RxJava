package rx.operators;

import static org.mockito.Mockito.*;
import static rx.operators.OperationAny.*;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.util.functions.Func1;

public class OperationAnyTest {

    @Test
    public void testAnyWithTwoItems() {
        Observable<Integer> w = Observable.from(1, 2);
        Observable<Boolean> observable = Observable.create(any(w));

        @SuppressWarnings("unchecked")
        Observer<Boolean> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, never()).onNext(false);
        verify(aObserver, times(1)).onNext(true);
        verify(aObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testIsEmptyWithTwoItems() {
        Observable<Integer> w = Observable.from(1, 2);
        Observable<Boolean> observable = Observable.create(isEmpty(w));

        @SuppressWarnings("unchecked")
        Observer<Boolean> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, never()).onNext(true);
        verify(aObserver, times(1)).onNext(false);
        verify(aObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithOneItem() {
        Observable<Integer> w = Observable.from(1);
        Observable<Boolean> observable = Observable.create(any(w));

        @SuppressWarnings("unchecked")
        Observer<Boolean> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, never()).onNext(false);
        verify(aObserver, times(1)).onNext(true);
        verify(aObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testIsEmptyWithOneItem() {
        Observable<Integer> w = Observable.from(1);
        Observable<Boolean> observable = Observable.create(isEmpty(w));

        @SuppressWarnings("unchecked")
        Observer<Boolean> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, never()).onNext(true);
        verify(aObserver, times(1)).onNext(false);
        verify(aObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = Observable.create(any(w));

        @SuppressWarnings("unchecked")
        Observer<Boolean> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, times(1)).onNext(false);
        verify(aObserver, never()).onNext(true);
        verify(aObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testIsEmptyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = Observable.create(isEmpty(w));

        @SuppressWarnings("unchecked")
        Observer<Boolean> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, times(1)).onNext(true);
        verify(aObserver, never()).onNext(false);
        verify(aObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithPredicate1() {
        Observable<Integer> w = Observable.from(1, 2, 3);
        Observable<Boolean> observable = Observable.create(any(w,
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 < 2;
                    }
                }));

        @SuppressWarnings("unchecked")
        Observer<Boolean> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, never()).onNext(false);
        verify(aObserver, times(1)).onNext(true);
        verify(aObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testExists1() {
        Observable<Integer> w = Observable.from(1, 2, 3);
        Observable<Boolean> observable = Observable.create(exists(w,
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 < 2;
                    }
                }));

        @SuppressWarnings("unchecked")
        Observer<Boolean> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, never()).onNext(false);
        verify(aObserver, times(1)).onNext(true);
        verify(aObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithPredicate2() {
        Observable<Integer> w = Observable.from(1, 2, 3);
        Observable<Boolean> observable = Observable.create(any(w,
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 < 1;
                    }
                }));

        @SuppressWarnings("unchecked")
        Observer<Boolean> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, times(1)).onNext(false);
        verify(aObserver, never()).onNext(true);
        verify(aObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithEmptyAndPredicate() {
        // If the source is empty, always output false.
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = Observable.create(any(w,
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return true;
                    }
                }));

        @SuppressWarnings("unchecked")
        Observer<Boolean> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, times(1)).onNext(false);
        verify(aObserver, never()).onNext(true);
        verify(aObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }
}
