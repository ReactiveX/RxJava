/**
 * Copyright 2014 Netflix, Inc.
 * 
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
 */
package rx.internal.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import rx.Subscription;
import rx.functions.Func1;

/**
 * Add/Remove without object allocation (after initial construction).
 * <p>
 * This is meant for hundreds or single-digit thousands of elements that need
 * to be rapidly added and randomly or sequentially removed while avoiding object allocation.
 * <p>
 * On Intel Core i7, 2.3Mhz, Mac Java 8:
 * <p>
 * - adds per second single-threaded => ~32,598,500 for 100
 * - adds per second single-threaded => ~23,200,000 for 10,000
 * - adds + removes per second single-threaded => 15,562,100 for 100
 * - adds + removes per second single-threaded => 8,760,000 for 10,000
 * 
 * <pre> {@code
 * Benchmark                                              (size)   Mode   Samples        Score  Score error    Units
 * r.i.IndexedRingBufferPerf.indexedRingBufferAdd            100  thrpt         5   263571.721     9856.994    ops/s
 * r.i.IndexedRingBufferPerf.indexedRingBufferAdd          10000  thrpt         5     1763.417      211.998    ops/s
 * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove      100  thrpt         5   139850.115    17143.705    ops/s
 * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove    10000  thrpt         5      809.982       72.931    ops/s
 * } </pre>
 * 
 * @param <E>
 */
public class IndexedRingBuffer<E> implements Subscription {

    private static final ObjectPool<IndexedRingBuffer> POOL = new ObjectPool<IndexedRingBuffer>() {

        @Override
        protected IndexedRingBuffer createObject() {
            return new IndexedRingBuffer();
        }

    };

    public final static IndexedRingBuffer getInstance() {
        return POOL.borrowObject();
    }

    private final ElementSection<E> elements = new ElementSection<E>();
    private final IndexSection removed = new IndexSection();
    /* package for unit testing */final AtomicInteger index = new AtomicInteger();
    /* package for unit testing */final AtomicInteger removedIndex = new AtomicInteger();
    /* package for unit testing */static final int SIZE = 512;

    /**
     * This resets the arrays, nulls out references and returns it to the pool.
     * This extra CPU cost is far smaller than the object allocation cost of not pooling.
     */
    public void releaseToPool() {
        // need to clear all elements so we don't leak memory
        int maxIndex = index.get();
        int realIndex = 0;
        ElementSection<E> section = elements;
        outer: while (section != null) {
            for (int i = 0; i < SIZE; i++, realIndex++) {
                if (realIndex >= maxIndex) {
                    section = null;
                    break outer;
                }
                // we can use lazySet here because we are nulling things out and not accessing them again
                // (relative on Mac Intel i7) lazySet gets us ~30m vs ~26m ops/second in the JMH test (100 adds per release)
                section.array.set(i, null);
            }
            section = section.next.get();
        }

        index.set(0);
        removedIndex.set(0);
        POOL.returnObject(this);
    }

    @Override
    public void unsubscribe() {
        releaseToPool();
    }

    private IndexedRingBuffer() {
    }

    /**
     * Add an element and return the index where it was added to allow removal.
     * 
     * @param e
     * @return
     */
    public int add(E e) {
        int i = getIndexForAdd();
        if (i < SIZE) {
            // fast-path when we are in the first section
            elements.array.set(i, e);
            return i;
        } else {
            int sectionIndex = i % SIZE;
            getElementSection(i).array.set(sectionIndex, e);
            return i;
        }
    }

    public E remove(int index) {
        E e;
        if (index < SIZE) {
            // fast-path when we are in the first section
            e = elements.array.getAndSet(index, null);
        } else {
            int sectionIndex = index % SIZE;
            e = getElementSection(index).array.getAndSet(sectionIndex, null);
        }
        pushRemovedIndex(index);
        return e;
    }

    private IndexSection getIndexSection(int index) {
        // short-cut the normal case
        if (index < SIZE) {
            return removed;
        }

        // if we have passed the first array we get more complicated and do recursive chaining
        int numSections = index / SIZE;
        IndexSection a = removed;
        for (int i = 0; i < numSections; i++) {
            a = a.getNext();
        }
        return a;
    }

    private ElementSection<E> getElementSection(int index) {
        // short-cut the normal case
        if (index < SIZE) {
            return elements;
        }

        // if we have passed the first array we get more complicated and do recursive chaining
        int numSections = index / SIZE;
        ElementSection<E> a = elements;
        for (int i = 0; i < numSections; i++) {
            a = a.getNext();
        }
        return a;
    }

    private synchronized int getIndexForAdd() {
        /*
         * Synchronized as I haven't yet figured out a way to do this in an atomic way that doesn't involve object allocation
         */
        int i;
        int ri = getIndexFromPreviouslyRemoved();
        if (ri >= 0) {
            if (ri < SIZE) {
                // fast-path when we are in the first section
                i = removed.getAndSet(ri, -1);
            } else {
                int sectionIndex = ri % SIZE;
                i = getIndexSection(ri).getAndSet(sectionIndex, -1);
            }
            if (i == index.get()) {
                // if it was the last index removed, when we pick it up again we want to increment
                index.getAndIncrement();
            }
        } else {
            i = index.getAndIncrement();
        }
        return i;
    }

    /**
     * Returns -1 if nothing, 0 or greater if the index should be used
     * 
     * @return
     */
    private synchronized int getIndexFromPreviouslyRemoved() {
        /*
         * Synchronized as I haven't yet figured out a way to do this in an atomic way that doesn't involve object allocation
         */

        // loop because of CAS
        while (true) {
            int currentRi = removedIndex.get();
            if (currentRi > 0) {
                // claim it
                if (removedIndex.compareAndSet(currentRi, currentRi - 1)) {
                    return currentRi - 1;
                }
            } else {
                // do nothing
                return -1;
            }
        }
    }

    private synchronized void pushRemovedIndex(int elementIndex) {
        /*
         * Synchronized as I haven't yet figured out a way to do this in an atomic way that doesn't involve object allocation
         */

        int i = removedIndex.getAndIncrement();
        if (i < SIZE) {
            // fast-path when we are in the first section
            removed.set(i, elementIndex);
        } else {
            int sectionIndex = i % SIZE;
            getIndexSection(i).set(sectionIndex, elementIndex);
        }
    }

    @Override
    public boolean isUnsubscribed() {
        return false;
    }

    public int forEach(Func1<? super E, Boolean> action) {
        return forEach(action, 0);
    }

    /**
     * 
     * @param action
     *            that processes each item and returns true if it wants to continue to the next
     * @return int of next index to process, or last index seen if it exited early
     */
    public int forEach(Func1<? super E, Boolean> action, int startIndex) {
        int endedAt = forEach(action, startIndex, index.get());
        if (startIndex > 0 && endedAt == index.get()) {
            // start at the beginning again and go up to startIndex
            endedAt = forEach(action, 0, startIndex);
        } else if (endedAt == index.get()) {
            // start back at the beginning
            endedAt = 0;
        }
        return endedAt;
    }

    private int forEach(Func1<? super E, Boolean> action, int startIndex, int endIndex) {
        int lastIndex = startIndex;
        int maxIndex = index.get();
        int realIndex = startIndex;
        ElementSection<E> section = elements;

        if (startIndex >= SIZE) {
            // move into the correct section
            section = getElementSection(startIndex);
            startIndex = startIndex % SIZE;
        }

        outer: while (section != null) {
            for (int i = startIndex; i < SIZE; i++, realIndex++) {
                if (realIndex >= maxIndex || realIndex >= endIndex) {
                    section = null;
                    break outer;
                }
                E element = section.array.get(i);
                if (element == null) {
                    continue;
                }
                lastIndex = realIndex;
                boolean continueLoop = action.call(element);
                if (!continueLoop) {
                    return lastIndex;
                }
            }
            section = section.next.get();
            startIndex = 0; // reset to start for next section
        }

        // return the OutOfBounds index position if we processed all of them ... the one we should be less-than
        return realIndex;
    }

    private static class ElementSection<E> {
        private final AtomicReferenceArray<E> array = new AtomicReferenceArray<E>(SIZE);
        private final AtomicReference<ElementSection<E>> next = new AtomicReference<ElementSection<E>>();

        ElementSection<E> getNext() {
            if (next.get() != null) {
                return next.get();
            } else {
                ElementSection<E> newSection = new ElementSection<E>();
                if (next.compareAndSet(null, newSection)) {
                    // we won
                    return newSection;
                } else {
                    // we lost so get the value that won
                    return next.get();
                }
            }
        }
    }

    private static class IndexSection {

        private final AtomicIntegerArray unsafeArray = new AtomicIntegerArray(SIZE);

        public int getAndSet(int expected, int newValue) {
            return unsafeArray.getAndSet(expected, newValue);
        }

        public void set(int i, int elementIndex) {
            unsafeArray.set(i, elementIndex);
        }

        private final AtomicReference<IndexSection> _next = new AtomicReference<IndexSection>();

        IndexSection getNext() {
            if (_next.get() != null) {
                return _next.get();
            } else {
                IndexSection newSection = new IndexSection();
                if (_next.compareAndSet(null, newSection)) {
                    // we won
                    return newSection;
                } else {
                    // we lost so get the value that won
                    return _next.get();
                }
            }
        }

    }

}
