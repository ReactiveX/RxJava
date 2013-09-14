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

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

/**
 * Returns an Observable that emits the results of shifting the items emitted by the source
 * Observable by a specified delay.
 */
public final class OperationDelay {

    /**
     * Delays the observable sequence by the given time interval.
     */
    public static <T> OnSubscribeFunc<T> delay(final Observable<? extends T> source, long delay, TimeUnit unit) {
        return new Delay<T>(source, delay, unit, Schedulers.executor(Executors.newSingleThreadScheduledExecutor()));
    }

    /**
     * Delays the observable sequence by the given time interval.
     */
    public static <T> OnSubscribeFunc<T> delay(final Observable<? extends T> source, long period, TimeUnit unit, Scheduler scheduler) {
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
                @Override
                public void onCompleted() {
                    scheduler.schedule(new Action0() {
                        @Override
                        public void call() {
                            observer.onCompleted();
                        }
                    }, delay, unit);
                }
        
                @Override
                public void onError(Throwable e) {
                    // errors get propagated without delay
                    observer.onError(e);
                }
        
                @Override
                public void onNext(final T value) {
                    scheduler.schedule(new Action0() {
                        @Override
                        public void call() {
                            observer.onNext(value);
                        }
                    }, delay, unit);
                }
            });
        }
    }
    
    public static class UnitTest {
        @Mock
        private Observer<Long> observer;

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
            verify(observer, never()).onNext(any(Long.class));
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
    }
}
