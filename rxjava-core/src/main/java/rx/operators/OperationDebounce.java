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
import java.util.concurrent.atomic.AtomicReference;

import rx.IObservable;
import rx.IObservable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.schedulers.Schedulers;
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
     *            The {@link IObservable} which is publishing events.
     * @param timeout
     *            How long each event has to be the 'last event' before it gets published.
     * @param unit
     *            The unit of time for the specified timeout.
     * @return A {@link Func1} which performs the throttle operation.
     */
    public static <T> IObservable<T> debounce(IObservable<T> items, long timeout, TimeUnit unit) {
        return debounce(items, timeout, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * This operation filters out events which are published too quickly in succession. This is done by dropping events which are
     * followed up by other events before a specified timer has expired. If the timer expires and no follow up event was published (yet)
     * the last received event is published.
     * 
     * @param items
     *            The {@link IObservable} which is publishing events.
     * @param timeout
     *            How long each event has to be the 'last event' before it gets published.
     * @param unit
     *            The unit of time for the specified timeout.
     * @param scheduler
     *            The {@link Scheduler} to use internally to manage the timers which handle timeout for each event.
     * @return A {@link Func1} which performs the throttle operation.
     */
    public static <T> IObservable<T> debounce(final IObservable<T> items, final long timeout, final TimeUnit unit, final Scheduler scheduler) {
        return new IObservable<T>() {
            @Override
            public Subscription subscribe(Observer<? super T> observer) {
                return new Debounce<T>(items, timeout, unit, scheduler).subscribe(observer);
            }
        };
    }

    private static class Debounce<T> implements IObservable<T> {

        private final IObservable<T> items;
        private final long timeout;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        public Debounce(IObservable<T> items, long timeout, TimeUnit unit, Scheduler scheduler) {
            this.items = items;
            this.timeout = timeout;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription subscribe(Observer<? super T> observer) {
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
}
