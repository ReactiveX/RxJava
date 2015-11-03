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


import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;

import rx.Producer;
import rx.Subscriber;
import rx.exceptions.Exceptions;

final class TakeLastQueueProducer<T> extends AtomicLong implements Producer {

    private final NotificationLite<T> notification;
    private final Deque<Object> deque;
    private final Subscriber<? super T> subscriber;
    private volatile boolean emittingStarted = false;

    public TakeLastQueueProducer(NotificationLite<T> n, Deque<Object> q, Subscriber<? super T> subscriber) {
        this.notification = n;
        this.deque = q;
        this.subscriber = subscriber;
    }

    void startEmitting() {
        if (!emittingStarted) {
            emittingStarted = true;
            emit(0); // start emitting
        }
    }

    @Override
    public void request(long n) {
        if (get() == Long.MAX_VALUE) {
            return;
        }
        long _c;
        if (n == Long.MAX_VALUE) {
            _c = getAndSet(Long.MAX_VALUE);
        } else {
            _c = BackpressureUtils.getAndAddRequest(this, n);
        }
        if (!emittingStarted) {
            // we haven't started yet, so record what was requested and return
            return;
        }
        emit(_c);
    }

    void emit(long previousRequested) {
        if (get() == Long.MAX_VALUE) {
            // fast-path without backpressure
            if (previousRequested == 0) {
                try {
                    for (Object value : deque) {
                        if (subscriber.isUnsubscribed())
                            return;
                        notification.accept(subscriber, value);
                    }
                } catch (Throwable e) {
                    Exceptions.throwOrReport(e, subscriber);
                } finally {
                    deque.clear();
                }
            } else {
                // backpressure path will handle Long.MAX_VALUE and emit the rest events.
            }
        } else {
            // backpressure is requested
            if (previousRequested == 0) {
                while (true) {
                        /*
                         * This complicated logic is done to avoid touching the volatile `requested` value
                         * during the loop itself. If it is touched during the loop the performance is impacted significantly.
                         */
                    long numToEmit = get();
                    int emitted = 0;
                    Object o;
                    while (--numToEmit >= 0 && (o = deque.poll()) != null) {
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }
                        if (notification.accept(subscriber, o)) {
                            // terminal event
                            return;
                        } else {
                            emitted++;
                        }
                    }
                    for (; ; ) {
                        long oldRequested = get();
                        long newRequested = oldRequested - emitted;
                        if (oldRequested == Long.MAX_VALUE) {
                            // became unbounded during the loop
                            // continue the outer loop to emit the rest events.
                            break;
                        }
                        if (compareAndSet(oldRequested, newRequested)) {
                            if (newRequested == 0) {
                                // we're done emitting the number requested so return
                                return;
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}
