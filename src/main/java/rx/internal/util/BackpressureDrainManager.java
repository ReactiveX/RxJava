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
package rx.internal.util;

import java.util.concurrent.atomic.AtomicLong;

import rx.Producer;
import rx.annotations.Experimental;

/**
 * Manages the producer-backpressure-consumer interplay by
 * matching up available elements with requested elements and/or
 * terminal events.
 * 
 * @since 1.1.0
 */
@Experimental
public final class BackpressureDrainManager extends AtomicLong implements Producer {
    /** */
    private static final long serialVersionUID = 2826241102729529449L;
    /**
     * Interface representing the minimal callbacks required
     * to operate the drain part of a backpressure system.
     */
    public interface BackpressureQueueCallback {
        /**
         * Override this method to peek for the next element,
         * null meaning no next element available now.
         * <p>It will be called plain and while holding this object's monitor.
         * @return the next element or null if no next element available
         */
        Object peek();
        /**
         * Override this method to poll (consume) the next element,
         * null meaning no next element available now.
         * @return the next element or null if no next element available
         */
        Object poll();
        /**
         * Override this method to deliver an element to downstream.
         * The logic ensures that this happens only in the right conditions.
         * @param value the value to deliver, not null
         * @return true indicates that one should terminate the emission loop unconditionally
         * and not deliver any further elements or terminal events.
         */
        boolean accept(Object value);
        /**
         * Override this method to deliver a normal or exceptional
         * terminal event.
         * @param exception if not null, contains the terminal exception
         */
        void complete(Throwable exception);
    }

    /** Indicates if one is in emitting phase, guarded by this. */
    protected boolean emitting;
    /** Indicates a terminal state. */
    protected volatile boolean terminated;
    /** Indicates an error state, barrier is provided via terminated. */
    protected Throwable exception;
    /** The callbacks to manage the drain. */
    protected final BackpressureQueueCallback actual;
    /**
     * Constructs a backpressure drain manager with 0 requestedCount,
     * no terminal event and not emitting.
     * @param actual he queue callback to check for new element availability
     */
    public BackpressureDrainManager(BackpressureQueueCallback actual) {
        this.actual = actual;
    }
    /**
     * Checks if a terminal state has been reached.
     * @return true if a terminal state has been reached
     */
    public final boolean isTerminated() {
        return terminated;
    }
    /**
     * Move into a terminal state. 
     * Call drain() anytime after.
     */
    public final void terminate() {
        terminated = true;
    }
    /**
     * Move into a terminal state with an exception.
     * Call drain() anytime after.
     * <p>Serialized access is expected with respect to
     * element emission.
     * @param error the exception to deliver
     */
    public final void terminate(Throwable error) {
        if (!terminated) {
            exception = error;
            terminated = true;
        }
    }
    /**
     * Move into a terminal state and drain. 
     */
    public final void terminateAndDrain() {
        terminated = true;
        drain();
    }
    /**
     * Move into a terminal state with an exception and drain.
     * <p>Serialized access is expected with respect to
     * element emission.
     * @param error the exception to deliver
     */
    public final void terminateAndDrain(Throwable error) {
        if (!terminated) {
            exception = error;
            terminated = true;
            drain();
        }
    }
    @Override
    public final void request(long n) {
        if (n == 0) {
            return;
        }
        boolean mayDrain;
        long r;
        long u;
        do {
            r = get();
            mayDrain = r == 0;
            if (r == Long.MAX_VALUE) {
                break;
            }
            if (n == Long.MAX_VALUE) {
                u = n;
                mayDrain = true;
            } else {
                if (r > Long.MAX_VALUE - n) {
                    u = Long.MAX_VALUE;
                } else {
                    u = r + n;
                }
            }
        } while (!compareAndSet(r, u));
        // since we implement producer, we have to call drain
        // on a 0-n request transition
        if (mayDrain) {
            drain();
        }
    }
    /**
     * Try to drain the "queued" elements and terminal events
     * by considering the available and requested event counts.
     */
    public final void drain() {
        long n;
        boolean term;
        synchronized (this) {
            if (emitting) {
                return;
            }
            emitting = true;
            term = terminated;
        }
        n = get();
        boolean skipFinal = false;
        try {
            BackpressureQueueCallback a = actual;
            while (true) {
                int emitted = 0;
                while (n > 0 || term) {
                    Object o;
                    if (term) {
                        o = a.peek();
                        if (o == null) {
                            skipFinal = true;
                            Throwable e = exception;
                            a.complete(e);
                            return;
                        }
                        if (n == 0) {
                            break;
                        }
                    }
                    o = a.poll();
                    if (o == null) {
                        break;
                    } else {
                        if (a.accept(o)) {
                            skipFinal = true;
                            return;
                        }
                        n--;
                        emitted++;
                    }
                }
                synchronized (this) {
                    term = terminated;
                    boolean more = a.peek() != null;
                    // if no backpressure below
                    if (get() == Long.MAX_VALUE) {
                        // no new data arrived since the last poll
                        if (!more && !term) {
                            skipFinal = true;
                            emitting = false;
                            return;
                        }
                        n = Long.MAX_VALUE;
                    } else {
                        n = addAndGet(-emitted);
                        if ((n == 0 || !more) && (!term || more)) {
                            skipFinal = true;
                            emitting = false;
                            return;
                        }
                    }
                }
            }
        } finally {
            if (!skipFinal) {
                synchronized (this) {
                    emitting = false;
                }
            }
        }

    }
}
