package rx.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationFilter.*;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.util.functions.Func1;

public class OperationFilterTest {

    @Test
    public void testFilter() {
        Observable<String> w = Observable.from("one", "two", "three");
        Observable<String> observable = Observable.create(filter(w, new Func1<String, Boolean>() {

            @Override
            public Boolean call(String t1) {
                return t1.equals("two");
            }
        }));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, Mockito.never()).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, Mockito.never()).onNext("three");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }
}
