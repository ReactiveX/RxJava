/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.operators.nbp.NbpOperatorReplay.*;
import io.reactivex.observables.nbp.NbpConnectableObservable;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.nbp.NbpPublishSubject;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorReplayTest {
    @Test
    public void testBufferedReplay() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();

        NbpConnectableObservable<Integer> co = source.replay(3);
        co.connect();

        {
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
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
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        TestScheduler scheduler = new TestScheduler();
        NbpConnectableObservable<Integer> co = source.replay(3, 100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
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
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
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

        NbpPublishSubject<Integer> source = NbpPublishSubject.create();

        NbpConnectableObservable<Integer> co = source.replay(100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
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
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);
            inOrder.verify(observer1, times(1)).onNext(3);

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

        Function<NbpObservable<Integer>, NbpObservable<Integer>> selector = new Function<NbpObservable<Integer>, NbpObservable<Integer>>() {

            @Override
            public NbpObservable<Integer> apply(NbpObservable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        NbpPublishSubject<Integer> source = NbpPublishSubject.create();

        NbpObservable<Integer> co = source.replay(selector);

        {
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
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
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
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

        Function<NbpObservable<Integer>, NbpObservable<Integer>> selector = new Function<NbpObservable<Integer>, NbpObservable<Integer>>() {

            @Override
            public NbpObservable<Integer> apply(NbpObservable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        NbpPublishSubject<Integer> source = NbpPublishSubject.create();

        NbpObservable<Integer> co = source.replay(selector, 3);

        {
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
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
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
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

        Function<NbpObservable<Integer>, NbpObservable<Integer>> selector = new Function<NbpObservable<Integer>, NbpObservable<Integer>>() {

            @Override
            public NbpObservable<Integer> apply(NbpObservable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        TestScheduler scheduler = new TestScheduler();

        NbpPublishSubject<Integer> source = NbpPublishSubject.create();

        NbpObservable<Integer> co = source.replay(selector, 100, TimeUnit.MILLISECONDS, scheduler);

        {
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
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
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testBufferedReplayError() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();

        NbpConnectableObservable<Integer> co = source.replay(3);
        co.connect();

        {
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
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
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
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

        NbpPublishSubject<Integer> source = NbpPublishSubject.create();

        NbpConnectableObservable<Integer> co = source.replay(100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
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
            NbpSubscriber<Object> observer1 = TestHelper.mockNbpSubscriber();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onComplete();
        }
    }
    
    @Test
    public void testSynchronousDisconnect() {
        final AtomicInteger effectCounter = new AtomicInteger();
        NbpObservable<Integer> source = NbpObservable.just(1, 2, 3, 4)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) {
                effectCounter.incrementAndGet();
                System.out.println("Sideeffect #" + v);
            }
        });
        
        NbpObservable<Integer> result = source.replay(
        new Function<NbpObservable<Integer>, NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> apply(NbpObservable<Integer> o) {
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
            new Runnable() {
                @Override
                public void run() {
                    System.out.println("Done");
                }
            });
            assertEquals(2, effectCounter.get());
        }
    }


    /**
     * test the basic expectation of OperatorMulticast via replay
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue2191_UnsubscribeSource() {
        // setup mocks
        Consumer<Integer> sourceNext = mock(Consumer.class);
        Runnable sourceCompleted = mock(Runnable.class);
        Runnable sourceUnsubscribed = mock(Runnable.class);
        NbpSubscriber<Integer> spiedSubscriberBeforeConnect = TestHelper.mockNbpSubscriber();
        NbpSubscriber<Integer> spiedSubscriberAfterConnect = TestHelper.mockNbpSubscriber();

        // NbpObservable under test
        NbpObservable<Integer> source = NbpObservable.just(1,2);

        NbpConnectableObservable<Integer> replay = source
                .doOnNext(sourceNext)
                .doOnCancel(sourceUnsubscribed)
                .doOnComplete(sourceCompleted)
                .replay();

        replay.subscribe(spiedSubscriberBeforeConnect);
        replay.subscribe(spiedSubscriberBeforeConnect);
        replay.connect();
        replay.subscribe(spiedSubscriberAfterConnect);
        replay.subscribe(spiedSubscriberAfterConnect);

        verify(spiedSubscriberBeforeConnect, times(2)).onSubscribe(any());
        verify(spiedSubscriberAfterConnect, times(2)).onSubscribe(any());

        // verify interactions
        verify(sourceNext, times(1)).accept(1);
        verify(sourceNext, times(1)).accept(2);
        verify(sourceCompleted, times(1)).run();
        verifyObserverMock(spiedSubscriberBeforeConnect, 2, 4);
        verifyObserverMock(spiedSubscriberAfterConnect, 2, 4);

        verify(sourceUnsubscribed, times(1)).run();

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
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue2191_SchedulerUnsubscribe() throws Exception {
        // setup mocks
        Consumer<Integer> sourceNext = mock(Consumer.class);
        Runnable sourceCompleted = mock(Runnable.class);
        Runnable sourceUnsubscribed = mock(Runnable.class);
        final TestScheduler mockScheduler = Schedulers.test();
        
        NbpSubscriber<Integer> mockObserverBeforeConnect = TestHelper.mockNbpSubscriber();
        NbpSubscriber<Integer> mockObserverAfterConnect = TestHelper.mockNbpSubscriber();

        // NbpObservable under test
        NbpConnectableObservable<Integer> replay = NbpObservable.just(1, 2, 3)
                .doOnNext(sourceNext)
                .doOnCancel(sourceUnsubscribed)
                .doOnComplete(sourceCompleted)
                .subscribeOn(mockScheduler).replay();

        replay.subscribe(mockObserverBeforeConnect);
        replay.connect();
        replay.subscribe(mockObserverAfterConnect);

        verify(mockObserverBeforeConnect).onSubscribe(any());
        verify(mockObserverAfterConnect).onSubscribe(any());

        mockScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        // verify interactions
        verify(sourceNext, times(1)).accept(1);
        verify(sourceNext, times(1)).accept(2);
        verify(sourceNext, times(1)).accept(3);
        verify(sourceCompleted, times(1)).run();
        verifyObserverMock(mockObserverBeforeConnect, 1, 3);
        verifyObserverMock(mockObserverAfterConnect, 1, 3);

        // FIXME not supported
//        verify(spiedWorker, times(1)).isUnsubscribed();
        // FIXME publish calls cancel too
        verify(sourceUnsubscribed, times(1)).run();

        verifyNoMoreInteractions(sourceNext);
        verifyNoMoreInteractions(sourceCompleted);
        verifyNoMoreInteractions(sourceUnsubscribed);
        verifyNoMoreInteractions(mockObserverBeforeConnect);
        verifyNoMoreInteractions(mockObserverAfterConnect);
    }

    /**
     * Specifically test interaction with a Scheduler with subscribeOn
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue2191_SchedulerUnsubscribeOnError() throws Exception {
        // setup mocks
        Consumer<Integer> sourceNext = mock(Consumer.class);
        Runnable sourceCompleted = mock(Runnable.class);
        Consumer<Throwable> sourceError = mock(Consumer.class);
        Runnable sourceUnsubscribed = mock(Runnable.class);
        final TestScheduler mockScheduler = new TestScheduler();
        NbpSubscriber<Integer> mockObserverBeforeConnect = TestHelper.mockNbpSubscriber();
        NbpSubscriber<Integer> mockObserverAfterConnect = TestHelper.mockNbpSubscriber();

        // NbpObservable under test
        Function<Integer, Integer> mockFunc = mock(Function.class);
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
        when(mockFunc.apply(1)).thenReturn(1);
        when(mockFunc.apply(2)).thenThrow(illegalArgumentException);
        NbpConnectableObservable<Integer> replay = NbpObservable.just(1, 2, 3).map(mockFunc)
                .doOnNext(sourceNext)
                .doOnCancel(sourceUnsubscribed)
                .doOnComplete(sourceCompleted)
                .doOnError(sourceError)
                .subscribeOn(mockScheduler).replay();

        replay.subscribe(mockObserverBeforeConnect);
        replay.connect();
        replay.subscribe(mockObserverAfterConnect);

        verify(mockObserverBeforeConnect).onSubscribe(any());
        verify(mockObserverAfterConnect).onSubscribe(any());
        
        
        mockScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        // verify interactions
        verify(sourceNext, times(1)).accept(1);
        verify(sourceError, times(1)).accept(illegalArgumentException);
        verifyObserver(mockObserverBeforeConnect, 1, 1, illegalArgumentException);
        verifyObserver(mockObserverAfterConnect, 1, 1, illegalArgumentException);

        // FIXME no longer supported
//        verify(spiedWorker, times(1)).isUnsubscribed();
        // FIXME publish also calls cancel
        verify(sourceUnsubscribed, times(1)).run();

        verifyNoMoreInteractions(sourceNext);
        verifyNoMoreInteractions(sourceCompleted);
        verifyNoMoreInteractions(sourceError);
        verifyNoMoreInteractions(sourceUnsubscribed);
        verifyNoMoreInteractions(mockObserverBeforeConnect);
        verifyNoMoreInteractions(mockObserverAfterConnect);
    }

    private static void verifyObserverMock(NbpSubscriber<Integer> mock, int numSubscriptions, int numItemsExpected) {
        verify(mock, times(numItemsExpected)).onNext((Integer) notNull());
        verify(mock, times(numSubscriptions)).onComplete();
        verifyNoMoreInteractions(mock);
    }

    private static void verifyObserver(NbpSubscriber<Integer> mock, int numSubscriptions, int numItemsExpected, Throwable error) {
        verify(mock, times(numItemsExpected)).onNext((Integer) notNull());
        verify(mock, times(numSubscriptions)).onError(error);
        verifyNoMoreInteractions(mock);
    }

    public static Worker workerSpy(final Disposable mockDisposable) {
        return spy(new InprocessWorker(mockDisposable));
    }


    private static class InprocessWorker extends Worker {
        private final Disposable mockDisposable;
//        public boolean unsubscribed;

        public InprocessWorker(Disposable mockDisposable) {
            this.mockDisposable = mockDisposable;
        }

        @Override
        public Disposable schedule(Runnable action) {
            action.run();
            return mockDisposable; // this subscription is returned but discarded
        }

        @Override
        public Disposable schedule(Runnable action, long delayTime, TimeUnit unit) {
            action.run();
            return mockDisposable;
        }

        @Override
        public void dispose() {
//            unsubscribed = true;
        }

        // FIXME no longer supported
//        @Override
//        public boolean isUnsubscribed() {
//            return unsubscribed;
//        }
    }

    @Test
    public void testBoundedReplayBuffer() {
        BoundedReplayBuffer<Integer> buf = new BoundedReplayBuffer<>();
        buf.addLast(new Node(1));
        buf.addLast(new Node(2));
        buf.addLast(new Node(3));
        buf.addLast(new Node(4));
        buf.addLast(new Node(5));
        
        List<Integer> values = new ArrayList<>();
        buf.collect(values);
        
        Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5), values);
        
        buf.removeSome(2);
        buf.removeFirst();
        buf.removeSome(2);
        
        values.clear();
        buf.collect(values);
        Assert.assertTrue(values.isEmpty());

        buf.addLast(new Node(5));
        buf.addLast(new Node(6));
        buf.collect(values);
        
        Assert.assertEquals(Arrays.asList(5, 6), values);
        
    }
    
    @Test
    public void testTimedAndSizedTruncation() {
        TestScheduler test = Schedulers.test();
        SizeAndTimeBoundReplayBuffer<Integer> buf = new SizeAndTimeBoundReplayBuffer<>(2, 2000, TimeUnit.MILLISECONDS, test);
        List<Integer> values = new ArrayList<>();
        
        buf.next(1);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        buf.next(2);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(1, 2), values);

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
    public void testColdReplayNoBackpressure() {
        NbpObservable<Integer> source = NbpObservable.range(0, 1000).replay().autoConnect();
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
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
    public void testCache() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        NbpObservable<String> o = NbpObservable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(final NbpSubscriber<? super String> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        System.out.println("published NbpObservable being executed");
                        NbpObserver.onNext("one");
                        NbpObserver.onComplete();
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
    public void testUnsubscribeSource() {
        Runnable unsubscribe = mock(Runnable.class);
        NbpObservable<Integer> o = NbpObservable.just(1).doOnCancel(unsubscribe).cache();
        o.subscribe();
        o.subscribe();
        o.subscribe();
        verify(unsubscribe, times(1)).run();
    }
    
    @Test
    public void testTake() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();

        NbpObservable<Integer> cached = NbpObservable.range(1, 100).replay().autoConnect();
        cached.take(10).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        // FIXME no longer assertable
//        ts.assertUnsubscribed();
    }
    
    @Test
    public void testAsync() {
        NbpObservable<Integer> source = NbpObservable.range(1, 10000);
        for (int i = 0; i < 100; i++) {
            NbpTestSubscriber<Integer> ts1 = new NbpTestSubscriber<>();
            
            NbpObservable<Integer> cached = source.replay().autoConnect();
            
            cached.observeOn(Schedulers.computation()).subscribe(ts1);
            
            ts1.awaitTerminalEvent(2, TimeUnit.SECONDS);
            ts1.assertNoErrors();
            ts1.assertTerminated();
            assertEquals(10000, ts1.values().size());
            
            NbpTestSubscriber<Integer> ts2 = new NbpTestSubscriber<>();
            cached.observeOn(Schedulers.computation()).subscribe(ts2);
            
            ts2.awaitTerminalEvent(2, TimeUnit.SECONDS);
            ts2.assertNoErrors();
            ts2.assertTerminated();
            assertEquals(10000, ts2.values().size());
        }
    }
    @Test
    public void testAsyncComeAndGo() {
        NbpObservable<Long> source = NbpObservable.interval(1, 1, TimeUnit.MILLISECONDS)
                .take(1000)
                .subscribeOn(Schedulers.io());
        NbpObservable<Long> cached = source.replay().autoConnect();
        
        NbpObservable<Long> output = cached.observeOn(Schedulers.computation());
        
        List<NbpTestSubscriber<Long>> list = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            NbpTestSubscriber<Long> ts = new NbpTestSubscriber<>();
            list.add(ts);
            output.skip(i * 10).take(10).subscribe(ts);
        }

        List<Long> expected = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            expected.add((long)(i - 10));
        }
        int j = 0;
        for (NbpTestSubscriber<Long> ts : list) {
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
        NbpObservable<Integer> firehose = NbpObservable.create(new NbpOnSubscribe<Integer>() {
            @Override
            public void accept(NbpSubscriber<? super Integer> t) {
                t.onSubscribe(EmptyDisposable.INSTANCE);
                for (int i = 0; i < m; i++) {
                    t.onNext(i);
                }
                t.onComplete();
            }
        });
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        firehose.replay().autoConnect().observeOn(Schedulers.computation()).takeLast(100).subscribe(ts);
        
        ts.awaitTerminalEvent(3, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertTerminated();
        
        assertEquals(100, ts.values().size());
    }
    
    @Test
    public void testValuesAndThenError() {
        NbpObservable<Integer> source = NbpObservable.range(1, 10)
                .concatWith(NbpObservable.<Integer>error(new TestException()))
                .replay().autoConnect();
        
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        source.subscribe(ts);
        
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts.assertNotComplete();
        Assert.assertEquals(1, ts.errors().size());
        
        NbpTestSubscriber<Integer> ts2 = new NbpTestSubscriber<>();
        source.subscribe(ts2);
        
        ts2.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts2.assertNotComplete();
        Assert.assertEquals(1, ts2.errors().size());
    }
    
    @Test
    public void unsafeChildThrows() {
        final AtomicInteger count = new AtomicInteger();
        
        NbpObservable<Integer> source = NbpObservable.range(1, 100)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        })
        .replay().autoConnect();
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };
        
        source.unsafeSubscribe(ts);
        
        Assert.assertEquals(100, count.get());

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }
}