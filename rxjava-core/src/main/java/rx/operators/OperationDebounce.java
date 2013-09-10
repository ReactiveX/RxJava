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
public final class OperationDebounce {

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
    public static <T> OnSubscribeFunc<T> debounce(Observable<T> items, long timeout, TimeUnit unit) {
        return debounce(items, timeout, unit, Schedulers.threadPoolForComputation());
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
    public static <T> OnSubscribeFunc<T> debounce(final Observable<T> items, final long timeout, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new Debounce<T>(items, timeout, unit, scheduler).onSubscribe(observer);
            }
        };
    }

    private static class Debounce<T> implements OnSubscribeFunc<T> {

        private final Observable<T> items;
        private final long timeout;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        public Debounce(Observable<T> items, long timeout, TimeUnit unit, Scheduler scheduler) {
            this.items = items;
            this.timeout = timeout;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            return items.subscribe(new DebounceObserver<T>(observer, timeout, unit, scheduler));
        }
    }

    private static class DebounceObserver<T> implements Observer<T> {

        private final Observer<? super T> observer;
        private final long timeout;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        private final AtomicReference<Subscription> lastScheduledNotification = new AtomicReference<Subscription>();

        public DebounceObserver(Observer<? super T> observer, long timeout, TimeUnit unit, Scheduler scheduler) {
            // we need to synchronize the observer since the on* events can be coming from different
            // threads and are thus non-deterministic and could be interleaved
            this.observer = new SynchronizedObserver<T>(observer);
            this.timeout = timeout;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public void onCompleted() {
            /*
             * Cancel previous subscription if it has not already executed.
             * Expected that some race-condition will occur as this is crossing over thread boundaries
             * We are using SynchronizedObserver around 'observer' to handle interleaving and out-of-order calls.
             */
            lastScheduledNotification.get().unsubscribe();
            observer.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            /*
             * Cancel previous subscription if it has not already executed.
             * Expected that some race-condition will occur as this is crossing over thread boundaries
             * We are using SynchronizedObserver around 'observer' to handle interleaving and out-of-order calls.
             */
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
        public void testDebounceWithCompleted() {
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

            Observable<String> sampled = Observable.create(OperationDebounce.debounce(source, 400, TimeUnit.MILLISECONDS, scheduler));
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
        public void testDebounceNeverEmits() {
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>() {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer) {
                    // all should be skipped since they are happening faster than the 200ms timeout
                    publishNext(observer, 100, "a");    // Should be skipped
                    publishNext(observer, 200, "b");    // Should be skipped
                    publishNext(observer, 300, "c");    // Should be skipped
                    publishNext(observer, 400, "d");    // Should be skipped
                    publishNext(observer, 500, "e");    // Should be skipped
                    publishNext(observer, 600, "f");    // Should be skipped
                    publishNext(observer, 700, "g");    // Should be skipped
                    publishNext(observer, 800, "h");    // Should be skipped
                    publishCompleted(observer, 900);     // Should be published as soon as the timeout expires.

                    return Subscriptions.empty();
                }
            });

            Observable<String> sampled = Observable.create(OperationDebounce.debounce(source, 200, TimeUnit.MILLISECONDS, scheduler));
            sampled.subscribe(observer);

            scheduler.advanceTimeTo(0, TimeUnit.MILLISECONDS);
            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(0)).onNext(anyString());
            scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testDebounceWithError() {
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

            Observable<String> sampled = Observable.create(OperationDebounce.debounce(source, 400, TimeUnit.MILLISECONDS, scheduler));
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
