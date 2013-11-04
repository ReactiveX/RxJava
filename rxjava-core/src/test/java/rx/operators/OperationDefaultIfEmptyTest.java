package rx.operators;

import org.junit.Test;
import rx.Observable;
import rx.Observer;

import static org.mockito.Mockito.*;
import static rx.operators.OperationDefaultIfEmpty.defaultIfEmpty;

public class OperationDefaultIfEmptyTest {

  @Test
  public void testDefaultIfEmpty() {
    Observable<Integer> source = Observable.from(1, 2, 3);
    Observable<Integer> observable = Observable.create(defaultIfEmpty(
        source, 10));

    @SuppressWarnings("unchecked")
    Observer<Integer> aObserver = mock(Observer.class);
    observable.subscribe(aObserver);
    verify(aObserver, never()).onNext(10);
    verify(aObserver, times(1)).onNext(1);
    verify(aObserver, times(1)).onNext(2);
    verify(aObserver, times(1)).onNext(3);
    verify(aObserver, never()).onError(
        org.mockito.Matchers.any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
  }

  @Test
  public void testDefaultIfEmptyWithEmpty() {
    Observable<Integer> source = Observable.empty();
    Observable<Integer> observable = Observable.create(defaultIfEmpty(
        source, 10));

    @SuppressWarnings("unchecked")
    Observer<Integer> aObserver = mock(Observer.class);
    observable.subscribe(aObserver);
    verify(aObserver, times(1)).onNext(10);
    verify(aObserver, never()).onError(
        org.mockito.Matchers.any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
  }
}
