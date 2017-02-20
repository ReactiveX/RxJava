/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.observers;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.util.*;

/**
 * Abstract base class for subscribers that hold another subscriber, a queue
 * and requires queue-drain behavior.
 *
 * @param <T> the source type to which this subscriber will be subscribed
 * @param <U> the value type in the queue
 * @param <V> the value type the child subscriber accepts
 */
public abstract class QueueDrainObserver<T, U, V> extends QueueDrainSubscriberPad2 implements Observer<T>, ObservableQueueDrain<U, V> {
    protected final Observer<? super V> actual;
    protected final SimplePlainQueue<U> queue;

    protected volatile boolean cancelled;

    protected volatile boolean done;
    protected Throwable error;

    public QueueDrainObserver(Observer<? super V> actual, SimplePlainQueue<U> queue) {
        this.actual = actual;
        this.queue = queue;
    }

    @Override
    public final boolean cancelled() {
        return cancelled;
    }

    @Override
    public final boolean done() {
        return done;
    }

    @Override
    public final boolean enter() {
        return wip.getAndIncrement() == 0;
    }

    public final boolean fastEnter() {
        return wip.get() == 0 && wip.compareAndSet(0, 1);
    }

    protected final void fastPathEmit(U value, boolean delayError, Disposable dispose) {
        final Observer<? super V> s = actual;
        final SimplePlainQueue<U> q = queue;

        if (wip.get() == 0 && wip.compareAndSet(0, 1)) {
            accept(s, value);
            if (leave(-1) == 0) {
                return;
            }
        } else {
            q.offer(value);
            if (!enter()) {
                return;
            }
        }
        QueueDrainHelper.drainLoop(q, s, delayError, dispose, this);
    }

    /**
     * Makes sure the fast-path emits in order.
     * @param value the value to emit or queue up
     * @param delayError if true, errors are delayed until the source has terminated
     * @param disposable the resource to dispose if the drain terminates
     */
    protected final void fastPathOrderedEmit(U value, boolean delayError, Disposable disposable) {
        final Observer<? super V> s = actual;
        final SimplePlainQueue<U> q = queue;

        if (wip.get() == 0 && wip.compareAndSet(0, 1)) {
            if (q.isEmpty()) {
                accept(s, value);
                if (leave(-1) == 0) {
                    return;
                }
            } else {
                q.offer(value);
            }
        } else {
            q.offer(value);
            if (!enter()) {
                return;
            }
        }
        QueueDrainHelper.drainLoop(q, s, delayError, disposable, this);
    }

    @Override
    public final Throwable error() {
        return error;
    }

    @Override
    public final int leave(int m) {
        return wip.addAndGet(m);
    }

    @Override
    public void accept(Observer<? super V> a, U v) {
        // ignored by default
    }
}

// -------------------------------------------------------------------
// Padding superclasses
//-------------------------------------------------------------------

/** Pads the header away from other fields. */
class QueueDrainSubscriberPad0 {
    volatile long p1, p2, p3, p4, p5, p6, p7;
    volatile long p8, p9, p10, p11, p12, p13, p14, p15;
}

/** The wip counter. */
class QueueDrainSubscriberWip extends QueueDrainSubscriberPad0 {
    final AtomicInteger wip = new AtomicInteger();
}

/** Pads away the wip from the other fields. */
class QueueDrainSubscriberPad2 extends QueueDrainSubscriberWip {
    volatile long p1a, p2a, p3a, p4a, p5a, p6a, p7a;
    volatile long p8a, p9a, p10a, p11a, p12a, p13a, p14a, p15a;
}

