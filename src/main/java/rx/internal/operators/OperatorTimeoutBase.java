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

import java.util.concurrent.TimeoutException;

import rx.*;
import rx.Observable.Operator;
import rx.functions.*;
import rx.internal.producers.ProducerArbiter;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.SerialSubscription;

class OperatorTimeoutBase<T> implements Operator<T, T> {

    /**
     * Set up the timeout action on the first value.
     * 
     * @param <T>
     */
    /* package-private */interface FirstTimeoutStub<T> extends
            Func3<TimeoutSubscriber<T>, Long, Scheduler.Worker, Subscription> {
    }

    /**
     * Set up the timeout action based on every value
     * 
     * @param <T>
     */
    /* package-private */interface TimeoutStub<T> extends
            Func4<TimeoutSubscriber<T>, Long, T, Scheduler.Worker, Subscription> {
    }

    final FirstTimeoutStub<T> firstTimeoutStub;
    final TimeoutStub<T> timeoutStub;
    final Observable<? extends T> other;
    final Scheduler scheduler;

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
        // Use SynchronizedSubscriber for safe memory access
        // as the subscriber will be accessed in the current thread or the
        // scheduler or other Observables.
        final SerializedSubscriber<T> synchronizedSubscriber = new SerializedSubscriber<T>(subscriber);

        final SerialSubscription serial = new SerialSubscription();
        synchronizedSubscriber.add(serial);

        TimeoutSubscriber<T> timeoutSubscriber = new TimeoutSubscriber<T>(synchronizedSubscriber, timeoutStub, serial, other, inner);
        
        synchronizedSubscriber.add(timeoutSubscriber);
        synchronizedSubscriber.setProducer(timeoutSubscriber.arbiter);
        
        serial.set(firstTimeoutStub.call(timeoutSubscriber, 0L, inner));
        
        return timeoutSubscriber;
    }

    /* package-private */static final class TimeoutSubscriber<T> extends
            Subscriber<T> {

        final SerialSubscription serial;

        final SerializedSubscriber<T> serializedSubscriber;

        final TimeoutStub<T> timeoutStub;

        final Observable<? extends T> other;
        
        final Scheduler.Worker inner;
        
        final ProducerArbiter arbiter;
        
        /** Guarded by this. */
        boolean terminated;
        /** Guarded by this. */
        long actual;
        
        TimeoutSubscriber(
                SerializedSubscriber<T> serializedSubscriber,
                TimeoutStub<T> timeoutStub, SerialSubscription serial,
                Observable<? extends T> other,
                Scheduler.Worker inner) {
            this.serializedSubscriber = serializedSubscriber;
            this.timeoutStub = timeoutStub;
            this.serial = serial;
            this.other = other;
            this.inner = inner;
            this.arbiter = new ProducerArbiter();
        }

        @Override
        public void setProducer(Producer p) {
            arbiter.setProducer(p);
        }
        
        @Override
        public void onNext(T value) {
            boolean onNextWins = false;
            long a;
            synchronized (this) {
                if (!terminated) {
                    a = ++actual;
                    onNextWins = true;
                } else {
                    a = actual;
                }
            }
            if (onNextWins) {
                serializedSubscriber.onNext(value);
                serial.set(timeoutStub.call(this, a, value, inner));
            }
        }

        @Override
        public void onError(Throwable error) {
            boolean onErrorWins = false;
            synchronized (this) {
                if (!terminated) {
                    terminated = true;
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
            synchronized (this) {
                if (!terminated) {
                    terminated = true;
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
            synchronized (this) {
                if (expected == actual && !terminated) {
                    terminated = true;
                    timeoutWins = true;
                }
            }
            if (timeoutWins) {
                if (other == null) {
                    serializedSubscriber.onError(new TimeoutException());
                } else {
                    Subscriber<T> second = new Subscriber<T>() {
                        @Override
                        public void onNext(T t) {
                            serializedSubscriber.onNext(t);
                        }
                        
                        @Override
                        public void onError(Throwable e) {
                            serializedSubscriber.onError(e);
                        }
                        
                        @Override
                        public void onCompleted() {
                            serializedSubscriber.onCompleted();
                        }
                        
                        @Override
                        public void setProducer(Producer p) {
                            arbiter.setProducer(p);
                        }
                    };
                    other.unsafeSubscribe(second);
                    serial.set(second);
                }
            }
        }
    }
}