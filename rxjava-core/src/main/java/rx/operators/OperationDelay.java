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
import static org.mockito.MockitoAnnotations.initMocks;
import static rx.Observable.interval;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.concurrency.TestScheduler;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

/**
 * Returns an Observable that emits the results of shifting the items emitted by the source
 * Observable by a specified delay.
 */
public final class OperationDelay {

    /**
     * Delays the observable sequence by the given time interval.
     */
    public static <T> OnSubscribeFunc<T> delay(final Observable<? extends T> source, long delay, TimeUnit unit) {
        return delay(source, delay, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * Delays the observable sequence by a time interval so that it starts at the given due time.
     */
    public static <T> OnSubscribeFunc<T> delay(final Observable<? extends T> source, Date dueTime) {
        return delay(source, dueTime, Schedulers.threadPoolForComputation());
    }
    
    /**
     * Delays the observable sequence by a time interval so that it starts at the given due time.
     */
    public static <T> OnSubscribeFunc<T> delay(final Observable<? extends T> source, Date dueTime, final Scheduler scheduler) {
        long scheduledTime = dueTime.getTime();
        long delay = scheduledTime - scheduler.now();
        if (delay < 0L) {
            delay = 0L;
        }
        return new Delay<T>(source, delay, TimeUnit.MILLISECONDS, scheduler);
    }

    /**
     * Delays the observable sequence by the given time interval.
     */
    public static <T> OnSubscribeFunc<T> delay(final Observable<? extends T> source, final long period, final TimeUnit unit, final Scheduler scheduler) {
        return new Delay<T>(source, period, unit, scheduler);
    }

    private static class Delay<T> implements OnSubscribeFunc<T> {
        private final Observable<? extends T> source;
        private final long delay;
        private final TimeUnit unit;
        private final Scheduler scheduler;
        
        private Delay(Observable<? extends T> source, long delay, TimeUnit unit, Scheduler scheduler) {
            this.source = source;
            this.delay = delay;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            return source.subscribe(new Observer<T>() {
                private AtomicBoolean errorOccurred = new AtomicBoolean();
                
                @Override
                public void onCompleted() {
                    if (!errorOccurred.get()) {
                        scheduler.schedule(new Action0() {
                            @Override
                            public void call() {
                                observer.onCompleted();
                            }
                        }, delay, unit);
                    }
                }
        
                @Override
                public void onError(Throwable e) {
                    // errors get propagated without delay
                    errorOccurred.set(true);
                    observer.onError(e);
                }
        
                @Override
                public void onNext(final T value) {
                    if (!errorOccurred.get()) {
                        scheduler.schedule(new Action0() {
                            @Override
                            public void call() {
                                try {
                                    observer.onNext(value);
                                } catch (Throwable t) {
                                    errorOccurred.set(true);
                                    observer.onError(t);
                                }
                            }
                        }, delay, unit);
                    }
                }
            });
        }
    }
    
    public static class UnitTest {
        @Mock
        private Observer<Long> observer;
        @Mock
        private Observer<Long> observer2;

        private TestScheduler scheduler;

        @Before
        public void before() {
            initMocks(this);
            scheduler = new TestScheduler();
        }
        
        @Test
        public void testDelay() {
            Observable<Long> source = interval(1L, TimeUnit.SECONDS, scheduler).take(3);
            Observable<Long> delayed = Observable.create(OperationDelay.delay(source, 500L, TimeUnit.MILLISECONDS, scheduler));
            delayed.subscribe(observer);
            
            InOrder inOrder = inOrder(observer);
            scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
            verify(observer, never()).onNext(anyLong());
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));
            
            scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext(0L);
            inOrder.verify(observer, never()).onNext(anyLong());
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));

            scheduler.advanceTimeTo(2400L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, never()).onNext(anyLong());
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));
            
            scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext(1L);
            inOrder.verify(observer, never()).onNext(anyLong());
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));
            
            scheduler.advanceTimeTo(3400L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, never()).onNext(anyLong());
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));

            scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext(2L);
            verify(observer, times(1)).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));
        }
        
        @Test
        public void testDelayWithDueTime() {
            Observable<Long> source = interval(1L, TimeUnit.SECONDS, scheduler).first();
            Observable<Long> delayed = Observable.create(OperationDelay.delay(source, new Date(1500L), scheduler));
            delayed.subscribe(observer);
            
            InOrder inOrder = inOrder(observer);
            
            scheduler.advanceTimeTo(2499L, TimeUnit.MILLISECONDS);
            verify(observer, never()).onNext(anyLong());
            verify(observer, never()).onCompleted();
            
            scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext(0L);
            inOrder.verify(observer, times(1)).onCompleted();
            
            verify(observer, never()).onError(any(Throwable.class));
        }
        
        @Test
        public void testLongDelay() {
            Observable<Long> source = interval(1L, TimeUnit.SECONDS, scheduler).take(3);
            Observable<Long> delayed = Observable.create(OperationDelay.delay(source, 5L, TimeUnit.SECONDS, scheduler));
            delayed.subscribe(observer);
            
            InOrder inOrder = inOrder(observer);
            
            scheduler.advanceTimeTo(5999L, TimeUnit.MILLISECONDS);
            verify(observer, never()).onNext(anyLong());
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));
            
            scheduler.advanceTimeTo(6000L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext(0L);
            scheduler.advanceTimeTo(6999L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, never()).onNext(anyLong());
            scheduler.advanceTimeTo(7000L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext(1L);
            scheduler.advanceTimeTo(7999L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, never()).onNext(anyLong());
            scheduler.advanceTimeTo(8000L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext(2L);
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verify(observer, never()).onNext(anyLong());
            inOrder.verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));
        }
        
        @Test
        public void testDelayWithError() {
            Observable<Long> source = interval(1L, TimeUnit.SECONDS, scheduler).map(new Func1<Long, Long>() {
                @Override
                public Long call(Long value) {
                    if (value == 1L) {
                        throw new RuntimeException("error!");
                    }
                    return value; 
                }
            });
            Observable<Long> delayed = Observable.create(OperationDelay.delay(source, 1L, TimeUnit.SECONDS, scheduler));
            delayed.subscribe(observer);

            InOrder inOrder = inOrder(observer);
            
            scheduler.advanceTimeTo(1999L, TimeUnit.MILLISECONDS);
            verify(observer, never()).onNext(anyLong());
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Throwable.class));
            
            scheduler.advanceTimeTo(2000L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onError(any(Throwable.class));
            inOrder.verify(observer, never()).onNext(anyLong());
            verify(observer, never()).onCompleted();
            
            scheduler.advanceTimeTo(5000L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, never()).onNext(anyLong());
            inOrder.verify(observer, never()).onError(any(Throwable.class));
            verify(observer, never()).onCompleted();
        }
        
        @Test
        public void testDelayWithMultipleSubscriptions() {
            Observable<Long> source = interval(1L, TimeUnit.SECONDS, scheduler).take(3);
            Observable<Long> delayed = Observable.create(OperationDelay.delay(source, 500L, TimeUnit.MILLISECONDS, scheduler));
            delayed.subscribe(observer);
            delayed.subscribe(observer2);
            
            InOrder inOrder = inOrder(observer);
            InOrder inOrder2 = inOrder(observer2);
            
            scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
            verify(observer, never()).onNext(anyLong());
            verify(observer2, never()).onNext(anyLong());

            scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext(0L);
            inOrder2.verify(observer2, times(1)).onNext(0L);

            scheduler.advanceTimeTo(2499L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, never()).onNext(anyLong());
            inOrder2.verify(observer2, never()).onNext(anyLong());
            
            scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext(1L);
            inOrder2.verify(observer2, times(1)).onNext(1L);

            verify(observer, never()).onCompleted();
            verify(observer2, never()).onCompleted();

            scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext(2L);
            inOrder2.verify(observer2, times(1)).onNext(2L);
            inOrder.verify(observer, never()).onNext(anyLong());
            inOrder2.verify(observer2, never()).onNext(anyLong());
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder2.verify(observer2, times(1)).onCompleted();

            verify(observer, never()).onError(any(Throwable.class));
            verify(observer2, never()).onError(any(Throwable.class));
        }
    }
}
