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

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable.Operator;
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.ImmediateScheduler;
import rx.schedulers.TestScheduler;
import rx.schedulers.TrampolineScheduler;
import rx.subscriptions.Subscriptions;

/**
 * Delivers events on the specified Scheduler.
 * <p>
 * This provides backpressure by blocking the incoming onNext when there is already one in the queue.
 * <p>
 * This means that at any given time the max number of "onNext" in flight is 3:
 * -> 1 being delivered on the Scheduler
 * -> 1 in the queue waiting for the Scheduler
 * -> 1 blocking on the queue waiting to deliver it
 * 
 * I have chosen to allow 1 in the queue rather than using an Exchanger style process so that the Scheduler
 * can loop and have something to do each time around to optimize for avoiding rescheduling when it
 * can instead just loop. I'm avoiding having the Scheduler thread ever block as it could be an event-loop
 * thus if the queue is empty it exits and next time something is added it will reschedule.
 * 
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/observeOn.png">
 */
public class OperatorObserveOnBounded<T> implements Operator<T, T> {

    private final Scheduler scheduler;
    private final int bufferSize;

    /**
     * 
     * @param scheduler
     * @param bufferSize
     *            that will be rounded up to the next power of 2
     */
    public OperatorObserveOnBounded(Scheduler scheduler, int bufferSize) {
        this.scheduler = scheduler;
        this.bufferSize = roundToNextPowerOfTwoIfNecessary(bufferSize);
    }

    public OperatorObserveOnBounded(Scheduler scheduler) {
        this(scheduler, 1);
    }

    private static int roundToNextPowerOfTwoIfNecessary(int num) {
        if ((num & -num) == num) {
            return num;
        } else {
            int result = 1;
            while (num != 0)
            {
                num >>= 1;
                result <<= 1;
            }
            return result;
        }
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        if (scheduler instanceof ImmediateScheduler) {
            // avoid overhead, execute directly
            return child;
        } else if (scheduler instanceof TrampolineScheduler) {
            // avoid overhead, execute directly
            return child;
        } else if (scheduler instanceof TestScheduler) {
            // this one will deadlock as it is single-threaded and won't run the scheduled
            // work until it manually advances, which it won't be able to do as it will block
            return child;
        } else {
            return new ObserveOnSubscriber(child);
        }
    }

    private static Object NULL_SENTINEL = new Object();
    private static Object COMPLETE_SENTINEL = new Object();

    private static class ErrorSentinel {
        final Throwable e;

        ErrorSentinel(Throwable e) {
            this.e = e;
        }
    }

    /** Observe through individual queue per observer. */
    private class ObserveOnSubscriber extends Subscriber<T> {
        final Subscriber<? super T> observer;
        private volatile Scheduler.Inner recursiveScheduler;

        private final InterruptibleBlockingQueue<Object> queue = new InterruptibleBlockingQueue<Object>(bufferSize);
        final AtomicLong counter = new AtomicLong(0);

        public ObserveOnSubscriber(Subscriber<? super T> observer) {
            super(observer);
            this.observer = observer;
        }

        @Override
        public void onNext(final T t) {
            try {
                // we want to block for natural back-pressure
                // so that the producer waits for each value to be consumed
                if (t == null) {
                    queue.addBlocking(NULL_SENTINEL);
                } else {
                    queue.addBlocking(t);
                }
                schedule();
            } catch (InterruptedException e) {
                if (!isUnsubscribed()) {
                    onError(e);
                }
            }
        }

        @Override
        public void onCompleted() {
            try {
                // we want to block for natural back-pressure
                // so that the producer waits for each value to be consumed
                queue.addBlocking(COMPLETE_SENTINEL);
                schedule();
            } catch (InterruptedException e) {
                onError(e);
            }
        }

        @Override
        public void onError(final Throwable e) {
            try {
                // we want to block for natural back-pressure
                // so that the producer waits for each value to be consumed
                queue.addBlocking(new ErrorSentinel(e));
                schedule();
            } catch (InterruptedException e2) {
                // call directly if we can't schedule
                observer.onError(e2);
            }
        }

        protected void schedule() {
            if (counter.getAndIncrement() == 0) {
                if (recursiveScheduler == null) {
                    // first time through, register a Subscription
                    // that can interrupt this thread
                    add(Subscriptions.create(new Action0() {

                        @Override
                        public void call() {
                            // we have to interrupt the parent thread because
                            // it can be blocked on queue.put
                            queue.interrupt();
                        }

                    }));
                    add(scheduler.schedule(new Action1<Inner>() {

                        @Override
                        public void call(Inner inner) {
                            recursiveScheduler = inner;
                            pollQueue();
                        }

                    }));
                } else {
                    recursiveScheduler.schedule(new Action1<Inner>() {

                        @Override
                        public void call(Inner inner) {
                            pollQueue();
                        }

                    });
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void pollQueue() {
            do {
                Object v = queue.poll();
                if (v != null) {
                    if (v == NULL_SENTINEL) {
                        observer.onNext(null);
                    } else if (v == COMPLETE_SENTINEL) {
                        observer.onCompleted();
                    } else if (v instanceof ErrorSentinel) {
                        observer.onError(((ErrorSentinel) v).e);
                    } else {
                        observer.onNext((T) v);
                    }
                }
            } while (counter.decrementAndGet() > 0);
        }

    }

    /**
     * Single-producer-single-consumer queue (only thread-safe for 1 producer thread with 1 consumer thread).
     * 
     * This supports an interrupt() being called externally rather than needing to interrupt the thread. This allows
     * unsubscribe behavior when this queue is being used.
     * 
     * @param <E>
     */
    private static class InterruptibleBlockingQueue<E> {

        private final Semaphore semaphore;
        private volatile boolean interrupted = false;

        private final E[] buffer;

        private AtomicLong tail = new AtomicLong();
        private AtomicLong head = new AtomicLong();
        private final int capacity;
        private final int mask;

        @SuppressWarnings("unchecked")
        public InterruptibleBlockingQueue(final int size) {
            this.semaphore = new Semaphore(size);
            this.capacity = size;
            this.mask = size - 1;
            buffer = (E[]) new Object[size];
        }

        /**
         * Used to unsubscribe and interrupt the producer if blocked in put()
         */
        public void interrupt() {
            interrupted = true;
            semaphore.release();
        }

        public void addBlocking(final E e) throws InterruptedException {
            if (interrupted) {
                throw new InterruptedException("Interrupted by Unsubscribe");
            }
            semaphore.acquire();
            if (interrupted) {
                throw new InterruptedException("Interrupted by Unsubscribe");
            }
            if (e == null) {
                throw new IllegalArgumentException("Can not put null");
            }

            if (offer(e)) {
                return;
            } else {
                throw new IllegalStateException("Queue is full");
            }
        }

        private boolean offer(final E e) {
            final long _t = tail.get();
            if (_t - head.get() == capacity) {
                // queue is full
                return false;
            }
            int index = (int) (_t & mask);
            buffer[index] = e;
            // move the tail forward
            tail.lazySet(_t + 1);

            return true;
        }

        public E poll() {
            if (interrupted) {
                return null;
            }
            final long _h = head.get();
            if (tail.get() == _h) {
                // nothing available
                return null;
            }
            int index = (int) (_h & mask);

            // fetch the item
            E v = buffer[index];
            // allow GC to happen
            buffer[index] = null;
            // increment and signal we're done
            head.lazySet(_h + 1);
            if (v != null) {
                semaphore.release();
            }
            return v;
        }

        public int size()
        {
            int size;
            do
            {
                final long currentHead = head.get();
                final long currentTail = tail.get();
                size = (int) (currentTail - currentHead);
            } while (size > buffer.length);

            return size;
        }

    }
}