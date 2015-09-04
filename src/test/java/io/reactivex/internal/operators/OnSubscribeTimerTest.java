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

package io.reactivex.internal.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.mockito.*;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subscribers.TestSubscriber;

public class OnSubscribeTimerTest {
    @Mock
    Subscriber<Object> observer;
    @Mock
    Subscriber<Long> observer2;
    
    TestScheduler scheduler;

    @Before
    public void before() {
        observer = TestHelper.mockSubscriber();
        
        observer2 = TestHelper.mockSubscriber();
        
        scheduler = new TestScheduler();
    }

    @Test
    public void testTimerOnce() {
        Observable.timer(100, TimeUnit.MILLISECONDS, scheduler).subscribe(observer);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        verify(observer, times(1)).onNext(0L);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTimerPeriodically() {
        TestSubscriber<Long> ts = new TestSubscriber<>();
        
        Observable.interval(100, 100, TimeUnit.MILLISECONDS, scheduler).subscribe(ts);
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        ts.assertValue(0L);

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        ts.assertValues(0L, 1L);

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        ts.assertValues(0L, 1L, 2L);

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        ts.assertValues(0L, 1L, 2L, 3L);

        ts.dispose();
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        ts.assertValues(0L, 1L, 2L, 3L);

        ts.assertNotComplete();
        ts.assertNoErrors();
    }
    @Test
    public void testInterval() {
        Observable<Long> w = Observable.interval(1, TimeUnit.SECONDS, scheduler);
        TestSubscriber<Long> ts = new TestSubscriber<>();
        w.subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);

        ts.assertValues(0L, 1L);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.dispose();
        
        scheduler.advanceTimeTo(4, TimeUnit.SECONDS);
        ts.assertValues(0L, 1L);
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void testWithMultipleSubscribersStartingAtSameTime() {
        Observable<Long> w = Observable.interval(1, TimeUnit.SECONDS, scheduler);
        
        TestSubscriber<Long> ts1 = new TestSubscriber<>();
        TestSubscriber<Long> ts2 = new TestSubscriber<>();
        
        w.subscribe(ts1);
        w.subscribe(ts2);

        ts1.assertNoValues();
        ts2.assertNoValues();

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);

        ts1.assertValues(0L, 1L);
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        ts2.assertValues(0L, 1L);
        ts2.assertNoErrors();
        ts2.assertNotComplete();

        ts1.dispose();
        ts2.dispose();
        
        scheduler.advanceTimeTo(4, TimeUnit.SECONDS);

        ts1.assertValues(0L, 1L);
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        ts2.assertValues(0L, 1L);
        ts2.assertNoErrors();
        ts2.assertNotComplete();
    }

    @Test
    public void testWithMultipleStaggeredSubscribers() {
        Observable<Long> w = Observable.interval(1, TimeUnit.SECONDS, scheduler);
        
        TestSubscriber<Long> ts1 = new TestSubscriber<>();
        
        w.subscribe(ts1);

        ts1.assertNoErrors();
        
        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);
        
        TestSubscriber<Long> ts2 = new TestSubscriber<>();
        
        w.subscribe(ts2);

        ts1.assertValues(0L, 1L);
        ts1.assertNoErrors();
        ts1.assertNotComplete();
        
        ts2.assertNoValues();

        scheduler.advanceTimeTo(4, TimeUnit.SECONDS);

        ts1.assertValues(0L, 1L, 2L, 3L);

        ts2.assertValues(0L, 1L);

        ts1.dispose();
        ts2.dispose();

        ts1.assertValues(0L, 1L, 2L, 3L);
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        ts2.assertValues(0L, 1L);
        ts2.assertNoErrors();
        ts2.assertNotComplete();
    }

    @Test
    public void testWithMultipleStaggeredSubscribersAndPublish() {
        ConnectableObservable<Long> w = Observable.interval(1, TimeUnit.SECONDS, scheduler).publish();
        
        TestSubscriber<Long> ts1 = new TestSubscriber<>();
        
        w.subscribe(ts1);
        w.connect();
        
        ts1.assertNoValues();

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);
        
        TestSubscriber<Long> ts2 = new TestSubscriber<>();
        w.subscribe(ts2);

        ts1.assertValues(0L, 1L);
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        ts2.assertNoValues();

        scheduler.advanceTimeTo(4, TimeUnit.SECONDS);

        ts1.assertValues(0L, 1L, 2L, 3L);

        ts2.assertValues(2L, 3L);

        ts1.dispose();
        ts2.dispose();

        ts1.assertValues(0L, 1L, 2L, 3L);
        ts1.assertNoErrors();
        ts1.assertNotComplete();
        
        ts2.assertValues(2L, 3L);
        ts2.assertNoErrors();
        ts2.assertNotComplete();
    }
    @Test
    public void testOnceObserverThrows() {
        Observable<Long> source = Observable.timer(100, TimeUnit.MILLISECONDS, scheduler);
        
        source.safeSubscribe(new Observer<Long>() {

            @Override
            public void onNext(Long t) {
                throw new TestException();
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onComplete() {
                observer.onComplete();
            }
        });
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        verify(observer).onError(any(TestException.class));
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
    }
    @Test
    public void testPeriodicObserverThrows() {
        Observable<Long> source = Observable.interval(100, 100, TimeUnit.MILLISECONDS, scheduler);
        
        InOrder inOrder = inOrder(observer);
        
        source.safeSubscribe(new Observer<Long>() {

            @Override
            public void onNext(Long t) {
                if (t > 0) {
                    throw new TestException();
                }
                observer.onNext(t);
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onComplete() {
                observer.onComplete();
            }
        });
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        inOrder.verify(observer).onNext(0L);
        inOrder.verify(observer).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(observer, never()).onComplete();
    }
}