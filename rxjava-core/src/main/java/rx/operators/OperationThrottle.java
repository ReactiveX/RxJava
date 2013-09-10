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
import java.util.concurrent.atomic.AtomicLong;
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
public final class OperationThrottle {

    /**
     * This operation filters out events which are published too quickly in succession. This is done by dropping events which are 
     * followed up by other events before a specified timer has expired. If the timer expires and no follow up event was published (yet)
     * the last received event is published.
     * 
     * @param items      The {@link Observable} which is publishing events.
     * @param timeout    How long each event has to be the 'last event' before it gets published.
     * @param unit       The unit of time for the specified timeout.
     * @return A {@link Func1} which performs the throttle operation.
     */
    public static <T> OnSubscribeFunc<T> throttle(Observable<T> items, long timeout, TimeUnit unit) {
        return throttle(items, timeout, unit, Schedulers.executor(Executors.newSingleThreadScheduledExecutor()));
    }

    /**
     * This operation filters out events which are published too quickly in succession. This is done by dropping events which are 
     * followed up by other events before a specified timer has expired. If the timer expires and no follow up event was published (yet)
     * the last received event is published.
     * 
     * @param items      The {@link Observable} which is publishing events.
     * @param timeout    How long each event has to be the 'last event' before it gets published.
     * @param unit       The unit of time for the specified timeout.
     * @param scheduler  The {@link Scheduler} to use internally to manage the timers which handle timeout for each event.
     * @return A {@link Func1} which performs the throttle operation.
     */
    public static <T> OnSubscribeFunc<T> throttle(final Observable<T> items, final long timeout, final TimeUnit unit, final Scheduler scheduler) {
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

        private final AtomicLong waitUntil = new AtomicLong();
        private final AtomicReference<Subscription> subscription = new AtomicReference<Subscription>(Subscriptions.empty());

        public ThrottledObserver(Observer<? super T> observer, long timeout, TimeUnit unit, Scheduler scheduler) {
            this.observer = observer;
            this.timeout = timeout;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public void onCompleted() {
            throttle(new ThrottledOnComplete<T>(observer));
        }

        @Override
        public void onError(Throwable e) {
            throttle(new ThrottledOnError<T>(observer, e));
        }

        @Override
        public void onNext(T args) {
            throttle(new ThrottledOnNext<T>(observer, args));
        }

        private void throttle(Action0 action) {
            synchronized (subscription) {
                if (!timerHasExpired()) {
                    subscription.get().unsubscribe();
                }
                subscription.set(scheduler.schedule(action, timeout, unit));
            }
        }

        private boolean timerHasExpired() {
            long now = scheduler.now();
            long nextTimeout = now + unit.toMillis(timeout);
            long previousTimeout = waitUntil.getAndSet(nextTimeout);
            return previousTimeout <= now;
        }
    }

    private static final class ThrottledOnNext<T> implements Action0 {

        private final Observer<? super T> observer;
        private final T value;

        public ThrottledOnNext(Observer<? super T> observer, T value) {
            this.observer = observer;
            this.value = value;
        }

        @Override
        public void call() {
            observer.onNext(value);
        }
    }

    private static final class ThrottledOnError<T> implements Action0 {

        private final Observer<? super T> observer;
        private final Throwable error;

        public ThrottledOnError(Observer<? super T> observer, Throwable error) {
            this.observer = observer;
            this.error = error;
        }

        @Override
        public void call() {
            observer.onError(error);
        }
    }

    private static final class ThrottledOnComplete<T> implements Action0 {

        private final Observer<? super T> observer;

        public ThrottledOnComplete(Observer<? super T> observer) {
            this.observer = observer;
        }

        @Override
        public void call() {
            observer.onCompleted();
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
                    publishNext(observer, 900, "four");   // Should be skipped since onCompleted will arrive before the timeout expires.
                    publishCompleted(observer, 1000);     // Should be published as soon as the timeout expires.

                    return Subscriptions.empty();
                }
            });

            Observable<String> sampled = Observable.create(OperationThrottle.throttle(source, 400, TimeUnit.MILLISECONDS, scheduler));
            sampled.subscribe(observer);

            InOrder inOrder = inOrder(observer);

            scheduler.advanceTimeTo(700, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, never()).onNext(anyString());
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));

            scheduler.advanceTimeTo(900, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("two");
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));

            scheduler.advanceTimeTo(1600, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, never()).onNext(anyString());
            verify(observer, times(1)).onCompleted();
            verify(observer, never()).onError(any(Exception.class));
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

            Observable<String> sampled = Observable.create(OperationThrottle.throttle(source, 400, TimeUnit.MILLISECONDS, scheduler));
            sampled.subscribe(observer);

            InOrder inOrder = inOrder(observer);

            scheduler.advanceTimeTo(300, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, never()).onNext(anyString());
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));

            scheduler.advanceTimeTo(600, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("one");
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));

            scheduler.advanceTimeTo(1200, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, never()).onNext(anyString());
            verify(observer, never()).onCompleted();
            verify(observer, times(1)).onError(any(TestException.class));
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
        private class TestException extends Exception { }

    }

}
