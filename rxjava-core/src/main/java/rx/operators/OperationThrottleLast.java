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
public final class OperationThrottleLast {

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
    public static <T> OnSubscribeFunc<T> throttleLast(Observable<T> items, long timeout, TimeUnit unit) {
        return throttleLast(items, timeout, unit, Schedulers.threadPoolForComputation());
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
    public static <T> OnSubscribeFunc<T> throttleLast(final Observable<T> items, final long timeout, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return items.window(timeout, unit, scheduler).flatMap(new Func1<Observable<T>, Observable<T>>() {

                    @Override
                    public Observable<T> call(Observable<T> o) {
                        return o.takeLast(1);
                    }
                }).subscribe(observer);
            }
        };
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

            Observable<String> sampled = Observable.create(OperationThrottleLast.throttleLast(source, 400, TimeUnit.MILLISECONDS, scheduler));
            sampled.subscribe(observer);

            InOrder inOrder = inOrder(observer);

            scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("two");
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

            Observable<String> sampled = Observable.create(OperationThrottleLast.throttleLast(source, 400, TimeUnit.MILLISECONDS, scheduler));
            sampled.subscribe(observer);

            InOrder inOrder = inOrder(observer);

            scheduler.advanceTimeTo(400, TimeUnit.MILLISECONDS);
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

        private <T> void publishNext(final Observer<T> observer, long delay, final T value) {
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
