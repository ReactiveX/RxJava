package rx.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationTakeLast.*;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;

public class OperationTakeLastTest {

    @Test
    public void testTakeLastEmpty() {
        Observable<String> w = Observable.empty();
        Observable<String> take = Observable.create(takeLast(w, 2));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        take.subscribe(aObserver);
        verify(aObserver, never()).onNext(any(String.class));
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testTakeLast1() {
        Observable<String> w = Observable.from("one", "two", "three");
        Observable<String> take = Observable.create(takeLast(w, 2));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        InOrder inOrder = inOrder(aObserver);
        take.subscribe(aObserver);
        inOrder.verify(aObserver, times(1)).onNext("two");
        inOrder.verify(aObserver, times(1)).onNext("three");
        verify(aObserver, never()).onNext("one");
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testTakeLast2() {
        Observable<String> w = Observable.from("one");
        Observable<String> take = Observable.create(takeLast(w, 10));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        take.subscribe(aObserver);
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testTakeLastWithZeroCount() {
        Observable<String> w = Observable.from("one");
        Observable<String> take = Observable.create(takeLast(w, 0));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        take.subscribe(aObserver);
        verify(aObserver, never()).onNext("one");
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testTakeLastWithNull() {
        Observable<String> w = Observable.from("one", null, "three");
        Observable<String> take = Observable.create(takeLast(w, 2));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        take.subscribe(aObserver);
        verify(aObserver, never()).onNext("one");
        verify(aObserver, times(1)).onNext(null);
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testTakeLastWithNegativeCount() {
        Observable<String> w = Observable.from("one");
        Observable<String> take = Observable.create(takeLast(w, -1));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        take.subscribe(aObserver);
        verify(aObserver, never()).onNext("one");
        verify(aObserver, times(1)).onError(
                any(IndexOutOfBoundsException.class));
        verify(aObserver, never()).onCompleted();
    }
}
