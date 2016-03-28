/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.producers;

import java.util.Queue;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.exceptions.*;
import rx.internal.operators.BackpressureUtils;
import rx.internal.util.atomic.SpscLinkedAtomicQueue;
import rx.internal.util.unsafe.*;

/**
 * Producer that holds an unbounded (or custom) queue to enqueue values and relays them
 * to a child subscriber on request.
 *
 * @param <T> the value type
 */
public final class QueuedValueProducer<T> extends AtomicLong implements Producer {
     
    /** */
    private static final long serialVersionUID = 7277121710709137047L;
    
    final Subscriber<? super T> child;
    final Queue<Object> queue;
    final AtomicInteger wip;
    
    static final Object NULL_SENTINEL = new Object();
    
    /**
     * Constructs an instance with the target child subscriber and an Spsc Linked (Atomic) Queue
     * as the queue implementation.
     * @param child the target child subscriber
     */
    public QueuedValueProducer(Subscriber<? super T> child) {
        this(child, UnsafeAccess.isUnsafeAvailable() 
                ? new SpscLinkedQueue<Object>() : new SpscLinkedAtomicQueue<Object>());
    }
    /**
     * Constructs an instance with the target child subscriber and a custom queue implementation
     * @param child the target child subscriber
     * @param queue the queue to use
     */
    public QueuedValueProducer(Subscriber<? super T> child, Queue<Object> queue) {
        this.child = child;
        this.queue = queue;
        this.wip = new AtomicInteger();
    }
     
    @Override
    public void request(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("n >= 0 required");
        }
        if (n > 0) {
            BackpressureUtils.getAndAddRequest(this, n);
            drain();
        }
    }
    
    /**
     * Offers a value to this producer and tries to emit any queued values
     * if the child requests allow it.
     * @param value the value to enqueue and attempt to drain
     * @return true if the queue accepted the offer, false otherwise
     */
    public boolean offer(T value) {
        if (value == null) {
            if (!queue.offer(NULL_SENTINEL)) {
                return false;
            }
        } else {
            if (!queue.offer(value)) {
                return false;
            }
        }
        drain();
        return true;
    }
    
    private void drain() {
        if (wip.getAndIncrement() == 0) {
            final Subscriber<? super T> c = child;
            final Queue<Object> q = queue;
            do {
                if (c.isUnsubscribed()) {
                    return;
                }
 
                wip.lazySet(1);
                 
                long r = get();
                long e = 0;
                Object v;
                 
                while (r != 0 && (v = q.poll()) != null) {
                    try {
                        if (v == NULL_SENTINEL) {
                            c.onNext(null);
                        } else {
                            @SuppressWarnings("unchecked")
                            T t = (T)v;
                            c.onNext(t);
                        }
                    } catch (Throwable ex) {
                        Exceptions.throwOrReport(ex, c, v != NULL_SENTINEL ? v : null);
                        return;
                    }
                    if (c.isUnsubscribed()) {
                        return;
                    }
                    r--;
                    e++;
                }
                 
                if (e != 0 && get() != Long.MAX_VALUE) {
                    addAndGet(-e);
                }
            } while (wip.decrementAndGet() != 0);
        }
    }
}