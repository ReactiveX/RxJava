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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
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
        verify(spiedWorker, times(1)).unsubscribe();
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
        verify(spiedWorker, times(1)).unsubscribe();
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

}