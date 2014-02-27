package rx.operators;

import org.junit.Test;
import rx.Observable;
import rx.Observer;

import static org.mockito.Mockito.*;

public class OperatorSkipTest {

    @Test
    public void testSkip1() {
        Observable<String> w = Observable.from("one", "two", "three");
        Observable<String> skip = w.lift(new OperatorSkip<String>(2));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        skip.subscribe(observer);
        verify(observer, never()).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSkip2() {
        Observable<String> w = Observable.from("one", "two", "three");
        Observable<String> skip = w.lift(new OperatorSkip<String>(1));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        skip.subscribe(observer);
        verify(observer, never()).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }
}
