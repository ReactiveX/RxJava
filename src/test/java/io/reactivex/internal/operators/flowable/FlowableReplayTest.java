/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import io.reactivex.annotations.NonNull;
import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.internal.operators.flowable.FlowableReplay.*;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableReplayTest {
    @Test
    public void testBufferedReplay() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        ConnectableFlowable<Integer> co = source.replay(3);
        co.connect();

        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            source.onNext(4);
            source.onComplete();
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testBufferedWindowReplay() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        TestScheduler scheduler = new TestScheduler();
        ConnectableFlowable<Integer> co = source.replay(3, 100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            source.onNext(4);
            source.onNext(5);
            scheduler.advanceTimeBy(90, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(4);

            inOrder.verify(observer1, times(1)).onNext(5);

            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(5);
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testWindowedReplay() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> source = PublishProcessor.create();

        ConnectableFlowable<Integer> co = source.replay(100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onComplete();
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }
        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);
            inOrder.verify(observer1, never()).onNext(3);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testReplaySelector() {
        final Function<Integer, Integer> dbl = new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                return t1 * 2;
            }

        };

        Function<Flowable<Integer>, Flowable<Integer>> selector = new Function<Flowable<Integer>, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Flowable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        PublishProcessor<Integer> source = PublishProcessor.create();

        Flowable<Integer> co = source.replay(selector);

        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            source.onNext(4);
            source.onComplete();
            inOrder.verify(observer1, times(1)).onNext(8);
            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

    }

    @Test
    public void testBufferedReplaySelector() {

        final Function<Integer, Integer> dbl = new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                return t1 * 2;
            }

        };

        Function<Flowable<Integer>, Flowable<Integer>> selector = new Function<Flowable<Integer>, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Flowable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        PublishProcessor<Integer> source = PublishProcessor.create();

        Flowable<Integer> co = source.replay(selector, 3);

        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            source.onNext(4);
            source.onComplete();
            inOrder.verify(observer1, times(1)).onNext(8);
            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testWindowedReplaySelector() {

        final Function<Integer, Integer> dbl = new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                return t1 * 2;
            }

        };

        Function<Flowable<Integer>, Flowable<Integer>> selector = new Function<Flowable<Integer>, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Flowable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> source = PublishProcessor.create();

        Flowable<Integer> co = source.replay(selector, 100, TimeUnit.MILLISECONDS, scheduler);

        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onComplete();
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }
        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testBufferedReplayError() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        ConnectableFlowable<Integer> co = source.replay(3);
        co.connect();

        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            source.onNext(4);
            source.onError(new RuntimeException("Forced failure"));

            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onComplete();

        }

        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onComplete();
        }
    }

    @Test
    public void testWindowedReplayError() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> source = PublishProcessor.create();

        ConnectableFlowable<Integer> co = source.replay(100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onError(new RuntimeException("Forced failure"));
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onComplete();

        }
        {
            Subscriber<Object> observer1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);
            inOrder.verify(observer1, never()).onNext(3);

            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onComplete();
        }
    }

    @Test
    public void testSynchronousDisconnect() {
        final AtomicInteger effectCounter = new AtomicInteger();
        Flowable<Integer> source = Flowable.just(1, 2, 3, 4)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) {
                effectCounter.incrementAndGet();
                System.out.println("Sideeffect #" + v);
            }
        });

        Flowable<Integer> result = source.replay(
        new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return o.take(2);
            }
        });

        for (int i = 1; i < 3; i++) {
            effectCounter.set(0);
            System.out.printf("- %d -%n", i);
            result.subscribe(new Consumer<Integer>() {

                @Override
                public void accept(Integer t1) {
                    System.out.println(t1);
                }

            }, new Consumer<Throwable>() {

                @Override
                public void accept(Throwable t1) {
                    t1.printStackTrace();
                }
            },
            new Action() {
                @Override
                public void run() {
                    System.out.println("Done");
                }
            });
            assertEquals(2, effectCounter.get());
        }
    }


    /*
     * test the basic expectation of OperatorMulticast via replay
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue2191_UnsubscribeSource() throws Exception {
        // setup mocks
        Consumer<Integer> sourceNext = mock(Consumer.class);
        Action sourceCompleted = mock(Action.class);
        Action sourceUnsubscribed = mock(Action.class);
        Subscriber<Integer> spiedSubscriberBeforeConnect = TestHelper.mockSubscriber();
        Subscriber<Integer> spiedSubscriberAfterConnect = TestHelper.mockSubscriber();

        // Flowable under test
        Flowable<Integer> source = Flowable.just(1,2);

        ConnectableFlowable<Integer> replay = source
                .doOnNext(sourceNext)
                .doOnCancel(sourceUnsubscribed)
                .doOnComplete(sourceCompleted)
                .replay();

        replay.subscribe(spiedSubscriberBeforeConnect);
        replay.subscribe(spiedSubscriberBeforeConnect);
        replay.connect();
        replay.subscribe(spiedSubscriberAfterConnect);
        replay.subscribe(spiedSubscriberAfterConnect);

        verify(spiedSubscriberBeforeConnect, times(2)).onSubscribe((Subscription)any());
        verify(spiedSubscriberAfterConnect, times(2)).onSubscribe((Subscription)any());

        // verify interactions
        verify(sourceNext, times(1)).accept(1);
        verify(sourceNext, times(1)).accept(2);
        verify(sourceCompleted, times(1)).run();
        verifyObserverMock(spiedSubscriberBeforeConnect, 2, 4);
        verifyObserverMock(spiedSubscriberAfterConnect, 2, 4);

        verify(sourceUnsubscribed, never()).run();

        verifyNoMoreInteractions(sourceNext);
        verifyNoMoreInteractions(sourceCompleted);
        verifyNoMoreInteractions(sourceUnsubscribed);
        verifyNoMoreInteractions(spiedSubscriberBeforeConnect);
        verifyNoMoreInteractions(spiedSubscriberAfterConnect);

    }

    /**
     * Specifically test interaction with a Scheduler with subscribeOn.
     *
     * @throws Exception functional interfaces declare throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue2191_SchedulerUnsubscribe() throws Exception {
        // setup mocks
        Consumer<Integer> sourceNext = mock(Consumer.class);
        Action sourceCompleted = mock(Action.class);
        Action sourceUnsubscribed = mock(Action.class);
        final Scheduler mockScheduler = mock(Scheduler.class);
        final Disposable mockSubscription = mock(Disposable.class);
        Worker spiedWorker = workerSpy(mockSubscription);
        Subscriber<Integer> mockObserverBeforeConnect = TestHelper.mockSubscriber();
        Subscriber<Integer> mockObserverAfterConnect = TestHelper.mockSubscriber();

        when(mockScheduler.createWorker()).thenReturn(spiedWorker);

        // Flowable under test
        ConnectableFlowable<Integer> replay = Flowable.just(1, 2, 3)
                .doOnNext(sourceNext)
                .doOnCancel(sourceUnsubscribed)
                .doOnComplete(sourceCompleted)
                .subscribeOn(mockScheduler).replay();

        replay.subscribe(mockObserverBeforeConnect);
        replay.subscribe(mockObserverBeforeConnect);
        replay.connect();
        replay.subscribe(mockObserverAfterConnect);
        replay.subscribe(mockObserverAfterConnect);

        verify(mockObserverBeforeConnect, times(2)).onSubscribe((Subscription)any());
        verify(mockObserverAfterConnect, times(2)).onSubscribe((Subscription)any());

        // verify interactions
        verify(sourceNext, times(1)).accept(1);
        verify(sourceNext, times(1)).accept(2);
        verify(sourceNext, times(1)).accept(3);
        verify(sourceCompleted, times(1)).run();
        verify(mockScheduler, times(1)).createWorker();
        verify(spiedWorker, times(1)).schedule((Runnable)notNull());
        verifyObserverMock(mockObserverBeforeConnect, 2, 6);
        verifyObserverMock(mockObserverAfterConnect, 2, 6);

        // FIXME publish calls cancel too
        verify(spiedWorker, times(1)).dispose();
        verify(sourceUnsubscribed, never()).run();

        verifyNoMoreInteractions(sourceNext);
        verifyNoMoreInteractions(sourceCompleted);
        verifyNoMoreInteractions(sourceUnsubscribed);
        verifyNoMoreInteractions(spiedWorker);
        verifyNoMoreInteractions(mockSubscription);
        verifyNoMoreInteractions(mockScheduler);
        verifyNoMoreInteractions(mockObserverBeforeConnect);
        verifyNoMoreInteractions(mockObserverAfterConnect);
    }

    /**
     * Specifically test interaction with a Scheduler with subscribeOn.
     *
     * @throws Exception functional interfaces declare throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue2191_SchedulerUnsubscribeOnError() throws Exception {
        // setup mocks
        Consumer<Integer> sourceNext = mock(Consumer.class);
        Action sourceCompleted = mock(Action.class);
        Consumer<Throwable> sourceError = mock(Consumer.class);
        Action sourceUnsubscribed = mock(Action.class);
        final Scheduler mockScheduler = mock(Scheduler.class);
        final Disposable mockSubscription = mock(Disposable.class);
        Worker spiedWorker = workerSpy(mockSubscription);
        Subscriber<Integer> mockObserverBeforeConnect = TestHelper.mockSubscriber();
        Subscriber<Integer> mockObserverAfterConnect = TestHelper.mockSubscriber();

        when(mockScheduler.createWorker()).thenReturn(spiedWorker);

        // Flowable under test
        Function<Integer, Integer> mockFunc = mock(Function.class);
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
        when(mockFunc.apply(1)).thenReturn(1);
        when(mockFunc.apply(2)).thenThrow(illegalArgumentException);
        ConnectableFlowable<Integer> replay = Flowable.just(1, 2, 3).map(mockFunc)
                .doOnNext(sourceNext)
                .doOnCancel(sourceUnsubscribed)
                .doOnComplete(sourceCompleted)
                .doOnError(sourceError)
                .subscribeOn(mockScheduler).replay();

        replay.subscribe(mockObserverBeforeConnect);
        replay.subscribe(mockObserverBeforeConnect);
        replay.connect();
        replay.subscribe(mockObserverAfterConnect);
        replay.subscribe(mockObserverAfterConnect);

        verify(mockObserverBeforeConnect, times(2)).onSubscribe((Subscription)any());
        verify(mockObserverAfterConnect, times(2)).onSubscribe((Subscription)any());

        // verify interactions
        verify(mockScheduler, times(1)).createWorker();
        verify(spiedWorker, times(1)).schedule((Runnable)notNull());
        verify(sourceNext, times(1)).accept(1);
        verify(sourceError, times(1)).accept(illegalArgumentException);
        verifyObserver(mockObserverBeforeConnect, 2, 2, illegalArgumentException);
        verifyObserver(mockObserverAfterConnect, 2, 2, illegalArgumentException);

        // FIXME publish also calls cancel
        verify(spiedWorker, times(1)).dispose();
        verify(sourceUnsubscribed, never()).run();

        verifyNoMoreInteractions(sourceNext);
        verifyNoMoreInteractions(sourceCompleted);
        verifyNoMoreInteractions(sourceError);
        verifyNoMoreInteractions(sourceUnsubscribed);
        verifyNoMoreInteractions(spiedWorker);
        verifyNoMoreInteractions(mockSubscription);
        verifyNoMoreInteractions(mockScheduler);
        verifyNoMoreInteractions(mockObserverBeforeConnect);
        verifyNoMoreInteractions(mockObserverAfterConnect);
    }

    private static void verifyObserverMock(Subscriber<Integer> mock, int numSubscriptions, int numItemsExpected) {
        verify(mock, times(numItemsExpected)).onNext((Integer) notNull());
        verify(mock, times(numSubscriptions)).onComplete();
        verifyNoMoreInteractions(mock);
    }

    private static void verifyObserver(Subscriber<Integer> mock, int numSubscriptions, int numItemsExpected, Throwable error) {
        verify(mock, times(numItemsExpected)).onNext((Integer) notNull());
        verify(mock, times(numSubscriptions)).onError(error);
        verifyNoMoreInteractions(mock);
    }

    public static Worker workerSpy(final Disposable mockDisposable) {
        return spy(new InprocessWorker(mockDisposable));
    }


    private static class InprocessWorker extends Worker {
        private final Disposable mockDisposable;
        public boolean unsubscribed;

        InprocessWorker(Disposable mockDisposable) {
            this.mockDisposable = mockDisposable;
        }

        @NonNull
        @Override
        public Disposable schedule(@NonNull Runnable action) {
            action.run();
            return mockDisposable; // this subscription is returned but discarded
        }

        @NonNull
        @Override
        public Disposable schedule(@NonNull Runnable action, long delayTime, @NonNull TimeUnit unit) {
            action.run();
            return mockDisposable;
        }

        @Override
        public void dispose() {
            unsubscribed = true;
        }

        @Override
        public boolean isDisposed() {
            return unsubscribed;
        }
    }

    @Test
    public void testBoundedReplayBuffer() {
        BoundedReplayBuffer<Integer> buf = new BoundedReplayBuffer<Integer>();
        buf.addLast(new Node(1, 0));
        buf.addLast(new Node(2, 1));
        buf.addLast(new Node(3, 2));
        buf.addLast(new Node(4, 3));
        buf.addLast(new Node(5, 4));

        List<Integer> values = new ArrayList<Integer>();
        buf.collect(values);

        Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5), values);

        buf.removeSome(2);
        buf.removeFirst();
        buf.removeSome(2);

        values.clear();
        buf.collect(values);
        Assert.assertTrue(values.isEmpty());

        buf.addLast(new Node(5, 5));
        buf.addLast(new Node(6, 6));
        buf.collect(values);

        Assert.assertEquals(Arrays.asList(5, 6), values);

    }

    @Test
    public void testTimedAndSizedTruncation() {
        TestScheduler test = new TestScheduler();
        SizeAndTimeBoundReplayBuffer<Integer> buf = new SizeAndTimeBoundReplayBuffer<Integer>(2, 2000, TimeUnit.MILLISECONDS, test);
        List<Integer> values = new ArrayList<Integer>();

        buf.next(1);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        buf.next(2);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(2), values);

        buf.next(3);
        buf.next(4);
        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(3, 4), values);

        test.advanceTimeBy(2, TimeUnit.SECONDS);
        buf.next(5);

        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(5), values);

        test.advanceTimeBy(2, TimeUnit.SECONDS);
        buf.complete();

        values.clear();
        buf.collect(values);
        Assert.assertTrue(values.isEmpty());

        Assert.assertEquals(1, buf.size);
        Assert.assertTrue(buf.hasCompleted());
    }

    @Test
    public void testBackpressure() {
        final AtomicLong requested = new AtomicLong();
        Flowable<Integer> source = Flowable.range(1, 1000)
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long t) {
                        requested.addAndGet(t);
                    }
                });
        ConnectableFlowable<Integer> co = source.replay();

        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>(10L);
        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>(90L);

        co.subscribe(ts1);
        co.subscribe(ts2);

        ts2.request(10);

        co.connect();

        ts1.assertValueCount(10);
        ts1.assertNotTerminated();

        ts2.assertValueCount(100);
        ts2.assertNotTerminated();

        Assert.assertEquals(100, requested.get());
    }

    @Test
    public void testBackpressureBounded() {
        final AtomicLong requested = new AtomicLong();
        Flowable<Integer> source = Flowable.range(1, 1000)
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long t) {
                        requested.addAndGet(t);
                    }
                });
        ConnectableFlowable<Integer> co = source.replay(50);

        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>(10L);
        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>(90L);

        co.subscribe(ts1);
        co.subscribe(ts2);

        ts2.request(10);

        co.connect();

        ts1.assertValueCount(10);
        ts1.assertNotTerminated();

        ts2.assertValueCount(100);
        ts2.assertNotTerminated();

        Assert.assertEquals(100, requested.get());
    }

    @Test
    public void testColdReplayNoBackpressure() {
        Flowable<Integer> source = Flowable.range(0, 1000).replay().autoConnect();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminated();
        List<Integer> onNextEvents = ts.values();
        assertEquals(1000, onNextEvents.size());

        for (int i = 0; i < 1000; i++) {
            assertEquals((Integer)i, onNextEvents.get(i));
        }
    }
    @Test
    public void testColdReplayBackpressure() {
        Flowable<Integer> source = Flowable.range(0, 1000).replay().autoConnect();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);
        ts.request(10);

        source.subscribe(ts);

        ts.assertNoErrors();
        ts.assertNotComplete();
        List<Integer> onNextEvents = ts.values();
        assertEquals(10, onNextEvents.size());

        for (int i = 0; i < 10; i++) {
            assertEquals((Integer)i, onNextEvents.get(i));
        }

        ts.dispose();
    }

    @Test
    public void testCache() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        Flowable<String> o = Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(final Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        System.out.println("published observable being executed");
                        observer.onNext("one");
                        observer.onComplete();
                    }
                }).start();
            }
        }).replay().autoConnect();

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(new Consumer<String>() {

            @Override
            public void accept(String v) {
                assertEquals("one", v);
                System.out.println("v: " + v);
                latch.countDown();
            }
        });

        // subscribe again
        o.subscribe(new Consumer<String>() {

            @Override
            public void accept(String v) {
                assertEquals("one", v);
                System.out.println("v: " + v);
                latch.countDown();
            }
        });

        if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("subscriptions did not receive values");
        }
        assertEquals(1, counter.get());
    }

    @Test
    public void testUnsubscribeSource() throws Exception {
        Action unsubscribe = mock(Action.class);
        Flowable<Integer> o = Flowable.just(1).doOnCancel(unsubscribe).cache();
        o.subscribe();
        o.subscribe();
        o.subscribe();
        verify(unsubscribe, times(1)).run();
    }

    @Test
    public void testTake() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable<Integer> cached = Flowable.range(1, 100).replay().autoConnect();
        cached.take(10).subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testAsync() {
        Flowable<Integer> source = Flowable.range(1, 10000);
        for (int i = 0; i < 100; i++) {
            TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>();

            Flowable<Integer> cached = source.replay().autoConnect();

            cached.observeOn(Schedulers.computation()).subscribe(ts1);

            ts1.awaitTerminalEvent(2, TimeUnit.SECONDS);
            ts1.assertNoErrors();
            ts1.assertTerminated();
            assertEquals(10000, ts1.values().size());

            TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>();
            cached.observeOn(Schedulers.computation()).subscribe(ts2);

            ts2.awaitTerminalEvent(2, TimeUnit.SECONDS);
            ts2.assertNoErrors();
            ts2.assertTerminated();
            assertEquals(10000, ts2.values().size());
        }
    }
    @Test
    public void testAsyncComeAndGo() {
        Flowable<Long> source = Flowable.interval(1, 1, TimeUnit.MILLISECONDS)
                .take(1000)
                .subscribeOn(Schedulers.io());
        Flowable<Long> cached = source.replay().autoConnect();

        Flowable<Long> output = cached.observeOn(Schedulers.computation(), false, 1024);

        List<TestSubscriber<Long>> list = new ArrayList<TestSubscriber<Long>>(100);
        for (int i = 0; i < 100; i++) {
            TestSubscriber<Long> ts = new TestSubscriber<Long>();
            list.add(ts);
            output.skip(i * 10).take(10).subscribe(ts);
        }

        List<Long> expected = new ArrayList<Long>();
        for (int i = 0; i < 10; i++) {
            expected.add((long)(i - 10));
        }
        int j = 0;
        for (TestSubscriber<Long> ts : list) {
            ts.awaitTerminalEvent(3, TimeUnit.SECONDS);
            ts.assertNoErrors();
            ts.assertTerminated();

            for (int i = j * 10; i < j * 10 + 10; i++) {
                expected.set(i - j * 10, (long)i);
            }

            ts.assertValueSequence(expected);

            j++;
        }
    }

    @Test
    public void testNoMissingBackpressureException() {
        final int m = 4 * 1000 * 1000;
        Flowable<Integer> firehose = Flowable.unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> t) {
                t.onSubscribe(new BooleanSubscription());
                for (int i = 0; i < m; i++) {
                    t.onNext(i);
                }
                t.onComplete();
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        firehose.replay().autoConnect().observeOn(Schedulers.computation()).takeLast(100).subscribe(ts);

        ts.awaitTerminalEvent(3, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertTerminated();

        assertEquals(100, ts.values().size());
    }

    @Test
    public void testValuesAndThenError() {
        Flowable<Integer> source = Flowable.range(1, 10)
                .concatWith(Flowable.<Integer>error(new TestException()))
                .replay().autoConnect();


        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        source.subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts.assertNotComplete();
        Assert.assertEquals(1, ts.errors().size());

        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>();
        source.subscribe(ts2);

        ts2.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts2.assertNotComplete();
        Assert.assertEquals(1, ts2.errors().size());
    }

    @Test
    public void unsafeChildThrows() {
        final AtomicInteger count = new AtomicInteger();

        Flowable<Integer> source = Flowable.range(1, 100)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        })
        .replay().autoConnect();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };

        source.subscribe(ts);

        Assert.assertEquals(100, count.get());

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void unboundedLeavesEarly() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        final List<Long> requests = new ArrayList<Long>();

        Flowable<Integer> out = source
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long t) {
                        requests.add(t);
                    }
                }).replay().autoConnect();

        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>(5L);
        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>(10L);

        out.subscribe(ts1);
        out.subscribe(ts2);
        ts2.dispose();

        Assert.assertEquals(Arrays.asList(5L, 5L), requests);
    }

    @Test
    public void testSubscribersComeAndGoAtRequestBoundaries() {
        ConnectableFlowable<Integer> source = Flowable.range(1, 10).replay(1);
        source.connect();

        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>(2L);

        source.subscribe(ts1);

        ts1.assertValues(1, 2);
        ts1.assertNoErrors();
        ts1.dispose();

        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>(2L);

        source.subscribe(ts2);

        ts2.assertValues(2, 3);
        ts2.assertNoErrors();
        ts2.dispose();

        TestSubscriber<Integer> ts21 = new TestSubscriber<Integer>(1L);

        source.subscribe(ts21);

        ts21.assertValues(3);
        ts21.assertNoErrors();
        ts21.dispose();

        TestSubscriber<Integer> ts22 = new TestSubscriber<Integer>(1L);

        source.subscribe(ts22);

        ts22.assertValues(3);
        ts22.assertNoErrors();
        ts22.dispose();


        TestSubscriber<Integer> ts3 = new TestSubscriber<Integer>();

        source.subscribe(ts3);

        ts3.assertNoErrors();
        System.out.println(ts3.values());
        ts3.assertValues(3, 4, 5, 6, 7, 8, 9, 10);
        ts3.assertComplete();
    }

    @Test
    public void testSubscribersComeAndGoAtRequestBoundaries2() {
        ConnectableFlowable<Integer> source = Flowable.range(1, 10).replay(2);
        source.connect();

        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>(2L);

        source.subscribe(ts1);

        ts1.assertValues(1, 2);
        ts1.assertNoErrors();
        ts1.dispose();

        TestSubscriber<Integer> ts11 = new TestSubscriber<Integer>(2L);

        source.subscribe(ts11);

        ts11.assertValues(1, 2);
        ts11.assertNoErrors();
        ts11.dispose();

        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>(3L);

        source.subscribe(ts2);

        ts2.assertValues(1, 2, 3);
        ts2.assertNoErrors();
        ts2.dispose();

        TestSubscriber<Integer> ts21 = new TestSubscriber<Integer>(1L);

        source.subscribe(ts21);

        ts21.assertValues(2);
        ts21.assertNoErrors();
        ts21.dispose();

        TestSubscriber<Integer> ts22 = new TestSubscriber<Integer>(1L);

        source.subscribe(ts22);

        ts22.assertValues(2);
        ts22.assertNoErrors();
        ts22.dispose();


        TestSubscriber<Integer> ts3 = new TestSubscriber<Integer>();

        source.subscribe(ts3);

        ts3.assertNoErrors();
        System.out.println(ts3.values());
        ts3.assertValues(2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts3.assertComplete();
    }

    @Test
    public void replayScheduler() {

        Flowable.just(1).replay(Schedulers.computation())
        .autoConnect()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replayTime() {
        Flowable.just(1).replay(1, TimeUnit.MINUTES)
        .autoConnect()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySizeScheduler() {

        Flowable.just(1).replay(1, Schedulers.computation())
        .autoConnect()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySizeAndTime() {
        Flowable.just(1).replay(1, 1, TimeUnit.MILLISECONDS)
        .autoConnect()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySelectorSizeScheduler() {
        Flowable.just(1).replay(Functions.<Flowable<Integer>>identity(), 1, Schedulers.io())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySelectorScheduler() {
        Flowable.just(1).replay(Functions.<Flowable<Integer>>identity(), Schedulers.io())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySelectorTime() {
        Flowable.just(1).replay(Functions.<Flowable<Integer>>identity(), 1, TimeUnit.MINUTES)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void source() {
        Flowable<Integer> source = Flowable.range(1, 3);

        assertSame(source, (((HasUpstreamPublisher<?>)source.replay())).source());
    }

    @Test
    public void connectRace() {
        for (int i = 0; i < 500; i++) {
            final ConnectableFlowable<Integer> co = Flowable.range(1, 3).replay();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    co.connect();
                }
            };

            TestHelper.race(r, r);
        }
    }

    @Test
    public void subscribeRace() {
        for (int i = 0; i < 500; i++) {
            final ConnectableFlowable<Integer> co = Flowable.range(1, 3).replay();

            final TestSubscriber<Integer> to1 = new TestSubscriber<Integer>();
            final TestSubscriber<Integer> to2 = new TestSubscriber<Integer>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to2);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < 500; i++) {
            final ConnectableFlowable<Integer> co = Flowable.range(1, 3).replay();

            final TestSubscriber<Integer> to1 = new TestSubscriber<Integer>();
            final TestSubscriber<Integer> to2 = new TestSubscriber<Integer>();

            co.subscribe(to1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to1.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to2);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void cancelOnArrival() {
        Flowable.range(1, 2)
        .replay(Integer.MAX_VALUE)
        .autoConnect()
        .test(Long.MAX_VALUE, true)
        .assertEmpty();
    }

    @Test
    public void cancelOnArrival2() {
        ConnectableFlowable<Integer> co = PublishProcessor.<Integer>create()
        .replay(Integer.MAX_VALUE);

        co.test();

        co
        .autoConnect()
        .test(Long.MAX_VALUE, true)
        .assertEmpty();
    }

    @Test
    public void connectConsumerThrows() {
        ConnectableFlowable<Integer> co = Flowable.range(1, 2)
        .replay();

        try {
            co.connect(new Consumer<Disposable>() {
                @Override
                public void accept(Disposable t) throws Exception {
                    throw new TestException();
                }
            });
            fail("Should have thrown");
        } catch (TestException ex) {
            // expected
        }

        co.test().assertEmpty().cancel();

        co.connect();

        co.test().assertResult(1, 2);
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> observer) {
                    observer.onSubscribe(new BooleanSubscription());
                    observer.onError(new TestException("First"));
                    observer.onNext(1);
                    observer.onError(new TestException("Second"));
                    observer.onComplete();
                }
            }.replay()
            .autoConnect()
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void subscribeOnNextRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> ps = PublishProcessor.create();

            final ConnectableFlowable<Integer> co = ps.replay();

            final TestSubscriber<Integer> to1 = new TestSubscriber<Integer>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 1000; j++) {
                        ps.onNext(j);
                    }
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void unsubscribeOnNextRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> ps = PublishProcessor.create();

            final ConnectableFlowable<Integer> co = ps.replay();

            final TestSubscriber<Integer> to1 = new TestSubscriber<Integer>();

            co.subscribe(to1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to1.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 1000; j++) {
                        ps.onNext(j);
                    }
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void unsubscribeReplayRace() {
        for (int i = 0; i < 500; i++) {
            final ConnectableFlowable<Integer> co = Flowable.range(1, 1000).replay();

            final TestSubscriber<Integer> to1 = new TestSubscriber<Integer>();

            co.connect();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to1.dispose();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void reentrantOnNext() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> to = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    ps.onNext(2);
                    ps.onComplete();
                }
                super.onNext(t);
            }
        };

        ps.replay().autoConnect().subscribe(to);

        ps.onNext(1);

        to.assertResult(1, 2);
    }

    @Test
    public void reentrantOnNextBound() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> to = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    ps.onNext(2);
                    ps.onComplete();
                }
                super.onNext(t);
            }
        };

        ps.replay(10).autoConnect().subscribe(to);

        ps.onNext(1);

        to.assertResult(1, 2);
    }

    @Test
    public void reentrantOnNextCancel() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> to = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    ps.onNext(2);
                    cancel();
                }
                super.onNext(t);
            }
        };

        ps.replay().autoConnect().subscribe(to);

        ps.onNext(1);

        to.assertValues(1);
    }

    @Test
    public void reentrantOnNextCancelBounded() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> to = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    ps.onNext(2);
                    cancel();
                }
                super.onNext(t);
            }
        };

        ps.replay(10).autoConnect().subscribe(to);

        ps.onNext(1);

        to.assertValues(1);
    }

    @Test
    public void replayMaxInt() {
        Flowable.range(1, 2)
        .replay(Integer.MAX_VALUE)
        .autoConnect()
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void testTimedAndSizedTruncationError() {
        TestScheduler test = new TestScheduler();
        SizeAndTimeBoundReplayBuffer<Integer> buf = new SizeAndTimeBoundReplayBuffer<Integer>(2, 2000, TimeUnit.MILLISECONDS, test);

        Assert.assertFalse(buf.hasCompleted());
        Assert.assertFalse(buf.hasError());

        List<Integer> values = new ArrayList<Integer>();

        buf.next(1);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        buf.next(2);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(2), values);

        buf.next(3);
        buf.next(4);
        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(3, 4), values);

        test.advanceTimeBy(2, TimeUnit.SECONDS);
        buf.next(5);

        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(5), values);
        Assert.assertFalse(buf.hasCompleted());
        Assert.assertFalse(buf.hasError());

        test.advanceTimeBy(2, TimeUnit.SECONDS);
        buf.error(new TestException());

        values.clear();
        buf.collect(values);
        Assert.assertTrue(values.isEmpty());

        Assert.assertEquals(1, buf.size);
        Assert.assertFalse(buf.hasCompleted());
        Assert.assertTrue(buf.hasError());
    }

    @Test
    public void testSizedTruncation() {
        SizeBoundReplayBuffer<Integer> buf = new SizeBoundReplayBuffer<Integer>(2);
        List<Integer> values = new ArrayList<Integer>();

        buf.next(1);
        buf.next(2);
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(1, 2), values);

        buf.next(3);
        buf.next(4);
        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(3, 4), values);

        buf.next(5);

        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(4, 5), values);
        Assert.assertFalse(buf.hasCompleted());

        buf.complete();

        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(4, 5), values);

        Assert.assertEquals(3, buf.size);
        Assert.assertTrue(buf.hasCompleted());
        Assert.assertFalse(buf.hasError());
    }

    @Test
    public void delayedUpstreamOnSubscribe() {
        final Subscriber<?>[] sub = { null };

        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                sub[0] = s;
            }
        }
        .replay()
        .connect()
        .dispose();

        BooleanSubscription bs = new BooleanSubscription();

        sub[0].onSubscribe(bs);

        assertTrue(bs.isCancelled());
    }

    @Test
    public void timedNoOutdatedData() {
        TestScheduler scheduler = new TestScheduler();

        Flowable<Integer> source = Flowable.just(1)
                .replay(2, TimeUnit.SECONDS, scheduler)
                .autoConnect();

        source.test().assertResult(1);

        source.test().assertResult(1);

        scheduler.advanceTimeBy(3, TimeUnit.SECONDS);

        source.test().assertResult();
    }

    @Test
    public void replaySelectorReturnsNull() {
        Flowable.just(1)
        .replay(new Function<Flowable<Integer>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Integer> v) throws Exception {
                return null;
            }
        }, Schedulers.trampoline())
        .test()
        .assertFailureAndMessage(NullPointerException.class, "The selector returned a null Publisher");
    }
}
