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
import java.util.concurrent.atomic.AtomicReference;

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
import rx.util.functions.Func1;

/**
 * This operation is used to filter out bursts of events. This is done by ignoring the events from an observable which are too
 * quickly followed up with other values. Values which are not followed up by other values within the specified timeout are published
 * as soon as the timeout expires.
 */
public final class OperationThrottleWithTimeout {

    /**
     * This operation filters out events which are published too quickly in succession. This is done by dropping events which are
     * followed up by other events before a specified timer has expired. If the timer expires and no follow up event was published (yet)
     * the last received event is published.
     * 
     * @param items
     *            The {@link Observable} which is publishing events.
     * @param timeout
     *            How long each event has to be the 'last event' before it gets published.
     * @param unit
     *            The unit of time for the specified timeout.
     * @return A {@link Func1} which performs the throttle operation.
     */
    public static <T> OnSubscribeFunc<T> throttleWithTimeout(Observable<T> items, long timeout, TimeUnit unit) {
        return throttleWithTimeout(items, timeout, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * This operation filters out events which are published too quickly in succession. This is done by dropping events which are
     * followed up by other events before a specified timer has expired. If the timer expires and no follow up event was published (yet)
     * the last received event is published.
     * 
     * @param items
     *            The {@link Observable} which is publishing events.
     * @param timeout
     *            How long each event has to be the 'last event' before it gets published.
     * @param unit
     *            The unit of time for the specified timeout.
     * @param scheduler
     *            The {@link Scheduler} to use internally to manage the timers which handle timeout for each event.
     * @return A {@link Func1} which performs the throttle operation.
     */
    public static <T> OnSubscribeFunc<T> throttleWithTimeout(final Observable<T> items, final long timeout, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new Throttle<T>(items, timeout, unit, scheduler).onSubscribe(observer);
            }
        };
    }

    private static class Throttle<T> implements OnSubscribeFunc<T> {

        private final Observable<T> items;
        private final long timeout;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        public Throttle(Observable<T> items, long timeout, TimeUnit unit, Scheduler scheduler) {
            this.items = items;
            this.timeout = timeout;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            return items.subscribe(new ThrottledObserver<T>(observer, timeout, unit, scheduler));
        }
    }

    private static class ThrottledObserver<T> implements Observer<T> {

        private final Observer<? super T> observer;
        private final long timeout;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        private final AtomicReference<Subscription> lastScheduledNotification = new AtomicReference<Subscription>();

        public ThrottledObserver(Observer<? super T> observer, long timeout, TimeUnit unit, Scheduler scheduler) {
            this.observer = observer;
            this.timeout = timeout;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public void onCompleted() {
            observer.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            lastScheduledNotification.get().unsubscribe();
            observer.onError(e);
        }

        @Override
        public void onNext(final T v) {
            Subscription previousSubscription = lastScheduledNotification.getAndSet(scheduler.schedule(new Action0() {

                @Override
                public void call() {
                    observer.onNext(v);
                }

            }, timeout, unit));
            // cancel previous if not already executed
            if (previousSubscription != null) {
                previousSubscription.unsubscribe();
            }
        }
    }

    public static class UnitTest {

        private TestScheduler scheduler;
        private Observer<String> observer;

        @Before
        @SuppressWarnings("unchecked")
        public void before() {
            scheduler = new TestScheduler();
            observer = mock(Observer.class);
        }

        @Test
        public void testThrottlingWithCompleted() {
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>() {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer) {
                    publishNext(observer, 100, "one");    // Should be skipped since "two" will arrive before the timeout expires.
                    publishNext(observer, 400, "two");    // Should be published since "three" will arrive after the timeout expires.
                    publishNext(observer, 900, "three");   // Should be skipped since onCompleted will arrive before the timeout expires.
                    publishCompleted(observer, 1000);     // Should be published as soon as the timeout expires.

                    return Subscriptions.empty();
                }
            });

            Observable<String> sampled = Observable.create(OperationThrottleWithTimeout.throttleWithTimeout(source, 400, TimeUnit.MILLISECONDS, scheduler));
            sampled.subscribe(observer);

            scheduler.advanceTimeTo(0, TimeUnit.MILLISECONDS);
            InOrder inOrder = inOrder(observer);
            // must go to 800 since it must be 400 after when two is sent, which is at 400
            scheduler.advanceTimeTo(800, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("two");
            scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testThrottlingWithError() {
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>() {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer) {
                    Exception error = new TestException();
                    publishNext(observer, 100, "one");    // Should be published since "two" will arrive after the timeout expires.
                    publishNext(observer, 600, "two");    // Should be skipped since onError will arrive before the timeout expires.
                    publishError(observer, 700, error);   // Should be published as soon as the timeout expires.

                    return Subscriptions.empty();
                }
            });

            Observable<String> sampled = Observable.create(OperationThrottleWithTimeout.throttleWithTimeout(source, 400, TimeUnit.MILLISECONDS, scheduler));
            sampled.subscribe(observer);

            scheduler.advanceTimeTo(0, TimeUnit.MILLISECONDS);
            InOrder inOrder = inOrder(observer);
            // 100 + 400 means it triggers at 500
            scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
            inOrder.verify(observer).onNext("one");
            scheduler.advanceTimeTo(701, TimeUnit.MILLISECONDS);
            inOrder.verify(observer).onError(any(TestException.class));
            inOrder.verifyNoMoreInteractions();
        }

        private <T> void publishCompleted(final Observer<T> observer, long delay) {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    observer.onCompleted();
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        private <T> void publishError(final Observer<T> observer, long delay, final Exception error) {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    observer.onError(error);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        private <T> void publishNext(final Observer<T> observer, final long delay, final T value) {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    observer.onNext(value);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        @SuppressWarnings("serial")
        private class TestException extends Exception {
        }

    }

}
