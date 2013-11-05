package rx.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationToObservableIterable.*;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;

public class OperationToObservableIterableTest {

    @Test
    public void testIterable() {
        Observable<String> observable = Observable.create(toObservableIterable(Arrays.<String> asList("one", "two", "three")));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }
}
