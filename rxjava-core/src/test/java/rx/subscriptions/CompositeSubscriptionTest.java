package rx.subscriptions;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Subscription;
import rx.util.CompositeException;

public class CompositeSubscriptionTest {

    @Test
    public void testSuccess() {
        final AtomicInteger counter = new AtomicInteger();
        CompositeSubscription s = new CompositeSubscription();
        s.add(new Subscription() {

            @Override
            public void unsubscribe() {
                counter.incrementAndGet();
            }
        });

        s.add(new Subscription() {

            @Override
            public void unsubscribe() {
                counter.incrementAndGet();
            }
        });

        s.unsubscribe();

        assertEquals(2, counter.get());
    }

    @Test
    public void testException() {
        final AtomicInteger counter = new AtomicInteger();
        CompositeSubscription s = new CompositeSubscription();
        s.add(new Subscription() {

            @Override
            public void unsubscribe() {
                throw new RuntimeException("failed on first one");
            }
        });

        s.add(new Subscription() {

            @Override
            public void unsubscribe() {
                counter.incrementAndGet();
            }
        });

        try {
            s.unsubscribe();
            fail("Expecting an exception");
        } catch (CompositeException e) {
            // we expect this
            assertEquals(1, e.getExceptions().size());
        }

        // we should still have unsubscribed to the second one
        assertEquals(1, counter.get());
    }
}
