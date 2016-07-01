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

package io.reactivex.internal.subscribers.flowable;

import java.util.*;

import org.reactivestreams.Subscription;

import io.reactivex.internal.functions.Objects;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Base class for a fuseable intermediate subscriber.
 * @param <T> the upstream value type
 * @param <R> the downstream value type
 */
public abstract class BasicFuseableConditionalSubscriber<T, R> implements ConditionalSubscriber<T>, QueueSubscription<R> {

    /** The downstream subscriber. */
    protected final ConditionalSubscriber<? super R> actual;
    
    /** The upstream subscription. */
    protected Subscription s;
    
    /** The upstream's QueueSubscription if not null. */
    protected QueueSubscription<T> qs;
    
    /** Flag indicating no further onXXX event should be accepted. */
    protected boolean done;
    
    /** Holds the established fusion mode of the upstream. */
    protected int sourceMode;
    
    /**
     * Construct a BasicFuseableSubscriber by wrapping the given subscriber.
     * @param actual the subscriber, not null (not verified)
     */
    public BasicFuseableConditionalSubscriber(ConditionalSubscriber<? super R> actual) {
        this.actual = actual;
    }
    
    // final: fixed protocol steps to support fuseable and non-fuseable upstream
    @SuppressWarnings("unchecked")
    @Override
    public final void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validate(this.s, s)) {
            
            this.s = s;
            if (s instanceof QueueSubscription) {
                this.qs = (QueueSubscription<T>)s;
            }
            
            if (beforeDownstream()) {
                
                actual.onSubscribe(this);
                
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

    /**
     * Emits the value to the actual subscriber if {@link #done} is false. 
     * @param value the value to signal
     */
    protected final void next(R value) {
        if (done) {
            return;
        }
        actual.onNext(value);
    }
    
    /**
     * Tries to emit the value to the actual subscriber if {@link #done} is false
     * and returns the response from the {@link ConditionalSubscriber#tryOnNext(Object)}
     * call.
     * @param value the value to signal
     * @return the response from the actual subscriber: true indicates accepted value,
     * false indicates dropped value
     */
    protected final boolean tryNext(R value) {
        if (done) {
            return false;
        }
        return actual.tryOnNext(value);
    }
    
    @Override
    public void onError(Throwable t) {
        if (done) {
            RxJavaPlugins.onError(t);
            return;
        }
        done = true;
        actual.onError(t);
    }
    
    /**
     * Rethrows the throwable if it is a fatal exception or calls {@link #onError(Throwable)}.
     * @param t the throwable to rethrow or signal to the actual subscriber
     */
    protected final void fail(Throwable t) {
        Exceptions.throwIfFatal(t);
        s.cancel();
        onError(t);
    }
    
    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        done = true;
        actual.onComplete();
    }
    
    /**
     * Checks if the value is null and if so, throws a NullPointerException.
     * @param value the value to check
     * @param message the message to indicate the source of the value
     * @return the value if not null
     */
    protected final <V> V nullCheck(V value, String message) {
        return Objects.requireNonNull(value, message);
    }
    
    /**
     * Calls the upstream's QueueSubscription.requestFusion with the mode and
     * saves the established mode in {@link #sourceMode}.
     * <p>
     * If the upstream doesn't support fusion ({@link #qs} is null), the method
     * returns {@link QueueSubscription#NONE}.
     * @param mode the fusion mode requested
     * @return the established fusion mode
     */
    protected final int transitiveFusion(int mode) {
        QueueSubscription<T> qs = this.qs;
        if (qs != null) {
            int m = qs.requestFusion(mode);
            if (m != NONE) {
                sourceMode = m;
            }
            return m;
        }
        return NONE;
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
    // Default implementation of the RS and QS protocol (overridable)
    // --------------------------------------------------------------
    
    @Override
    public void request(long n) {
        s.request(n);
    }
    
    @Override
    public void cancel() {
        s.cancel();
    }
    
    @Override
    public boolean isEmpty() {
        return qs.isEmpty();
    }
    
    @Override
    public final int size() {
        return qs.size();
    }
    
    @Override
    public void clear() {
        qs.clear();
    }
    
    // -----------------------------------------------------------
    // The rest of the Queue interface methods shouldn't be called
    // -----------------------------------------------------------
    
    @Override
    public final boolean add(R e) {
        throw new UnsupportedOperationException("Should not be called!");
    }
    
    @Override
    public final boolean addAll(Collection<? extends R> c) {
        throw new UnsupportedOperationException("Should not be called!");
    }
    
    @Override
    public final boolean contains(Object o) {
        throw new UnsupportedOperationException("Should not be called!");
    }
    
    @Override
    public final boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public final R element() {
        throw new UnsupportedOperationException("Should not be called!");
    }
    
    @Override
    public final Iterator<R> iterator() {
        throw new UnsupportedOperationException("Should not be called!");
    }
    
    @Override
    public final boolean offer(R e) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public final R peek() {
        throw new UnsupportedOperationException("Should not be called!");
    }
    
    @Override
    public final R remove() {
        throw new UnsupportedOperationException("Should not be called!");
    }
    
    @Override
    public final boolean remove(Object o) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public final boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Should not be called!");
    }
    
    @Override
    public final boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Should not be called!");
    }
    
    @Override
    public final Object[] toArray() {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public <U extends Object> U[] toArray(U[] a) {
        throw new UnsupportedOperationException("Should not be called!");
    }
}
