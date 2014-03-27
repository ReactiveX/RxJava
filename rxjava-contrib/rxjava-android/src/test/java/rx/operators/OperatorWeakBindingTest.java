package rx.operators;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import rx.functions.Functions;
import rx.observers.TestSubscriber;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class OperatorWeakBindingTest {

    private TestSubscriber<String> subscriber = new TestSubscriber<String>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldForwardAllNotificationsWhenSubscriberAndTargetAlive() {
        OperatorWeakBinding<String, Object> op = new OperatorWeakBinding<String, Object>(new Object());
        OperatorWeakBinding.WeakSubscriber weakSub = (OperatorWeakBinding.WeakSubscriber) op.call(subscriber);
        weakSub.onNext("one");
        weakSub.onNext("two");
        weakSub.onCompleted();
        weakSub.onError(new Exception());

        subscriber.assertReceivedOnNext(Arrays.asList("one", "two"));
        assertEquals(1, subscriber.getOnCompletedEvents().size());
        assertEquals(1, subscriber.getOnErrorEvents().size());
    }

    @Test
    public void shouldUnsubscribeFromSourceSequenceWhenSubscriberReleased() {
        OperatorWeakBinding<String, Object> op = new OperatorWeakBinding<String, Object>(new Object());

        OperatorWeakBinding.WeakSubscriber weakSub = (OperatorWeakBinding.WeakSubscriber) op.call(subscriber);
        weakSub.onNext("one");
        weakSub.subscriberRef.clear();
        weakSub.onNext("two");
        weakSub.onCompleted();
        weakSub.onError(new Exception());

        subscriber.assertReceivedOnNext(Arrays.asList("one"));
        assertEquals(0, subscriber.getOnCompletedEvents().size());
        assertEquals(0, subscriber.getOnErrorEvents().size());
    }

    @Test
    public void shouldUnsubscribeFromSourceSequenceWhenTargetObjectReleased() {
        OperatorWeakBinding<String, Object> op = new OperatorWeakBinding<String, Object>(new Object());

        OperatorWeakBinding.WeakSubscriber weakSub = (OperatorWeakBinding.WeakSubscriber) op.call(subscriber);
        weakSub.onNext("one");
        op.boundRef.clear();
        weakSub.onNext("two");
        weakSub.onCompleted();
        weakSub.onError(new Exception());

        subscriber.assertReceivedOnNext(Arrays.asList("one"));
        assertEquals(0, subscriber.getOnCompletedEvents().size());
        assertEquals(0, subscriber.getOnErrorEvents().size());
    }

    @Test
    public void shouldUnsubscribeFromSourceSequenceWhenPredicateFailsToPass() {
        OperatorWeakBinding<String, Object> op = new OperatorWeakBinding<String, Object>(
                new Object(), Functions.alwaysFalse());

        OperatorWeakBinding.WeakSubscriber weakSub = (OperatorWeakBinding.WeakSubscriber) op.call(subscriber);
        weakSub.onNext("one");
        weakSub.onNext("two");
        weakSub.onCompleted();
        weakSub.onError(new Exception());

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
