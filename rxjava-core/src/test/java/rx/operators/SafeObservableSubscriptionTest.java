package rx.operators;

import org.junit.Ignore;
import org.junit.Test;
import rx.Subscription;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SafeObservableSubscriptionTest {

  @Test
  public void testWrapAfterUnsubscribe() {
    SafeObservableSubscription atomicObservableSubscription = new SafeObservableSubscription();
    atomicObservableSubscription.unsubscribe();
    Subscription innerSubscription = mock(Subscription.class);
    atomicObservableSubscription.wrap(innerSubscription);
    verify(innerSubscription, times(1)).unsubscribe();
  }
}
