package rx.operators;

import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.util.functions.Func0;

import static org.mockito.Mockito.*;

public class OperationDeferTest {

  @Test
  @SuppressWarnings("unchecked")
  public void testDefer() throws Throwable {

    Func0<Observable<String>> factory = mock(Func0.class);

    Observable<String> firstObservable = Observable.from("one", "two");
    Observable<String> secondObservable = Observable.from("three", "four");
    when(factory.call()).thenReturn(firstObservable, secondObservable);

    Observable<String> deferred = Observable.defer(factory);

    verifyZeroInteractions(factory);

    Observer<String> firstObserver = mock(Observer.class);
    deferred.subscribe(firstObserver);

    verify(factory, times(1)).call();
    verify(firstObserver, times(1)).onNext("one");
    verify(firstObserver, times(1)).onNext("two");
    verify(firstObserver, times(0)).onNext("three");
    verify(firstObserver, times(0)).onNext("four");
    verify(firstObserver, times(1)).onCompleted();

    Observer<String> secondObserver = mock(Observer.class);
    deferred.subscribe(secondObserver);

    verify(factory, times(2)).call();
    verify(secondObserver, times(0)).onNext("one");
    verify(secondObserver, times(0)).onNext("two");
    verify(secondObserver, times(1)).onNext("three");
    verify(secondObserver, times(1)).onNext("four");
    verify(secondObserver, times(1)).onCompleted();

  }
}
