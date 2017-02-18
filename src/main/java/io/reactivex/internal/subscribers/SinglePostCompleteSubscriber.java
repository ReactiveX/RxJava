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

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import io.reactivex.FlowableSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * Relays signals from upstream according to downstream requests and allows
 * signalling a final value followed by onComplete in a backpressure-aware manner.
 *
 * @param <T> the input value type
 * @param <R> the output value type
 */
public abstract class SinglePostCompleteSubscriber<T, R> extends AtomicLong implements FlowableSubscriber<T>, Subscription {
    private static final long serialVersionUID = 7917814472626990048L;

    /** The downstream consumer. */
    protected final Subscriber<? super R> actual;

    /** The upstream subscription. */
    protected Subscription s;

    /** The last value stored in case there is no request for it. */
    protected R value;

    /** Number of values emitted so far. */
    protected long produced;

    /** Masks out the 2^63 bit indicating a completed state. */
    static final long COMPLETE_MASK = Long.MIN_VALUE;
    /** Masks out the lower 63 bit holding the current request amount. */
    static final long REQUEST_MASK = Long.MAX_VALUE;

    public SinglePostCompleteSubscriber(Subscriber<? super R> actual) {
        this.actual = actual;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validate(this.s, s)) {
            this.s = s;
            actual.onSubscribe(this);
        }
    }

    /**
     * Signals the given value and an onComplete if the downstream is ready to receive the final value.
     * @param n the value to emit
     */
    protected final void complete(R n) {
        long p = produced;
        if (p != 0) {
            BackpressureHelper.produced(this, p);
        }

        for (;;) {
            long r = get();
            if ((r & COMPLETE_MASK) != 0) {
                onDrop(n);
                return;
            }
            if ((r & REQUEST_MASK) != 0) {
                lazySet(COMPLETE_MASK + 1);
                actual.onNext(n);
                actual.onComplete();
                return;
            }
            value = n;
            if (compareAndSet(0, COMPLETE_MASK)) {
                return;
            }
            value = null;
        }
    }

    /**
     * Called in case of multiple calls to complete.
     * @param n the value dropped
     */
    protected void onDrop(R n) {
        // default is no-op
    }

    @Override
    public final void request(long n) {
        if (SubscriptionHelper.validate(n)) {
            for (;;) {
                long r = get();
                if ((r & COMPLETE_MASK) != 0) {
                    if (compareAndSet(COMPLETE_MASK, COMPLETE_MASK + 1)) {
                        actual.onNext(value);
                        actual.onComplete();
                    }
                    break;
                }
                long u = BackpressureHelper.addCap(r, n);
                if (compareAndSet(r, u)) {
                    s.request(n);
                    break;
                }
            }
        }
    }

    @Override
    public void cancel() {
        s.cancel();
    }
}
