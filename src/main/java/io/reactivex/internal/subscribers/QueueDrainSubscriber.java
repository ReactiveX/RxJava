/**
 * Copyright 2015 Netflix, Inc.
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

import java.util.Queue;
import java.util.concurrent.atomic.*;

import org.reactivestreams.Subscriber;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.QueueDrain;

/**
 * Abstract base class for subscribers that hold another subscriber, a queue
 * and requires queue-drain behavior.
 * 
 * @param <T> the source type to which this subscriber will be subscribed
 * @param <U> the value type in the queue
 * @param <V> the value type the child subscriber accepts
 */
public abstract class QueueDrainSubscriber<T, U, V> extends QueueDrainSubscriberPad4 implements Subscriber<T>, QueueDrain<U, V> {
    protected final Subscriber<? super V> actual;
    protected final Queue<U> queue;
    
    protected volatile boolean cancelled;
    
    protected volatile boolean done;
    protected Throwable error;
    
    public QueueDrainSubscriber(Subscriber<? super V> actual, Queue<U> queue) {
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
        return WIP.getAndIncrement(this) == 0;
    }
    
    public final boolean fastEnter() {
        return wip == 0 && WIP.compareAndSet(this, 0, 1);
    }
    
    protected final void fastpathEmit(U value, boolean delayError) {
        final Subscriber<? super V> s = actual;
        final Queue<U> q = queue;
        
        if (wip == 0 && WIP.compareAndSet(this, 0, 1)) {
            long r = requested;
            if (r != 0L) {
                if (accept(s, value)) {
                    if (r != Long.MAX_VALUE) {
                        produced(1);
                    }
                }
                if (leave(-1) == 0) {
                    return;
                }
            }
            q.offer(value);
        } else {
            q.offer(value);
            if (!enter()) {
                return;
            }
        }
        drainLoop(q, s, delayError);
    }

    protected final void fastpathEmitMax(U value, boolean delayError, Disposable dispose) {
        final Subscriber<? super V> s = actual;
        final Queue<U> q = queue;
        
        if (wip == 0 && WIP.compareAndSet(this, 0, 1)) {
            long r = requested;
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
                s.onError(new IllegalStateException("Could not emit buffer due to lack of requests"));
                return;
            }
        } else {
            q.offer(value);
            if (!enter()) {
                return;
            }
        }
        drainMaxLoop(q, s, delayError, dispose);
    }

    protected final void fastpathOrderedEmitMax(U value, boolean delayError, Disposable dispose) {
        final Subscriber<? super V> s = actual;
        final Queue<U> q = queue;
        
        if (wip == 0 && WIP.compareAndSet(this, 0, 1)) {
            long r = requested;
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
                s.onError(new IllegalStateException("Could not emit buffer due to lack of requests"));
                return;
            }
        } else {
            q.offer(value);
            if (!enter()) {
                return;
            }
        }
        drainMaxLoop(q, s, delayError, dispose);
    }

    /**
     * Makes sure the fast-path emits in order.
     * @param value
     * @param delayError
     */
    protected final void fastpathOrderedEmit(U value, boolean delayError) {
        final Subscriber<? super V> s = actual;
        final Queue<U> q = queue;
        
        if (wip == 0 && WIP.compareAndSet(this, 0, 1)) {
            if (q.isEmpty()) {
                long r = requested;
                if (r != 0L) {
                    if (accept(s, value)) {
                        if (r != Long.MAX_VALUE) {
                            produced(1);
                        }
                    }
                    if (leave(-1) == 0) {
                        return;
                    }
                }
            }
            q.offer(value);
        } else {
            q.offer(value);
            if (!enter()) {
                return;
            }
        }
        drainLoop(q, s, delayError);
    }

    @Override
    public final Throwable error() {
        return error;
    }
    
    @Override
    public final int leave(int m) {
        return WIP.addAndGet(this, m);
    }
    
    @Override
    public final long requested() {
        return requested;
    }
    
    @Override
    public final long produced(long n) {
        return REQUESTED.addAndGet(this, -n);
    }
    
    public final void requested(long n) {
        if (SubscriptionHelper.validateRequest(n)) {
            return;
        }
        REQUESTED.addAndGet(this, n);
    }
    
    public void drain(boolean delayError) {
        if (enter()) {
            drainLoop(queue, actual, delayError);
        }
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

/** The WIP counter. */
class QueueDrainSubscriberWip extends QueueDrainSubscriberPad0 {
    volatile int wip;
    static final AtomicIntegerFieldUpdater<QueueDrainSubscriberWip> WIP =
            AtomicIntegerFieldUpdater.newUpdater(QueueDrainSubscriberWip.class, "wip");
}

/** Pads away the wip from the other fields. */
class QueueDrainSubscriberPad2 extends QueueDrainSubscriberWip {
    volatile long p1a, p2a, p3a, p4a, p5a, p6a, p7a;
    volatile long p8a, p9a, p10a, p11a, p12a, p13a, p14a, p15a;
}

/** Contains the requested field. */
class QueueDrainSubscriberPad3 extends QueueDrainSubscriberPad2 {
    volatile long requested;
    static final AtomicLongFieldUpdater<QueueDrainSubscriberPad3> REQUESTED =
            AtomicLongFieldUpdater.newUpdater(QueueDrainSubscriberPad3.class, "requested");
}

/** Pads away the requested from the other fields. */
class QueueDrainSubscriberPad4 extends QueueDrainSubscriberPad3 {
    volatile long q1, q2, q3, q4, q5, q6, q7;
    volatile long q8, q9, q10, q11, q12, q13, q14, q15;
}
