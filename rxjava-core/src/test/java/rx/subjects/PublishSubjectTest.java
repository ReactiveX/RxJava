package rx.subjects;

import junit.framework.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Action1;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class PublishSubjectTest {

  @Test
  public void test() {
    PublishSubject<Integer> subject = PublishSubject.create();
    final AtomicReference<List<Notification<Integer>>> actualRef = new AtomicReference<List<Notification<Integer>>>();

    Observable<List<Notification<Integer>>> wNotificationsList = subject.materialize().toList();
    wNotificationsList.subscribe(new Action1<List<Notification<Integer>>>() {
      @Override
      public void call(List<Notification<Integer>> actual) {
        actualRef.set(actual);
      }
    });

    Subscription sub = Observable.create(new Observable.OnSubscribeFunc<Integer>() {
      @Override
      public Subscription onSubscribe(final Observer<? super Integer> observer) {
        final AtomicBoolean stop = new AtomicBoolean(false);
        new Thread() {
          @Override
          public void run() {
            int i = 1;
            while (!stop.get()) {
              observer.onNext(i++);
            }
            observer.onCompleted();
          }
        }.start();
        return new Subscription() {
          @Override
          public void unsubscribe() {
            stop.set(true);
          }
        };
      }
    }).subscribe(subject);
    // the subject has received an onComplete from the first subscribe because
    // it is synchronous and the next subscribe won't do anything.
    Observable.from(-1, -2, -3).subscribe(subject);

    List<Notification<Integer>> expected = new ArrayList<Notification<Integer>>();
    expected.add(new Notification<Integer>(-1));
    expected.add(new Notification<Integer>(-2));
    expected.add(new Notification<Integer>(-3));
    expected.add(new Notification<Integer>());
    Assert.assertTrue(actualRef.get().containsAll(expected));

    sub.unsubscribe();
  }

  private final Throwable testException = new Throwable();

  @Test
  public void testCompleted() {
    PublishSubject<String> subject = PublishSubject.create();

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    subject.subscribe(aObserver);

    subject.onNext("one");
    subject.onNext("two");
    subject.onNext("three");
    subject.onCompleted();

    @SuppressWarnings("unchecked")
    Observer<String> anotherObserver = mock(Observer.class);
    subject.subscribe(anotherObserver);

    subject.onNext("four");
    subject.onCompleted();
    subject.onError(new Throwable());

    assertCompletedObserver(aObserver);
    // todo bug?            assertNeverObserver(anotherObserver);
  }

  private void assertCompletedObserver(Observer<String> aObserver) {
    verify(aObserver, times(1)).onNext("one");
    verify(aObserver, times(1)).onNext("two");
    verify(aObserver, times(1)).onNext("three");
    verify(aObserver, Mockito.never()).onError(any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
  }

  @Test
  public void testError() {
    PublishSubject<String> subject = PublishSubject.create();

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    subject.subscribe(aObserver);

    subject.onNext("one");
    subject.onNext("two");
    subject.onNext("three");
    subject.onError(testException);

    @SuppressWarnings("unchecked")
    Observer<String> anotherObserver = mock(Observer.class);
    subject.subscribe(anotherObserver);

    subject.onNext("four");
    subject.onError(new Throwable());
    subject.onCompleted();

    assertErrorObserver(aObserver);
    // todo bug?            assertNeverObserver(anotherObserver);
  }

  private void assertErrorObserver(Observer<String> aObserver) {
    verify(aObserver, times(1)).onNext("one");
    verify(aObserver, times(1)).onNext("two");
    verify(aObserver, times(1)).onNext("three");
    verify(aObserver, times(1)).onError(testException);
    verify(aObserver, Mockito.never()).onCompleted();
  }

  @Test
  public void testSubscribeMidSequence() {
    PublishSubject<String> subject = PublishSubject.create();

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    subject.subscribe(aObserver);

    subject.onNext("one");
    subject.onNext("two");

    assertObservedUntilTwo(aObserver);

    @SuppressWarnings("unchecked")
    Observer<String> anotherObserver = mock(Observer.class);
    subject.subscribe(anotherObserver);

    subject.onNext("three");
    subject.onCompleted();

    assertCompletedObserver(aObserver);
    assertCompletedStartingWithThreeObserver(anotherObserver);
  }

  private void assertCompletedStartingWithThreeObserver(Observer<String> aObserver) {
    verify(aObserver, Mockito.never()).onNext("one");
    verify(aObserver, Mockito.never()).onNext("two");
    verify(aObserver, times(1)).onNext("three");
    verify(aObserver, Mockito.never()).onError(any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
  }

  @Test
  public void testUnsubscribeFirstObserver() {
    PublishSubject<String> subject = PublishSubject.create();

    @SuppressWarnings("unchecked")
    Observer<String> aObserver = mock(Observer.class);
    Subscription subscription = subject.subscribe(aObserver);

    subject.onNext("one");
    subject.onNext("two");

    subscription.unsubscribe();
    assertObservedUntilTwo(aObserver);

    @SuppressWarnings("unchecked")
    Observer<String> anotherObserver = mock(Observer.class);
    subject.subscribe(anotherObserver);

    subject.onNext("three");
    subject.onCompleted();

    assertObservedUntilTwo(aObserver);
    assertCompletedStartingWithThreeObserver(anotherObserver);
  }

  private void assertObservedUntilTwo(Observer<String> aObserver) {
    verify(aObserver, times(1)).onNext("one");
    verify(aObserver, times(1)).onNext("two");
    verify(aObserver, Mockito.never()).onNext("three");
    verify(aObserver, Mockito.never()).onError(any(Throwable.class));
    verify(aObserver, Mockito.never()).onCompleted();
  }

  @Test
  public void testUnsubscribe() {
    UnsubscribeTester.test(
        new Func0<PublishSubject<Object>>() {
          @Override
          public PublishSubject<Object> call() {
            return PublishSubject.create();
          }
        }, new Action1<PublishSubject<Object>>() {
          @Override
          public void call(PublishSubject<Object> DefaultSubject) {
            DefaultSubject.onCompleted();
          }
        }, new Action1<PublishSubject<Object>>() {
          @Override
          public void call(PublishSubject<Object> DefaultSubject) {
            DefaultSubject.onError(new Throwable());
          }
        }, new Action1<PublishSubject<Object>>() {
          @Override
          public void call(PublishSubject<Object> DefaultSubject) {
            DefaultSubject.onNext("one");
          }
        }
    );
  }

  @Test
  public void testNestedSubscribe() {
    final PublishSubject<Integer> s = PublishSubject.create();

    final AtomicInteger countParent = new AtomicInteger();
    final AtomicInteger countChildren = new AtomicInteger();
    final AtomicInteger countTotal = new AtomicInteger();

    final ArrayList<String> list = new ArrayList<String>();

    s.mapMany(new Func1<Integer, Observable<String>>() {

      @Override
      public Observable<String> call(final Integer v) {
        countParent.incrementAndGet();

        // then subscribe to subject again (it will not receive the previous value)
        return s.map(new Func1<Integer, String>() {

          @Override
          public String call(Integer v2) {
            countChildren.incrementAndGet();
            return "Parent: " + v + " Child: " + v2;
          }

        });
      }

    }).subscribe(new Action1<String>() {

      @Override
      public void call(String v) {
        countTotal.incrementAndGet();
        list.add(v);
      }

    });

    for (int i = 0; i < 10; i++) {
      s.onNext(i);
    }
    s.onCompleted();

    //            System.out.println("countParent: " + countParent.get());
    //            System.out.println("countChildren: " + countChildren.get());
    //            System.out.println("countTotal: " + countTotal.get());

    // 9+8+7+6+5+4+3+2+1+0 == 45
    assertEquals(45, list.size());
  }

  /**
   * Should be able to unsubscribe all Observers, have it stop emitting, then subscribe new ones and it start emitting again.
   */
  @Test
  public void testReSubscribe() {
    final PublishSubject<Integer> ps = PublishSubject.create();

    Observer<Integer> o1 = mock(Observer.class);
    Subscription s1 = ps.subscribe(o1);

    // emit
    ps.onNext(1);

    // validate we got it
    InOrder inOrder1 = inOrder(o1);
    inOrder1.verify(o1, times(1)).onNext(1);
    inOrder1.verifyNoMoreInteractions();

    // unsubscribe
    s1.unsubscribe();

    // emit again but nothing will be there to receive it
    ps.onNext(2);

    Observer<Integer> o2 = mock(Observer.class);
    Subscription s2 = ps.subscribe(o2);

    // emit
    ps.onNext(3);

    // validate we got it
    InOrder inOrder2 = inOrder(o2);
    inOrder2.verify(o2, times(1)).onNext(3);
    inOrder2.verifyNoMoreInteractions();

    s2.unsubscribe();
  }

  /**
   * Even if subject received an onError/onCompleted, new subscriptions should be able to restart it.
   */
  @Test
  public void testReSubscribeAfterTerminalState() {
    final PublishSubject<Integer> ps = PublishSubject.create();

    Observer<Integer> o1 = mock(Observer.class);
    Subscription s1 = ps.subscribe(o1);

    // emit
    ps.onNext(1);

    // validate we got it
    InOrder inOrder1 = inOrder(o1);
    inOrder1.verify(o1, times(1)).onNext(1);
    inOrder1.verifyNoMoreInteractions();

    // unsubscribe
    s1.unsubscribe();

    ps.onCompleted();

    // emit again but nothing will be there to receive it
    ps.onNext(2);

    Observer<Integer> o2 = mock(Observer.class);
    Subscription s2 = ps.subscribe(o2);

    // emit
    ps.onNext(3);

    // validate we got it
    InOrder inOrder2 = inOrder(o2);
    inOrder2.verify(o2, times(1)).onNext(3);
    inOrder2.verifyNoMoreInteractions();

    s2.unsubscribe();
  }
}
