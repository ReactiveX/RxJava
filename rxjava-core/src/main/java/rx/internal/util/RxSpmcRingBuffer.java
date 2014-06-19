package rx.internal.util;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.MissingBackpressureException;
import rx.internal.operators.NotificationLite;
import rx.internal.util.jctools.SpmcArrayQueue;

public class RxSpmcRingBuffer implements RxRingBuffer, Subscription {

    private static final ObjectPool<RxSpmcRingBuffer> POOL = new ObjectPool<RxSpmcRingBuffer>() {

        @Override
        protected RxSpmcRingBuffer createObject() {
            return new RxSpmcRingBuffer();
        }

    };

    public final static RxSpmcRingBuffer getInstance() {
        return POOL.borrowObject();
    }

    private static final NotificationLite<Object> on = NotificationLite.instance();

    /**
     * Using ArrayBlockingQueue
     * <pre> {@code
     * 
     * Benchmark                              (size)   Mode   Samples         Mean   Mean error    Units
     * r.i.u.PerfRingBuffer.onNextConsume        100  thrpt         5   208825.389     4776.127    ops/s
     * r.i.u.PerfRingBuffer.onNextConsume       1023  thrpt         5    21480.352      140.259    ops/s
     * r.i.u.PerfRingBuffer.onNextPollLoop       100  thrpt         5   196311.218     9050.104    ops/s
     * r.i.u.PerfRingBuffer.onNextPollLoop   1000000  thrpt         5       22.498        0.403    ops/s
     * 
     * Using SpscArrayQueue
     * 
     * Benchmark                              (size)   Mode   Samples         Mean   Mean error    Units
     * r.i.u.PerfRingBuffer.onNextConsume        100  thrpt         5   548886.166    21575.383    ops/s
     * r.i.u.PerfRingBuffer.onNextConsume       1023  thrpt         5    63886.154     1451.675    ops/s
     * r.i.u.PerfRingBuffer.onNextPollLoop       100  thrpt         5   599163.735    11827.534    ops/s
     * r.i.u.PerfRingBuffer.onNextPollLoop   1000000  thrpt         5       77.983        2.202    ops/s
     * 
     * Using SpmcArrayQueue
     * 
     * Benchmark                              (size)   Mode   Samples         Mean   Mean error    Units
     * r.i.u.PerfRingBuffer.onNextConsume        100  thrpt         5   393642.647    10394.942    ops/s
     * r.i.u.PerfRingBuffer.onNextConsume       1023  thrpt         5    46423.593      277.894    ops/s
     * r.i.u.PerfRingBuffer.onNextPollLoop       100  thrpt         5   337851.387     4776.951    ops/s
     * r.i.u.PerfRingBuffer.onNextPollLoop   1000000  thrpt         5       38.273        0.663    ops/s
     * 
     * } </pre>
     */

    // despite performance hit this is using Spmc instead of Spsc to allow thread-stealing algorithms, such as in the merge operator where consumption can come from any thread
    private final Queue<Object> queue;

    private final int size;
    private final int requestThreshold;
    private volatile Object terminalState;
    public volatile int outstandingRequests = 0;

    // NOTE: if anything is added here, make sure it is cleared in `unsubscribe` as this object is reused
    private static final AtomicIntegerFieldUpdater<RxSpmcRingBuffer> OUTSTANDING_REQUEST_UPDATER = AtomicIntegerFieldUpdater.newUpdater(RxSpmcRingBuffer.class, "outstandingRequests");

    private RxSpmcRingBuffer(int size, int threshold) {
        queue = new SpmcArrayQueue<Object>(size);
        this.size = size;
        this.requestThreshold = size - threshold;
    }

    /* for unit tests */RxSpmcRingBuffer() {
        this(SIZE, THRESHOLD);
    }

    /**
     * Directly emit to `child.onNext` while also decrementing the request counters used by `requestIfNeeded`.
     * 
     * @param o
     * @param child
     */
    public void emitWithoutQueue(Object o, Observer child) {
        OUTSTANDING_REQUEST_UPDATER.decrementAndGet(this);
        if (o == null) {
            // this means a value has been given to us without being turned into a NULL_SENTINEL
            child.onNext(null);
        } else {
            on.accept(child, o);
        }
    }

    /**
     * 
     * @param o
     * @throws MissingBackpressureException
     *             if more onNext are sent than have been requested
     */
    public void onNext(Object o) throws MissingBackpressureException {
        // we received a requested item
        OUTSTANDING_REQUEST_UPDATER.decrementAndGet(this);
        int a = available();
        if (!queue.offer(on.next(o))) {
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

    public int count() {
        return queue.size();
    }

    public int available() {
        return size - count();
    }

    public int requested() {
        // it can be 0 if onNext are sent without having been requested
        return Math.max(0, outstandingRequests);
    }

    public int capacity() {
        return size;
    }

    public Object poll() {
        Object o = queue.poll();
        if (o == null && terminalState != null) {
            o = terminalState;
            // once emitted we clear so a poll loop will finish
            terminalState = null;
        }
        return o;
    }

    public boolean isCompleted(Object o) {
        return on.isCompleted(o);
    }

    public boolean isError(Object o) {
        return on.isError(o);
    }

    public void accept(Object o, Observer child) {
        on.accept(child, o);
    }

    public Throwable asError(Object o) {
        return on.getError(o);
    }

    public void requestIfNeeded(Subscriber<?> s) {
        /**
         * Example behavior:
         * 
         * <pre> {@code
         * outstanding  0      available    1024
         * outstanding 1024    available    1024
         *     onNext
         * outstanding 1023    available    1023
         *     onNext  
         * outstanding 1022    available    1022
         *     poll
         * outstanding 1022    available    1023
         *     onNext * 1000
         * outstanding 22      available    23
         *     poll * 100
         * outstanding 22      available    123
         *     poll * 500
         * outstanding 22      available    523 
         *     request a523 - o22 = 501
         * outstanding 523     available    523 
         *     onNext * 523
         * outstanding 0       available    0
         *     request a0 - o0 = 0
         *     poll 1024
         * outstanding 0       available    1024
         *     request a1024 - o0 = 1024
         * outstanding 1024    available    1024
         * } </pre>
         */
        do {
            int a = available();
            int _o = outstandingRequests;
            int o = _o;
            if (o < 0) {
                o = 0;
            }
            int r = a - o;
            if (r > requestThreshold) {
                int toSet = r + o;
                if (OUTSTANDING_REQUEST_UPDATER.compareAndSet(this, _o, toSet)) {
                    s.request(r - 1); // -1 is solving an off-by-one bug somewhere I can't figure out ... OperatorMergeTest.testConcurrencyWithBrokenOnCompleteContract fails without this
                    return;
                }
            } else {
                // nothing to request
                return;
            }
        } while (true);
    }

    @Override
    public void unsubscribe() {
        queue.clear();
        terminalState = null;
        outstandingRequests = 0;
        POOL.returnObject(this);
    }

    @Override
    public boolean isUnsubscribed() {
        return false;
    }
}
