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

/*
 * The code was inspired by the similarly named JCTools class: 
 * https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/atomic
 */

package io.reactivex.internal.queue;

import java.util.*;
import java.util.concurrent.atomic.*;

import io.reactivex.internal.functions.Objects;
import io.reactivex.internal.util.Pow2;

/**
 * A single-producer single-consumer array-backed queue with exact, non power-of-2 logical capacity.
 * @param <T> the contained value type
 */
public final class SpscExactArrayQueue<T> extends AtomicReferenceArray<T> implements Queue<T> {
    /** */
    private static final long serialVersionUID = 6210984603741293445L;
    final int mask;
    final int capacitySkip;
    
    final AtomicLong producerIndex = new AtomicLong();
    final AtomicLong consumerIndex = new AtomicLong();
    
    public SpscExactArrayQueue(int capacity) {
        super(Pow2.roundToPowerOfTwo(capacity));
        int len = length();
        this.mask = len - 1;
        this.capacitySkip = len - capacity; 
    }
    
    
    @Override
    public boolean offer(T value) {
        Objects.requireNonNull(value, "value is null");
        
        long pi = producerIndex.get();
        int m = mask;
        
        int fullCheck = (int)(pi + capacitySkip) & m;
        if (get(fullCheck) != null) {
            return false;
        }
        int offset = (int)pi & m;
        producerIndex.lazySet(pi + 1);
        lazySet(offset, value);
        return true;
    }
    @Override
    public T poll() {
        long ci = consumerIndex.get();
        int offset = (int)ci & mask;
        T value = get(offset);
        if (value == null) {
            return null;
        }
        consumerIndex.lazySet(ci + 1);
        lazySet(offset, null);
        return value;
    }
    @Override
    public T peek() {
        return get((int)consumerIndex.get() & mask);
    }
    @Override
    public void clear() {
        while (poll() != null || !isEmpty());
    }
    @Override
    public boolean isEmpty() {
        return producerIndex.get() == consumerIndex.get();
    }
    
    @Override
    public int size() {
        long ci = consumerIndex.get();
        for (;;) {
            long pi = producerIndex.get();
            long ci2 = consumerIndex.get();
            if (ci == ci2) {
                return (int)(pi - ci2);
            }
            ci = ci2;
        }
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <E> E[] toArray(E[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(T e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T element() {
        throw new UnsupportedOperationException();
    }
    
}
