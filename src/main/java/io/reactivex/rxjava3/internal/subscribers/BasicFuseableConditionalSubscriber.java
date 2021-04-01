/*
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

package io.reactivex.rxjava3.internal.subscribers;

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Base class for a fuseable intermediate subscriber.
 * @param <T> the upstream value type
 * @param <R> the downstream value type
 */
public abstract class BasicFuseableConditionalSubscriber<T, R> implements ConditionalSubscriber<T>, QueueSubscription<R> {

    /** The downstream subscriber. */
    protected final ConditionalSubscriber<? super R> downstream;

    /** The upstream subscription. */
    protected Subscription upstream;

    /** The upstream's QueueSubscription if not null. */
    protected QueueSubscription<T> qs;

    /** Flag indicating no further onXXX event should be accepted. */
    protected boolean done;

    /** Holds the established fusion mode of the upstream. */
    protected int sourceMode;

    /**
     * Construct a BasicFuseableSubscriber by wrapping the given subscriber.
     * @param downstream the subscriber, not null (not verified)
     */
    public BasicFuseableConditionalSubscriber(ConditionalSubscriber<? super R> downstream) {
        this.downstream = downstream;
    }

    // final: fixed protocol steps to support fuseable and non-fuseable upstream
    @SuppressWarnings("unchecked")
    @Override
    public final void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validate(this.upstream, s)) {

            this.upstream = s;
            if (s instanceof QueueSubscription) {
                this.qs = (QueueSubscription<T>)s;
            }

            if (beforeDownstream()) {

                downstream.onSubscribe(this);

                afterDownstream();
            }

        }
    }

    /**
     * Override this to perform actions before the call {@code actual.onSubscribe(this)} happens.
     * @return true if onSubscribe should continue with the call
     */
    protected boolean beforeDownstream() {
        return true;
    }

    /**
     * Override this to perform actions after the call to {@code actual.onSubscribe(this)} happened.
     */
    protected void afterDownstream() {
        // default no-op
    }

    // -----------------------------------
    // Convenience and state-aware methods
    // -----------------------------------

    @Override
    public void onError(Throwable t) {
        if (done) {
            RxJavaPlugins.onError(t);
            return;
        }
        done = true;
        downstream.onError(t);
    }

    /**
     * Rethrows the throwable if it is a fatal exception or calls {@link #onError(Throwable)}.
     * @param t the throwable to rethrow or signal to the actual subscriber
     */
    protected final void fail(Throwable t) {
        Exceptions.throwIfFatal(t);
        upstream.cancel();
        onError(t);
    }

    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        done = true;
        downstream.onComplete();
    }

    /**
     * Calls the upstream's QueueSubscription.requestFusion with the mode and
     * saves the established mode in {@link #sourceMode} if that mode doesn't
     * have the {@link QueueSubscription#BOUNDARY} flag set.
     * <p>
     * If the upstream doesn't support fusion ({@link #qs} is null), the method
     * returns {@link QueueSubscription#NONE}.
     * @param mode the fusion mode requested
     * @return the established fusion mode
     */
    protected final int transitiveBoundaryFusion(int mode) {
        QueueSubscription<T> qs = this.qs;
        if (qs != null) {
            if ((mode & BOUNDARY) == 0) {
                int m = qs.requestFusion(mode);
                if (m != NONE) {
                    sourceMode = m;
                }
                return m;
            }
        }
        return NONE;
    }

    // --------------------------------------------------------------
    // Default implementation of the RS and QS protocol (can be overridden)
    // --------------------------------------------------------------

    @Override
    public void request(long n) {
        upstream.request(n);
    }

    @Override
    public void cancel() {
        upstream.cancel();
    }

    @Override
    public boolean isEmpty() {
        return qs.isEmpty();
    }

    @Override
    public void clear() {
        qs.clear();
    }

    // -----------------------------------------------------------
    // The rest of the Queue interface methods shouldn't be called
    // -----------------------------------------------------------

    @Override
    public final boolean offer(R e) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public final boolean offer(R v1, R v2) {
        throw new UnsupportedOperationException("Should not be called!");
    }
}
