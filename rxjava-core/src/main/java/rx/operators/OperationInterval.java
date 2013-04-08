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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.concurrency.TestScheduler;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

/**
 * Returns an observable sequence that produces a value after each period.
 * The value starts at 0 and counts up each period.
 */
public final class OperationInterval {

    /**
     * Creates an event each time interval.
     */
    public static Func1<Observer<Long>, Subscription> interval(long interval, TimeUnit unit) {
        return new Interval(interval, unit, Schedulers.currentThread());
    }

    /**
     * Creates an event each time interval.
     */
    public static Func1<Observer<Long>, Subscription> interval(long interval, TimeUnit unit, Scheduler scheduler) {
        return new Interval(interval, unit, scheduler);
    }

    private static class Interval implements Func1<Observer<Long>, Subscription> {
        private final long interval;
        private final TimeUnit unit;
        private final Scheduler scheduler;
        
        private long currentValue;
        private final AtomicBoolean complete = new AtomicBoolean();

        private Interval(long interval, TimeUnit unit, Scheduler scheduler) {
            this.interval = interval;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription call(final Observer<Long> observer) {
            scheduler.schedule(new IntervalAction(observer), interval, unit);
            return Subscriptions.create(new Action0() {
              @Override
              public void call() {
                complete.set(true);
              }
            });
        }
        
        private class IntervalAction implements Action0 {
            private final Observer<Long> observer;
            
            private IntervalAction(Observer<Long> observer) {
                this.observer = observer;
            }
            
            @Override
            public void call() {
                if (complete.get()) {
                    observer.onCompleted();
                } else {
                    observer.onNext(currentValue);
                    currentValue++;
                    scheduler.schedule(this, interval, unit);
                }
            }
        }
    }
    
    public static class UnitTest {
        private TestScheduler scheduler;
        private Observer<Long> observer;
        
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
            verify(observer, never()).onError(any(Exception.class));
            
            scheduler.advanceTimeTo(2, TimeUnit.SECONDS);

            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext(0L);
            inOrder.verify(observer, times(1)).onNext(1L);
            inOrder.verify(observer, never()).onNext(2L);
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));
            
            sub.unsubscribe();
            scheduler.advanceTimeTo(4, TimeUnit.SECONDS);
            verify(observer, never()).onNext(2L);
            verify(observer, times(1)).onCompleted();
            verify(observer, never()).onError(any(Exception.class));
        }
    }
}
