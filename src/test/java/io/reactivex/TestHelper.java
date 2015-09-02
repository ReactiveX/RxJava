package io.reactivex;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import org.mockito.Mockito;
import org.reactivestreams.*;

/**
 * Common methods for helping with tests from 1.x mostly.
 */
public enum TestHelper {
    ;
    /**
     * Mocks a subscriber and prepares it to request Long.MAX_VALUE.
     * @return the mocked subscriber
     */
    @SuppressWarnings("unchecked")
    public static <T> Subscriber<T> mockSubscriber() {
        Subscriber<T> w = mock(Subscriber.class);
        
        Mockito.doAnswer(a -> {
            Subscription s = a.getArgumentAt(0, Subscription.class);
            s.request(Long.MAX_VALUE);
            return null;
        }).when(w).onSubscribe(any());
        
        return w;
    }
}
