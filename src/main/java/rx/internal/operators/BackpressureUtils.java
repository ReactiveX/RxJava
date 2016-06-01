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
package rx.internal.operators;

import java.util.Queue;
import java.util.concurrent.atomic.*;

import rx.Subscriber;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;

/**
 * Utility functions for use with backpressure.
 *
 */
public final class BackpressureUtils {
    /** Utility class, no instances. */
    private BackpressureUtils() {
        throw new IllegalStateException("No instances!");
    }
    /**
     * Adds {@code n} to {@code requested} field and returns the value prior to
     * addition once the addition is successful (uses CAS semantics). If
     * overflows then sets {@code requested} field to {@code Long.MAX_VALUE}.
     * 
     * @param <T> the type of the target object on which the field updater operates
     * 
     * @param requested
     *            atomic field updater for a request count
     * @param object
     *            contains the field updated by the updater
     * @param n
     *            the number of requests to add to the requested count
     * @return requested value just prior to successful addition
     * @deprecated Android has issues with reflection-based atomics
     */
    @Deprecated
    public static <T> long getAndAddRequest(AtomicLongFieldUpdater<T> requested, T object, long n) {
        // add n to field but check for overflow
        while (true) {
            long current = requested.get(object);
            long next = addCap(current, n);
            if (requested.compareAndSet(object, current, next)) {
                return current;
            }
        }
    }

    /**
     * Adds {@code n} to {@code requested} and returns the value prior to addition once the
     * addition is successful (uses CAS semantics). If overflows then sets
     * {@code requested} field to {@code Long.MAX_VALUE}.
     * 
     * @param requested
     *            atomic long that should be updated
     * @param n
     *            the number of requests to add to the requested count
     * @return requested value just prior to successful addition
     */
    public static long getAndAddRequest(AtomicLong requested, long n) {
        // add n to field but check for overflow
        while (true) {
            long current = requested.get();
            long next = addCap(current, n);
            if (requested.compareAndSet(current, next)) {
                return current;
            }
        }
    }
    
    /**
     * Multiplies two positive longs and caps the result at Long.MAX_VALUE.
     * @param a the first value
     * @param b the second value
     * @return the capped product of a and b
     */
    public static long multiplyCap(long a, long b) {
        long u = a * b;
        if (((a | b) >>> 31) != 0) {
            if (b != 0L && (u / b != a)) {
                u = Long.MAX_VALUE;
            }
        }
        return u;
    }
    
    /**
     * Adds two positive longs and caps the result at Long.MAX_VALUE.
     * @param a the first value
     * @param b the second value
     * @return the capped sum of a and b
     */
    public static long addCap(long a, long b) {
        long u = a + b;
        if (u < 0L) {
            u = Long.MAX_VALUE;
        }
        return u;
    }
    
    /**
     * Masks the most significant bit, i.e., 0x8000_0000_0000_0000L.
     */
    static final long COMPLETED_MASK = Long.MIN_VALUE;
    /**
     * Masks the request amount bits, i.e., 0x7FFF_FFFF_FFFF_FFFF.
     */
    static final long REQUESTED_MASK = Long.MAX_VALUE;
    
    /**
     * Signals the completion of the main sequence and switches to post-completion replay mode.
     * 
     * <p>
     * Don't modify the queue after calling this method!
     * 
     * <p>
     * Post-completion backpressure handles the case when a source produces values based on
     * requests when it is active but more values are available even after its completion.
     * In this case, the onCompleted() can't just emit the contents of the queue but has to
     * coordinate with the requested amounts. This requires two distinct modes: active and
     * completed. In active mode, requests flow through and the queue is not accessed but
     * in completed mode, requests no-longer reach the upstream but help in draining the queue.
     * <p>
     * The algorithm utilizes the most significant bit (bit 63) of a long value (AtomicLong) since
     * request amount only goes up to Long.MAX_VALUE (bits 0-62) and negative values aren't
     * allowed.
     * 
     * @param <T> the value type to emit
     * @param requested the holder of current requested amount
     * @param queue the queue holding values to be emitted after completion
     * @param actual the subscriber to receive the values
     */
    public static <T> void postCompleteDone(AtomicLong requested, Queue<T> queue, Subscriber<? super T> actual) {
        postCompleteDone(requested, queue, actual, UtilityFunctions.<T>identity());
    }
    
    /**
     * Accumulates requests (validated) and handles the completed mode draining of the queue based on the requests.
     * 
     * <p>
     * Post-completion backpressure handles the case when a source produces values based on
     * requests when it is active but more values are available even after its completion.
     * In this case, the onCompleted() can't just emit the contents of the queue but has to
     * coordinate with the requested amounts. This requires two distinct modes: active and
     * completed. In active mode, requests flow through and the queue is not accessed but
     * in completed mode, requests no-longer reach the upstream but help in draining the queue.
     * 
     * @param <T> the value type to emit
     * @param requested the holder of current requested amount
     * @param n the value requested;
     * @param queue the queue holding values to be emitted after completion
     * @param actual the subscriber to receive the values
     * @return true if in the active mode and the request amount of n can be relayed to upstream, false if
     * in the post-completed mode and the queue is draining.
     */
    public static <T> boolean postCompleteRequest(AtomicLong requested, long n, Queue<T> queue, Subscriber<? super T> actual) {
        return postCompleteRequest(requested, n, queue, actual, UtilityFunctions.<T>identity());
    }
    
    /**
     * Signals the completion of the main sequence and switches to post-completion replay mode
     * and allows exit transformation on the queued values.
     * 
     * <p>
     * Don't modify the queue after calling this method!
     * 
     * <p>
     * Post-completion backpressure handles the case when a source produces values based on
     * requests when it is active but more values are available even after its completion.
     * In this case, the onCompleted() can't just emit the contents of the queue but has to
     * coordinate with the requested amounts. This requires two distinct modes: active and
     * completed. In active mode, requests flow through and the queue is not accessed but
     * in completed mode, requests no-longer reach the upstream but help in draining the queue.
     * <p>
     * The algorithm utilizes the most significant bit (bit 63) of a long value (AtomicLong) since
     * request amount only goes up to Long.MAX_VALUE (bits 0-62) and negative values aren't
     * allowed.
     * 
     * @param <T> the value type in the queue
     * @param <R> the value type to emit
     * @param requested the holder of current requested amount
     * @param queue the queue holding values to be emitted after completion
     * @param actual the subscriber to receive the values
     * @param exitTransform the transformation to apply on the dequeued value to get the value to be emitted
     */
    public static <T, R> void postCompleteDone(AtomicLong requested, Queue<T> queue, Subscriber<? super R> actual, Func1<? super T, ? extends R> exitTransform) {
        for (;;) {
            long r = requested.get();
            
            // switch to completed mode only once
            if ((r & COMPLETED_MASK) != 0L) {
                return;
            }
            
            //
            long u = r | COMPLETED_MASK;
            
            if (requested.compareAndSet(r, u)) {
                // if we successfully switched to post-complete mode and there 
                // are requests available start draining the queue
                if (r != 0L) {
                    // if the switch happened when there was outstanding requests, start draining
                    postCompleteDrain(requested, queue, actual, exitTransform);
                }
                return;
            }
        }
    }
    
    /**
     * Accumulates requests (validated) and handles the completed mode draining of the queue based on the requests
     * and allows exit transformation on the queued values.
     * 
     * <p>
     * Post-completion backpressure handles the case when a source produces values based on
     * requests when it is active but more values are available even after its completion.
     * In this case, the onCompleted() can't just emit the contents of the queue but has to
     * coordinate with the requested amounts. This requires two distinct modes: active and
     * completed. In active mode, requests flow through and the queue is not accessed but
     * in completed mode, requests no-longer reach the upstream but help in draining the queue.
     * 
     * @param <T> the value type in the queue
     * @param <R> the value type to emit
     * @param requested the holder of current requested amount
     * @param n the value requested;
     * @param queue the queue holding values to be emitted after completion
     * @param actual the subscriber to receive the values
     * @param exitTransform the transformation to apply on the dequeued value to get the value to be emitted
     * @return true if in the active mode and the request amount of n can be relayed to upstream, false if
     * in the post-completed mode and the queue is draining.
     */
    public static <T, R> boolean postCompleteRequest(AtomicLong requested, long n, Queue<T> queue, Subscriber<? super R> actual, Func1<? super T, ? extends R> exitTransform) {
        if (n < 0L) {
            throw new IllegalArgumentException("n >= 0 required but it was " + n);
        }
        if (n == 0) {
            return (requested.get() & COMPLETED_MASK) == 0;
        }
        
        for (;;) {
            long r = requested.get();
            
            // mask of the completed flag
            long c = r & COMPLETED_MASK;
            // mask of the requested amount
            long u = r & REQUESTED_MASK;
            
            // add the current requested amount and the new requested amount
            // cap at Long.MAX_VALUE;
            long v = addCap(u, n);
            
            // restore the completed flag
            v |= c;
            
            if (requested.compareAndSet(r, v)) {
                // if there was no outstanding request before and in
                // the post-completed state, start draining
                if (r == COMPLETED_MASK) {
                    postCompleteDrain(requested, queue, actual, exitTransform);
                    return false;
                }
                // returns true for active mode and false if the completed flag was set
                return c == 0L;
            }
        }
    }
    
    /**
     * Drains the queue based on the outstanding requests in post-completed mode (only!)
     * and allows exit transformation on the queued values.
     * 
     * @param <T> the value type in the queue
     * @param <R> the value type to emit
     * @param requested the holder of current requested amount
     * @param queue the queue holding values to be emitted after completion
     * @param subscriber the subscriber to receive the values
     * @param exitTransform the transformation to apply on the dequeued value to get the value to be emitted
     */
    static <T, R> void postCompleteDrain(AtomicLong requested, Queue<T> queue, Subscriber<? super R> subscriber, Func1<? super T, ? extends R> exitTransform) {
        
        long r = requested.get();
        
        // Run on a fast-path if the downstream is unbounded
        if (r == Long.MAX_VALUE) {
            for (;;) {
                if (subscriber.isUnsubscribed()) {
                    return;
                }
                
                T v = queue.poll();
                
                if (v == null) {
                    subscriber.onCompleted();
                    return;
                }
                
                subscriber.onNext(exitTransform.call(v));
            }
        }
        /*
         * Since we are supposed to be in the post-complete state, 
         * requested will have its top bit set.
         * To allow direct comparison, we start with an emission value which has also
         * this flag set, then increment it as usual.
         * Since COMPLETED_MASK is essentially Long.MIN_VALUE, 
         * there won't be any overflow or sign flip.
         */
        long e = COMPLETED_MASK;
        
        for (;;) {
            
            /*
             * This is an improved queue-drain algorithm with a specialization 
             * in which we know the queue won't change anymore (i.e., done is always true
             * when looking at the classical algorithm and there is no error).
             * 
             * Note that we don't check for cancellation or emptiness upfront for two reasons:
             * 1) if e != r, the loop will do this and we quit appropriately
             * 2) if e == r, then either there was no outstanding requests or we emitted the requested amount
             *    and the execution simply falls to the e == r check below which checks for emptiness anyway.
             */
            
            while (e != r) {
                if (subscriber.isUnsubscribed()) {
                    return;
                }
                
                T v = queue.poll();
                
                if (v == null) {
                    subscriber.onCompleted();
                    return;
                }
                
                subscriber.onNext(exitTransform.call(v));
                
                e++;
            }
            
            /*
             * If the emission count reaches the requested amount the same time the queue becomes empty
             * this will make sure the subscriber is completed immediately instead of on the next request.
             * This is also true if there are no outstanding requests (this the while loop doesn't run)
             * and the queue is empty from the start.
             */
            if (e == r) {
                if (subscriber.isUnsubscribed()) {
                    return;
                }
                if (queue.isEmpty()) {
                    subscriber.onCompleted();
                    return;
                }
            }
            
            /*
             * Fast flow: see if more requests have arrived in the meantime.
             * This avoids an atomic add (~40 cycles) and resumes the emission immediately.
             */
            r = requested.get();
            
            if (r == e) {
                /* 
                 * Atomically decrement the requested amount by the emission amount.
                 * We can't use the full emission value because of the completed flag,
                 * however, due to two's complement representation, the flag on requested
                 * is preserved.
                 */
                r = requested.addAndGet(-(e & REQUESTED_MASK));
                // The requested amount actually reached zero, quit
                if (r == COMPLETED_MASK) {
                    return;
                }
                // reset the emission count
                e = COMPLETED_MASK;
            }
        }
    }

    /**
     * Atomically subtracts a value from the requested amount unless it's at Long.MAX_VALUE.
     * @param requested the requested amount holder
     * @param n the value to subtract from the requested amount, has to be positive (not verified)
     * @return the new requested amount
     * @throws IllegalStateException if n is greater than the current requested amount, which
     * indicates a bug in the request accounting logic
     */
    public static long produced(AtomicLong requested, long n) {
        for (;;) {
            long current = requested.get();
            if (current == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long next = current - n;
            if (next < 0L) {
                throw new IllegalStateException("More produced than requested: " + next);
            }
            if (requested.compareAndSet(current, next)) {
                return next;
            }
        }
    }
}
