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
package rx.operators;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.TestException;
import rx.observables.ConnectableObservable;
import rx.schedulers.TestScheduler;

public class OperatorTimerTest {
    @Mock
    Observer<Object> observer;
    @Mock
    Observer<Long> observer2;
    TestScheduler scheduler;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        scheduler = new TestScheduler();
    }

    @Test
    public void testTimerOnce() {
        Observable.timer(100, TimeUnit.MILLISECONDS, scheduler).subscribe(observer);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        verify(observer, times(1)).onNext(0L);
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTimerPeriodically() {
        Subscription c = Observable.timer(100, 100, TimeUnit.MILLISECONDS, scheduler).subscribe(observer);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(0L);

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(3L);

        c.unsubscribe();
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(any());

        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }
    @Test
    public void testInterval() {
        Observable<Long> w = Observable.interval(1, TimeUnit.SECONDS, scheduler);
        Subscription sub = w.subscribe(observer);

        verify(observer, never()).onNext(0L);
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(0L);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder.verify(observer, never()).onNext(2L);
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        sub.unsubscribe();
        scheduler.advanceTimeTo(4, TimeUnit.SECONDS);
        verify(observer, never()).onNext(2L);
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testWithMultipleSubscribersStartingAtSameTime() {
        Observable<Long> w = Observable.interval(1, TimeUnit.SECONDS, scheduler);
        Subscription sub1 = w.subscribe(observer);
        Subscription sub2 = w.subscribe(observer2);

        verify(observer, never()).onNext(anyLong());
        verify(observer2, never()).onNext(anyLong());

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);

        InOrder inOrder1 = inOrder(observer);
        InOrder inOrder2 = inOrder(observer2);

        inOrder1.verify(observer, times(1)).onNext(0L);
        inOrder1.verify(observer, times(1)).onNext(1L);
        inOrder1.verify(observer, never()).onNext(2L);
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        inOrder2.verify(observer2, times(1)).onNext(0L);
        inOrder2.verify(observer2, times(1)).onNext(1L);
        inOrder2.verify(observer2, never()).onNext(2L);
        verify(observer2, never()).onCompleted();
        verify(observer2, never()).onError(any(Throwable.class));

        sub1.unsubscribe();
        sub2.unsubscribe();
        scheduler.advanceTimeTo(4, TimeUnit.SECONDS);

        verify(observer, never()).onNext(2L);
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        verify(observer2, never()).onNext(2L);
        verify(observer2, never()).onCompleted();
        verify(observer2, never()).onError(any(Throwable.class));
    }

    @Test
    public void testWithMultipleStaggeredSubscribers() {
        Observable<Long> w = Observable.interval(1, TimeUnit.SECONDS, scheduler);
        Subscription sub1 = w.subscribe(observer);

        verify(observer, never()).onNext(anyLong());

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);
        Subscription sub2 = w.subscribe(observer2);

        InOrder inOrder1 = inOrder(observer);
        inOrder1.verify(observer, times(1)).onNext(0L);
        inOrder1.verify(observer, times(1)).onNext(1L);
        inOrder1.verify(observer, never()).onNext(2L);

        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer2, never()).onNext(anyLong());

        scheduler.advanceTimeTo(4, TimeUnit.SECONDS);

        inOrder1.verify(observer, times(1)).onNext(2L);
        inOrder1.verify(observer, times(1)).onNext(3L);

        InOrder inOrder2 = inOrder(observer2);
        inOrder2.verify(observer2, times(1)).onNext(0L);
        inOrder2.verify(observer2, times(1)).onNext(1L);

        sub1.unsubscribe();
        sub2.unsubscribe();

        inOrder1.verify(observer, never()).onNext(anyLong());
        inOrder1.verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        inOrder2.verify(observer2, never()).onNext(anyLong());
        inOrder2.verify(observer2, never()).onCompleted();
        verify(observer2, never()).onError(any(Throwable.class));
    }

    @Test
    public void testWithMultipleStaggeredSubscribersAndPublish() {
        ConnectableObservable<Long> w = Observable.interval(1, TimeUnit.SECONDS, scheduler).publish();
        Subscription sub1 = w.subscribe(observer);
        w.connect();

        verify(observer, never()).onNext(anyLong());

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);
        Subscription sub2 = w.subscribe(observer2);

        InOrder inOrder1 = inOrder(observer);
        inOrder1.verify(observer, times(1)).onNext(0L);
        inOrder1.verify(observer, times(1)).onNext(1L);
        inOrder1.verify(observer, never()).onNext(2L);

        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer2, never()).onNext(anyLong());

        scheduler.advanceTimeTo(4, TimeUnit.SECONDS);

        inOrder1.verify(observer, times(1)).onNext(2L);
        inOrder1.verify(observer, times(1)).onNext(3L);

        InOrder inOrder2 = inOrder(observer2);
        inOrder2.verify(observer2, times(1)).onNext(2L);
        inOrder2.verify(observer2, times(1)).onNext(3L);

        sub1.unsubscribe();
        sub2.unsubscribe();

        inOrder1.verify(observer, never()).onNext(anyLong());
        inOrder1.verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        inOrder2.verify(observer2, never()).onNext(anyLong());
        inOrder2.verify(observer2, never()).onCompleted();
        verify(observer2, never()).onError(any(Throwable.class));
    }
    @Test
    public void testOnceObserverThrows() {
        Observable<Long> source = Observable.timer(100, TimeUnit.MILLISECONDS, scheduler);
        
        source.subscribe(new Subscriber<Long>() {

            @Override
            public void onNext(Long t) {
                throw new TestException();
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onCompleted() {
                observer.onCompleted();
            }
        });
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        verify(observer).onError(any(TestException.class));
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
    }
    @Test
    public void testPeriodicObserverThrows() {
        Observable<Long> source = Observable.timer(100, 100, TimeUnit.MILLISECONDS, scheduler);
        
        InOrder inOrder = inOrder(observer);
        
        source.subscribe(new Subscriber<Long>() {

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
            public void onCompleted() {
                observer.onCompleted();
            }
        });
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        inOrder.verify(observer).onNext(0L);
        inOrder.verify(observer).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(observer, never()).onCompleted();
    }
}