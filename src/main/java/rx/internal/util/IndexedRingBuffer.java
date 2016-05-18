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
public final class IndexedRingBuffer<E> implements Subscription {

    private static final ObjectPool<IndexedRingBuffer<?>> POOL = new ObjectPool<IndexedRingBuffer<?>>() {

        @Override
        protected IndexedRingBuffer<?> createObject() {
            return new IndexedRingBuffer<Object>();
        }

    };

    @SuppressWarnings("unchecked")
    public static <T> IndexedRingBuffer<T> getInstance() {
        return (IndexedRingBuffer<T>) POOL.borrowObject();
    }

    private final ElementSection<E> elements = new ElementSection<E>();
    private final IndexSection removed = new IndexSection();
    /* package for unit testing */final AtomicInteger index = new AtomicInteger();
    /* package for unit testing */final AtomicInteger removedIndex = new AtomicInteger();
    
    // default size of ring buffer
    /**
     * Set at 256 ... Android defaults far smaller which likely will never hit the use cases that require the higher buffers.
     * <p>
     * The 10000 size test represents something that should be a rare use case (merging 10000 concurrent Observables for example) 
     * 
     * <pre> {@code
     * ./gradlew benchmarks '-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*IndexedRingBufferPerf.*'
     * 
     * 1024
     * 
     * Benchmark                                              (size)   Mode   Samples        Score  Score error    Units
     * r.i.IndexedRingBufferPerf.indexedRingBufferAdd            100  thrpt         5   269292.006     6013.347    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAdd          10000  thrpt         5     2217.103      163.396    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove      100  thrpt         5   139349.608     9397.232    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove    10000  thrpt         5     1045.323       30.991    ops/s
     * 
     * 512
     * 
     * Benchmark                                              (size)   Mode   Samples        Score  Score error    Units
     * r.i.IndexedRingBufferPerf.indexedRingBufferAdd            100  thrpt         5   270919.870     5381.793    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAdd          10000  thrpt         5     1724.436       42.287    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove      100  thrpt         5   141478.813     3696.030    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove    10000  thrpt         5      719.447       75.629    ops/s
     * 
     * 
     * 256
     * 
     * Benchmark                                              (size)   Mode   Samples        Score  Score error    Units
     * r.i.IndexedRingBufferPerf.indexedRingBufferAdd            100  thrpt         5   272042.605     7954.982    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAdd          10000  thrpt         5     1101.329       23.566    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove      100  thrpt         5   140479.804     6389.060    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove    10000  thrpt         5      397.306       24.222    ops/s
     * 
     * 128
     * 
     * Benchmark                                              (size)   Mode   Samples        Score  Score error    Units
     * r.i.IndexedRingBufferPerf.indexedRingBufferAdd            100  thrpt         5   263065.312    11168.941    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAdd          10000  thrpt         5      581.708       17.397    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove      100  thrpt         5   138051.488     4618.935    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove    10000  thrpt         5      176.873       35.669    ops/s
     * 
     * 32
     * 
     * Benchmark                                              (size)   Mode   Samples        Score  Score error    Units
     * r.i.IndexedRingBufferPerf.indexedRingBufferAdd            100  thrpt         5   250737.473    17260.148    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAdd          10000  thrpt         5      144.725       26.284    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove      100  thrpt         5   118832.832     9082.658    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove    10000  thrpt         5       32.133        8.048    ops/s
     * 
     * 8
     * 
     * Benchmark                                              (size)   Mode   Samples        Score  Score error    Units
     * r.i.IndexedRingBufferPerf.indexedRingBufferAdd            100  thrpt         5   209192.847    25558.124    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAdd          10000  thrpt         5       26.520        3.100    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove      100  thrpt         5   100200.463     1854.259    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove    10000  thrpt         5        8.456        2.114    ops/s
     * 
     * 2
     * 
     * Benchmark                                              (size)   Mode   Samples        Score  Score error    Units
     * r.i.IndexedRingBufferPerf.indexedRingBufferAdd            100  thrpt         5    96549.208     4427.239    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAdd          10000  thrpt         5        6.637        2.025    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove      100  thrpt         5    34553.169     4904.197    ops/s
     * r.i.IndexedRingBufferPerf.indexedRingBufferAddRemove    10000  thrpt         5        2.159        0.700    ops/s
     * } </pre>
     * 
     * Impact of IndexedRingBuffer size on merge
     * 
     * <pre> {@code
     * ./gradlew benchmarks '-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*OperatorMergePerf.*'
     * 
     * 512
     * 
     * Benchmark                                          (size)   Mode   Samples        Score  Score error    Units
     * r.o.OperatorMergePerf.merge1SyncStreamOfN               1  thrpt         5  5282500.038   530541.761    ops/s
     * r.o.OperatorMergePerf.merge1SyncStreamOfN            1000  thrpt         5    49327.272     6382.189    ops/s
     * r.o.OperatorMergePerf.merge1SyncStreamOfN         1000000  thrpt         5       53.025        4.724    ops/s
     * r.o.OperatorMergePerf.mergeNAsyncStreamsOfN             1  thrpt         5    97395.148     2489.303    ops/s
     * r.o.OperatorMergePerf.mergeNAsyncStreamsOfN          1000  thrpt         5        4.723        1.479    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1              1  thrpt         5  4534067.250   116321.725    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1            100  thrpt         5   458561.098    27652.081    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1           1000  thrpt         5    43267.381     2648.107    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOfN              1  thrpt         5  5581051.672   144191.849    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOfN           1000  thrpt         5       50.643        4.354    ops/s
     * r.o.OperatorMergePerf.mergeTwoAsyncStreamsOfN           1  thrpt         5    76437.644      959.748    ops/s
     * r.o.OperatorMergePerf.mergeTwoAsyncStreamsOfN        1000  thrpt         5     2965.306      272.928    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1         1  thrpt         5  5026522.098   364196.255    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1      1000  thrpt         5    34926.819      938.612    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1   1000000  thrpt         5       33.342        1.701    ops/s
     * 
     * 
     * 128
     * 
     * Benchmark                                          (size)   Mode   Samples        Score  Score error    Units
     * r.o.OperatorMergePerf.merge1SyncStreamOfN               1  thrpt         5  5144891.776   271990.561    ops/s
     * r.o.OperatorMergePerf.merge1SyncStreamOfN            1000  thrpt         5    53580.161     2370.204    ops/s
     * r.o.OperatorMergePerf.merge1SyncStreamOfN         1000000  thrpt         5       53.265        2.236    ops/s
     * r.o.OperatorMergePerf.mergeNAsyncStreamsOfN             1  thrpt         5    96634.426     1417.430    ops/s
     * r.o.OperatorMergePerf.mergeNAsyncStreamsOfN          1000  thrpt         5        4.648        0.255    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1              1  thrpt         5  4601280.220    53157.938    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1            100  thrpt         5   463394.568    58612.882    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1           1000  thrpt         5    50503.565     2394.168    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOfN              1  thrpt         5  5490315.842   228654.817    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOfN           1000  thrpt         5       50.661        3.385    ops/s
     * r.o.OperatorMergePerf.mergeTwoAsyncStreamsOfN           1  thrpt         5    74716.169     7413.642    ops/s
     * r.o.OperatorMergePerf.mergeTwoAsyncStreamsOfN        1000  thrpt         5     3009.476      277.075    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1         1  thrpt         5  4953313.642   307512.126    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1      1000  thrpt         5    35335.579     2368.377    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1   1000000  thrpt         5       37.450        0.655    ops/s
     * 
     * 32
     * 
     * Benchmark                                          (size)   Mode   Samples        Score  Score error    Units
     * r.o.OperatorMergePerf.merge1SyncStreamOfN               1  thrpt         5  4975957.497   365423.694    ops/s
     * r.o.OperatorMergePerf.merge1SyncStreamOfN            1000  thrpt         5    52141.226     5056.658    ops/s
     * r.o.OperatorMergePerf.merge1SyncStreamOfN         1000000  thrpt         5       53.663        2.671    ops/s
     * r.o.OperatorMergePerf.mergeNAsyncStreamsOfN             1  thrpt         5    96507.893     1833.371    ops/s
     * r.o.OperatorMergePerf.mergeNAsyncStreamsOfN          1000  thrpt         5        4.850        0.782    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1              1  thrpt         5  4557128.302   118516.934    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1            100  thrpt         5   339005.037    10594.737    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1           1000  thrpt         5    50781.535     6071.787    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOfN              1  thrpt         5  5604920.068   209285.840    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOfN           1000  thrpt         5       50.413        7.496    ops/s
     * r.o.OperatorMergePerf.mergeTwoAsyncStreamsOfN           1  thrpt         5    76098.942      558.187    ops/s
     * r.o.OperatorMergePerf.mergeTwoAsyncStreamsOfN        1000  thrpt         5     2988.137      193.255    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1         1  thrpt         5  5177255.256   150253.086    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1      1000  thrpt         5    34772.490      909.967    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1   1000000  thrpt         5       34.847        0.606    ops/s
     * 
     * 8
     * 
     * Benchmark                                          (size)   Mode   Samples        Score  Score error    Units
     * r.o.OperatorMergePerf.merge1SyncStreamOfN               1  thrpt         5  5027331.903   337986.410    ops/s
     * r.o.OperatorMergePerf.merge1SyncStreamOfN            1000  thrpt         5    51746.540     3585.450    ops/s
     * r.o.OperatorMergePerf.merge1SyncStreamOfN         1000000  thrpt         5       52.682        4.026    ops/s
     * r.o.OperatorMergePerf.mergeNAsyncStreamsOfN             1  thrpt         5    96805.587     2868.112    ops/s
     * r.o.OperatorMergePerf.mergeNAsyncStreamsOfN          1000  thrpt         5        4.598        0.290    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1              1  thrpt         5  4390912.630   300687.310    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1            100  thrpt         5   458615.731    56125.958    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1           1000  thrpt         5    49033.105     6132.936    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOfN              1  thrpt         5  5090614.100   649439.778    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOfN           1000  thrpt         5       48.548        3.586    ops/s
     * r.o.OperatorMergePerf.mergeTwoAsyncStreamsOfN           1  thrpt         5    72285.482    16820.952    ops/s
     * r.o.OperatorMergePerf.mergeTwoAsyncStreamsOfN        1000  thrpt         5     2981.576      316.140    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1         1  thrpt         5  4993609.293   267975.397    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1      1000  thrpt         5    33228.972     1554.924    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1   1000000  thrpt         5       32.994        3.615    ops/s
     * 
     * 
     * 2
     * 
     * Benchmark                                          (size)   Mode   Samples        Score  Score error    Units
     * r.o.OperatorMergePerf.merge1SyncStreamOfN               1  thrpt         5  5103812.234   939461.192    ops/s
     * r.o.OperatorMergePerf.merge1SyncStreamOfN            1000  thrpt         5    51491.116     3790.056    ops/s
     * r.o.OperatorMergePerf.merge1SyncStreamOfN         1000000  thrpt         5       54.043        2.340    ops/s
     * r.o.OperatorMergePerf.mergeNAsyncStreamsOfN             1  thrpt         5    96575.834    13416.541    ops/s
     * r.o.OperatorMergePerf.mergeNAsyncStreamsOfN          1000  thrpt         5        4.740        0.047    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1              1  thrpt         5  4435909.832   899133.671    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1            100  thrpt         5   392382.445    59814.783    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOf1           1000  thrpt         5    50429.258     7489.849    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOfN              1  thrpt         5  5637321.803   161838.195    ops/s
     * r.o.OperatorMergePerf.mergeNSyncStreamsOfN           1000  thrpt         5       51.065        2.138    ops/s
     * r.o.OperatorMergePerf.mergeTwoAsyncStreamsOfN           1  thrpt         5    76366.764     2631.710    ops/s
     * r.o.OperatorMergePerf.mergeTwoAsyncStreamsOfN        1000  thrpt         5     2978.302      296.418    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1         1  thrpt         5  5280829.290  1602542.493    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1      1000  thrpt         5    35070.518     3565.672    ops/s
     * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1   1000000  thrpt         5       34.501        0.991    ops/s
     * 
     * } </pre>
     */
    static int _size = 256;
    static {
        // lower default for Android (https://github.com/ReactiveX/RxJava/issues/1820)
        if (PlatformDependent.isAndroid()) {
            _size = 8;
        }

        // possible system property for overriding
        String sizeFromProperty = System.getProperty("rx.indexed-ring-buffer.size"); // also see RxRingBuffer
        if (sizeFromProperty != null) {
            try {
                _size = Integer.parseInt(sizeFromProperty);
            } catch (Exception e) {
                System.err.println("Failed to set 'rx.indexed-ring-buffer.size' with value " + sizeFromProperty + " => " + e.getMessage());
            }
        }
    }
    
    /* package for unit testing */static final int SIZE = _size;

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

    IndexedRingBuffer() {
    }

    /**
     * Add an element and return the index where it was added to allow removal.
     * 
     * @param e the element to add
     * @return the index where the element was added
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
     * Loop through each element in the buffer and call a specific function.
     * @param action
     *            that processes each item and returns true if it wants to continue to the next
     * @param startIndex at which index the loop should start
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
        final AtomicReferenceArray<E> array = new AtomicReferenceArray<E>(SIZE);
        final AtomicReference<ElementSection<E>> next = new AtomicReference<ElementSection<E>>();

        ElementSection() {
        }

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

        IndexSection() {
        }

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
