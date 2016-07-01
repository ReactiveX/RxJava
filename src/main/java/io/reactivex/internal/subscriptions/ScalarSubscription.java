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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Subscriber;

import io.reactivex.internal.fuseable.QueueSubscription;

/**
 * A Subscription that holds a constant value and emits it only when requested.
 * @param <T> the value type
 */
public final class ScalarSubscription<T> extends AtomicInteger implements QueueSubscription<T> {
    /** */
    private static final long serialVersionUID = -3830916580126663321L;
    /** The single value to emit, set to null. */
    final T value;
    /** The actual subscriber. */
    final Subscriber<? super T> subscriber;
    
    /** No request has been issued yet. */
    static final int NO_REQUEST = 0;
    /** Request has been called.*/
    static final int REQUESTED = 1;
    /** Cancel has been called. */
    static final int CANCELLED = 2;
    
    public ScalarSubscription(Subscriber<? super T> subscriber, T value) {
        this.subscriber = subscriber;
        this.value = value;
    }
    
    @Override
    public void request(long n) {
        if (!SubscriptionHelper.validateRequest(n)) {
            return;
        }
        if (compareAndSet(NO_REQUEST, REQUESTED)) {
            T v = value;
            Subscriber<? super T> s = subscriber;

            s.onNext(v);
            if (get() != CANCELLED) {
                s.onComplete();
            }
        }
        
    }
    
    @Override
    public void cancel() {
        lazySet(CANCELLED);
    }

    @Override
    public boolean add(T e) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public boolean offer(T e) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public T remove() {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public T poll() {
        if (get() == NO_REQUEST) {
            lazySet(REQUESTED);
            return value;
        }
        return null;
    }

    @Override
    public T element() {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public T peek() {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public boolean isEmpty() {
        return get() != NO_REQUEST;
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public <U> U[] toArray(U[] a) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public void clear() {
        lazySet(1);
    }

    @Override
    public int requestFusion(int mode) {
        return mode & SYNC;
    }
}
