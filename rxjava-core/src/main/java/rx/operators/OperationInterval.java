/**
 * Copyright 2013 Netflix, Inc.
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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.concurrency.TestScheduler;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;

/**
 * Returns an observable sequence that produces a value after each period.
 * The value starts at 0 and counts up each period.
 */
public final class OperationInterval {

    /**
     * Creates an event each time interval.
     */
    public static OnSubscribeFunc<Long> interval(long interval, TimeUnit unit) {
        return new Interval(interval, unit, Schedulers.executor(Executors.newSingleThreadScheduledExecutor()));
    }

    /**
     * Creates an event each time interval.
     */
    public static OnSubscribeFunc<Long> interval(long interval, TimeUnit unit, Scheduler scheduler) {
        return new Interval(interval, unit, scheduler);
    }

    private static class Interval implements OnSubscribeFunc<Long> {
        private final long period;
        private final TimeUnit unit;
        private final Scheduler scheduler;
        
        private long currentValue;

        private Interval(long period, TimeUnit unit, Scheduler scheduler) {
            this.period = period;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super Long> observer) {
            final Subscription wrapped = scheduler.schedulePeriodically(new Action0() {
                @Override
                public void call() {
                    observer.onNext(currentValue);
                    currentValue++;
                }
            }, period, period, unit);
            
            return Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    wrapped.unsubscribe();
                    observer.onCompleted();
                }
            });
        }
    }
    
    public static class UnitTest {
        private TestScheduler scheduler;
        private Observer<Long> observer;
        private Observer<Long> observer2;
        
        @Before
        @SuppressWarnings("unchecked") // due to mocking
        public void before() {
            scheduler = new TestScheduler();
            observer = mock(Observer.class);
        }
        
        @Test
        public void testInterval() {
            Observable<Long> w = Observable.create(OperationInterval.interval(1, TimeUnit.SECONDS, scheduler));
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
            verify(observer, times(1)).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));
        }
        
        @Test
        public void testWithMultipleSubscribersStartingAtSameTime() {
            Observable<Long> w = Observable.create(OperationInterval.interval(1, TimeUnit.SECONDS, scheduler));
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
            verify(observer, times(1)).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));

            verify(observer2, never()).onNext(2L);
            verify(observer2, times(1)).onCompleted();
            verify(observer2, never()).onError(any(Throwable.class));
        }
    }
}
