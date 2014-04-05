package rx.operators;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import rx.Subscriber;
import rx.functions.Functions;
import rx.observers.TestSubscriber;
import rx.operators.OperatorWeakBinding.BoundPayload;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class OperatorWeakBindingTest {

    private TestSubscriber<BoundPayload<Object, String>> subscriber = new TestSubscriber<BoundPayload<Object, String>>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldForwardAllNotificationsWhenTargetAlive() {
        final Object target = new Object();
        OperatorWeakBinding<String, Object> op = new OperatorWeakBinding<String, Object>(target);

        final Subscriber<? super String> extendedSub = op.call(subscriber);
        extendedSub.onNext("one");
        extendedSub.onNext("two");
        subscriber.onCompleted();
        subscriber.onError(new Exception());

        subscriber.assertReceivedOnNext(Arrays.asList(BoundPayload.of(target, "one"), BoundPayload.of(target, "two")));
        assertEquals(1, subscriber.getOnCompletedEvents().size());
        assertEquals(1, subscriber.getOnErrorEvents().size());
    }

    @Test
    public void shouldUnsubscribeFromSourceSequenceWhenTargetObjectReleased() {
        final Object target = new Object();
        OperatorWeakBinding<String, Object> op = new OperatorWeakBinding<String, Object>(target);

        final Subscriber<? super String> extendedSub = op.call(subscriber);
        extendedSub.onNext("one");
        op.boundRef.clear();
        extendedSub.onNext("two");
        extendedSub.onCompleted();
        extendedSub.onError(new Exception());

        subscriber.assertReceivedOnNext(Arrays.asList(BoundPayload.of(target, "one")));
        assertEquals(0, subscriber.getOnCompletedEvents().size());
        assertEquals(0, subscriber.getOnErrorEvents().size());
    }

    @Test
    public void shouldUnsubscribeFromSourceSequenceWhenPredicateFailsToPass() {
        OperatorWeakBinding<String, Object> op = new OperatorWeakBinding<String, Object>(
                new Object(), Functions.alwaysFalse());

        final Subscriber<? super String> extendedSub = op.call(subscriber);
        extendedSub.onNext("one");
        extendedSub.onNext("two");
        extendedSub.onCompleted();
        extendedSub.onError(new Exception());

        assertEquals(0, subscriber.getOnNextEvents().size());
        assertEquals(0, subscriber.getOnCompletedEvents().size());
        assertEquals(0, subscriber.getOnErrorEvents().size());
    }

    @Test
    public void unsubscribeWillUnsubscribeFromWrappedSubscriber() {
        OperatorWeakBinding<String, Object> op = new OperatorWeakBinding<String, Object>(new Object());

        op.call(subscriber).unsubscribe();
        subscriber.assertUnsubscribed();
    }
}
