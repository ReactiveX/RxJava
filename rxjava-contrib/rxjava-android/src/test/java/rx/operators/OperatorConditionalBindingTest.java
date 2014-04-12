package rx.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.observers.TestSubscriber;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
public class OperatorConditionalBindingTest {

    private TestSubscriber<String> subscriber = new TestSubscriber<String>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldReleaseBoundReferenceIfPredicateFailsToPass() {
        final AtomicBoolean toggle = new AtomicBoolean(true);
        OperatorConditionalBinding<String, Object> op = new OperatorConditionalBinding<String, Object>(
                new Object(), new Func1<Object, Boolean>() {
            @Override
            public Boolean call(Object o) {
                return toggle.get();
            }
        });

        Subscriber<? super String> sub = op.call(subscriber);
        sub.onNext("one");
        toggle.set(false);
        sub.onNext("two");
        sub.onCompleted();
        sub.onError(new Exception());

        assertEquals(1, subscriber.getOnNextEvents().size());
        assertEquals(0, subscriber.getOnCompletedEvents().size());
        assertEquals(0, subscriber.getOnErrorEvents().size());
        assertNull(op.getBoundRef());
    }

    @Test
    public void shouldUnsubscribeFromSourceSequenceWhenPredicateFailsToPass() {
        OperatorConditionalBinding<String, Object> op = new OperatorConditionalBinding<String, Object>(
                new Object(), Functions.alwaysFalse());

        Subscriber<? super String> sub = op.call(subscriber);
        sub.onNext("one");
        sub.onNext("two");
        sub.onCompleted();
        sub.onError(new Exception());

        assertEquals(0, subscriber.getOnNextEvents().size());
        assertEquals(0, subscriber.getOnCompletedEvents().size());
        assertEquals(0, subscriber.getOnErrorEvents().size());
    }

    @Test
    public void unsubscribeWillUnsubscribeFromWrappedSubscriber() {
        OperatorConditionalBinding<String, Object> op = new OperatorConditionalBinding<String, Object>(new Object());

        op.call(subscriber).unsubscribe();
        subscriber.assertUnsubscribed();
    }
}
