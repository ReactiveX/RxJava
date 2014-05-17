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
package rx.operators;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable;
import rx.Observable.Operator;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.SerialSubscription;

class OperatorTimeoutBase<T> implements Operator<T, T> {

    /**
     * Set up the timeout action on the first value.
     * 
     * @param <T>
     */
    /* package-private */static interface FirstTimeoutStub<T> extends
            Func3<TimeoutSubscriber<T>, Long, Scheduler.Worker, Subscription> {
    }

    /**
     * Set up the timeout action based on every value
     * 
     * @param <T>
     */
    /* package-private */static interface TimeoutStub<T> extends
            Func4<TimeoutSubscriber<T>, Long, T, Scheduler.Worker, Subscription> {
    }

    private final FirstTimeoutStub<T> firstTimeoutStub;
    private final TimeoutStub<T> timeoutStub;
    private final Observable<? extends T> other;
    private final Scheduler scheduler;

    /* package-private */OperatorTimeoutBase(FirstTimeoutStub<T> firstTimeoutStub, TimeoutStub<T> timeoutStub, Observable<? extends T> other, Scheduler scheduler) {
        this.firstTimeoutStub = firstTimeoutStub;
        this.timeoutStub = timeoutStub;
        this.other = other;
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> subscriber) {
        Scheduler.Worker inner = scheduler.createWorker();
        subscriber.add(inner);
        final SerialSubscription serial = new SerialSubscription();
        subscriber.add(serial);
        // Use SynchronizedSubscriber for safe memory access
        // as the subscriber will be accessed in the current thread or the
        // scheduler or other Observables.
        final SerializedSubscriber<T> synchronizedSubscriber = new SerializedSubscriber<T>(subscriber);

        TimeoutSubscriber<T> timeoutSubscriber = new TimeoutSubscriber<T>(synchronizedSubscriber, timeoutStub, serial, other, inner);
        serial.set(firstTimeoutStub.call(timeoutSubscriber, 0L, inner));
        return timeoutSubscriber;
    }

    /* package-private */static class TimeoutSubscriber<T> extends
            Subscriber<T> {

        private final AtomicBoolean terminated = new AtomicBoolean(false);
        private final AtomicLong actual = new AtomicLong(0L);
        private final SerialSubscription serial;
        private final Object gate = new Object();

        private final SerializedSubscriber<T> serializedSubscriber;

        private final TimeoutStub<T> timeoutStub;

        private final Observable<? extends T> other;
        private final Scheduler.Worker inner;

        private TimeoutSubscriber(
                SerializedSubscriber<T> serializedSubscriber,
                TimeoutStub<T> timeoutStub, SerialSubscription serial,
                Observable<? extends T> other,
                Scheduler.Worker inner) {
            super(serializedSubscriber);
            this.serializedSubscriber = serializedSubscriber;
            this.timeoutStub = timeoutStub;
            this.serial = serial;
            this.other = other;
            this.inner = inner;
        }

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
                serializedSubscriber.onNext(value);
                serial.set(timeoutStub.call(this, actual.get(), value, inner));
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
                serializedSubscriber.onError(error);
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
                serializedSubscriber.onCompleted();
            }
        }

        public void onTimeout(long seqId) {
            long expected = seqId;
            boolean timeoutWins = false;
            synchronized (gate) {
                if (expected == actual.get() && !terminated.getAndSet(true)) {
                    timeoutWins = true;
                }
            }
            if (timeoutWins) {
                if (other == null) {
                    serializedSubscriber.onError(new TimeoutException());
                } else {
                    other.unsafeSubscribe(serializedSubscriber);
                    serial.set(serializedSubscriber);
                }
            }
        }
    }
}
