package rx.subscriptions;

import static org.mockito.Mockito.*;
import static rx.subscriptions.Subscriptions.*;

import org.junit.Test;

import rx.Subscription;
import rx.util.functions.Action0;

public class SubscriptionsTest {

    @Test
    public void testUnsubscribeOnlyOnce() {
        Action0 unsubscribe = mock(Action0.class);
        Subscription subscription = create(unsubscribe);
        subscription.unsubscribe();
        subscription.unsubscribe();
        verify(unsubscribe, times(1)).call();
    }
}
