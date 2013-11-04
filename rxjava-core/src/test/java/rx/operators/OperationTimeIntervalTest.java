package rx.operators;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.Observable;
import rx.Observer;
import rx.concurrency.TestScheduler;
import rx.subjects.PublishSubject;
import rx.util.TimeInterval;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

public class OperationTimeIntervalTest {

  private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

  @Mock
  private Observer<TimeInterval<Integer>> observer;

  private TestScheduler testScheduler;
  private PublishSubject<Integer> subject;
  private Observable<TimeInterval<Integer>> observable;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    testScheduler = new TestScheduler();
    subject = PublishSubject.create();
    observable = subject.timeInterval(testScheduler);
  }

  @Test
  public void testTimeInterval() {
    InOrder inOrder = inOrder(observer);
    observable.subscribe(observer);

    testScheduler.advanceTimeBy(1000, TIME_UNIT);
    subject.onNext(1);
    testScheduler.advanceTimeBy(2000, TIME_UNIT);
    subject.onNext(2);
    testScheduler.advanceTimeBy(3000, TIME_UNIT);
    subject.onNext(3);
    subject.onCompleted();

    inOrder.verify(observer, times(1)).onNext(
        new TimeInterval<Integer>(1000, 1));
    inOrder.verify(observer, times(1)).onNext(
        new TimeInterval<Integer>(2000, 2));
    inOrder.verify(observer, times(1)).onNext(
        new TimeInterval<Integer>(3000, 3));
    inOrder.verify(observer, times(1)).onCompleted();
    inOrder.verifyNoMoreInteractions();
  }
}
