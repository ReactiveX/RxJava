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

package io.reactivex.internal.subscriptions;

import io.reactivex.annotations.Nullable;
import org.reactivestreams.Subscriber;

/**
 * A subscription that signals a single value eventually.
 * <p>
 * Note that the class leaks all methods of {@link java.util.concurrent.atomic.AtomicLong}.
 * Use {@link #complete(Object)} to signal the single value.
 * <p>
 * The this atomic integer stores a bit field:<br>
 * bit 0: indicates that there is a value available<br>
 * bit 1: indicates that there was a request made<br>
 * bit 2: indicates there was a cancellation, exclusively set<br>
 * bit 3: indicates in fusion mode but no value yet, exclusively set<br>
 * bit 4: indicates in fusion mode and value is available, exclusively set<br>
 * bit 5: indicates in fusion mode and value has been consumed, exclusively set<br>
 * Where exclusively set means any other bits are 0 when that bit is set.
 * @param <T> the value type
 */
public class DeferredScalarSubscription<T> extends BasicIntQueueSubscription<T> {


    private static final long serialVersionUID = -2151279923272604993L;

    /** The Subscriber to emit the value to. */
    protected final Subscriber<? super T> actual;

    /** The value is stored here if there is no request yet or in fusion mode. */
    protected T value;

    /** Indicates this Subscription has no value and not requested yet. */
    static final int NO_REQUEST_NO_VALUE = 0;
    /** Indicates this Subscription has a value but not requested yet. */
    static final int NO_REQUEST_HAS_VALUE = 1;
    /** Indicates this Subscription has been requested but there is no value yet. */
    static final int HAS_REQUEST_NO_VALUE = 2;
    /** Indicates this Subscription has both request and value. */
    static final int HAS_REQUEST_HAS_VALUE = 3;

    /** Indicates the Subscription has been cancelled. */
    static final int CANCELLED = 4;

    /** Indicates this Subscription is in fusion mode and is currently empty. */
    static final int FUSED_EMPTY = 8;
    /** Indicates this Subscription is in fusion mode and has a value. */
    static final int FUSED_READY = 16;
    /** Indicates this Subscription is in fusion mode and its value has been consumed. */
    static final int FUSED_CONSUMED = 32;

    /**
     * Creates a DeferredScalarSubscription by wrapping the given Subscriber.
     * @param actual the Subscriber to wrap, not null (not verified)
     */
    public DeferredScalarSubscription(Subscriber<? super T> actual) {
        this.actual = actual;
    }

    @Override
    public final void request(long n) {
        if (SubscriptionHelper.validate(n)) {
            for (;;) {
                int state = get();
                // if the any bits 1-31 are set, we are either in fusion mode (FUSED_*)
                // or request has been called (HAS_REQUEST_*)
                if ((state & ~NO_REQUEST_HAS_VALUE) != 0) {
                    return;
                }
                if (state == NO_REQUEST_HAS_VALUE) {
                    if (compareAndSet(NO_REQUEST_HAS_VALUE, HAS_REQUEST_HAS_VALUE)) {
                        T v = value;
                        if (v != null) {
                            value = null;
                            Subscriber<? super T> a = actual;
                            a.onNext(v);
                            if (get() != CANCELLED) {
                                a.onComplete();
                            }
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
        int state = get();
        for (;;) {
            if (state == FUSED_EMPTY) {
                value = v;
                lazySet(FUSED_READY);

                Subscriber<? super T> a = actual;
                a.onNext(v);
                if (get() != CANCELLED) {
                    a.onComplete();
                }
                return;
            }

            // if state is >= CANCELLED or bit zero is set (*_HAS_VALUE) case, return
            if ((state & ~HAS_REQUEST_NO_VALUE) != 0) {
                return;
            }

            if (state == HAS_REQUEST_NO_VALUE) {
                lazySet(HAS_REQUEST_HAS_VALUE);
                Subscriber<? super T> a = actual;
                a.onNext(v);
                if (get() != CANCELLED) {
                    a.onComplete();
                }
                return;
            }
            value = v;
            if (compareAndSet(NO_REQUEST_NO_VALUE, NO_REQUEST_HAS_VALUE)) {
                return;
            }
            state = get();
            if (state == CANCELLED) {
                value = null;
                return;
            }
        }
    }

    @Override
    public final int requestFusion(int mode) {
        if ((mode & ASYNC) != 0) {
            lazySet(FUSED_EMPTY);
            return ASYNC;
        }
        return NONE;
    }

    @Nullable
    @Override
    public final T poll() {
        if (get() == FUSED_READY) {
            lazySet(FUSED_CONSUMED);
            T v = value;
            value = null;
            return v;
        }
        return null;
    }

    @Override
    public final boolean isEmpty() {
        return get() != FUSED_READY;
    }

    @Override
    public final void clear() {
        lazySet(FUSED_CONSUMED);
        value = null;
    }

    @Override
    public void cancel() {
        set(CANCELLED);
        value = null;
    }

    /**
     * Returns true if this Subscription has been cancelled.
     * @return true if this Subscription has been cancelled
     */
    public final boolean isCancelled() {
        return get() == CANCELLED;
    }

    /**
     * Atomically sets a cancelled state and returns true if
     * the current thread did it successfully.
     * @return true if the current thread cancelled
     */
    public final boolean tryCancel() {
        return getAndSet(CANCELLED) != CANCELLED;
    }
}
