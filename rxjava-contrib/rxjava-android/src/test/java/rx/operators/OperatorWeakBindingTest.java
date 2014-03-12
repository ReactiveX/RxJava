package rx.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import rx.Subscriber;
import rx.functions.Functions;

@RunWith(RobolectricTestRunner.class)
public class OperatorWeakBindingTest {

    @Mock
    private Subscriber<String> subscriber;

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

        verify(subscriber).onNext("one");
        verify(subscriber).onNext("two");
        verify(subscriber).onCompleted();
        verify(subscriber).onError(any(Exception.class));
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

        verify(subscriber).onNext("one");
        verifyNoMoreInteractions(subscriber);
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

        verify(subscriber).onNext("one");
        verifyNoMoreInteractions(subscriber);
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

        verifyZeroInteractions(subscriber);
    }
}
