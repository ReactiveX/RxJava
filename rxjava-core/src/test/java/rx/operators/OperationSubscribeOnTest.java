package rx.operators;

import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.test.OperatorTester;
import rx.util.functions.Action0;
import rx.util.functions.Func2;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;
import static rx.operators.OperationSubscribeOn.subscribeOn;

public class OperationSubscribeOnTest {

  @Test
  @SuppressWarnings("unchecked")
  public void testSubscribeOn() {
    Observable<Integer> w = Observable.from(1, 2, 3);

    Scheduler scheduler = spy(OperatorTester.forwardingScheduler(Schedulers.immediate()));

    Observer<Integer> observer = mock(Observer.class);
    Subscription subscription = Observable.create(subscribeOn(w, scheduler)).subscribe(observer);

    verify(scheduler, times(1)).schedule(isNull(), any(Func2.class));
    subscription.unsubscribe();
    verify(scheduler, times(1)).schedule(any(Action0.class));
    verifyNoMoreInteractions(scheduler);

    verify(observer, times(1)).onNext(1);
    verify(observer, times(1)).onNext(2);
    verify(observer, times(1)).onNext(3);
    verify(observer, times(1)).onCompleted();
  }
}
