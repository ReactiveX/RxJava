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

package io.reactivex.internal.subscribers;

import java.util.concurrent.atomic.*;

import org.reactivestreams.Subscriber;

import io.reactivex.FlowableSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;

/**
 * Abstract base class for subscribers that hold another subscriber, a queue
 * and requires queue-drain behavior.
 *
 * @param <T> the source type to which this subscriber will be subscribed
 * @param <U> the value type in the queue
 * @param <V> the value type the child subscriber accepts
 */
public abstract class QueueDrainSubscriber<T, U, V> extends QueueDrainSubscriberPad3 implements FlowableSubscriber<T>, QueueDrain<U, V> {
    protected final Subscriber<? super V> actual;
    protected final SimplePlainQueue<U> queue;

    protected volatile boolean cancelled;

    protected volatile boolean done;
    protected Throwable error;

    public QueueDrainSubscriber(Subscriber<? super V> actual, SimplePlainQueue<U> queue) {
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

    protected final void fastPathEmitMax(U value, boolean delayError, Disposable dispose) {
        final Subscriber<? super V> s = actual;
        final SimplePlainQueue<U> q = queue;

        if (wip.get() == 0 && wip.compareAndSet(0, 1)) {
            long r = requested.get();
            if (r != 0L) {
                if (accept(s, value)) {
                    if (r != Long.MAX_VALUE) {
                        produced(1);
                    }
                }
                if (leave(-1) == 0) {
                    return;
                }
            } else {
                dispose.dispose();
                s.onError(new MissingBackpressureException("Could not emit buffer due to lack of requests"));
                return;
            }
        } else {
            q.offer(value);
            if (!enter()) {
                return;
            }
        }
        QueueDrainHelper.drainMaxLoop(q, s, delayError, dispose, this);
    }

    protected final void fastPathOrderedEmitMax(U value, boolean delayError, Disposable dispose) {
        final Subscriber<? super V> s = actual;
        final SimplePlainQueue<U> q = queue;

        if (wip.get() == 0 && wip.compareAndSet(0, 1)) {
            long r = requested.get();
            if (r != 0L) {
                if (q.isEmpty()) {
                    if (accept(s, value)) {
                        if (r != Long.MAX_VALUE) {
                            produced(1);
                        }
                    }
                    if (leave(-1) == 0) {
                        return;
                    }
                } else {
                    q.offer(value);
                }
            } else {
                cancelled = true;
                dispose.dispose();
                s.onError(new MissingBackpressureException("Could not emit buffer due to lack of requests"));
                return;
            }
        } else {
            q.offer(value);
            if (!enter()) {
                return;
            }
        }
        QueueDrainHelper.drainMaxLoop(q, s, delayError, dispose, this);
    }

    @Override
    public boolean accept(Subscriber<? super V> a, U v) {
        return false;
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
    public final long requested() {
        return requested.get();
    }

    @Override
    public final long produced(long n) {
        return requested.addAndGet(-n);
    }

    public final void requested(long n) {
        if (SubscriptionHelper.validate(n)) {
            BackpressureHelper.add(requested, n);
        }
    }

}

// -------------------------------------------------------------------
// Padding superclasses
//-------------------------------------------------------------------


/** The WIP counter. */
class QueueDrainSubscriberWip {
    final AtomicInteger wip = new AtomicInteger();
}

/** Contains the requested field. */
class QueueDrainSubscriberPad3 extends QueueDrainSubscriberWip{
    final AtomicLong requested = new AtomicLong();
}

