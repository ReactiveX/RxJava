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
package rx.observers;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import rx.Observer;
import rx.operators.NotificationLite;

/**
 * Enforces single-threaded, serialized, ordered execution of {@link #onNext}, {@link #onCompleted}, and
 * {@link #onError}.
 * <p>
 * When multiple threads are emitting and/or notifying they will be serialized by:
 * </p><ul>
 * <li>Allowing only one thread at a time to emit</li>
 * <li>Adding notifications to a queue if another thread is already emitting</li>
 * <li>Not holding any locks or blocking any threads while emitting</li>
 * </ul>
 * 
 * @param <T>
 *          the type of items expected to be observed by the {@code Observer}
 */
public final class SerializedObserver<T> implements Observer<T> {
    /** The actual observer that receives the values. */
    private final Observer<Object> actual;
    /** The queue in case of concurrent access. */
    final MpscPaddedQueue<Object> queue;
    /** Number of queued events. */
    final PaddedAtomicInteger wip;
    /** 
     * Adaptively enable and disable fast path. 0 means fast path enabled, 1 means fast path disabled,
     * 2 means the observer is terminated.
     */
    volatile int fastpath;
    /**
     * Atomic updater for the fastpath variable.
     */
    @SuppressWarnings("rawtypes")
    static final AtomicIntegerFieldUpdater<SerializedObserver> FASTPATH_UPDATER
            = AtomicIntegerFieldUpdater.newUpdater(SerializedObserver.class, "fastpath");
    /** Lightweight notification transformation. */
    static final NotificationLite<Object> nl = NotificationLite.instance();
    /**
     * Constructor, takes the actual observer and initializes the queue and
     * work counters.
     * @param s the actual observer to wrap
     */
    @SuppressWarnings("unchecked")
    public SerializedObserver(Observer<? super T> s) {
        this.actual = (Observer<Object>)s;
        this.queue = new MpscPaddedQueue<Object>();
        this.wip = new PaddedAtomicInteger();
    }

    @Override
    public void onCompleted() {
        int n = fastpath;
        // let's try the fast path
        if (n == 0 && wip.compareAndSet(0, 1)) {
            FASTPATH_UPDATER.lazySet(this, 2);
            actual.onCompleted();
            // just return since nooen else will be able to 
            // put anything else into the queue at this point
            return;
        }
        if (n == 2) {
            return;
        }
        queue.offer(nl.completed());
        if (wip.getAndIncrement() == 0) {
            n = fastpath; // re-read fastpath
            if (n == 2) {
                queue.clear();
                return;
            }
            FASTPATH_UPDATER.lazySet(this, 2);
            do {
                if (nl.accept2(actual, queue.poll())) {
                    queue.clear();
                    return;
                }
            } while (wip.decrementAndGet() > 0);
        }
    }

    @Override
    public void onError(final Throwable e) {
        int n = fastpath;
        // let's try the fast path
        if (n == 0 && wip.compareAndSet(0, 1)) {
            FASTPATH_UPDATER.lazySet(this, 2);
            actual.onError(e);
            // just return since nooen else will be able to 
            // put anything else into the queue at this point
            return;
        }
        // or are we terminated?
        if (n == 2) {
            return;
        }
        queue.offer(nl.error(e));
        if (wip.getAndIncrement() == 0) {
            n = fastpath; // re-read fastpath
            if (n == 2) { // are we terminated now?
                queue.clear();
                return;
            }
            FASTPATH_UPDATER.lazySet(this, 2);
            do {
                if (nl.accept2(actual, queue.poll())) {
                    queue.clear();
                    return;
                }
            } while (wip.decrementAndGet() > 0);
        }
    }

    @Override
    public void onNext(final T t) {
        int n = fastpath;
        if (n == 0 && wip.compareAndSet(0, 1)) {
            int w;
            try {
                actual.onNext(t);
            } finally {
                w = wip.decrementAndGet();
            }
            if (w == 0) {
                return;
            }
            FASTPATH_UPDATER.lazySet(this, 0);
        } else {
            if (n == 2) {
                return;
            }
            queue.offer(nl.next(t));
            if (wip.getAndIncrement() != 0) {
                return;
            }
            n = fastpath; // we won the emission race, are we done btw?
            if (n == 2) {
                return;
            }
        }
        int c = 0;
        boolean endRegular = false;
        try {
            do {
                if (nl.accept2(actual, queue.poll())) {
                    FASTPATH_UPDATER.lazySet(this, 2);
                    queue.clear();
                    return;
                }
                c++;
            } while (wip.decrementAndGet() > 0);
            endRegular = true;
            if (c < 3) {
                FASTPATH_UPDATER.lazySet(this, 1);
            }
        } finally {
            if (!endRegular) {
                wip.set(0); // allow onError to enter
            }
        }
    }
}
