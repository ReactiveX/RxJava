/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.observable;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.mockito.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.flowable.TestHelper;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.observers.*;
import io.reactivex.schedulers.TestScheduler;

public class NbpOnSubscribeTimerTest {
    @Mock
    Observer<Object> NbpObserver;
    @Mock
    Observer<Long> observer2;
    
    TestScheduler scheduler;

    @Before
    public void before() {
        NbpObserver = TestHelper.mockNbpSubscriber();
        
        observer2 = TestHelper.mockNbpSubscriber();
        
        scheduler = new TestScheduler();
    }

    @Test
    public void testTimerOnce() {
        Observable.timer(100, TimeUnit.MILLISECONDS, scheduler).subscribe(NbpObserver);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        verify(NbpObserver, times(1)).onNext(0L);
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTimerPeriodically() {
        TestObserver<Long> ts = new TestObserver<Long>();
        
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
        TestObserver<Long> ts = new TestObserver<Long>();
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
        
        TestObserver<Long> ts1 = new TestObserver<Long>();
        TestObserver<Long> ts2 = new TestObserver<Long>();
        
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
        
        TestObserver<Long> ts1 = new TestObserver<Long>();
        
        w.subscribe(ts1);

        ts1.assertNoErrors();
        
        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);
        
        TestObserver<Long> ts2 = new TestObserver<Long>();
        
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
        
        TestObserver<Long> ts1 = new TestObserver<Long>();
        
        w.subscribe(ts1);
        w.connect();
        
        ts1.assertNoValues();

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);
        
        TestObserver<Long> ts2 = new TestObserver<Long>();
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
        
        source.safeSubscribe(new DefaultObserver<Long>() {

            @Override
            public void onNext(Long t) {
                throw new TestException();
            }

            @Override
            public void onError(Throwable e) {
                NbpObserver.onError(e);
            }

            @Override
            public void onComplete() {
                NbpObserver.onComplete();
            }
        });
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        verify(NbpObserver).onError(any(TestException.class));
        verify(NbpObserver, never()).onNext(anyLong());
        verify(NbpObserver, never()).onComplete();
    }
    @Test
    public void testPeriodicObserverThrows() {
        Observable<Long> source = Observable.interval(100, 100, TimeUnit.MILLISECONDS, scheduler);
        
        InOrder inOrder = inOrder(NbpObserver);
        
        source.safeSubscribe(new DefaultObserver<Long>() {

            @Override
            public void onNext(Long t) {
                if (t > 0) {
                    throw new TestException();
                }
                NbpObserver.onNext(t);
            }

            @Override
            public void onError(Throwable e) {
                NbpObserver.onError(e);
            }

            @Override
            public void onComplete() {
                NbpObserver.onComplete();
            }
        });
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        inOrder.verify(NbpObserver).onNext(0L);
        inOrder.verify(NbpObserver).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(NbpObserver, never()).onComplete();
    }
}