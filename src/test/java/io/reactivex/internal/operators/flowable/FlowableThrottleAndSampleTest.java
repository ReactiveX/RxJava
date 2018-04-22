package io.reactivex.internal.operators.flowable;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.TestHelper;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FlowableThrottleAndSampleTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;
    private Subscriber<Long> observer;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
        observer = TestHelper.mockSubscriber();
    }

    @Test
    public void testThrottleAndSample() {
        // Source: -1-2-3-45------6-7-8-9|
        // Output: -1---3---5-----6---8--|

        final Flowable<Long> source = Flowable.unsafeCreate(new Publisher<Long>() {
            @Override
            public void subscribe(final Subscriber<? super Long> observer1) {
                observer1.onSubscribe(new BooleanSubscription());
                innerScheduler.schedule(new PublishRunnable(observer1, 1L), 1, TimeUnit.SECONDS);
                innerScheduler.schedule(new PublishRunnable(observer1, 2L), 3, TimeUnit.SECONDS);
                innerScheduler.schedule(new PublishRunnable(observer1, 3L), 5, TimeUnit.SECONDS);
                innerScheduler.schedule(new PublishRunnable(observer1, 4L), 7, TimeUnit.SECONDS);
                innerScheduler.schedule(new PublishRunnable(observer1, 5L), 8, TimeUnit.SECONDS);
                innerScheduler.schedule(new PublishRunnable(observer1, 6L), 15, TimeUnit.SECONDS);
                innerScheduler.schedule(new PublishRunnable(observer1, 7L), 17, TimeUnit.SECONDS);
                innerScheduler.schedule(new PublishRunnable(observer1, 8L), 19, TimeUnit.SECONDS);
                innerScheduler.schedule(new PublishRunnable(observer1, 9L), 21, TimeUnit.SECONDS);
                innerScheduler.schedule(new CompleteRunnable(observer1), 22, TimeUnit.SECONDS);
            }
        });

        final Flowable<Long> throttledAndSampled = source.throttleAndSample(4, TimeUnit.SECONDS, scheduler);
        throttledAndSampled.subscribe(observer);

        final InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(800L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(any(Long.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1L, TimeUnit.SECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        verify(observer, never()).onNext(2L);
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3L, TimeUnit.SECONDS);
        inOrder.verify(observer, never()).onNext(1L);
        inOrder.verify(observer, never()).onNext(2L);
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(5L, TimeUnit.SECONDS);
        inOrder.verify(observer, never()).onNext(1L);
        inOrder.verify(observer, times(1)).onNext(3L);
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(8L, TimeUnit.SECONDS);
        inOrder.verify(observer, never()).onNext(4L);
        inOrder.verify(observer, never()).onNext(5L);
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(9L, TimeUnit.SECONDS);
        inOrder.verify(observer, times(1)).onNext(5L);
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(15L, TimeUnit.SECONDS);
        inOrder.verify(observer, times(1)).onNext(6L);
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(18L, TimeUnit.SECONDS);
        inOrder.verify(observer, never()).onNext(7L);
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(19L, TimeUnit.SECONDS);
        inOrder.verify(observer, times(1)).onNext(8L);
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(22L, TimeUnit.SECONDS);
        inOrder.verify(observer, never()).onNext(9L);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testThrottleAndSampleEmitLast() {
        // Source: -1-2-(| )
        // Output: -1---(2|)

        final Flowable<Long> source = Flowable.unsafeCreate(new Publisher<Long>() {
            @Override
            public void subscribe(final Subscriber<? super Long> observer1) {
                observer1.onSubscribe(new BooleanSubscription());
                innerScheduler.schedule(new PublishRunnable(observer1, 1L), 1, TimeUnit.SECONDS);
                innerScheduler.schedule(new PublishRunnable(observer1, 2L), 3, TimeUnit.SECONDS);
                innerScheduler.schedule(new CompleteRunnable(observer1), 4, TimeUnit.SECONDS);
            }
        });

        final Flowable<Long> throttledAndSampled = source.throttleAndSample(4, TimeUnit.SECONDS, scheduler, true);
        throttledAndSampled.subscribe(observer);

        final InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(800L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(any(Long.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1L, TimeUnit.SECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        verify(observer, never()).onNext(2L);
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3L, TimeUnit.SECONDS);
        inOrder.verify(observer, never()).onNext(1L);
        inOrder.verify(observer, never()).onNext(2L);
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(4L, TimeUnit.SECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSampleUnsubscribe() {
        final Subscription subscription = mock(Subscription.class);
        final Flowable<Integer> o = Flowable.unsafeCreate(
                new Publisher<Integer>() {
                    @Override
                    public void subscribe(Subscriber<? super Integer> subscriber) {
                        subscriber.onSubscribe(subscription);
                    }
                }
        );
        o.throttleAndSample(1, TimeUnit.MILLISECONDS).subscribe().dispose();
        verify(subscription).cancel();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishProcessor.create().throttleAndSample(1, TimeUnit.SECONDS, new TestScheduler()));
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
                .throttleAndSample(1, TimeUnit.SECONDS)
                .test()
                .assertFailure(TestException.class);
    }

    @Test
    public void backpressureOverflow() {
        BehaviorProcessor.createDefault(1)
                .throttleAndSample(1, TimeUnit.MILLISECONDS)
                .test(0L)
                .awaitDone(5, TimeUnit.SECONDS)
                .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void emitLastTimed() {
        Flowable.just(1)
                .throttleAndSample(1, TimeUnit.DAYS, true)
                .test()
                .assertResult(1);
    }

    @Test
    public void emitLastTimedEmpty() {
        Flowable.empty()
                .throttleAndSample(1, TimeUnit.DAYS, true)
                .test()
                .assertResult();
    }

    @Test
    public void emitLastTimedCustomScheduler() {
        Flowable.just(1)
                .throttleAndSample(1, TimeUnit.DAYS, Schedulers.single(), true)
                .test()
                .assertResult(1);
    }

    @Test
    public void emitLastTimedRunCompleteRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestScheduler scheduler = new TestScheduler();

            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = pp.throttleAndSample(1, TimeUnit.SECONDS, scheduler, true)
                    .test();

            pp.onNext(1);

            final Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };

            final Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
                }
            };

            TestHelper.race(r1, r2);

            ts.assertResult(1);
        }
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> o) {
                return o.sample(1, TimeUnit.SECONDS);
            }
        });
    }

    private static final class PublishRunnable implements Runnable {

        final Subscriber<? super Long> observer;
        final long valueToPublish;


        public PublishRunnable(final Subscriber<? super Long> observer, final long valueToPublish) {
            this.observer = observer;
            this.valueToPublish = valueToPublish;
        }

        @Override
        public void run() {
            observer.onNext(valueToPublish);
        }
    }

    private static final class CompleteRunnable implements Runnable {

        final Subscriber<? super Long> observer;


        public CompleteRunnable(final Subscriber<? super Long> observer) {
            this.observer = observer;
        }

        @Override
        public void run() {
            observer.onComplete();
        }
    }
}