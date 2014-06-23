package rx.internal.util;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import rx.Observer;
import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.internal.operators.NotificationLite;

public class RxRingBuffer {

    public static RxRingBuffer getInstance() {
        return new RxRingBuffer();
    }

    private static final NotificationLite<Object> on = NotificationLite.instance();

    private final Queue<Object> queue;

    private final int size;
    private final int requestThreshold;
    private volatile Object terminalState;
    public volatile int outstandingRequests = 0;

    private static final AtomicIntegerFieldUpdater<RxRingBuffer> OUTSTANDING_REQUEST_UPDATER = AtomicIntegerFieldUpdater.newUpdater(RxRingBuffer.class, "outstandingRequests");

    public static final int SIZE = 1024;
    public static final int THRESHOLD = 256;

    private RxRingBuffer(int size, int threshold) {
        queue = new LinkedList<Object>();
        this.size = size;
        this.requestThreshold = size - threshold;
    }

    /* for unit tests */RxRingBuffer() {
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
        synchronized (queue) {
            if (queue.size() < SIZE) {
                queue.add(on.next(o));
            } else {
                throw new MissingBackpressureException();
            }
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
        synchronized (queue) {
            return queue.size();
        }
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
        Object o;
        synchronized (queue) {
            o = queue.poll();
        }
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

}
