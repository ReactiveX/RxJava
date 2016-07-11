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

package io.reactivex.internal.subscribers.observable;

import java.util.*;

import io.reactivex.internal.fuseable.QueueDisposable;

/**
 * An abstract QueueDisposable implementation that defaults all 
 * unnecessary Queue methods to throw UnsupportedOperationException.
 * @param <T> the output value type
 */
public abstract class BaseQueueDisposable<T> implements QueueDisposable<T> {

    @Override
    public final boolean add(T e) {
        throw new UnsupportedOperationException("Should not be called");
    }
    
    @Override
    public final boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException("Should not be called");
    }
    
    @Override
    public final boolean contains(Object o) {
        throw new UnsupportedOperationException("Should not be called");
    }
    
    @Override
    public final boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Should not be called");
    }
    
    @Override
    public final T element() {
        throw new UnsupportedOperationException("Should not be called");
    }
    
    @Override
    public final Iterator<T> iterator() {
        throw new UnsupportedOperationException("Should not be called");
    }
    
    @Override
    public final boolean offer(T e) {
        throw new UnsupportedOperationException("Should not be called");
    }
    
    @Override
    public final T peek() {
        throw new UnsupportedOperationException("Should not be called");
    }
    
    @Override
    public final T remove() {
        throw new UnsupportedOperationException("Should not be called");
    }
    
    @Override
    public final boolean remove(Object o) {
        throw new UnsupportedOperationException("Should not be called");
    }
    
    @Override
    public final boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Should not be called");
    }
    
    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Should not be called");
    }
    
    @Override
    public final int size() {
        throw new UnsupportedOperationException("Should not be called");
    }
    
    @Override
    public final Object[] toArray() {
        throw new UnsupportedOperationException("Should not be called");
    }
    
    @Override
    public final <U extends Object> U[] toArray(U[] a) {
        throw new UnsupportedOperationException("Should not be called");
    }
}
