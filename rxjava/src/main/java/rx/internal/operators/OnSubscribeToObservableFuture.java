/**
 * Copyright 2014 Netflix, Inc.
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
package rx.internal.operators;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable.OnSubscribe;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Converts a {@code Future} into an {@code Observable}.
 * <p>
 * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/from.Future.png">
 * <p>
 * You can convert any object that supports the {@code Future} interface into an {@code Observable} that emits
 * the return value of the {@code get} method of that object, by using this operator.
 * <p>
 * This is non blocking implementation. If a future value is not available, a task will be scheduled
 * with initial delay of {@code INITIAL_DELAY_MICRO_SEC} or a user provided timeout if the latter is smaller.
 * The delay will be doubled each time the task is run and the future value is still not available.
 * The future will be cancelled on unsubscribe or when there is a timeout.
 */
public class OnSubscribeToObservableFuture {

    private static final int INITIAL_DELAY_MICRO_SEC = 100;

    private static final int MAX_DELAY_MICRO_SEC = 100000;

    static class ToObservableFuture<T> implements OnSubscribe<T> {
        private final Future<? extends T> that;
        private final Scheduler scheduler;
        private final long time;
        private final TimeUnit unit;

        ToObservableFuture(Future<? extends T> that, Scheduler scheduler) {
            this.that = that;
            this.scheduler = scheduler;
            time = 0;
            unit = null;
        }

        ToObservableFuture(Future<? extends T> that, long time, TimeUnit unit, Scheduler scheduler) {
            this.that = that;
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public void call(Subscriber<? super T> subscriber) {
            if (that.isDone()) {
                handleDone(subscriber);
                return;
            }
            final FuturePollingTask pollingTask = new FuturePollingTask(subscriber);
            subscriber.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    // If the Future is already completed, "cancel" does nothing.
                    that.cancel(true);
                    pollingTask.cancel();
                }
            }));
            pollingTask.call();
        }

        private void handleDone(Subscriber<? super T> subscriber) {
            try {
                subscriber.onNext(that.get()); // We know its done, so it should never block.
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }

        private void handleTimeout(Subscriber<? super T> subscriber) {
            subscriber.onError(new TimeoutException("future timeout"));
        }

        private class FuturePollingTask implements Action0 {

            private final Subscriber<? super T> subscriber;

            private final Worker worker = scheduler.createWorker();

            private final long startTime = scheduler.now();

            private final long expireAfter = (time == 0) ? -1 : startTime + unit.toMillis(time);

            private final MultipleAssignmentSubscription taskSubscription = new MultipleAssignmentSubscription();

            FuturePollingTask(Subscriber<? super T> subscriber) {
                this.subscriber = subscriber;
                subscriber.add(taskSubscription);
            }

            @Override
            public void call() {
                if (that.isDone()) {
                    handleDone(subscriber);
                    return;
                }
                long currentTime = scheduler.now();
                if (expireAfter > 0 && currentTime > expireAfter) {
                    handleTimeout(subscriber);
                    return;
                }
                // This seemingly complex computation approximates the process of doubling the waiting time, and
                // capping it at MAX_DELAY_MICRO_SEC level. Its accuracy may vary depending on a scheduler and
                // waiting time in a task queue. The major benefit - FuturePollingTask is immutable.
                long delayBy = Math.min(Math.max(INITIAL_DELAY_MICRO_SEC, (currentTime - startTime) * 1000), MAX_DELAY_MICRO_SEC);

                // It is possible that subscriber unsubscribed after {code}that.isDone(){code} above was called,
                // and cancelled future and unsubscribed the worker. In such case this call will return empty
                // subscription, and a task will not be scheduled.
                Subscription newSubscription = worker.schedule(this, delayBy, TimeUnit.MICROSECONDS);
                taskSubscription.set(newSubscription);
            }

            public void cancel() {
                worker.unsubscribe();
            }
        }
    }

    public static <T> OnSubscribe<T> toObservableFuture(final Future<? extends T> that) {
        return new ToObservableFuture<T>(that, Schedulers.computation());
    }

    public static <T> OnSubscribe<T> toObservableFuture(Future<? extends T> that, Scheduler scheduler) {
        return new ToObservableFuture<T>(that, scheduler);
    }

    public static <T> OnSubscribe<T> toObservableFuture(final Future<? extends T> that, long time, TimeUnit unit) {
        return new ToObservableFuture<T>(that, time, unit, Schedulers.computation());
    }

    public static <T> OnSubscribe<T> toObservableFuture(final Future<? extends T> that, long time, TimeUnit unit, Scheduler scheduler) {
        return new ToObservableFuture<T>(that, time, unit, scheduler);
    }
}
