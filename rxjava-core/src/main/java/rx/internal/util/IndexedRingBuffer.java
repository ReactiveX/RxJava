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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import rx.Subscription;
import rx.functions.Action1;

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
 * Benchmark                                                (size)   Mode   Samples         Mean   Mean error    Units
 * r.i.u.PerfIndexedRingBuffer.indexedRingBufferAdd            100  thrpt         5   307403.329    17487.185    ops/s
 * r.i.u.PerfIndexedRingBuffer.indexedRingBufferAdd          10000  thrpt         5     1819.151      764.603    ops/s
 * r.i.u.PerfIndexedRingBuffer.indexedRingBufferAddRemove      100  thrpt         5   149649.075     4765.899    ops/s
 * r.i.u.PerfIndexedRingBuffer.indexedRingBufferAddRemove    10000  thrpt         5      825.304       14.079    ops/s
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
            section = section.next;
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
        try {
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
        } catch (NullPointerException ne) {
            ne.printStackTrace();
            throw ne;
        }
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
                i = removed.array.getAndSet(ri, -1);
            } else {
                int sectionIndex = ri % SIZE;
                i = getIndexSection(ri).array.getAndSet(sectionIndex, -1);
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
            removed.array.set(i, elementIndex);
        } else {
            int sectionIndex = i % SIZE;
            getIndexSection(i).array.set(sectionIndex, elementIndex);
        }
    }

    @Override
    public boolean isUnsubscribed() {
        return false;
    }

    public List<Throwable> forEach(Action1<? super E> action) {
        List<Throwable> es = null;

        int maxIndex = index.get();
        int realIndex = 0;
        ElementSection<E> section = elements;
        outer: while (section != null) {
            for (int i = 0; i < SIZE; i++, realIndex++) {
                if (realIndex >= maxIndex) {
                    section = null;
                    break outer;
                }
                E element = section.array.get(i);
                if (element == null) {
                    continue;
                }
                try {
                    action.call(element);
                } catch (Throwable e) {
                    if (es == null) {
                        es = new ArrayList<Throwable>();
                    }
                    es.add(e);
                }
            }
            section = section.next;
        }

        if (es == null) {
            return Collections.emptyList();
        } else {
            return es;
        }
    }

    private static class ElementSection<E> {
        final AtomicReferenceArray<E> array = new AtomicReferenceArray<E>(SIZE);
        volatile ElementSection<E> next;
        private static final long _nextOffset;

        static {
            try {
                _nextOffset = UnsafeAccess.UNSAFE.objectFieldOffset(ElementSection.class.getDeclaredField("next"));
            } catch (Exception ex) {
                throw new Error(ex);
            }
        }

        ElementSection<E> getNext() {
            if (next != null) {
                return next;
            } else {
                ElementSection<E> newSection = new ElementSection<E>();
                if (UnsafeAccess.UNSAFE.compareAndSwapObject(this, _nextOffset, null, newSection)) {
                    // we won
                    return newSection;
                } else {
                    // we lost so get the value that won
                    return next;
                }
            }
        }
    }

    private static class IndexSection {
        final AtomicIntReferenceArray array = new AtomicIntReferenceArray(SIZE);
        private volatile IndexSection next;
        private static final long _nextOffset;

        static {
            try {
                _nextOffset = UnsafeAccess.UNSAFE.objectFieldOffset(IndexSection.class.getDeclaredField("next"));
            } catch (Exception ex) {
                throw new Error(ex);
            }
        }

        IndexSection getNext() {
            if (next != null) {
                return next;
            } else {
                IndexSection newSection = new IndexSection();
                if (UnsafeAccess.UNSAFE.compareAndSwapObject(this, _nextOffset, null, newSection)) {
                    // we won
                    return newSection;
                } else {
                    // we lost so get the value that won
                    return next;
                }
            }
        }
    }
}
