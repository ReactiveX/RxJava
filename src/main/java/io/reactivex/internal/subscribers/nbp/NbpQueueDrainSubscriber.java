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

package io.reactivex.internal.subscribers.nbp;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.util.NbpQueueDrain;

/**
 * Abstract base class for subscribers that hold another subscriber, a queue
 * and requires queue-drain behavior.
 * 
 * @param <T> the source type to which this subscriber will be subscribed
 * @param <U> the value type in the queue
 * @param <V> the value type the child subscriber accepts
 */
public abstract class NbpQueueDrainSubscriber<T, U, V> extends QueueDrainSubscriberPad2 implements NbpSubscriber<T>, NbpQueueDrain<U, V> {
    protected final NbpSubscriber<? super V> actual;
    protected final Queue<U> queue;
    
    protected volatile boolean cancelled;
    
    protected volatile boolean done;
    protected Throwable error;
    
    public NbpQueueDrainSubscriber(NbpSubscriber<? super V> actual, Queue<U> queue) {
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
    
    protected final void fastpathEmit(U value, boolean delayError, Disposable dispose) {
        final NbpSubscriber<? super V> s = actual;
        final Queue<U> q = queue;
        
        if (wip == 0 && WIP.compareAndSet(this, 0, 1)) {
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
        drainLoop(q, s, delayError, dispose);
    }

    /**
     * Makes sure the fast-path emits in order.
     * @param value
     * @param delayError
     */
    protected final void fastpathOrderedEmit(U value, boolean delayError, Disposable disposable) {
        final NbpSubscriber<? super V> s = actual;
        final Queue<U> q = queue;
        
        if (wip == 0 && WIP.compareAndSet(this, 0, 1)) {
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
        drainLoop(q, s, delayError, disposable);
    }

    @Override
    public final Throwable error() {
        return error;
    }
    
    @Override
    public final int leave(int m) {
        return WIP.addAndGet(this, m);
    }
    
    public void drain(boolean delayError, Disposable dispose) {
        if (enter()) {
            drainLoop(queue, actual, delayError, dispose);
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

