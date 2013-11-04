package rx.operators;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import rx.Observable;
import rx.Observer;
import rx.concurrency.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static rx.operators.OperationObserveOn.observeOn;

public class OperationObserveOnTest {

  /**
   * This is testing a no-op path since it uses Schedulers.immediate() which will not do scheduling.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testObserveOn() {
    Observer<Integer> observer = mock(Observer.class);
    Observable.create(observeOn(Observable.from(1, 2, 3), Schedulers.immediate())).subscribe(observer);

    verify(observer, times(1)).onNext(1);
    verify(observer, times(1)).onNext(2);
    verify(observer, times(1)).onNext(3);
    verify(observer, times(1)).onCompleted();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOrdering() throws InterruptedException {
    Observable<String> obs = Observable.from("one", null, "two", "three", "four");

    Observer<String> observer = mock(Observer.class);

    InOrder inOrder = inOrder(observer);

    final CountDownLatch completedLatch = new CountDownLatch(1);
    doAnswer(new Answer<Void>() {

      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        completedLatch.countDown();
        return null;
      }
    }).when(observer).onCompleted();

    obs.observeOn(Schedulers.threadPoolForComputation()).subscribe(observer);

    if (!completedLatch.await(1000, TimeUnit.MILLISECONDS)) {
      fail("timed out waiting");
    }

    inOrder.verify(observer, times(1)).onNext("one");
    inOrder.verify(observer, times(1)).onNext(null);
    inOrder.verify(observer, times(1)).onNext("two");
    inOrder.verify(observer, times(1)).onNext("three");
    inOrder.verify(observer, times(1)).onNext("four");
    inOrder.verify(observer, times(1)).onCompleted();
    inOrder.verifyNoMoreInteractions();
  }
}
