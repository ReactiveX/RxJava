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

import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import rx.Observable.Operator;
import rx.Producer;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;
import rx.internal.util.RxRingBuffer;
import rx.internal.util.SynchronizedQueue;
import rx.internal.util.unsafe.SpscArrayQueue;
import rx.internal.util.unsafe.UnsafeAccess;
import rx.schedulers.ImmediateScheduler;
import rx.schedulers.TrampolineScheduler;

/**
 * Delivers events on the specified {@code Scheduler} asynchronously via an unbounded buffer.
 * 
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/observeOn.png" alt="">
 * 
 * @param <T>
 *            the transmitted value type
 */
public final class OperatorObserveOn<T> implements Operator<T, T> {

    private final Scheduler scheduler;

    /**
     * @param scheduler
     */
    public OperatorObserveOn(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        if (scheduler instanceof ImmediateScheduler) {
            // avoid overhead, execute directly
            return child;
        } else if (scheduler instanceof TrampolineScheduler) {
            // avoid overhead, execute directly
            return child;
        } else {
            ObserveOnSubscriber<T> parent = new ObserveOnSubscriber<T>(scheduler, child);
            parent.init();
            return parent;
        }
    }

    /** Observe through individual queue per observer. */
    private static final class ObserveOnSubscriber<T> extends Subscriber<T> {
        final Subscriber<? super T> child;
        final Scheduler.Worker recursiveScheduler;
        final ScheduledUnsubscribe scheduledUnsubscribe;
        final NotificationLite<T> on = NotificationLite.instance();

        final Queue<Object> queue;
        volatile boolean completed = false;
        volatile boolean failure = false;

        volatile long requested = 0;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<ObserveOnSubscriber> REQUESTED = AtomicLongFieldUpdater.newUpdater(ObserveOnSubscriber.class, "requested");

        @SuppressWarnings("unused")
        volatile long counter;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<ObserveOnSubscriber> COUNTER_UPDATER = AtomicLongFieldUpdater.newUpdater(ObserveOnSubscriber.class, "counter");

        volatile Throwable error;

        // do NOT pass the Subscriber through to couple the subscription chain ... unsubscribing on the parent should
        // not prevent anything downstream from consuming, which will happen if the Subscription is chained
        public ObserveOnSubscriber(Scheduler scheduler, Subscriber<? super T> child) {
            this.child = child;
            this.recursiveScheduler = scheduler.createWorker();
            if (UnsafeAccess.isUnsafeAvailable()) {
                queue = new SpscArrayQueue<Object>(RxRingBuffer.SIZE);
            } else {
                queue = new SynchronizedQueue<Object>(RxRingBuffer.SIZE);
            }
            this.scheduledUnsubscribe = new ScheduledUnsubscribe(recursiveScheduler);
        }
        
        void init() {
            // don't want this code in the constructor because `this` can escape through the 
            // setProducer call
            child.add(scheduledUnsubscribe);
            child.setProducer(new Producer() {

                @Override
                public void request(long n) {
                    BackpressureUtils.getAndAddRequest(REQUESTED, ObserveOnSubscriber.this, n);
                    schedule();
                }

            });
            child.add(recursiveScheduler);
            child.add(this);
        }

        @Override
        public void onStart() {
            // signal that this is an async operator capable of receiving this many
            request(RxRingBuffer.SIZE);
        }

        @Override
        public void onNext(final T t) {
            if (isUnsubscribed() || completed) {
                return;
            }
            if (!queue.offer(on.next(t))) {
                onError(new MissingBackpressureException());
                return;
            }
            schedule();
        }

        @Override
        public void onCompleted() {
            if (isUnsubscribed() || completed) {
                return;
            }
            if (error != null) {
                return;
            }
            completed = true;
            schedule();
        }

        @Override
        public void onError(final Throwable e) {
            if (isUnsubscribed() || completed) {
                return;
            }
            if (error != null) {
                return;
            }
            error = e;
            // unsubscribe eagerly since time will pass before the scheduled onError results in an unsubscribe event
            unsubscribe();
            // mark failure so the polling thread will skip onNext still in the queue
            completed = true;
            failure = true;
            schedule();
        }

        final Action0 action = new Action0() {

            @Override
            public void call() {
                pollQueue();
            }

        };

        protected void schedule() {
            if (COUNTER_UPDATER.getAndIncrement(this) == 0) {
                recursiveScheduler.schedule(action);
            }
        }

        // only execute this from schedule()
        void pollQueue() {
            int emitted = 0;
            do {
                /*
                 * Set to 1 otherwise it could have grown very large while in the last poll loop
                 * and then we can end up looping all those times again here before exiting even once we've drained
                 */
                counter = 1;

//                middle:
                while (!scheduledUnsubscribe.isUnsubscribed()) {
                    if (failure) {
                        child.onError(error);
                        return;
                    } else {
                        if (requested == 0 && completed && queue.isEmpty()) {
                            child.onCompleted();
                            return;
                        }
                        if (REQUESTED.getAndDecrement(this) != 0) {
                            Object o = queue.poll();
                            if (o == null) {
                                if (completed) {
                                    if (failure) {
                                        child.onError(error);
                                    } else {
                                        child.onCompleted();
                                    }
                                    return;
                                }
                                // nothing in queue
                                REQUESTED.incrementAndGet(this);
                                break;
                            } else {
                                if (!on.accept(child, o)) {
                                    // non-terminal event so let's increment count
                                    emitted++;
                                }
                            }
                        } else {
                            // we hit the end ... so increment back to 0 again
                            REQUESTED.incrementAndGet(this);
                            break;
                        }
                    }
                }
            } while (COUNTER_UPDATER.decrementAndGet(this) > 0);

            // request the number of items that we emitted in this poll loop
            if (emitted > 0) {
                request(emitted);
            }
        }
    }

    static final class ScheduledUnsubscribe implements Subscription {
        final Scheduler.Worker worker;
        volatile int once;
        static final AtomicIntegerFieldUpdater<ScheduledUnsubscribe> ONCE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(ScheduledUnsubscribe.class, "once");
        volatile boolean unsubscribed = false;

        public ScheduledUnsubscribe(Scheduler.Worker worker) {
            this.worker = worker;
        }

        @Override
        public boolean isUnsubscribed() {
            return unsubscribed;
        }

        @Override
        public void unsubscribe() {
            if (ONCE_UPDATER.getAndSet(this, 1) == 0) {
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        worker.unsubscribe();
                        unsubscribed = true;
                    }
                });
            }
        }

    }
}
