package rx.operators;

import org.junit.Test;
import rx.Observable;
import rx.Observer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static rx.operators.OperationSkip.skip;

public class OperationSkipTest {

  @Test
  public void testSkip1() {
    Observable<String> w = Observable.from("one", "two", "three");
    Observable<String> skip = Observable.create(skip(w, 2));

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    skip.subscribe(aObserver);
    verify(aObserver, never()).onNext("one");
    verify(aObserver, never()).onNext("two");
    verify(aObserver, times(1)).onNext("three");
    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
  }

  @Test
  public void testSkip2() {
    Observable<String> w = Observable.from("one", "two", "three");
    Observable<String> skip = Observable.create(skip(w, 1));

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    skip.subscribe(aObserver);
    verify(aObserver, never()).onNext("one");
    verify(aObserver, times(1)).onNext("two");
    verify(aObserver, times(1)).onNext("three");
    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
  }
}
