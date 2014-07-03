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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;

/**
 * Returns an Observable that emits the last <code>count</code> items emitted by the source
 * Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/last.png">
 */
public final class OperatorTakeLast<T> implements Operator<T, T> {

    private final int count;

    public OperatorTakeLast(int count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("count could not be negative");
        }
        this.count = count;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        final Deque<Object> deque = new ArrayDeque<Object>();
        final NotificationLite<T> notification = NotificationLite.instance();
        final QueueProducer<T> producer = new QueueProducer<T>(notification, deque, subscriber);
        subscriber.setProducer(producer);

        // no backpressure up as it wants to receive and discard all but the last
        return new Subscriber<T>(subscriber, -1) {

            @Override
            public void onCompleted() {
                deque.offer(notification.completed());
                producer.startEmitting();
            }

            @Override
            public void onError(Throwable e) {
                deque.clear();
                subscriber.onError(e);
            }

            @Override
            public void onNext(T value) {
                if (count == 0) {
                    // If count == 0, we do not need to put value into deque and
                    // remove it at once. We can ignore the value directly.
                    return;
                }
                if (deque.size() == count) {
                    deque.removeFirst();
                }
                deque.offerLast(notification.next(value));
            }
        };
    }

    private static final class QueueProducer<T> implements Producer {

        private final NotificationLite<T> notification;
        private final Deque<Object> deque;
        private final Subscriber<? super T> subscriber;
        private volatile boolean emittingStarted = false;

        public QueueProducer(NotificationLite<T> n, Deque<Object> q, Subscriber<? super T> subscriber) {
            this.notification = n;
            this.deque = q;
            this.subscriber = subscriber;
        }

        private volatile int requested = 0;
        @SuppressWarnings("rawtypes")
        private static final AtomicIntegerFieldUpdater<QueueProducer> REQUESTED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(QueueProducer.class, "requested");

        void startEmitting() {
            if (!emittingStarted) {
                emittingStarted = true;
                emit(0); // start emitting
            }
        }

        @Override
        public void request(int n) {
            int _c = 0;
            if (n < 0) {
                requested = -1;
            } else {
                _c = REQUESTED_UPDATER.getAndAdd(this, n);
            }
            if (!emittingStarted) {
                // we haven't started yet, so record what was requested and return
                return;
            }
            emit(_c);
        }

        void emit(int previousRequested) {
            if (requested < 0) {
                // fast-path without backpressure
                try {
                    for (Object value : deque) {
                        notification.accept(subscriber, value);
                    }
                } catch (Throwable e) {
                    subscriber.onError(e);
                } finally {
                    deque.clear();
                }
            } else {
                // backpressure is requested
                if (previousRequested == 0) {
                    while (true) {
                        /*
                         * This complicated logic is done to avoid touching the volatile `requested` value
                         * during the loop itself. If it is touched during the loop the performance is impacted significantly.
                         */
                        int numToEmit = requested;
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

                        if (REQUESTED_UPDATER.addAndGet(this, -emitted) == 0) {
                            // we're done emitting the number requested so return
                            return;
                        }

                    }
                }
            }
        }
    }
}
