package rx.operators;

import org.junit.Test;
import rx.Observer;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import static org.mockito.Mockito.*;

public class OperationMulticastTest {

  @Test
  public void testMulticast() {
    Subject<String, String> source = PublishSubject.create();

    ConnectableObservable<String> multicasted = OperationMulticast.multicast(source,
        PublishSubject.<String>create());

    @SuppressWarnings("unchecked")
    Observer<String> observer = mock(Observer.class);
    multicasted.subscribe(observer);

    source.onNext("one");
    source.onNext("two");

    multicasted.connect();

    source.onNext("three");
    source.onNext("four");
    source.onCompleted();

    verify(observer, never()).onNext("one");
    verify(observer, never()).onNext("two");
    verify(observer, times(1)).onNext("three");
    verify(observer, times(1)).onNext("four");
    verify(observer, times(1)).onCompleted();

  }

  @Test
  public void testMulticastConnectTwice() {
    Subject<String, String> source = PublishSubject.create();

    ConnectableObservable<String> multicasted = OperationMulticast.multicast(source,
        PublishSubject.<String>create());

    @SuppressWarnings("unchecked")
    Observer<String> observer = mock(Observer.class);
    multicasted.subscribe(observer);

    source.onNext("one");

    multicasted.connect();
    multicasted.connect();

    source.onNext("two");
    source.onCompleted();

    verify(observer, never()).onNext("one");
    verify(observer, times(1)).onNext("two");
    verify(observer, times(1)).onCompleted();

  }

  @Test
  public void testMulticastDisconnect() {
    Subject<String, String> source = PublishSubject.create();

    ConnectableObservable<String> multicasted = OperationMulticast.multicast(source,
        PublishSubject.<String>create());

    @SuppressWarnings("unchecked")
    Observer<String> observer = mock(Observer.class);
    multicasted.subscribe(observer);

    source.onNext("one");

    Subscription connection = multicasted.connect();
    source.onNext("two");

    connection.unsubscribe();
    source.onNext("three");

    multicasted.connect();
    source.onNext("four");
    source.onCompleted();

    verify(observer, never()).onNext("one");
    verify(observer, times(1)).onNext("two");
    verify(observer, never()).onNext("three");
    verify(observer, times(1)).onNext("four");
    verify(observer, times(1)).onCompleted();

  }
}
