package rx.operators;

import org.junit.Test;
import rx.Notification;
import rx.Observable;
import rx.Observer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static rx.operators.OperationDematerialize.dematerialize;

public class OperationDematerializeTest {

  @Test
  @SuppressWarnings("unchecked")
  public void testDematerialize1() {
    Observable<Notification<Integer>> notifications = Observable.from(1, 2).materialize();
    Observable<Integer> dematerialize = notifications.dematerialize();

    Observer<Integer> aObserver = mock(Observer.class);
    dematerialize.subscribe(aObserver);

    verify(aObserver, times(1)).onNext(1);
    verify(aObserver, times(1)).onNext(2);
    verify(aObserver, times(1)).onCompleted();
    verify(aObserver, never()).onError(any(Throwable.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testDematerialize2() {
    Throwable exception = new Throwable("test");
    Observable<Integer> observable = Observable.error(exception);
    Observable<Integer> dematerialize = Observable.create(dematerialize(observable.materialize()));

    Observer<Integer> aObserver = mock(Observer.class);
    dematerialize.subscribe(aObserver);

    verify(aObserver, times(1)).onError(exception);
    verify(aObserver, times(0)).onCompleted();
    verify(aObserver, times(0)).onNext(any(Integer.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testDematerialize3() {
    Exception exception = new Exception("test");
    Observable<Integer> observable = Observable.error(exception);
    Observable<Integer> dematerialize = Observable.create(dematerialize(observable.materialize()));

    Observer<Integer> aObserver = mock(Observer.class);
    dematerialize.subscribe(aObserver);

    verify(aObserver, times(1)).onError(exception);
    verify(aObserver, times(0)).onCompleted();
    verify(aObserver, times(0)).onNext(any(Integer.class));
  }
}
