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

import java.util.concurrent.TimeUnit;

import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
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
    public static OnSubscribeFunc<Long> interval(long interval, TimeUnit unit) {
        return interval(interval, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * Creates an event each time interval.
     */
    public static OnSubscribeFunc<Long> interval(final long interval, final TimeUnit unit, final Scheduler scheduler) {
        // wrapped in order to work with multiple subscribers
        return new OnSubscribeFunc<Long>() {
            @Override
            public Subscription onSubscribe(Observer<? super Long> observer) {
                return new Interval(interval, unit, scheduler).onSubscribe(observer);
            }
        };
    }

    private static class Interval implements OnSubscribeFunc<Long> {
        private final long period;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        private long currentValue;
        private boolean errorOccurred;

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
                    if (!errorOccurred) {
                        try {
                            observer.onNext(currentValue);
                            currentValue++;
                        } catch (Throwable t) {
                            errorOccurred = true;
                            observer.onError(t);
                        }
                    }
                }
            }, period, period, unit);

            return Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    wrapped.unsubscribe();
                    if (!errorOccurred) {
                        observer.onCompleted();
                    }
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
            observer2 = mock(Observer.class);
        }
        
        @Test
        public void testInterval() {
            Observable<Long> w = Observable.create(interval(1, TimeUnit.SECONDS, scheduler));
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
        public void testIntervalWithError() {
            Observable<Long> w = Observable.create(interval(1, TimeUnit.SECONDS, scheduler)).map(new Func1<Long, Long>() {
                @Override
                public Long call(Long value) {
                    if (value == 2L) {
                        throw new RuntimeException("error!");
                    }
                    return value;
                }
            });
            w.subscribe(observer);
            InOrder inOrder = inOrder(observer);

            scheduler.advanceTimeTo(2, TimeUnit.SECONDS);
            inOrder.verify(observer, times(1)).onNext(0L);
            inOrder.verify(observer, times(1)).onNext(1L);
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));

            scheduler.advanceTimeTo(3, TimeUnit.SECONDS);
            inOrder.verify(observer, never()).onNext(anyLong());
            inOrder.verify(observer, times(1)).onError(any(RuntimeException.class));
            verify(observer, never()).onCompleted();
        }

        @Test
        public void testWithMultipleSubscribersStartingAtSameTime() {
            Observable<Long> w = Observable.create(interval(1, TimeUnit.SECONDS, scheduler));
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

        @Test
        public void testWithMultipleStaggeredSubscribers() {
            Observable<Long> w = Observable.create(interval(1, TimeUnit.SECONDS, scheduler));
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
            inOrder1.verify(observer, times(1)).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));

            inOrder2.verify(observer2, never()).onNext(anyLong());
            inOrder2.verify(observer2, times(1)).onCompleted();
            verify(observer2, never()).onError(any(Throwable.class));
        }

        @Test
        public void testWithMultipleStaggeredSubscribersAndPublish() {
            ConnectableObservable<Long> w = Observable.create(interval(1, TimeUnit.SECONDS, scheduler)).publish();
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
    }
}
