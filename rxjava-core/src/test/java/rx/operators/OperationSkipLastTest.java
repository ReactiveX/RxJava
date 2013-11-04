package rx.operators;

import org.junit.Test;
import org.mockito.InOrder;
import rx.Observable;
import rx.Observer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static rx.operators.OperationSkipLast.skipLast;

public class OperationSkipLastTest {

  @Test
  public void testSkipLastEmpty() {
    Observable<String> w = Observable.empty();
    Observable<String> observable = Observable.create(skipLast(w, 2));

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    observable.subscribe(aObserver);
    verify(aObserver, never()).onNext(any(String.class));
    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
  }

  @Test
  public void testSkipLast1() {
    Observable<String> w = Observable.from("one", "two", "three");
    Observable<String> observable = Observable.create(skipLast(w, 2));

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    InOrder inOrder = inOrder(aObserver);
    observable.subscribe(aObserver);
    inOrder.verify(aObserver, never()).onNext("two");
    inOrder.verify(aObserver, never()).onNext("three");
    verify(aObserver, times(1)).onNext("one");
    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
  }

  @Test
  public void testSkipLast2() {
    Observable<String> w = Observable.from("one", "two");
    Observable<String> observable = Observable.create(skipLast(w, 2));

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    observable.subscribe(aObserver);
    verify(aObserver, never()).onNext(any(String.class));
    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
  }

  @Test
  public void testSkipLastWithZeroCount() {
    Observable<String> w = Observable.from("one", "two");
    Observable<String> observable = Observable.create(skipLast(w, 0));

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    observable.subscribe(aObserver);
    verify(aObserver, times(1)).onNext("one");
    verify(aObserver, times(1)).onNext("two");
    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
  }

  @Test
  public void testSkipLastWithNull() {
    Observable<String> w = Observable.from("one", null, "two");
    Observable<String> observable = Observable.create(skipLast(w, 1));

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    observable.subscribe(aObserver);
    verify(aObserver, times(1)).onNext("one");
    verify(aObserver, times(1)).onNext(null);
    verify(aObserver, never()).onNext("two");
    verify(aObserver, never()).onError(any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
  }

  @Test
  public void testSkipLastWithNegativeCount() {
    Observable<String> w = Observable.from("one");
    Observable<String> observable = Observable.create(skipLast(w, -1));

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    observable.subscribe(aObserver);
    verify(aObserver, never()).onNext(any(String.class));
    verify(aObserver, times(1)).onError(
        any(IndexOutOfBoundsException.class));
    verify(aObserver, never()).onCompleted();
  }
}
