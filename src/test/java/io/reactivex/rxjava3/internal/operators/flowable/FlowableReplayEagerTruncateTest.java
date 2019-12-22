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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableReplay.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableReplayEagerTruncateTest extends RxJavaTest {
    @Test
    public void bufferedReplay() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        ConnectableFlowable<Integer> cf = source.replay(3, true);
        cf.connect();

        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            cf.subscribe(subscriber1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(subscriber1, times(1)).onNext(1);
            inOrder.verify(subscriber1, times(1)).onNext(2);
            inOrder.verify(subscriber1, times(1)).onNext(3);

            source.onNext(4);
            source.onComplete();
            inOrder.verify(subscriber1, times(1)).onNext(4);
            inOrder.verify(subscriber1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onError(any(Throwable.class));

        }

        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            cf.subscribe(subscriber1);

            inOrder.verify(subscriber1, times(1)).onNext(2);
            inOrder.verify(subscriber1, times(1)).onNext(3);
            inOrder.verify(subscriber1, times(1)).onNext(4);
            inOrder.verify(subscriber1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void bufferedWindowReplay() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        TestScheduler scheduler = new TestScheduler();
        ConnectableFlowable<Integer> cf = source.replay(3, 100, TimeUnit.MILLISECONDS, scheduler, true);
        cf.connect();

        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            cf.subscribe(subscriber1);

            source.onNext(1);
            scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS);

            inOrder.verify(subscriber1, times(1)).onNext(1);
            inOrder.verify(subscriber1, times(1)).onNext(2);
            inOrder.verify(subscriber1, times(1)).onNext(3);

            source.onNext(4);
            source.onNext(5);
            scheduler.advanceTimeBy(90, TimeUnit.MILLISECONDS);

            inOrder.verify(subscriber1, times(1)).onNext(4);

            inOrder.verify(subscriber1, times(1)).onNext(5);

            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onError(any(Throwable.class));

        }

        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            cf.subscribe(subscriber1);

            inOrder.verify(subscriber1, times(1)).onNext(4);
            inOrder.verify(subscriber1, times(1)).onNext(5);
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void windowedReplay() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> source = PublishProcessor.create();

        ConnectableFlowable<Integer> cf = source.replay(100, TimeUnit.MILLISECONDS, scheduler, true);
        cf.connect();

        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            cf.subscribe(subscriber1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onComplete();
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(subscriber1, times(1)).onNext(1);
            inOrder.verify(subscriber1, times(1)).onNext(2);
            inOrder.verify(subscriber1, times(1)).onNext(3);

            inOrder.verify(subscriber1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onError(any(Throwable.class));

        }
        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            cf.subscribe(subscriber1);
            inOrder.verify(subscriber1, never()).onNext(3);

            inOrder.verify(subscriber1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void replaySelector() {
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
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            co.subscribe(subscriber1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(subscriber1, times(1)).onNext(2);
            inOrder.verify(subscriber1, times(1)).onNext(4);
            inOrder.verify(subscriber1, times(1)).onNext(6);

            source.onNext(4);
            source.onComplete();
            inOrder.verify(subscriber1, times(1)).onNext(8);
            inOrder.verify(subscriber1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onError(any(Throwable.class));

        }

        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            co.subscribe(subscriber1);

            inOrder.verify(subscriber1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onError(any(Throwable.class));

        }

    }

    @Test
    public void bufferedReplaySelector() {

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

        Flowable<Integer> co = source.replay(selector, 3, true);

        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            co.subscribe(subscriber1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(subscriber1, times(1)).onNext(2);
            inOrder.verify(subscriber1, times(1)).onNext(4);
            inOrder.verify(subscriber1, times(1)).onNext(6);

            source.onNext(4);
            source.onComplete();
            inOrder.verify(subscriber1, times(1)).onNext(8);
            inOrder.verify(subscriber1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onError(any(Throwable.class));

        }

        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            co.subscribe(subscriber1);

            inOrder.verify(subscriber1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void windowedReplaySelector() {

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

        Flowable<Integer> co = source.replay(selector, 100, TimeUnit.MILLISECONDS, scheduler, true);

        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            co.subscribe(subscriber1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onComplete();
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(subscriber1, times(1)).onNext(2);
            inOrder.verify(subscriber1, times(1)).onNext(4);
            inOrder.verify(subscriber1, times(1)).onNext(6);

            inOrder.verify(subscriber1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onError(any(Throwable.class));

        }
        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            co.subscribe(subscriber1);

            inOrder.verify(subscriber1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void bufferedReplayError() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        ConnectableFlowable<Integer> cf = source.replay(3, true);
        cf.connect();

        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            cf.subscribe(subscriber1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(subscriber1, times(1)).onNext(1);
            inOrder.verify(subscriber1, times(1)).onNext(2);
            inOrder.verify(subscriber1, times(1)).onNext(3);

            source.onNext(4);
            source.onError(new RuntimeException("Forced failure"));

            inOrder.verify(subscriber1, times(1)).onNext(4);
            inOrder.verify(subscriber1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onComplete();

        }

        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            cf.subscribe(subscriber1);

            inOrder.verify(subscriber1, times(1)).onNext(2);
            inOrder.verify(subscriber1, times(1)).onNext(3);
            inOrder.verify(subscriber1, times(1)).onNext(4);
            inOrder.verify(subscriber1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onComplete();
        }
    }

    @Test
    public void windowedReplayError() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> source = PublishProcessor.create();

        ConnectableFlowable<Integer> cf = source.replay(100, TimeUnit.MILLISECONDS, scheduler, true);
        cf.connect();

        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            cf.subscribe(subscriber1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onError(new RuntimeException("Forced failure"));
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(subscriber1, times(1)).onNext(1);
            inOrder.verify(subscriber1, times(1)).onNext(2);
            inOrder.verify(subscriber1, times(1)).onNext(3);

            inOrder.verify(subscriber1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onComplete();

        }
        {
            Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(subscriber1);

            cf.subscribe(subscriber1);
            inOrder.verify(subscriber1, never()).onNext(3);

            inOrder.verify(subscriber1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(subscriber1, never()).onComplete();
        }
    }

    @Test
    public void synchronousDisconnect() {
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
            public Flowable<Integer> apply(Flowable<Integer> f) {
                return f.take(2);
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
    public void issue2191_UnsubscribeSource() throws Throwable {
        // setup mocks
        Consumer<Integer> sourceNext = mock(Consumer.class);
        Action sourceCompleted = mock(Action.class);
        Action sourceUnsubscribed = mock(Action.class);
        Subscriber<Integer> spiedSubscriberBeforeConnect = TestHelper.mockSubscriber();
        Subscriber<Integer> spiedSubscriberAfterConnect = TestHelper.mockSubscriber();

        // Flowable under test
        Flowable<Integer> source = Flowable.just(1, 2);

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
     * @throws Throwable functional interfaces declare throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void issue2191_SchedulerUnsubscribe() throws Throwable {
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
     * @throws Throwable functional interfaces declare throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void issue2191_SchedulerUnsubscribeOnError() throws Throwable {
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
    public void boundedReplayBuffer() {
        BoundedReplayBuffer<Integer> buf = new BoundedReplayBuffer<>(true);
        buf.addLast(new Node(1, 0));
        buf.addLast(new Node(2, 1));
        buf.addLast(new Node(3, 2));
        buf.addLast(new Node(4, 3));
        buf.addLast(new Node(5, 4));

        List<Integer> values = new ArrayList<>();
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
    public void timedAndSizedTruncation() {
        TestScheduler test = new TestScheduler();
        SizeAndTimeBoundReplayBuffer<Integer> buf = new SizeAndTimeBoundReplayBuffer<>(2, 2000, TimeUnit.MILLISECONDS, test, true);
        List<Integer> values = new ArrayList<>();

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
    public void backpressure() {
        final AtomicLong requested = new AtomicLong();
        Flowable<Integer> source = Flowable.range(1, 1000)
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long t) {
                        requested.addAndGet(t);
                    }
                });
        ConnectableFlowable<Integer> cf = source.replay();

        TestSubscriberEx<Integer> ts1 = new TestSubscriberEx<>(10L);
        TestSubscriberEx<Integer> ts2 = new TestSubscriberEx<>(90L);

        cf.subscribe(ts1);
        cf.subscribe(ts2);

        ts2.request(10);

        cf.connect();

        ts1.assertValueCount(10);
        ts1.assertNotTerminated();

        ts2.assertValueCount(100);
        ts2.assertNotTerminated();

        Assert.assertEquals(100, requested.get());
    }

    @Test
    public void backpressureBounded() {
        final AtomicLong requested = new AtomicLong();
        Flowable<Integer> source = Flowable.range(1, 1000)
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long t) {
                        requested.addAndGet(t);
                    }
                });
        ConnectableFlowable<Integer> cf = source.replay(50, true);

        TestSubscriberEx<Integer> ts1 = new TestSubscriberEx<>(10L);
        TestSubscriberEx<Integer> ts2 = new TestSubscriberEx<>(90L);

        cf.subscribe(ts1);
        cf.subscribe(ts2);

        ts2.request(10);

        cf.connect();

        ts1.assertValueCount(10);
        ts1.assertNotTerminated();

        ts2.assertValueCount(100);
        ts2.assertNotTerminated();

        Assert.assertEquals(100, requested.get());
    }

    @Test
    public void coldReplayNoBackpressure() {
        Flowable<Integer> source = Flowable.range(0, 1000).replay().autoConnect();

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
    public void coldReplayBackpressure() {
        Flowable<Integer> source = Flowable.range(0, 1000).replay().autoConnect();

        TestSubscriber<Integer> ts = new TestSubscriber<>(0L);
        ts.request(10);

        source.subscribe(ts);

        ts.assertNoErrors();
        ts.assertNotComplete();
        List<Integer> onNextEvents = ts.values();
        assertEquals(10, onNextEvents.size());

        for (int i = 0; i < 10; i++) {
            assertEquals((Integer)i, onNextEvents.get(i));
        }

        ts.cancel();
    }

    @Test
    public void cache() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        Flowable<String> f = Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(final Subscriber<? super String> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        System.out.println("published observable being executed");
                        subscriber.onNext("one");
                        subscriber.onComplete();
                    }
                }).start();
            }
        }).replay().autoConnect();

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        f.subscribe(new Consumer<String>() {

            @Override
            public void accept(String v) {
                assertEquals("one", v);
                System.out.println("v: " + v);
                latch.countDown();
            }
        });

        // subscribe again
        f.subscribe(new Consumer<String>() {

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
    public void unsubscribeSource() throws Throwable {
        Action unsubscribe = mock(Action.class);
        Flowable<Integer> f = Flowable.just(1).doOnCancel(unsubscribe).replay().autoConnect();
        f.subscribe();
        f.subscribe();
        f.subscribe();
        verify(unsubscribe, never()).run();
    }

    @Test
    public void take() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        Flowable<Integer> cached = Flowable.range(1, 100).replay().autoConnect();
        cached.take(10).subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void async() {
        Flowable<Integer> source = Flowable.range(1, 10000);
        for (int i = 0; i < 100; i++) {
            TestSubscriberEx<Integer> ts1 = new TestSubscriberEx<>();

            Flowable<Integer> cached = source.replay().autoConnect();

            cached.observeOn(Schedulers.computation()).subscribe(ts1);

            ts1.awaitDone(2, TimeUnit.SECONDS);
            ts1.assertNoErrors();
            ts1.assertTerminated();
            assertEquals(10000, ts1.values().size());

            TestSubscriberEx<Integer> ts2 = new TestSubscriberEx<>();
            cached.observeOn(Schedulers.computation()).subscribe(ts2);

            ts2.awaitDone(2, TimeUnit.SECONDS);
            ts2.assertNoErrors();
            ts2.assertTerminated();
            assertEquals(10000, ts2.values().size());
        }
    }

    @Test
    public void asyncComeAndGo() {
        Flowable<Long> source = Flowable.interval(1, 1, TimeUnit.MILLISECONDS)
                .take(1000)
                .subscribeOn(Schedulers.io());
        Flowable<Long> cached = source.replay().autoConnect();

        Flowable<Long> output = cached.observeOn(Schedulers.computation(), false, 1024);

        List<TestSubscriberEx<Long>> list = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            TestSubscriberEx<Long> ts = new TestSubscriberEx<>();
            list.add(ts);
            output.skip(i * 10).take(10).subscribe(ts);
        }

        List<Long> expected = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            expected.add((long)(i - 10));
        }
        int j = 0;
        for (TestSubscriberEx<Long> ts : list) {
            ts.awaitDone(3, TimeUnit.SECONDS);
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
    public void noMissingBackpressureException() {
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

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        firehose.replay().autoConnect().observeOn(Schedulers.computation()).takeLast(100).subscribe(ts);

        ts.awaitDone(3, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertTerminated();

        assertEquals(100, ts.values().size());
    }

    @Test
    public void valuesAndThenError() {
        Flowable<Integer> source = Flowable.range(1, 10)
                .concatWith(Flowable.<Integer>error(new TestException()))
                .replay().autoConnect();

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        source.subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts.assertNotComplete();
        Assert.assertEquals(1, ts.errors().size());

        TestSubscriberEx<Integer> ts2 = new TestSubscriberEx<>();
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

        final List<Long> requests = new ArrayList<>();

        Flowable<Integer> out = source
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long t) {
                        requests.add(t);
                    }
                }).replay().autoConnect();

        TestSubscriber<Integer> ts1 = new TestSubscriber<>(5L);
        TestSubscriber<Integer> ts2 = new TestSubscriber<>(10L);

        out.subscribe(ts1);
        out.subscribe(ts2);
        ts2.cancel();

        Assert.assertEquals(Arrays.asList(5L, 5L), requests);
    }

    @Test
    public void subscribersComeAndGoAtRequestBoundaries() {
        ConnectableFlowable<Integer> source = Flowable.range(1, 10).replay(1, true);
        source.connect();

        TestSubscriber<Integer> ts1 = new TestSubscriber<>(2L);

        source.subscribe(ts1);

        ts1.assertValues(1, 2);
        ts1.assertNoErrors();
        ts1.cancel();

        TestSubscriber<Integer> ts2 = new TestSubscriber<>(2L);

        source.subscribe(ts2);

        ts2.assertValues(2, 3);
        ts2.assertNoErrors();
        ts2.cancel();

        TestSubscriber<Integer> ts21 = new TestSubscriber<>(1L);

        source.subscribe(ts21);

        ts21.assertValues(3);
        ts21.assertNoErrors();
        ts21.cancel();

        TestSubscriber<Integer> ts22 = new TestSubscriber<>(1L);

        source.subscribe(ts22);

        ts22.assertValues(3);
        ts22.assertNoErrors();
        ts22.cancel();

        TestSubscriber<Integer> ts3 = new TestSubscriber<>();

        source.subscribe(ts3);

        ts3.assertNoErrors();
        System.out.println(ts3.values());
        ts3.assertValues(3, 4, 5, 6, 7, 8, 9, 10);
        ts3.assertComplete();
    }

    @Test
    public void subscribersComeAndGoAtRequestBoundaries2() {
        ConnectableFlowable<Integer> source = Flowable.range(1, 10).replay(2, true);
        source.connect();

        TestSubscriber<Integer> ts1 = new TestSubscriber<>(2L);

        source.subscribe(ts1);

        ts1.assertValues(1, 2);
        ts1.assertNoErrors();
        ts1.cancel();

        TestSubscriber<Integer> ts11 = new TestSubscriber<>(2L);

        source.subscribe(ts11);

        ts11.assertValues(1, 2);
        ts11.assertNoErrors();
        ts11.cancel();

        TestSubscriber<Integer> ts2 = new TestSubscriber<>(3L);

        source.subscribe(ts2);

        ts2.assertValues(1, 2, 3);
        ts2.assertNoErrors();
        ts2.cancel();

        TestSubscriber<Integer> ts21 = new TestSubscriber<>(1L);

        source.subscribe(ts21);

        ts21.assertValues(2);
        ts21.assertNoErrors();
        ts21.cancel();

        TestSubscriber<Integer> ts22 = new TestSubscriber<>(1L);

        source.subscribe(ts22);

        ts22.assertValues(2);
        ts22.assertNoErrors();
        ts22.cancel();

        TestSubscriber<Integer> ts3 = new TestSubscriber<>();

        source.subscribe(ts3);

        ts3.assertNoErrors();
        System.out.println(ts3.values());
        ts3.assertValues(2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts3.assertComplete();
    }

    @Test
    public void replayTime() {
        Flowable.just(1).replay(1, TimeUnit.MINUTES, Schedulers.computation(), true)
        .autoConnect()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySizeAndTime() {
        Flowable.just(1).replay(1, 1, TimeUnit.MILLISECONDS, Schedulers.computation(), true)
        .autoConnect()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySelectorTime() {
        Flowable.just(1).replay(Functions.<Flowable<Integer>>identity(), 1, TimeUnit.MINUTES, Schedulers.computation(), true)
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
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final ConnectableFlowable<Integer> cf = Flowable.range(1, 3).replay();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    cf.connect();
                }
            };

            TestHelper.race(r, r);
        }
    }

    @Test
    public void subscribeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final ConnectableFlowable<Integer> cf = Flowable.range(1, 3).replay();

            final TestSubscriber<Integer> ts1 = new TestSubscriber<>();
            final TestSubscriber<Integer> ts2 = new TestSubscriber<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    cf.subscribe(ts1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    cf.subscribe(ts2);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final ConnectableFlowable<Integer> cf = Flowable.range(1, 3).replay();

            final TestSubscriber<Integer> ts1 = new TestSubscriber<>();
            final TestSubscriber<Integer> ts2 = new TestSubscriber<>();

            cf.subscribe(ts1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    cf.subscribe(ts2);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void cancelOnArrival() {
        Flowable.range(1, 2)
        .replay(Integer.MAX_VALUE, true)
        .autoConnect()
        .test(Long.MAX_VALUE, true)
        .assertEmpty();
    }

    @Test
    public void cancelOnArrival2() {
        ConnectableFlowable<Integer> cf = PublishProcessor.<Integer>create()
        .replay(Integer.MAX_VALUE, true);

        cf.test();

        cf
        .autoConnect()
        .test(Long.MAX_VALUE, true)
        .assertEmpty();
    }

    @Test
    public void connectConsumerThrows() {
        ConnectableFlowable<Integer> cf = Flowable.range(1, 2)
        .replay();

        try {
            cf.connect(new Consumer<Disposable>() {
                @Override
                public void accept(Disposable t) throws Exception {
                    throw new TestException();
                }
            });
            fail("Should have thrown");
        } catch (TestException ex) {
            // expected
        }

        cf.test().assertEmpty().cancel();

        cf.connect();

        cf.test().assertResult(1, 2);
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                    subscriber.onSubscribe(new BooleanSubscription());
                    subscriber.onError(new TestException("First"));
                    subscriber.onNext(1);
                    subscriber.onError(new TestException("Second"));
                    subscriber.onComplete();
                }
            }.replay()
            .autoConnect()
            .to(TestHelper.<Integer>testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void subscribeOnNextRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final ConnectableFlowable<Integer> cf = pp.replay();

            final TestSubscriber<Integer> ts1 = new TestSubscriber<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    cf.subscribe(ts1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 1000; j++) {
                        pp.onNext(j);
                    }
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void unsubscribeOnNextRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final ConnectableFlowable<Integer> cf = pp.replay();

            final TestSubscriber<Integer> ts1 = new TestSubscriber<>();

            cf.subscribe(ts1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 1000; j++) {
                        pp.onNext(j);
                    }
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void unsubscribeReplayRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final ConnectableFlowable<Integer> cf = Flowable.range(1, 1000).replay();

            final TestSubscriber<Integer> ts1 = new TestSubscriber<>();

            cf.connect();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    cf.subscribe(ts1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void reentrantOnNext() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    pp.onNext(2);
                    pp.onComplete();
                }
                super.onNext(t);
            }
        };

        pp.replay().autoConnect().subscribe(ts);

        pp.onNext(1);

        ts.assertResult(1, 2);
    }

    @Test
    public void reentrantOnNextBound() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    pp.onNext(2);
                    pp.onComplete();
                }
                super.onNext(t);
            }
        };

        pp.replay(10, true).autoConnect().subscribe(ts);

        pp.onNext(1);

        ts.assertResult(1, 2);
    }

    @Test
    public void reentrantOnNextCancel() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    pp.onNext(2);
                    cancel();
                }
                super.onNext(t);
            }
        };

        pp.replay().autoConnect().subscribe(ts);

        pp.onNext(1);

        ts.assertValues(1);
    }

    @Test
    public void reentrantOnNextCancelBounded() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    pp.onNext(2);
                    cancel();
                }
                super.onNext(t);
            }
        };

        pp.replay(10, true).autoConnect().subscribe(ts);

        pp.onNext(1);

        ts.assertValues(1);
    }

    @Test
    public void replayMaxInt() {
        Flowable.range(1, 2)
        .replay(Integer.MAX_VALUE, true)
        .autoConnect()
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void timedAndSizedTruncationError() {
        TestScheduler test = new TestScheduler();
        SizeAndTimeBoundReplayBuffer<Integer> buf = new SizeAndTimeBoundReplayBuffer<>(2, 2000, TimeUnit.MILLISECONDS, test, true);

        Assert.assertFalse(buf.hasCompleted());
        Assert.assertFalse(buf.hasError());

        List<Integer> values = new ArrayList<>();

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
    public void sizedTruncation() {
        SizeBoundReplayBuffer<Integer> buf = new SizeBoundReplayBuffer<>(2, true);
        List<Integer> values = new ArrayList<>();

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
                .replay(2, TimeUnit.SECONDS, scheduler, true)
                .autoConnect();

        source.test().assertResult(1);

        source.test().assertResult(1);

        scheduler.advanceTimeBy(3, TimeUnit.SECONDS);

        source.test().assertResult();
    }

    @Test
    public void multicastSelectorCallableConnectableCrash() {
        FlowableReplay.multicastSelector(new Supplier<ConnectableFlowable<Object>>() {
            @Override
            public ConnectableFlowable<Object> get() throws Exception {
                throw new TestException();
            }
        }, Functions.<Flowable<Object>>identity())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(
            Flowable.never()
            .replay()
        );
    }

    @Test
    public void noHeadRetentionCompleteSize() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        FlowableReplay<Integer> co = (FlowableReplay<Integer>)source
                .replay(1, true);

        // the backpressure coordination would not accept items from source otherwise
        co.test();

        co.connect();

        BoundedReplayBuffer<Integer> buf = (BoundedReplayBuffer<Integer>)(co.current.get().buffer);

        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        assertNull(buf.get().value);

        Object o = buf.get();

        buf.trimHead();

        assertSame(o, buf.get());
    }

    @Test
    public void noHeadRetentionErrorSize() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        FlowableReplay<Integer> co = (FlowableReplay<Integer>)source
                .replay(1, true);

        co.test();

        co.connect();

        BoundedReplayBuffer<Integer> buf = (BoundedReplayBuffer<Integer>)(co.current.get().buffer);

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        assertNull(buf.get().value);

        Object o = buf.get();

        buf.trimHead();

        assertSame(o, buf.get());
    }

    @Test
    public void noHeadRetentionSize() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        FlowableReplay<Integer> co = (FlowableReplay<Integer>)source
                .replay(1, true);

        co.test();

        co.connect();

        BoundedReplayBuffer<Integer> buf = (BoundedReplayBuffer<Integer>)(co.current.get().buffer);

        source.onNext(1);
        source.onNext(2);

        assertNull(buf.get().value);

        buf.trimHead();

        assertNull(buf.get().value);

        Object o = buf.get();

        buf.trimHead();

        assertSame(o, buf.get());
    }

    @Test
    public void noHeadRetentionCompleteTime() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        FlowableReplay<Integer> co = (FlowableReplay<Integer>)source
                .replay(1, TimeUnit.MINUTES, Schedulers.computation(), true);

        co.test();

        co.connect();

        BoundedReplayBuffer<Integer> buf = (BoundedReplayBuffer<Integer>)(co.current.get().buffer);

        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        assertNull(buf.get().value);

        Object o = buf.get();

        buf.trimHead();

        assertSame(o, buf.get());
    }

    @Test
    public void noHeadRetentionErrorTime() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        FlowableReplay<Integer> co = (FlowableReplay<Integer>)source
                .replay(1, TimeUnit.MINUTES, Schedulers.computation(), true);

        co.test();

        co.connect();

        BoundedReplayBuffer<Integer> buf = (BoundedReplayBuffer<Integer>)(co.current.get().buffer);

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        assertNull(buf.get().value);

        Object o = buf.get();

        buf.trimHead();

        assertSame(o, buf.get());
    }

    @Test
    public void noHeadRetentionTime() {
        TestScheduler sch = new TestScheduler();

        PublishProcessor<Integer> source = PublishProcessor.create();

        FlowableReplay<Integer> co = (FlowableReplay<Integer>)source
                .replay(1, TimeUnit.MILLISECONDS, sch, true);

        co.test();

        co.connect();

        BoundedReplayBuffer<Integer> buf = (BoundedReplayBuffer<Integer>)(co.current.get().buffer);

        source.onNext(1);

        sch.advanceTimeBy(2, TimeUnit.MILLISECONDS);

        source.onNext(2);

        assertNull(buf.get().value);

        buf.trimHead();

        assertNull(buf.get().value);

        Object o = buf.get();

        buf.trimHead();

        assertSame(o, buf.get());
    }

    @Test(expected = TestException.class)
    public void createBufferFactoryCrash() {
        FlowableReplay.create(Flowable.just(1), new Supplier<ReplayBuffer<Integer>>() {
            @Override
            public ReplayBuffer<Integer> get() throws Exception {
                throw new TestException();
            }
        })
        .connect();
    }

    @Test
    public void createBufferFactoryCrashOnSubscribe() {
        FlowableReplay.create(Flowable.just(1), new Supplier<ReplayBuffer<Integer>>() {
            @Override
            public ReplayBuffer<Integer> get() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void currentDisposedWhenConnecting() {
        FlowableReplay<Integer> fr = (FlowableReplay<Integer>)FlowableReplay.create(Flowable.<Integer>never(), 16, true);
        fr.connect();

        fr.current.get().dispose();
        assertTrue(fr.current.get().isDisposed());

        fr.connect();

        assertFalse(fr.current.get().isDisposed());
    }

    @Test
    public void noBoundedRetentionViaThreadLocal() throws Exception {
        Flowable<byte[]> source = Flowable.range(1, 200)
        .map(new Function<Integer, byte[]>() {
            @Override
            public byte[] apply(Integer v) throws Exception {
                return new byte[1024 * 1024];
            }
        })
        .replay(new Function<Flowable<byte[]>, Publisher<byte[]>>() {
            @Override
            public Publisher<byte[]> apply(final Flowable<byte[]> f) throws Exception {
                return f.take(1)
                .concatMap(new Function<byte[], Publisher<byte[]>>() {
                    @Override
                    public Publisher<byte[]> apply(byte[] v) throws Exception {
                        return f;
                    }
                });
            }
        }, 1, true)
        .takeLast(1)
        ;

        System.out.println("Bounded Replay Leak check: Wait before GC");
        Thread.sleep(1000);

        System.out.println("Bounded Replay Leak check: GC");
        System.gc();

        Thread.sleep(500);

        final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage memHeap = memoryMXBean.getHeapMemoryUsage();
        long initial = memHeap.getUsed();

        System.out.printf("Bounded Replay Leak check: Starting: %.3f MB%n", initial / 1024.0 / 1024.0);

        final AtomicLong after = new AtomicLong();

        source.subscribe(new Consumer<byte[]>() {
            @Override
            public void accept(byte[] v) throws Exception {
                System.out.println("Bounded Replay Leak check: Wait before GC 2");
                Thread.sleep(1000);

                System.out.println("Bounded Replay Leak check:  GC 2");
                System.gc();

                Thread.sleep(500);

                after.set(memoryMXBean.getHeapMemoryUsage().getUsed());
            }
        });

        System.out.printf("Bounded Replay Leak check: After: %.3f MB%n", after.get() / 1024.0 / 1024.0);

        if (initial + 100 * 1024 * 1024 < after.get()) {
            Assert.fail("Bounded Replay Leak check: Memory leak detected: " + (initial / 1024.0 / 1024.0)
                    + " -> " + after.get() / 1024.0 / 1024.0);
        }
    }

    @Test
    public void sizeBoundEagerTruncate() throws Exception {

        PublishProcessor<int[]> pp = PublishProcessor.create();

        ConnectableFlowable<int[]> cf = pp.replay(1, true);

        TestSubscriber<int[]> ts = cf.test();

        cf.connect();

        Thread.sleep(200);
        System.gc();
        Thread.sleep(200);

        final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long initial = memoryMXBean.getHeapMemoryUsage().getUsed();

        System.out.printf("Bounded Replay Leak check: Starting: %.3f MB%n", initial / 1024.0 / 1024.0);

        pp.onNext(new int[100 * 1024 * 1024]);

        ts.assertValueCount(1);
        ts.values().clear();

        pp.onNext(new int[0]);

        Thread.sleep(200);
        System.gc();
        Thread.sleep(200);

        long after = memoryMXBean.getHeapMemoryUsage().getUsed();

        ts.cancel();

        System.out.printf("Bounded Replay Leak check: After: %.3f MB%n", after / 1024.0 / 1024.0);

        if (initial + 100 * 1024 * 1024 < after) {
            Assert.fail("Bounded Replay Leak check: Memory leak detected: " + (initial / 1024.0 / 1024.0)
                    + " -> " + after / 1024.0 / 1024.0);
        }
    }

    @Test
    public void timeBoundEagerTruncate() throws Exception {

        PublishProcessor<int[]> pp = PublishProcessor.create();

        TestScheduler scheduler = new TestScheduler();

        ConnectableFlowable<int[]> cf = pp.replay(1, TimeUnit.SECONDS, scheduler, true);

        TestSubscriber<int[]> ts = cf.test();

        cf.connect();

        Thread.sleep(200);
        System.gc();
        Thread.sleep(200);

        final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long initial = memoryMXBean.getHeapMemoryUsage().getUsed();

        System.out.printf("Bounded Replay Leak check: Starting: %.3f MB%n", initial / 1024.0 / 1024.0);

        pp.onNext(new int[100 * 1024 * 1024]);

        ts.assertValueCount(1);
        ts.values().clear();

        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        pp.onNext(new int[0]);

        Thread.sleep(200);
        System.gc();
        Thread.sleep(200);

        long after = memoryMXBean.getHeapMemoryUsage().getUsed();

        ts.cancel();

        System.out.printf("Bounded Replay Leak check: After: %.3f MB%n", after / 1024.0 / 1024.0);

        if (initial + 100 * 1024 * 1024 < after) {
            Assert.fail("Bounded Replay Leak check: Memory leak detected: " + (initial / 1024.0 / 1024.0)
                    + " -> " + after / 1024.0 / 1024.0);
        }
    }

    @Test
    public void timeAndSizeBoundEagerTruncate() throws Exception {

        PublishProcessor<int[]> pp = PublishProcessor.create();

        TestScheduler scheduler = new TestScheduler();

        ConnectableFlowable<int[]> cf = pp.replay(1, 5, TimeUnit.SECONDS, scheduler, true);

        TestSubscriber<int[]> ts = cf.test();

        cf.connect();

        Thread.sleep(200);
        System.gc();
        Thread.sleep(200);

        final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long initial = memoryMXBean.getHeapMemoryUsage().getUsed();

        System.out.printf("Bounded Replay Leak check: Starting: %.3f MB%n", initial / 1024.0 / 1024.0);

        pp.onNext(new int[100 * 1024 * 1024]);

        ts.assertValueCount(1);
        ts.values().clear();

        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        pp.onNext(new int[0]);

        Thread.sleep(200);
        System.gc();
        Thread.sleep(200);

        long after = memoryMXBean.getHeapMemoryUsage().getUsed();

        ts.cancel();

        System.out.printf("Bounded Replay Leak check: After: %.3f MB%n", after / 1024.0 / 1024.0);

        if (initial + 100 * 1024 * 1024 < after) {
            Assert.fail("Bounded Replay Leak check: Memory leak detected: " + (initial / 1024.0 / 1024.0)
                    + " -> " + after / 1024.0 / 1024.0);
        }
    }

    @Test
    public void sizeBoundSelectorEagerTruncate() throws Exception {

        PublishProcessor<int[]> pp = PublishProcessor.create();

        Flowable<int[]> cf = pp.replay(Functions.<Flowable<int[]>>identity(), 1, true);

        TestSubscriber<int[]> ts = cf.test();

        Thread.sleep(200);
        System.gc();
        Thread.sleep(200);

        final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long initial = memoryMXBean.getHeapMemoryUsage().getUsed();

        System.out.printf("Bounded Replay Leak check: Starting: %.3f MB%n", initial / 1024.0 / 1024.0);

        pp.onNext(new int[100 * 1024 * 1024]);

        ts.assertValueCount(1);
        ts.values().clear();

        pp.onNext(new int[0]);

        Thread.sleep(200);
        System.gc();
        Thread.sleep(200);

        long after = memoryMXBean.getHeapMemoryUsage().getUsed();

        ts.cancel();

        System.out.printf("Bounded Replay Leak check: After: %.3f MB%n", after / 1024.0 / 1024.0);

        if (initial + 100 * 1024 * 1024 < after) {
            Assert.fail("Bounded Replay Leak check: Memory leak detected: " + (initial / 1024.0 / 1024.0)
                    + " -> " + after / 1024.0 / 1024.0);
        }
    }

    @Test
    public void timeBoundSelectorEagerTruncate() throws Exception {

        PublishProcessor<int[]> pp = PublishProcessor.create();

        TestScheduler scheduler = new TestScheduler();

        Flowable<int[]> cf = pp.replay(Functions.<Flowable<int[]>>identity(), 1, TimeUnit.SECONDS, scheduler, true);

        TestSubscriber<int[]> ts = cf.test();

        Thread.sleep(200);
        System.gc();
        Thread.sleep(200);

        final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long initial = memoryMXBean.getHeapMemoryUsage().getUsed();

        System.out.printf("Bounded Replay Leak check: Starting: %.3f MB%n", initial / 1024.0 / 1024.0);

        pp.onNext(new int[100 * 1024 * 1024]);

        ts.assertValueCount(1);
        ts.values().clear();

        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        pp.onNext(new int[0]);

        Thread.sleep(200);
        System.gc();
        Thread.sleep(200);

        long after = memoryMXBean.getHeapMemoryUsage().getUsed();

        ts.cancel();

        System.out.printf("Bounded Replay Leak check: After: %.3f MB%n", after / 1024.0 / 1024.0);

        if (initial + 100 * 1024 * 1024 < after) {
            Assert.fail("Bounded Replay Leak check: Memory leak detected: " + (initial / 1024.0 / 1024.0)
                    + " -> " + after / 1024.0 / 1024.0);
        }
    }

    @Test
    public void timeAndSizeBoundSelectorEagerTruncate() throws Exception {

        PublishProcessor<int[]> pp = PublishProcessor.create();

        TestScheduler scheduler = new TestScheduler();

        Flowable<int[]> cf = pp.replay(Functions.<Flowable<int[]>>identity(), 1, 5, TimeUnit.SECONDS, scheduler, true);

        TestSubscriber<int[]> ts = cf.test();

        Thread.sleep(200);
        System.gc();
        Thread.sleep(200);

        final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long initial = memoryMXBean.getHeapMemoryUsage().getUsed();

        System.out.printf("Bounded Replay Leak check: Starting: %.3f MB%n", initial / 1024.0 / 1024.0);

        pp.onNext(new int[100 * 1024 * 1024]);

        ts.assertValueCount(1);
        ts.values().clear();

        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        pp.onNext(new int[0]);

        Thread.sleep(200);
        System.gc();
        Thread.sleep(200);

        long after = memoryMXBean.getHeapMemoryUsage().getUsed();

        ts.cancel();

        System.out.printf("Bounded Replay Leak check: After: %.3f MB%n", after / 1024.0 / 1024.0);

        if (initial + 100 * 1024 * 1024 < after) {
            Assert.fail("Bounded Replay Leak check: Memory leak detected: " + (initial / 1024.0 / 1024.0)
                    + " -> " + after / 1024.0 / 1024.0);
        }
    }

    @Test
    public void timeAndSizeNoTerminalTruncationOnTimechange() {
        Flowable.just(1).replay(1, 1, TimeUnit.SECONDS, new TimesteppingScheduler(), true)
        .autoConnect()
        .test()
        .assertComplete()
        .assertNoErrors();
    }
}
