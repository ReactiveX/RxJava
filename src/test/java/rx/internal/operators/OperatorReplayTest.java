/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.TestException;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.internal.operators.OperatorReplay.BoundedReplayBuffer;
import rx.internal.operators.OperatorReplay.Node;
import rx.internal.operators.OperatorReplay.SizeAndTimeBoundReplayBuffer;
import rx.internal.util.PlatformDependent;
import rx.observables.ConnectableObservable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class OperatorReplayTest {
    @Test
    public void testBufferedReplay() {
        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(3);
        co.connect();

        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            source.onNext(4);
            source.onCompleted();
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testBufferedWindowReplay() {
        PublishSubject<Integer> source = PublishSubject.create();
        TestScheduler scheduler = new TestScheduler();
        ConnectableObservable<Integer> co = source.replay(3, 100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
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
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
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

        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onCompleted();
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }
        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testReplaySelector() {
        final Func1<Integer, Integer> dbl = new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                return t1 * 2;
            }

        };

        Func1<Observable<Integer>, Observable<Integer>> selector = new Func1<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Observable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> co = source.replay(selector);

        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            source.onNext(4);
            source.onCompleted();
            inOrder.verify(observer1, times(1)).onNext(8);
            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

    }

    @Test
    public void testBufferedReplaySelector() {

        final Func1<Integer, Integer> dbl = new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                return t1 * 2;
            }

        };

        Func1<Observable<Integer>, Observable<Integer>> selector = new Func1<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Observable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> co = source.replay(selector, 3);

        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            source.onNext(4);
            source.onCompleted();
            inOrder.verify(observer1, times(1)).onNext(8);
            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testWindowedReplaySelector() {

        final Func1<Integer, Integer> dbl = new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                return t1 * 2;
            }

        };

        Func1<Observable<Integer>, Observable<Integer>> selector = new Func1<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Observable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> co = source.replay(selector, 100, TimeUnit.MILLISECONDS, scheduler);

        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onCompleted();
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }
        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testBufferedReplayError() {
        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(3);
        co.connect();

        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
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
            verify(observer1, never()).onCompleted();

        }

        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onCompleted();
        }
    }

    @Test
    public void testWindowedReplayError() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
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
            verify(observer1, never()).onCompleted();

        }
        {
            @SuppressWarnings("unchecked")
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onCompleted();
        }
    }
    @Test
    public void testSynchronousDisconnect() {
        final AtomicInteger effectCounter = new AtomicInteger();
        Observable<Integer> source = Observable.just(1, 2, 3, 4)
        .doOnNext(new Action1<Integer>() {
            @Override
            public void call(Integer v) {
                effectCounter.incrementAndGet();
                System.out.println("Sideeffect #" + v);
            }
        });
        
        Observable<Integer> result = source.replay(
        new Func1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Observable<Integer> o) {
                return o.take(2);
            }
        });
        
        for (int i = 1; i < 3; i++) {
            effectCounter.set(0);
            System.out.printf("- %d -%n", i);
            result.subscribe(new Action1<Integer>() {

                @Override
                public void call(Integer t1) {
                    System.out.println(t1);
                }
                
            }, new Action1<Throwable>() {

                @Override
                public void call(Throwable t1) {
                    t1.printStackTrace();
                }
            }, 
            new Action0() {
                @Override
                public void call() {
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
        Action1<Integer> sourceNext = mock(Action1.class);
        Action0 sourceCompleted = mock(Action0.class);
        Action0 sourceUnsubscribed = mock(Action0.class);
        Observer<Integer> spiedSubscriberBeforeConnect = mock(Observer.class);
        Observer<Integer> spiedSubscriberAfterConnect = mock(Observer.class);

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
        verifyObserverMock(spiedSubscriberBeforeConnect, 2, 4);
        verifyObserverMock(spiedSubscriberAfterConnect, 2, 4);

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
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue2191_SchedulerUnsubscribe() throws Exception {
        // setup mocks
        Action1<Integer> sourceNext = mock(Action1.class);
        Action0 sourceCompleted = mock(Action0.class);
        Action0 sourceUnsubscribed = mock(Action0.class);
        final Scheduler mockScheduler = mock(Scheduler.class);
        final Subscription mockSubscription = mock(Subscription.class);
        Worker spiedWorker = workerSpy(mockSubscription);
        Observer<Integer> mockObserverBeforeConnect = mock(Observer.class);
        Observer<Integer> mockObserverAfterConnect = mock(Observer.class);

        when(mockScheduler.createWorker()).thenReturn(spiedWorker);

        // Observable under test
        ConnectableObservable<Integer> replay = Observable.just(1, 2, 3)
                .doOnNext(sourceNext)
                .doOnUnsubscribe(sourceUnsubscribed)
                .doOnCompleted(sourceCompleted)
                .subscribeOn(mockScheduler).replay();

        replay.subscribe(mockObserverBeforeConnect);
        replay.subscribe(mockObserverBeforeConnect);
        replay.connect();
        replay.subscribe(mockObserverAfterConnect);
        replay.subscribe(mockObserverAfterConnect);

        // verify interactions
        verify(sourceNext, times(1)).call(1);
        verify(sourceNext, times(1)).call(2);
        verify(sourceNext, times(1)).call(3);
        verify(sourceCompleted, times(1)).call();
        verify(mockScheduler, times(1)).createWorker();
        verify(spiedWorker, times(1)).schedule((Action0)notNull());
        verifyObserverMock(mockObserverBeforeConnect, 2, 6);
        verifyObserverMock(mockObserverAfterConnect, 2, 6);

        verify(spiedWorker, times(1)).isUnsubscribed();
        // subscribeOn didn't unsubscribe the worker before but it should have
        verify(spiedWorker, times(2)).unsubscribe();
        verify(sourceUnsubscribed, times(1)).call();

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
     * Specifically test interaction with a Scheduler with subscribeOn
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue2191_SchedulerUnsubscribeOnError() throws Exception {
        // setup mocks
        Action1<Integer> sourceNext = mock(Action1.class);
        Action0 sourceCompleted = mock(Action0.class);
        Action1<Throwable> sourceError = mock(Action1.class);
        Action0 sourceUnsubscribed = mock(Action0.class);
        final Scheduler mockScheduler = mock(Scheduler.class);
        final Subscription mockSubscription = mock(Subscription.class);
        Worker spiedWorker = workerSpy(mockSubscription);
        Observer<Integer> mockObserverBeforeConnect = mock(Observer.class);
        Observer<Integer> mockObserverAfterConnect = mock(Observer.class);

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

        replay.subscribe(mockObserverBeforeConnect);
        replay.subscribe(mockObserverBeforeConnect);
        replay.connect();
        replay.subscribe(mockObserverAfterConnect);
        replay.subscribe(mockObserverAfterConnect);

        // verify interactions
        verify(mockScheduler, times(1)).createWorker();
        verify(spiedWorker, times(1)).schedule((Action0)notNull());
        verify(sourceNext, times(1)).call(1);
        verify(sourceError, times(1)).call(illegalArgumentException);
        verifyObserver(mockObserverBeforeConnect, 2, 2, illegalArgumentException);
        verifyObserver(mockObserverAfterConnect, 2, 2, illegalArgumentException);

        verify(spiedWorker, times(1)).isUnsubscribed();
        // subscribeOn didn't unsubscribe the worker before but it should have
        verify(spiedWorker, times(2)).unsubscribe();
        verify(sourceUnsubscribed, times(1)).call();

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

    private static void verifyObserverMock(Observer<Integer> mock, int numSubscriptions, int numItemsExpected) {
        verify(mock, times(numItemsExpected)).onNext((Integer) notNull());
        verify(mock, times(numSubscriptions)).onCompleted();
        verifyNoMoreInteractions(mock);
    }

    private static void verifyObserver(Observer<Integer> mock, int numSubscriptions, int numItemsExpected, Throwable error) {
        verify(mock, times(numItemsExpected)).onNext((Integer) notNull());
        verify(mock, times(numSubscriptions)).onError(error);
        verifyNoMoreInteractions(mock);
    }

    public static Worker workerSpy(final Subscription mockSubscription) {
        return spy(new InprocessWorker(mockSubscription));
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
        TestScheduler test = Schedulers.test();
        SizeAndTimeBoundReplayBuffer<Integer> buf = new SizeAndTimeBoundReplayBuffer<Integer>(2, 2000, test);
        List<Integer> values = new ArrayList<Integer>();
        
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
    public void testBackpressure() {
        final AtomicLong requested = new AtomicLong();
        Observable<Integer> source = Observable.range(1, 1000)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        requested.addAndGet(t);
                    }
                });
        ConnectableObservable<Integer> co = source.replay();
        
        TestSubscriber<Integer> ts1 = TestSubscriber.create(10);
        TestSubscriber<Integer> ts2 = TestSubscriber.create(90);
        
        co.subscribe(ts1);
        co.subscribe(ts2);
        
        ts2.requestMore(10);
        
        co.connect();
        
        ts1.assertValueCount(10);
        ts1.assertNoTerminalEvent();
        
        ts2.assertValueCount(100);
        ts2.assertNoTerminalEvent();
        
        Assert.assertEquals(100, requested.get());
    }
    
    @Test
    public void testBackpressureBounded() {
        final AtomicLong requested = new AtomicLong();
        Observable<Integer> source = Observable.range(1, 1000)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        requested.addAndGet(t);
                    }
                });
        ConnectableObservable<Integer> co = source.replay(50);
        
        TestSubscriber<Integer> ts1 = TestSubscriber.create(10);
        TestSubscriber<Integer> ts2 = TestSubscriber.create(90);
        
        co.subscribe(ts1);
        co.subscribe(ts2);
        
        ts2.requestMore(10);
        
        co.connect();
        
        ts1.assertValueCount(10);
        ts1.assertNoTerminalEvent();
        
        ts2.assertValueCount(100);
        ts2.assertNoTerminalEvent();
        
        Assert.assertEquals(100, requested.get());
    }
    
    @Test
    public void testColdReplayNoBackpressure() {
        Observable<Integer> source = Observable.range(0, 1000).replay().autoConnect();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        source.subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminalEvent();
        List<Integer> onNextEvents = ts.getOnNextEvents();
        assertEquals(1000, onNextEvents.size());

        for (int i = 0; i < 1000; i++) {
            assertEquals((Integer)i, onNextEvents.get(i));
        }
    }
    @Test
    public void testColdReplayBackpressure() {
        Observable<Integer> source = Observable.range(0, 1000).replay().autoConnect();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.requestMore(10);
        
        source.subscribe(ts);

        ts.assertNoErrors();
        assertTrue(ts.getOnCompletedEvents().isEmpty());
        List<Integer> onNextEvents = ts.getOnNextEvents();
        assertEquals(10, onNextEvents.size());

        for (int i = 0; i < 10; i++) {
            assertEquals((Integer)i, onNextEvents.get(i));
        }
        
        ts.unsubscribe();
    }
    
    @Test
    public void testCache() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        Observable<String> o = Observable.create(new Observable.OnSubscribe<String>() {

            @Override
            public void call(final Subscriber<? super String> observer) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        System.out.println("published observable being executed");
                        observer.onNext("one");
                        observer.onCompleted();
                    }
                }).start();
            }
        }).replay().autoConnect();

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(new Action1<String>() {

            @Override
            public void call(String v) {
                assertEquals("one", v);
                System.out.println("v: " + v);
                latch.countDown();
            }
        });

        // subscribe again
        o.subscribe(new Action1<String>() {

            @Override
            public void call(String v) {
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
        Action0 unsubscribe = mock(Action0.class);
        Observable<Integer> o = Observable.just(1).doOnUnsubscribe(unsubscribe).cache();
        o.subscribe();
        o.subscribe();
        o.subscribe();
        verify(unsubscribe, times(1)).call();
    }
    
    @Test
    public void testTake() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Observable<Integer> cached = Observable.range(1, 100).replay().autoConnect();
        cached.take(10).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        ts.assertUnsubscribed();
    }
    
    @Test
    public void testAsync() {
        Observable<Integer> source = Observable.range(1, 10000);
        for (int i = 0; i < 100; i++) {
            TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>();
            
            Observable<Integer> cached = source.replay().autoConnect();
            
            cached.observeOn(Schedulers.computation()).subscribe(ts1);
            
            ts1.awaitTerminalEvent(2, TimeUnit.SECONDS);
            ts1.assertNoErrors();
            ts1.assertTerminalEvent();
            assertEquals(10000, ts1.getOnNextEvents().size());
            
            TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>();
            cached.observeOn(Schedulers.computation()).subscribe(ts2);
            
            ts2.awaitTerminalEvent(2, TimeUnit.SECONDS);
            ts2.assertNoErrors();
            ts2.assertTerminalEvent();
            assertEquals(10000, ts2.getOnNextEvents().size());
        }
    }
    @Test
    public void testAsyncComeAndGo() {
        Observable<Long> source = Observable.interval(1, 1, TimeUnit.MILLISECONDS)
                .take(1000)
                .subscribeOn(Schedulers.io());
        Observable<Long> cached = source.replay().autoConnect();
        
        Observable<Long> output = cached.observeOn(Schedulers.computation());
        
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
            ts.assertTerminalEvent();
            
            for (int i = j * 10; i < j * 10 + 10; i++) {
                expected.set(i - j * 10, (long)i);
            }
            
            ts.assertReceivedOnNext(expected);
            
            j++;
        }
    }
    
    @Test
    public void testNoMissingBackpressureException() {
        final int m;
        if (PlatformDependent.isAndroid()) {
            m = 500 * 1000;
        } else {
            m = 4 * 1000 * 1000;
        }
        
        Observable<Integer> firehose = Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> t) {
                for (int i = 0; i < m; i++) {
                    t.onNext(i);
                }
                t.onCompleted();
            }
        });
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        firehose.replay().autoConnect().observeOn(Schedulers.computation()).takeLast(100).subscribe(ts);
        
        ts.awaitTerminalEvent(3, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        
        assertEquals(100, ts.getOnNextEvents().size());
    }
    
    @Test
    public void testValuesAndThenError() {
        Observable<Integer> source = Observable.range(1, 10)
                .concatWith(Observable.<Integer>error(new TestException()))
                .replay().autoConnect();
        
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        source.subscribe(ts);
        
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        Assert.assertTrue(ts.getOnCompletedEvents().isEmpty());
        Assert.assertEquals(1, ts.getOnErrorEvents().size());
        
        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>();
        source.subscribe(ts2);
        
        ts2.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        Assert.assertTrue(ts2.getOnCompletedEvents().isEmpty());
        Assert.assertEquals(1, ts2.getOnErrorEvents().size());
    }
    
    @Test
    public void unsafeChildThrows() {
        final AtomicInteger count = new AtomicInteger();
        
        Observable<Integer> source = Observable.range(1, 100)
        .doOnNext(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
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
        
        source.unsafeSubscribe(ts);
        
        Assert.assertEquals(100, count.get());

        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(TestException.class);
    }
    
    @Test
    public void unboundedLeavesEarly() {
        PublishSubject<Integer> source = PublishSubject.create();

        final List<Long> requests = new ArrayList<Long>();

        Observable<Integer> out = source
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        requests.add(t);
                    }
                }).replay().autoConnect();
        
        TestSubscriber<Integer> ts1 = TestSubscriber.create(5);
        TestSubscriber<Integer> ts2 = TestSubscriber.create(10);
        
        out.subscribe(ts1);
        out.subscribe(ts2);
        ts2.unsubscribe();
        
        Assert.assertEquals(Arrays.asList(5L, 5L), requests);
    }
    
    @Test
    public void testSubscribersComeAndGoAtRequestBoundaries() {
        ConnectableObservable<Integer> source = Observable.range(1, 10).replay(1);
        source.connect();
        
        TestSubscriber<Integer> ts1 = TestSubscriber.create(2);
        
        source.subscribe(ts1);
        
        ts1.assertValues(1, 2);
        ts1.assertNoErrors();
        ts1.unsubscribe();
        
        TestSubscriber<Integer> ts2 = TestSubscriber.create(2);
        
        source.subscribe(ts2);
        
        ts2.assertValues(2, 3);
        ts2.assertNoErrors();
        ts2.unsubscribe();

        TestSubscriber<Integer> ts21 = TestSubscriber.create(1);
        
        source.subscribe(ts21);
        
        ts21.assertValues(3);
        ts21.assertNoErrors();
        ts21.unsubscribe();

        TestSubscriber<Integer> ts22 = TestSubscriber.create(1);
        
        source.subscribe(ts22);
        
        ts22.assertValues(3);
        ts22.assertNoErrors();
        ts22.unsubscribe();

        
        TestSubscriber<Integer> ts3 = TestSubscriber.create();
        
        source.subscribe(ts3);
        
        ts3.assertNoErrors();
        System.out.println(ts3.getOnNextEvents());
        ts3.assertValues(3, 4, 5, 6, 7, 8, 9, 10);
        ts3.assertCompleted();
    }
    
    @Test
    public void testSubscribersComeAndGoAtRequestBoundaries2() {
        ConnectableObservable<Integer> source = Observable.range(1, 10).replay(2);
        source.connect();
        
        TestSubscriber<Integer> ts1 = TestSubscriber.create(2);
        
        source.subscribe(ts1);
        
        ts1.assertValues(1, 2);
        ts1.assertNoErrors();
        ts1.unsubscribe();

        TestSubscriber<Integer> ts11 = TestSubscriber.create(2);
        
        source.subscribe(ts11);
        
        ts11.assertValues(1, 2);
        ts11.assertNoErrors();
        ts11.unsubscribe();

        TestSubscriber<Integer> ts2 = TestSubscriber.create(3);
        
        source.subscribe(ts2);
        
        ts2.assertValues(1, 2, 3);
        ts2.assertNoErrors();
        ts2.unsubscribe();

        TestSubscriber<Integer> ts21 = TestSubscriber.create(1);
        
        source.subscribe(ts21);
        
        ts21.assertValues(2);
        ts21.assertNoErrors();
        ts21.unsubscribe();

        TestSubscriber<Integer> ts22 = TestSubscriber.create(1);
        
        source.subscribe(ts22);
        
        ts22.assertValues(2);
        ts22.assertNoErrors();
        ts22.unsubscribe();

        
        TestSubscriber<Integer> ts3 = TestSubscriber.create();
        
        source.subscribe(ts3);
        
        ts3.assertNoErrors();
        System.out.println(ts3.getOnNextEvents());
        ts3.assertValues(2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts3.assertCompleted();
    }
}