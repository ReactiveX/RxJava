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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;

public class OperatorOnBackpressureBuffer<T> implements Operator<T, T> {

    private final NotificationLite<T> on = NotificationLite.instance();

    private final Long capacity;
    private final Action0 onOverflow;

    public OperatorOnBackpressureBuffer() {
        this.capacity = null;
        this.onOverflow = null;
    }

    public OperatorOnBackpressureBuffer(long capacity) {
        this(capacity, null);
    }

    public OperatorOnBackpressureBuffer(long capacity, Action0 onOverflow) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Buffer capacity must be > 0");
        }
        this.capacity = capacity;
        this.onOverflow = onOverflow;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        // TODO get a different queue implementation
        final Object lock = new Object();
        // `queue`, `wip` and `requested` are guarded by `lock`
        final Queue<Object> queue = new LinkedList<Object>();
        final AtomicLong wip = new AtomicLong();
        final AtomicLong requested = new AtomicLong();

        final AtomicLong capacity = (this.capacity == null) ? null : new AtomicLong(this.capacity);

        child.setProducer(new Producer() {

            @Override
            public void request(long n) {
                pollQueue(null, n, lock, wip, requested, capacity, queue, child);
            }

        });
        // don't pass through subscriber as we are async and doing queue draining
        // a parent being unsubscribed should not affect the children
        Subscriber<T> parent = new Subscriber<T>() {

            private AtomicBoolean saturated = new AtomicBoolean(false);

            @Override
            public void onStart() {
                request(Long.MAX_VALUE);
            }

            @Override
            public void onCompleted() {
                if (!saturated.get()) {
                    pollQueue(on.completed(), 0, lock, wip, requested, capacity, queue, child);
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!saturated.get()) {
                    pollQueue(on.error(e), 0, lock, wip, requested, capacity, queue, child);
                }
            }

            @Override
            public void onNext(T t) {
                if (!assertCapacity()) {
                    return;
                }
                pollQueue(on.next(t), 0, lock, wip, requested, capacity, queue, child);
            }

            private boolean assertCapacity() {
                if (capacity == null) {
                    return true;
                }

                long currCapacity;
                do {
                    currCapacity = capacity.get();
                    if (currCapacity <= 0) {
                        if (saturated.compareAndSet(false, true)) {
                            unsubscribe();
                            child.onError(new MissingBackpressureException(
                                "Overflowed buffer of "
                                + OperatorOnBackpressureBuffer.this.capacity));
                            if (onOverflow != null) {
                                onOverflow.call();
                            }
                        }
                        return false;
                    }
                // ensure no other thread stole our slot, or retry
                } while (!capacity.compareAndSet(currCapacity, currCapacity - 1));
                return true;
            }
        };
        
        // if child unsubscribes it should unsubscribe the parent, but not the other way around
        child.add(parent);
        
        return parent;
    }

    private void pollQueue(Object newElem, long newRequest, Object lock, AtomicLong wip, AtomicLong requested, AtomicLong capacity, Queue<Object> queue, Subscriber<? super T> child) {
        // TODO can we do this without putting everything in the queue first so we can fast-path the case when we don't need to queue?
        boolean win;
        synchronized (lock) {
            requested.addAndGet(newRequest);
            if (newElem != null) {
                queue.offer(newElem);
            }
            win = wip.getAndIncrement() == 0;
        }

        if (win) {
            while (true) {
                Object o;
                synchronized (lock) {
                    if (requested.get() > 0) {
                        o = queue.poll();
                        if (o == null) {
                            // nothing in queue
                            wip.decrementAndGet();
                            return;
                        }
                        requested.decrementAndGet();
                    } else {
                        wip.decrementAndGet();
                        return;
                    }
                }
                if (capacity != null) { // it's bounded
                    capacity.incrementAndGet();
                }
                on.accept(child, o);
            }
        }
    }
}
