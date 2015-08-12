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

import java.util.Queue;

import rx.Observer;
import rx.Subscription;
import rx.exceptions.MissingBackpressureException;
import rx.internal.operators.NotificationLite;
import rx.internal.util.unsafe.SpmcArrayQueue;
import rx.internal.util.unsafe.SpscArrayQueue;
import rx.internal.util.unsafe.UnsafeAccess;

/**
 * This assumes Spsc or Spmc usage. This means only a single producer calling the on* methods. This is the Rx
 * contract of an Observer (see http://reactivex.io/documentation/contract.html). Concurrent invocations of
 * on* methods will not be thread-safe.
 */
public class RxRingBuffer implements Subscription {

    public static RxRingBuffer getSpscInstance() {
        if (UnsafeAccess.isUnsafeAvailable()) {
            return new RxRingBuffer(SPSC_POOL, SIZE);
        } else {
            return new RxRingBuffer();
        }
    }

    public static RxRingBuffer getSpmcInstance() {
        if (UnsafeAccess.isUnsafeAvailable()) {
            return new RxRingBuffer(SPMC_POOL, SIZE);
        } else {
            return new RxRingBuffer();
        }
    }

    /**
     * Queue implementation testing that led to current choices of data structures:
     * 
     * With synchronized LinkedList
     * <pre> {@code
     * Benchmark                                        Mode   Samples        Score  Score error    Units
     * r.i.RxRingBufferPerf.ringBufferAddRemove1       thrpt         5 19118392.046  1002814.238    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1000    thrpt         5    17891.641      252.747    ops/s
     * 
     * With MpscPaddedQueue (single consumer, so failing 1 unit test)
     * 
     * Benchmark                                        Mode   Samples        Score  Score error    Units
     * r.i.RxRingBufferPerf.ringBufferAddRemove1       thrpt         5 22164483.238  3035027.348    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1000    thrpt         5    23154.303      602.548    ops/s
     * 
     * 
     * With ConcurrentLinkedQueue (tracking count separately)
     * 
     * Benchmark                                        Mode   Samples        Score  Score error    Units
     * r.i.RxRingBufferPerf.ringBufferAddRemove1       thrpt         5 17353906.092   378756.411    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1000    thrpt         5    19224.411     1010.610    ops/s
     * 
     * With ConcurrentLinkedQueue (using queue.size() method for count)
     * 
     * Benchmark                                        Mode   Samples        Score  Score error    Units
     * r.i.RxRingBufferPerf.ringBufferAddRemove1       thrpt         5 23951121.098  1982380.330    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1000    thrpt         5     1142.351       33.592    ops/s
     * 
     * With SynchronizedQueue (synchronized LinkedList ... no object pooling)
     * 
     * r.i.RxRingBufferPerf.createUseAndDestroy1       thrpt         5 33231667.136   685757.510    ops/s
     * r.i.RxRingBufferPerf.createUseAndDestroy1000    thrpt         5    74623.614     5493.766    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1       thrpt         5 22907359.257   707026.632    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1000    thrpt         5    22222.410      320.829    ops/s
     * 
     * With ArrayBlockingQueue
     * 
     * Benchmark                                            Mode   Samples        Score  Score error    Units
     * r.i.RxRingBufferPerf.createUseAndDestroy1       thrpt         5  2389804.664    68990.804    ops/s
     * r.i.RxRingBufferPerf.createUseAndDestroy1000    thrpt         5    27384.274     1411.789    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1       thrpt         5 26497037.559    91176.247    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1000    thrpt         5    17985.144      237.771    ops/s
     * 
     * With ArrayBlockingQueue and Object Pool
     * 
     * Benchmark                                            Mode   Samples        Score  Score error    Units
     * r.i.RxRingBufferPerf.createUseAndDestroy1       thrpt         5 12465685.522   399070.770    ops/s
     * r.i.RxRingBufferPerf.createUseAndDestroy1000    thrpt         5    27701.294      395.217    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1       thrpt         5 26399625.086   695639.436    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1000    thrpt         5    17985.427      253.190    ops/s
     * 
     * With SpscArrayQueue (single consumer, so failing 1 unit test)
     *  - requires access to Unsafe
     * 
     * Benchmark                                        Mode   Samples        Score  Score error    Units
     * r.i.RxRingBufferPerf.createUseAndDestroy1       thrpt         5  1922996.035    49183.766    ops/s
     * r.i.RxRingBufferPerf.createUseAndDestroy1000    thrpt         5    70890.186     1382.550    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1       thrpt         5 80637811.605  3509706.954    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1000    thrpt         5    71822.453     4127.660    ops/s
     * 
     * 
     * With SpscArrayQueue and Object Pool (object pool improves createUseAndDestroy1 by 10x)
     * 
     * Benchmark                                        Mode   Samples        Score  Score error    Units
     * r.i.RxRingBufferPerf.createUseAndDestroy1       thrpt         5 25220069.264  1329078.785    ops/s
     * r.i.RxRingBufferPerf.createUseAndDestroy1000    thrpt         5    72313.457     3535.447    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1       thrpt         5 81863840.884  2191416.069    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1000    thrpt         5    73140.822     1528.764    ops/s
     * 
     * With SpmcArrayQueue
     *  - requires access to Unsafe
     *  
     * Benchmark                                            Mode   Samples        Score  Score error    Units
     * r.i.RxRingBufferPerf.spmcCreateUseAndDestroy1       thrpt         5 27630345.474   769219.142    ops/s
     * r.i.RxRingBufferPerf.spmcCreateUseAndDestroy1000    thrpt         5    80052.046     4059.541    ops/s
     * r.i.RxRingBufferPerf.spmcRingBufferAddRemove1       thrpt         5 44449524.222   563068.793    ops/s
     * r.i.RxRingBufferPerf.spmcRingBufferAddRemove1000    thrpt         5    65231.253     1805.732    ops/s
     * 
     * With SpmcArrayQueue and ObjectPool (object pool improves createUseAndDestroy1 by 10x)
     * 
     * Benchmark                                        Mode   Samples        Score  Score error    Units
     * r.i.RxRingBufferPerf.createUseAndDestroy1       thrpt         5 18489343.061  1011872.825    ops/s
     * r.i.RxRingBufferPerf.createUseAndDestroy1000    thrpt         5    46416.434     1439.144    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove        thrpt         5 38280945.847  1071801.279    ops/s
     * r.i.RxRingBufferPerf.ringBufferAddRemove1000    thrpt         5    42337.663     1052.231    ops/s
     * 
     * --------------
     * 
     * When UnsafeAccess.isUnsafeAvailable() == true we can use the Spmc/SpscArrayQueue implementations.
     * 
     * } </pre>
     */

    private static final NotificationLite<Object> on = NotificationLite.instance();

    private Queue<Object> queue;

    private final int size;
    private final ObjectPool<Queue<Object>> pool;

    /**
     * We store the terminal state separately so it doesn't count against the size.
     * We don't just +1 the size since some of the queues require sizes that are a power of 2.
     * This is a subjective thing ... wanting to keep the size (ie 1024) the actual number of onNext
     * that can be sent rather than something like 1023 onNext + 1 terminal event. It also simplifies
     * checking that we have received only 1 terminal event, as we don't need to peek at the last item
     * or retain a boolean flag.
     */
    public volatile Object terminalState;

    // default size of ring buffer
    /**
     * 128 was chosen as the default based on the numbers below. A stream processing system may benefit from increasing to 512+.
     * 
     * <pre> {@code
     * ./gradlew benchmarks '-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*OperatorObserveOnPerf.*'
     * 
     * 1024
     * 
     * Benchmark                                         (size)   Mode   Samples        Score  Score error    Units
     * r.o.OperatorObserveOnPerf.observeOnComputation         1  thrpt         5   100642.874    24676.478    ops/s
     * r.o.OperatorObserveOnPerf.observeOnComputation      1000  thrpt         5     4095.901       90.730    ops/s
     * r.o.OperatorObserveOnPerf.observeOnComputation   1000000  thrpt         5        9.797        4.982    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate           1  thrpt         5 15536155.489   758579.454    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate        1000  thrpt         5   156257.341     6324.176    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate     1000000  thrpt         5      157.099        7.143    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread           1  thrpt         5    16864.641     1826.877    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread        1000  thrpt         5     4269.317      169.480    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread     1000000  thrpt         5       13.393        1.047    ops/s
     * 
     * 512
     * 
     * Benchmark                                         (size)   Mode   Samples        Score  Score error    Units
     * r.o.OperatorObserveOnPerf.observeOnComputation         1  thrpt         5    98945.980    48050.282    ops/s
     * r.o.OperatorObserveOnPerf.observeOnComputation      1000  thrpt         5     4111.149       95.987    ops/s
     * r.o.OperatorObserveOnPerf.observeOnComputation   1000000  thrpt         5       12.483        3.067    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate           1  thrpt         5 16032469.143   620157.818    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate        1000  thrpt         5   157997.290     5097.718    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate     1000000  thrpt         5      156.462        7.728    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread           1  thrpt         5    15813.984     8260.170    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread        1000  thrpt         5     4358.334      251.609    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread     1000000  thrpt         5       13.647        0.613    ops/s
     * 
     * 256
     * 
     * Benchmark                                         (size)   Mode   Samples        Score  Score error    Units
     * r.o.OperatorObserveOnPerf.observeOnComputation         1  thrpt         5   108489.834     2688.489    ops/s
     * r.o.OperatorObserveOnPerf.observeOnComputation      1000  thrpt         5     4526.674      728.019    ops/s
     * r.o.OperatorObserveOnPerf.observeOnComputation   1000000  thrpt         5       13.372        0.457    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate           1  thrpt         5 16435113.709   311602.627    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate        1000  thrpt         5   157611.204    13146.108    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate     1000000  thrpt         5      158.346        2.500    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread           1  thrpt         5    16976.775      968.191    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread        1000  thrpt         5     6238.210     2060.387    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread     1000000  thrpt         5       13.465        0.566    ops/s
     * 
     * 128
     * 
     * Benchmark                                         (size)   Mode   Samples        Score  Score error    Units
     * r.o.OperatorObserveOnPerf.observeOnComputation         1  thrpt         5   106887.027    29307.913    ops/s
     * r.o.OperatorObserveOnPerf.observeOnComputation      1000  thrpt         5     6713.891      202.989    ops/s
     * r.o.OperatorObserveOnPerf.observeOnComputation   1000000  thrpt         5       11.929        0.187    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate           1  thrpt         5 16055774.724   350633.068    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate        1000  thrpt         5   153403.821    17976.156    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate     1000000  thrpt         5      153.559       20.178    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread           1  thrpt         5    17172.274      236.816    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread        1000  thrpt         5     7073.555      595.990    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread     1000000  thrpt         5       11.855        1.093    ops/s
     * 
     * 32
     * 
     * Benchmark                                         (size)   Mode   Samples        Score  Score error    Units
     * r.o.OperatorObserveOnPerf.observeOnComputation         1  thrpt         5   106128.589    20986.201    ops/s
     * r.o.OperatorObserveOnPerf.observeOnComputation      1000  thrpt         5     6396.607       73.627    ops/s
     * r.o.OperatorObserveOnPerf.observeOnComputation   1000000  thrpt         5        7.643        0.668    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate           1  thrpt         5 16012419.447   409004.521    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate        1000  thrpt         5   157907.001     5772.849    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate     1000000  thrpt         5      155.308       23.853    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread           1  thrpt         5    16927.513      606.692    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread        1000  thrpt         5     5191.084      244.876    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread     1000000  thrpt         5        8.288        0.217    ops/s
     * 
     * 16
     * 
     * Benchmark                                         (size)   Mode   Samples        Score  Score error    Units
     * r.o.OperatorObserveOnPerf.observeOnComputation         1  thrpt         5   109974.741      839.064    ops/s
     * r.o.OperatorObserveOnPerf.observeOnComputation      1000  thrpt         5     4538.912      173.561    ops/s
     * r.o.OperatorObserveOnPerf.observeOnComputation   1000000  thrpt         5        5.420        0.111    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate           1  thrpt         5 16017466.785   768748.695    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate        1000  thrpt         5   157934.065    13479.575    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate     1000000  thrpt         5      155.922       17.781    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread           1  thrpt         5    14903.686     3325.205    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread        1000  thrpt         5     3784.776     1054.131    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread     1000000  thrpt         5        5.624        0.130    ops/s
     * 
     * 2
     * 
     * Benchmark                                         (size)   Mode   Samples        Score  Score error    Units
     * r.o.OperatorObserveOnPerf.observeOnComputation         1  thrpt         5   112663.216      899.005    ops/s
     * r.o.OperatorObserveOnPerf.observeOnComputation      1000  thrpt         5      899.737        9.460    ops/s
     * r.o.OperatorObserveOnPerf.observeOnComputation   1000000  thrpt         5        0.999        0.100    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate           1  thrpt         5 16087325.336   783206.227    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate        1000  thrpt         5   156747.025     4880.489    ops/s
     * r.o.OperatorObserveOnPerf.observeOnImmediate     1000000  thrpt         5      156.645        3.810    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread           1  thrpt         5    15958.711      673.895    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread        1000  thrpt         5      884.624       47.692    ops/s
     * r.o.OperatorObserveOnPerf.observeOnNewThread     1000000  thrpt         5        1.173        0.100    ops/s
     * } </pre>
     */
    static int _size = 128;
    static {
        // lower default for Android (https://github.com/ReactiveX/RxJava/issues/1820)
        if (PlatformDependent.isAndroid()) {
            _size = 16;
        }

        // possible system property for overriding
        String sizeFromProperty = System.getProperty("rx.ring-buffer.size"); // also see IndexedRingBuffer
        if (sizeFromProperty != null) {
            try {
                _size = Integer.parseInt(sizeFromProperty);
            } catch (Exception e) {
                System.err.println("Failed to set 'rx.buffer.size' with value " + sizeFromProperty + " => " + e.getMessage());
            }
        }
    }
    public static final int SIZE = _size;

    /* Public so Schedulers can manage the lifecycle of the inner worker. */
    public static ObjectPool<Queue<Object>> SPSC_POOL = new ObjectPool<Queue<Object>>() {

        @Override
        protected SpscArrayQueue<Object> createObject() {
            return new SpscArrayQueue<Object>(SIZE);
        }

    };

    /* Public so Schedulers can manage the lifecycle of the inner worker. */
    public static ObjectPool<Queue<Object>> SPMC_POOL = new ObjectPool<Queue<Object>>() {

        @Override
        protected SpmcArrayQueue<Object> createObject() {
            return new SpmcArrayQueue<Object>(SIZE);
        }

    };
    
    private RxRingBuffer(Queue<Object> queue, int size) {
        this.queue = queue;
        this.pool = null;
        this.size = size;
    }

    private RxRingBuffer(ObjectPool<Queue<Object>> pool, int size) {
        this.pool = pool;
        this.queue = pool.borrowObject();
        this.size = size;
    }

    public synchronized void release() {
        Queue<Object> q = queue;
        ObjectPool<Queue<Object>> p = pool;
        if (p != null && q != null) {
            q.clear();
            queue = null;
            p.returnObject(q);
        }
    }

    @Override
    public void unsubscribe() {
        release();
    }

    /* package accessible for unit tests */RxRingBuffer() {
        this(new SynchronizedQueue<Object>(SIZE), SIZE);
    }

    /**
     * 
     * @param o
     * @throws MissingBackpressureException
     *             if more onNext are sent than have been requested
     */
    public void onNext(Object o) throws MissingBackpressureException {
        boolean iae = false;
        boolean mbe = false;
        synchronized (this) {
            Queue<Object> q = queue;
            if (q != null) {
                mbe = !q.offer(on.next(o));
            } else {
                iae = true;
            }
        }
        
        if (iae) {
            throw new IllegalStateException("This instance has been unsubscribed and the queue is no longer usable.");
        }
        if (mbe) {
            throw new MissingBackpressureException();
        }
    }

    public void onCompleted() {
        // we ignore terminal events if we already have one
        if (terminalState == null) {
            terminalState = on.completed();
        }
    }

    public void onError(Throwable t) {
        // we ignore terminal events if we already have one
        if (terminalState == null) {
            terminalState = on.error(t);
        }
    }

    public int available() {
        return size - count();
    }

    public int capacity() {
        return size;
    }

    public int count() {
        Queue<Object> q = queue;
        if (q == null) {
            return 0;
        }
        return q.size();
    }

    public boolean isEmpty() {
        Queue<Object> q = queue;
        if (q == null) {
            return true;
        }
        return q.isEmpty();
    }

    public Object poll() {
        Object o;
        synchronized (this) {
            Queue<Object> q = queue;
            if (q == null) {
                // we are unsubscribed and have released the underlying queue
                return null;
            }
            o = q.poll();
            
            Object ts = terminalState;
            if (o == null && ts != null && q.peek() == null) {
                o = ts;
                // once emitted we clear so a poll loop will finish
                terminalState = null;
            }
        }
        return o;
    }

    public Object peek() {
        Object o;
        synchronized (this) {
            Queue<Object> q = queue;
            if (q == null) {
                // we are unsubscribed and have released the underlying queue
                return null;
            }
            o = q.peek();
            Object ts = terminalState;
            if (o == null && ts != null && q.peek() == null) {
                o = ts;
            }
        }
        return o;
    }

    public boolean isCompleted(Object o) {
        return on.isCompleted(o);
    }

    public boolean isError(Object o) {
        return on.isError(o);
    }

    public Object getValue(Object o) {
        return on.getValue(o);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean accept(Object o, Observer child) {
        return on.accept(child, o);
    }

    public Throwable asError(Object o) {
        return on.getError(o);
    }

    @Override
    public boolean isUnsubscribed() {
        return queue == null;
    }

}