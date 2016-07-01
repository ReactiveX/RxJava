/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.subscriptions;

import org.reactivestreams.Subscriber;

/**
 * A subscription that signals a single value eventually.
 * <p>
 * Note that the class leaks all methods of {@link java.util.concurrent.atomic.AtomicLong}.
 * Use {@link #complete(Object)} to signal the single value.
 * @param <T> the value type
 */
public class DeferredScalarSubscription<T> extends BasicQueueSubscription<T> {

    /** */
    private static final long serialVersionUID = -2151279923272604993L;

    protected final Subscriber<? super T> actual;
    
    protected T value;
    
    protected int fusionState;
    
    /** Constant for the this state. */
    static final long NO_REQUEST_NO_VALUE = 0;
    /** Constant for the this state. */
    static final long NO_REQUEST_HAS_VALUE = 1;
    /** Constant for the this state. */
    static final long HAS_REQUEST_NO_VALUE = 2;
    /** Constant for the this state. */
    static final long HAS_REQUEST_HAS_VALUE = 3;
    /** Constant for the this state. */
    static final long CANCELLED = 4;

    /** Constant for the {@link fusionState} field. */
    static final int NOT_FUSED = 0;
    /** Constant for the {@link fusionState} field. */
    static final int EMPTY = 1;
    /** Constant for the {@link fusionState} field. */
    static final int HAS_VALUE = 2;
    /** Constant for the {@link fusionState} field. */
    static final int CONSUMED = 3;
    
    public DeferredScalarSubscription(Subscriber<? super T> actual) {
        this.actual = actual;
    }
    
    @Override
    public void request(long n) {
        if (SubscriptionHelper.validate(n)) {
            for (;;) {
                long state = get();
                if (state == HAS_REQUEST_NO_VALUE || state == HAS_REQUEST_HAS_VALUE || state == CANCELLED) {
                    return;
                }
                if (state == NO_REQUEST_HAS_VALUE) {
                    // unlike complete() we need to CAS as multiple concurrent requests are allowed
                    if (compareAndSet(NO_REQUEST_HAS_VALUE, HAS_REQUEST_HAS_VALUE)) {
                        if (fusionState == EMPTY) {
                            fusionState = HAS_VALUE;
                        }
                        actual.onNext(value);
                        if (get() != CANCELLED) {
                            actual.onComplete();
                        }
                    }
                    return;
                }
                if (compareAndSet(NO_REQUEST_NO_VALUE, HAS_REQUEST_NO_VALUE)) {
                    return;
                }
            }
        }
    }

    /**
     * Completes this subscription by indicating the given value should
     * be emitted when the first request arrives.
     * <p>Make sure this is called exactly once.
     * @param v the value to signal, not null (not validated)
     */
    public final void complete(T v) {
        for (;;) {
            long state = get();
            if (state == NO_REQUEST_HAS_VALUE || state == HAS_REQUEST_HAS_VALUE || state == CANCELLED) {
                return;
            }
            if (state == HAS_REQUEST_NO_VALUE) {
                // no need to CAS in the terminal state because complete() is called at most once
                if (fusionState == EMPTY) {
                    value = v;
                    fusionState = HAS_VALUE;
                }
                actual.onNext(v);
                if (get() != CANCELLED) {
                    actual.onComplete();
                }
                return;
            }
            this.value = v;
            if (compareAndSet(NO_REQUEST_NO_VALUE, NO_REQUEST_HAS_VALUE)) {
                return;
            }
        }
    }
    
    @Override
    public final int requestFusion(int mode) {
        if ((mode & ASYNC) != 0) {
            fusionState = EMPTY;
            return ASYNC;
        }
        return NONE;
    }

    @Override
    public final T poll() {
        if (fusionState == HAS_VALUE) {
            fusionState = CONSUMED;
            return value;
        }
        return null;
    }

    @Override
    public final boolean isEmpty() {
        return fusionState != HAS_VALUE;
    }

    @Override
    public final void clear() {
        fusionState = CONSUMED;
    }

    @Override
    public void cancel() {
        set(CANCELLED);
    }

    /**
     * Returns true if this Subscription has been cancelled.
     * @return true if this Subscription has been cancelled
     */
    public final boolean isCancelled() {
        return get() == CANCELLED;
    }
}
