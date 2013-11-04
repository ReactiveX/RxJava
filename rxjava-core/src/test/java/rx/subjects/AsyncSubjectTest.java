package rx.subjects;

import org.junit.Test;
import org.mockito.Mockito;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Action1;
import rx.util.functions.Func0;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class AsyncSubjectTest {


  private final Throwable testException = new Throwable();

  @Test
  public void testNeverCompleted() {
    AsyncSubject<String> subject = AsyncSubject.create();

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    subject.subscribe(aObserver);

    subject.onNext("one");
    subject.onNext("two");
    subject.onNext("three");

    assertNeverCompletedObserver(aObserver);
  }

  private void assertNeverCompletedObserver(Observer<String> aObserver) {
    verify(aObserver, Mockito.never()).onNext(anyString());
    verify(aObserver, Mockito.never()).onError(testException);
    verify(aObserver, Mockito.never()).onCompleted();
  }

  @Test
  public void testCompleted() {
    AsyncSubject<String> subject = AsyncSubject.create();

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    subject.subscribe(aObserver);

    subject.onNext("one");
    subject.onNext("two");
    subject.onNext("three");
    subject.onCompleted();

    assertCompletedObserver(aObserver);
  }

  private void assertCompletedObserver(Observer<String> aObserver) {
    verify(aObserver, times(1)).onNext("three");
    verify(aObserver, Mockito.never()).onError(any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
  }

  @Test
  public void testError() {
    AsyncSubject<String> subject = AsyncSubject.create();

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    subject.subscribe(aObserver);

    subject.onNext("one");
    subject.onNext("two");
    subject.onNext("three");
    subject.onError(testException);
    subject.onNext("four");
    subject.onError(new Throwable());
    subject.onCompleted();

    assertErrorObserver(aObserver);
  }

  private void assertErrorObserver(Observer<String> aObserver) {
    verify(aObserver, Mockito.never()).onNext(anyString());
    verify(aObserver, times(1)).onError(testException);
    verify(aObserver, Mockito.never()).onCompleted();
  }

  @Test
  public void testUnsubscribeBeforeCompleted() {
    AsyncSubject<String> subject = AsyncSubject.create();

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    Subscription subscription = subject.subscribe(aObserver);

    subject.onNext("one");
    subject.onNext("two");

    subscription.unsubscribe();
    assertNoOnNextEventsReceived(aObserver);

    subject.onNext("three");
    subject.onCompleted();

    assertNoOnNextEventsReceived(aObserver);
  }

  private void assertNoOnNextEventsReceived(Observer<String> aObserver) {
    verify(aObserver, Mockito.never()).onNext(anyString());
    verify(aObserver, Mockito.never()).onError(any(Throwable.class));
    verify(aObserver, Mockito.never()).onCompleted();
  }

  @Test
  public void testUnsubscribe() {
    UnsubscribeTester.test(
        new Func0<AsyncSubject<Object>>() {
          @Override
          public AsyncSubject<Object> call() {
            return AsyncSubject.create();
          }
        }, new Action1<AsyncSubject<Object>>() {
          @Override
          public void call(AsyncSubject<Object> DefaultSubject) {
            DefaultSubject.onCompleted();
          }
        }, new Action1<AsyncSubject<Object>>() {
          @Override
          public void call(AsyncSubject<Object> DefaultSubject) {
            DefaultSubject.onError(new Throwable());
          }
        },
        null
    );
  }
}
