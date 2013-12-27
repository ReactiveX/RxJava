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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import rx.IObservable;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;
import rx.util.functions.Action0;
import rx.util.functions.Func0;

/**
 * Applies a timeout policy for each element in the observable sequence, using
 * the specified scheduler to run timeout timers. If the next element isn't
 * received within the specified timeout duration starting from its predecessor,
 * the other observable sequence is used to produce future messages from that
 * point on.
 */
public final class OperationTimeout {

    public static <T> OnSubscribeFunc<T> timeout(IObservable<? extends T> source, long timeout, TimeUnit timeUnit) {
        return new Timeout<T>(source, timeout, timeUnit, null, Schedulers.threadPoolForComputation());
    }

    public static <T> OnSubscribeFunc<T> timeout(IObservable<? extends T> sequence, long timeout, TimeUnit timeUnit, IObservable<? extends T> other) {
        return new Timeout<T>(sequence, timeout, timeUnit, other, Schedulers.threadPoolForComputation());
    }

    public static <T> OnSubscribeFunc<T> timeout(IObservable<? extends T> source, long timeout, TimeUnit timeUnit, Scheduler scheduler) {
        return new Timeout<T>(source, timeout, timeUnit, null, scheduler);
    }

    public static <T> OnSubscribeFunc<T> timeout(IObservable<? extends T> sequence, long timeout, TimeUnit timeUnit, IObservable<? extends T> other, Scheduler scheduler) {
        return new Timeout<T>(sequence, timeout, timeUnit, other, scheduler);
    }

    private static class Timeout<T> implements Observable.OnSubscribeFunc<T> {
        private final IObservable<? extends T> source;
        private final long timeout;
        private final TimeUnit timeUnit;
        private final Scheduler scheduler;
        private final IObservable<? extends T> other;

        private Timeout(IObservable<? extends T> source, long timeout, TimeUnit timeUnit, IObservable<? extends T> other, Scheduler scheduler) {
            this.source = source;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
            this.other = other;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            final AtomicBoolean terminated = new AtomicBoolean(false);
            final AtomicLong actual = new AtomicLong(0L);  // Required to handle race between onNext and timeout
            final SerialSubscription serial = new SerialSubscription();
            final Object gate = new Object();
            CompositeSubscription composite = new CompositeSubscription();
            final Func0<Subscription> schedule = new Func0<Subscription>() {
                @Override
                public Subscription call() {
                    final long expected = actual.get();
                    return scheduler.schedule(new Action0() {
                        @Override
                        public void call() {
                            boolean timeoutWins = false;
                            synchronized (gate) {
                                if (expected == actual.get() && !terminated.getAndSet(true)) {
                                    timeoutWins = true;
                                }
                            }
                            if (timeoutWins) {
                                if (other == null) {
                                    observer.onError(new TimeoutException());
                                }
                                else {
                                    serial.setSubscription(other.subscribe(observer));
                                }
                            }

                        }
                    }, timeout, timeUnit);
                }
            };
            SafeObservableSubscription subscription = new SafeObservableSubscription();
            composite.add(subscription.wrap(source.subscribe(new Observer<T>() {
                @Override
                public void onNext(T value) {
                    boolean onNextWins = false;
                    synchronized (gate) {
                        if (!terminated.get()) {
                            actual.incrementAndGet();
                            onNextWins = true;
                        }
                    }
                    if (onNextWins) {
                        serial.setSubscription(schedule.call());
                        observer.onNext(value);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    boolean onErrorWins = false;
                    synchronized (gate) {
                        if (!terminated.getAndSet(true)) {
                            onErrorWins = true;
                        }
                    }
                    if (onErrorWins) {
                        serial.unsubscribe();
                        observer.onError(error);
                    }
                }

                @Override
                public void onCompleted() {
                    boolean onCompletedWins = false;
                    synchronized (gate) {
                        if (!terminated.getAndSet(true)) {
                            onCompletedWins = true;
                        }
                    }
                    if (onCompletedWins) {
                        serial.unsubscribe();
                        observer.onCompleted();
                    }
                }
            })));
            composite.add(serial);
            serial.setSubscription(schedule.call());
            return composite;
        }
    }
}
