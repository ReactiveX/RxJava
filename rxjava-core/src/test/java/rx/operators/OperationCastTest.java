package rx.operators;

import static org.mockito.Mockito.*;
import static rx.operators.OperationCast.*;

import org.junit.Test;

import rx.Observable;
import rx.Observer;

public class OperationCastTest {

    @Test
    public void testCast() {
        Observable<?> source = Observable.from(1, 2);
        Observable<Integer> observable = Observable.create(cast(source,
                Integer.class));

        @SuppressWarnings("unchecked")
        Observer<Integer> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, times(1)).onNext(1);
        verify(aObserver, times(1)).onNext(1);
        verify(aObserver, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testCastWithWrongType() {
        Observable<?> source = Observable.from(1, 2);
        Observable<Boolean> observable = Observable.create(cast(source,
                Boolean.class));

        @SuppressWarnings("unchecked")
        Observer<Boolean> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, times(1)).onError(
                org.mockito.Matchers.any(ClassCastException.class));
    }
}
