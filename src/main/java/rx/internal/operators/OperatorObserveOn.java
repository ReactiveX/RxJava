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
        
        // the status of the current stream
        volatile boolean finished = false;

        @SuppressWarnings("unused")
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
            if (isUnsubscribed()) {
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
            if (isUnsubscribed() || finished) {
                return;
            }
            finished = true;
            schedule();
        }

        @Override
        public void onError(final Throwable e) {
            if (isUnsubscribed() || finished) {
                return;
            }
            error = e;
            // unsubscribe eagerly since time will pass before the scheduled onError results in an unsubscribe event
            unsubscribe();
            finished = true;
            // polling thread should skip any onNext still in the queue
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
                counter = 1;
                long produced = 0;
                long r = requested;
                for (;;) {
                    if (child.isUnsubscribed())
                        return;
                    Throwable error;
                    if (finished) {
                        if ((error = this.error) != null) {
                            // errors shortcut the queue so 
                            // release the elements in the queue for gc
                            queue.clear();
                            child.onError(error);
                            return;
                        } else
                        if (queue.isEmpty()) {
                            child.onCompleted();
                            return;
                        }
                    }
                    if (r > 0) {
                        Object o = queue.poll();
                        if (o != null) {
                            child.onNext(on.getValue(o));
                            r--;
                            emitted++;
                            produced++;
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (produced > 0 && requested != Long.MAX_VALUE) {
                    REQUESTED.addAndGet(this, -produced);
                }
            } while (COUNTER_UPDATER.decrementAndGet(this) > 0);
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
