/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.concurrent.atomic.AtomicInteger;

import rx.*;

/**
 * Base class for Subscribers that consume the entire upstream and signal
 * zero or one element (or an error) in a backpressure honoring fashion.
 * <p>
 * Store any temporary value in {@link #value} and indicate there is
 * a value available when completing by setting {@link #hasValue}.
 * <p.
 * Use {@link #subscribeTo(Observable)} to properly setup the link between this and the downstream
 * subscriber.
 *
 * @param <T> the source value type
 * @param <R> the result value type
 */
public abstract class DeferredScalarSubscriber<T, R> extends Subscriber<T> {

    /** The downstream subscriber. */
    protected final Subscriber<? super R> actual;

    /** Indicates there is a value available in value. */
    protected boolean hasValue;

    /** The holder of the single value. */
    protected R value;

    /** The state, see the constants below. */
    final AtomicInteger state;

    /** Initial state. */
    static final int NO_REQUEST_NO_VALUE = 0;
    /** Request came first. */
    static final int HAS_REQUEST_NO_VALUE = 1;
    /** Value came first. */
    static final int NO_REQUEST_HAS_VALUE = 2;
    /** Value will be emitted. */
    static final int HAS_REQUEST_HAS_VALUE = 3;

    public DeferredScalarSubscriber(Subscriber<? super R> actual) {
        this.actual = actual;
        this.state = new AtomicInteger();
    }

    @Override
    public void onError(Throwable ex) {
        value = null;
        actual.onError(ex);
    }

    @Override
    public void onCompleted() {
        if (hasValue) {
            complete(value);
        } else {
            complete();
        }
    }

    /**
     * Signals onCompleted() to the downstream subscriber.
     */
    protected final void complete() {
        actual.onCompleted();
    }

    /**
     * Atomically switches to the terminal state and emits the value if
     * there is a request for it or stores it for retrieval by {@code downstreamRequest(long)}.
     * @param value the value to complete with
     */
    protected final void complete(R value) {
        Subscriber<? super R> a = actual;
        for (;;) {
            int s = state.get();

            if (s == NO_REQUEST_HAS_VALUE || s == HAS_REQUEST_HAS_VALUE || a.isUnsubscribed()) {
                return;
            }
            if (s == HAS_REQUEST_NO_VALUE) {
                a.onNext(value);
                if (!a.isUnsubscribed()) {
                    a.onCompleted();
                }
                state.lazySet(HAS_REQUEST_HAS_VALUE);
                return;
            }
            this.value = value;
            if (state.compareAndSet(NO_REQUEST_NO_VALUE, NO_REQUEST_HAS_VALUE)) {
                return;
            }
        }
    }

    final void downstreamRequest(long n) {
        if (n < 0L) {
            throw new IllegalArgumentException("n >= 0 required but it was " + n);
        }
        if (n != 0L) {
            Subscriber<? super R> a = actual;
            for (;;) {
                int s = state.get();
                if (s == HAS_REQUEST_NO_VALUE || s == HAS_REQUEST_HAS_VALUE || a.isUnsubscribed()) {
                    return;
                }
                if (s == NO_REQUEST_HAS_VALUE) {
                    if (state.compareAndSet(NO_REQUEST_HAS_VALUE, HAS_REQUEST_HAS_VALUE)) {
                        a.onNext(value);
                        if (!a.isUnsubscribed()) {
                            a.onCompleted();
                        }
                    }
                    return;
                }
                if (state.compareAndSet(NO_REQUEST_NO_VALUE, HAS_REQUEST_NO_VALUE)) {
                    return;
                }
            }
        }
    }

    @Override
    public final void setProducer(Producer p) {
        p.request(Long.MAX_VALUE);
    }

    /**
     * Links up with the downstream Subscriber (cancellation, backpressure) and
     * subscribes to the source Observable.
     * @param source the source Observable
     */
    public final void subscribeTo(Observable<? extends T> source) {
        setupDownstream();
        source.unsafeSubscribe(this);
    }

    /* test */ final void setupDownstream() {
        Subscriber<? super R> a = actual;
        a.add(this);
        a.setProducer(new InnerProducer(this));
    }

    /**
     * Redirects the downstream request amount bach to the DeferredScalarSubscriber.
     */
    static final class InnerProducer implements Producer {
        final DeferredScalarSubscriber<?, ?> parent;

        public InnerProducer(DeferredScalarSubscriber<?, ?> parent) {
            this.parent = parent;
        }

        @Override
        public void request(long n) {
            parent.downstreamRequest(n);
        }
    }
}
