/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Original License: https://github.com/JCTools/JCTools/blob/master/LICENSE
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/atomic/SpscAtomicArrayQueue.java
 */

package rx.internal.util.atomic;

import java.util.*;
import java.util.concurrent.atomic.*;

import rx.internal.util.unsafe.Pow2;

/**
 * A single-producer single-consumer bounded queue with exact capacity tracking.
 * <p>This means that a queue of 10 will allow exactly 10 offers, however, the underlying storage is still power-of-2.
 * <p>The implementation uses field updaters and thus should be platform-safe.
 * @param <T> the value type held by this queue
 */
public final class SpscExactAtomicArrayQueue<T> extends AtomicReferenceArray<T> implements Queue<T> {
    /** */
    private static final long serialVersionUID = 6210984603741293445L;
    final int mask;
    final int capacitySkip;
    final AtomicLong producerIndex;
    final AtomicLong consumerIndex;
    
    public SpscExactAtomicArrayQueue(int capacity) {
        super(Pow2.roundToPowerOfTwo(capacity));
        int len = length();
        this.mask = len - 1;
        this.capacitySkip = len - capacity;
        this.producerIndex = new AtomicLong();
        this.consumerIndex = new AtomicLong();
    }
    
    
    @Override
    public boolean offer(T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        
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
        return producerIndex == consumerIndex;
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
