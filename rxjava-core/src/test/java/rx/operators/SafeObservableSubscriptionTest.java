package rx.operators;

import static org.mockito.Mockito.*;

import org.junit.Test;

import rx.Subscription;

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
