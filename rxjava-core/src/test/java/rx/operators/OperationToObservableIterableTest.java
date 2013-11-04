package rx.operators;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static rx.operators.OperationToObservableIterable.toObservableIterable;

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
