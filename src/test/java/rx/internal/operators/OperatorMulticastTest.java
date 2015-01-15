package rx.internal.operators;

import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;

public class OperatorMulticastTest {

    /**
     * test the basic expectation of OperatorMulticast via replay
     */
    @Test
    public void testIssue2191_UnsubscribeSource() {
        // setup mocks
        Action1 sourceNext = mock(Action1.class);
        Action0 sourceCompleted = mock(Action0.class);
        Action0 sourceUnsubscribed = mock(Action0.class);
        Subscriber spiedSubscriberBeforeConnect = subscriberSpy();
        Subscriber spiedSubscriberAfterConnect = subscriberSpy();

        // Observable under test
        Observable<Integer> source = Observable.just(1,2);

        ConnectableObservable<Integer> replay = source
                .doOnNext(sourceNext)
                .doOnUnsubscribe(sourceUnsubscribed)
                .doOnCompleted(sourceCompleted)
                .replay();

        replay.subscribe(spiedSubscriberBeforeConnect);
        replay.subscribe(spiedSubscriberBeforeConnect);
        replay.connect();
        replay.subscribe(spiedSubscriberAfterConnect);
        replay.subscribe(spiedSubscriberAfterConnect);


        // verify interactions
        verify(sourceNext, times(1)).call(1);
        verify(sourceNext, times(1)).call(2);
        verify(sourceCompleted, times(1)).call();
        verifySubscriberSpy(spiedSubscriberBeforeConnect, 2, 4);
        verifySubscriberSpy(spiedSubscriberAfterConnect, 2, 4);

        verify(sourceUnsubscribed, times(1)).call();

        verifyNoMoreInteractions(sourceNext);
        verifyNoMoreInteractions(sourceCompleted);
        verifyNoMoreInteractions(sourceUnsubscribed);
        verifyNoMoreInteractions(spiedSubscriberBeforeConnect);
        verifyNoMoreInteractions(spiedSubscriberAfterConnect);

    }

    /**
     * Specifically test interaction with a Scheduler with subscribeOn
     *
     * @throws Exception
     */
    @Test
    public void testIssue2191_SchedulerUnsubscribe() throws Exception {
        // setup mocks
        Action1 sourceNext = mock(Action1.class);
        Action0 sourceCompleted = mock(Action0.class);
        Action0 sourceUnsubscribed = mock(Action0.class);
        final Scheduler mockScheduler = mock(Scheduler.class);
        final Subscription mockSubscription = mock(Subscription.class);
        Worker spiedWorker = workerSpy(mockSubscription);
        Subscriber spiedSubscriberBeforeConnect = subscriberSpy();
        Subscriber spiedSubscriberAfterConnect = subscriberSpy();

        when(mockScheduler.createWorker()).thenReturn(spiedWorker);

        // Observable under test
        ConnectableObservable<Integer> replay = Observable.just(1, 2, 3)
                .doOnNext(sourceNext)
                .doOnUnsubscribe(sourceUnsubscribed)
                .doOnCompleted(sourceCompleted)
                .subscribeOn(mockScheduler).replay();

        replay.subscribe(spiedSubscriberBeforeConnect);
        replay.subscribe(spiedSubscriberBeforeConnect);
        replay.connect();
        replay.subscribe(spiedSubscriberAfterConnect);
        replay.subscribe(spiedSubscriberAfterConnect);

        // verify interactions
        verify(sourceNext, times(1)).call(1);
        verify(sourceNext, times(1)).call(2);
        verify(sourceNext, times(1)).call(3);
        verify(sourceCompleted, times(1)).call();
        verify(mockScheduler, times(1)).createWorker();
        verify(spiedWorker, times(1)).schedule((Action0)notNull());
        verifySubscriberSpy(spiedSubscriberBeforeConnect, 2, 6);
        verifySubscriberSpy(spiedSubscriberAfterConnect, 2, 6);

        verify(spiedWorker, times(1)).unsubscribe();
        verify(sourceUnsubscribed, times(1)).call();

        verifyNoMoreInteractions(sourceNext);
        verifyNoMoreInteractions(sourceCompleted);
        verifyNoMoreInteractions(sourceUnsubscribed);
        verifyNoMoreInteractions(spiedWorker);
        verifyNoMoreInteractions(mockSubscription);
        verifyNoMoreInteractions(mockScheduler);
        verifyNoMoreInteractions(spiedSubscriberBeforeConnect);
        verifyNoMoreInteractions(spiedSubscriberAfterConnect);
    }

    /**
     * Specifically test interaction with a Scheduler with subscribeOn
     *
     * @throws Exception
     */
    @Test
    public void testIssue2191_SchedulerUnsubscribeOnError() throws Exception {
        // setup mocks
        Action1 sourceNext = mock(Action1.class);
        Action0 sourceCompleted = mock(Action0.class);
        Action1 sourceError = mock(Action1.class);
        Action0 sourceUnsubscribed = mock(Action0.class);
        final Scheduler mockScheduler = mock(Scheduler.class);
        final Subscription mockSubscription = mock(Subscription.class);
        Worker spiedWorker = workerSpy(mockSubscription);
        Subscriber spiedSubscriberBeforeConnect = subscriberSpy();
        Subscriber spiedSubscriberAfterConnect = subscriberSpy();

        when(mockScheduler.createWorker()).thenReturn(spiedWorker);

        // Observable under test
        Func1<Integer, Integer> mockFunc = mock(Func1.class);
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
        when(mockFunc.call(1)).thenReturn(1);
        when(mockFunc.call(2)).thenThrow(illegalArgumentException);
        ConnectableObservable<Integer> replay = Observable.just(1, 2, 3).map(mockFunc)
                .doOnNext(sourceNext)
                .doOnUnsubscribe(sourceUnsubscribed)
                .doOnCompleted(sourceCompleted)
                .doOnError(sourceError)
                .subscribeOn(mockScheduler).replay();

        replay.subscribe(spiedSubscriberBeforeConnect);
        replay.subscribe(spiedSubscriberBeforeConnect);
        replay.connect();
        replay.subscribe(spiedSubscriberAfterConnect);
        replay.subscribe(spiedSubscriberAfterConnect);

        // verify interactions
        verify(mockScheduler, times(1)).createWorker();
        verify(spiedWorker, times(1)).schedule((Action0)notNull());
        verify(sourceNext, times(1)).call(1);
        verify(sourceError, times(1)).call(illegalArgumentException);
        verifySubscriberSpy(spiedSubscriberBeforeConnect, 2, 2, illegalArgumentException);
        verifySubscriberSpy(spiedSubscriberAfterConnect, 2, 2, illegalArgumentException);

        verify(spiedWorker, times(1)).unsubscribe();
        verify(sourceUnsubscribed, times(1)).call();

        verifyNoMoreInteractions(sourceNext);
        verifyNoMoreInteractions(sourceCompleted);
        verifyNoMoreInteractions(sourceError);
        verifyNoMoreInteractions(sourceUnsubscribed);
        verifyNoMoreInteractions(spiedWorker);
        verifyNoMoreInteractions(mockSubscription);
        verifyNoMoreInteractions(mockScheduler);
        verifyNoMoreInteractions(spiedSubscriberBeforeConnect);
        verifyNoMoreInteractions(spiedSubscriberAfterConnect);
    }

    public static Subscriber subscriberSpy() {
        return spy(new EmptySubscriber());
    }

    private void verifySubscriberSpy(Subscriber spiedSubscriber, int numSubscriptions, int numItemsExpected) {
        verify(spiedSubscriber, times(numSubscriptions)).onStart();
        verify(spiedSubscriber, times(numItemsExpected)).onNext(notNull());
        verify(spiedSubscriber, times(numSubscriptions)).onCompleted();
        verifyNoMoreInteractions(spiedSubscriber);
    }
    private void verifySubscriberSpy(Subscriber spiedSubscriber, int numSubscriptions, int numItemsExpected, Throwable error) {
        verify(spiedSubscriber, times(numSubscriptions)).onStart();
        verify(spiedSubscriber, times(numItemsExpected)).onNext(notNull());
        verify(spiedSubscriber, times(numSubscriptions)).onError(error);
        verifyNoMoreInteractions(spiedSubscriber);
    }

    public static Worker workerSpy(final Subscription mockSubscription) {
        return spy(new InprocessWorker(mockSubscription));
    }

    private static class EmptySubscriber extends Subscriber {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(Object o) {

        }
    }

    private static class InprocessWorker extends Worker {
        private final Subscription mockSubscription;
        public boolean unsubscribed;

        public InprocessWorker(Subscription mockSubscription) {
            this.mockSubscription = mockSubscription;
        }

        @Override
        public Subscription schedule(Action0 action) {
            action.call();
            return mockSubscription; // this subscription is returned but discarded
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            action.call();
            return mockSubscription;
        }

        @Override
        public void unsubscribe() {
            unsubscribed = true;
        }

        @Override
        public boolean isUnsubscribed() {
            return unsubscribed;
        }
    }
}