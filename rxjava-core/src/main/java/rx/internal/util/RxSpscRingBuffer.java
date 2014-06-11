package rx.internal.util;

import java.util.concurrent.atomic.AtomicInteger;

import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.internal.operators.NotificationLite;
import rx.internal.util.jctools.SpscArrayQueue;

/**
 * Ring Buffer customized to Rx use-case.
 * <p>
 * Single-producer-single-consumer.
 */
public class RxSpscRingBuffer {

    public static final int SIZE = 1023; // +1 = 1024 which is a power of 2
    public static final int THRESHOLD = 768;

    private static final NotificationLite<Object> on = NotificationLite.instance();

    /**
     * Using ArrayBlockingQueue
     * <pre> {@code
     * 
     * Benchmark                              (size)   Mode   Samples         Mean   Mean error    Units
     * r.i.u.PerfRingBuffer.onNextConsume        100  thrpt         5   159236.300     7108.964    ops/s
     * r.i.u.PerfRingBuffer.onNextConsume       1023  thrpt         5    15615.351      953.520    ops/s
     * r.i.u.PerfRingBuffer.onNextPollLoop       100  thrpt         5   158659.665     7847.576    ops/s
     * r.i.u.PerfRingBuffer.onNextPollLoop   1000000  thrpt         5       17.495        0.246    ops/s
     * 
     * Using SpscArrayQueue
     * 
     * Benchmark                              (size)   Mode   Samples         Mean   Mean error    Units
     * r.i.u.PerfRingBuffer.onNextConsume        100  thrpt         5   321948.603     9071.204    ops/s
     * r.i.u.PerfRingBuffer.onNextConsume       1023  thrpt         5    35104.267     1586.282    ops/s
     * r.i.u.PerfRingBuffer.onNextPollLoop       100  thrpt         5   324645.717    11050.170    ops/s
     * r.i.u.PerfRingBuffer.onNextPollLoop   1000000  thrpt         5       41.269        0.630    ops/s
     * } </pre>
     */
    private final SpscArrayQueue<Object> queue;

    private final int size;
    private final int threshold;
    private final AtomicInteger outstandingRequests = new AtomicInteger();
    private final AtomicInteger count = new AtomicInteger();

    private RxSpscRingBuffer(int size, int threshold) {
        queue = new SpscArrayQueue<Object>(size + 1); // +1 for onCompleted if onNext fills buffer
        this.size = size;
        this.threshold = threshold;
    }

    public RxSpscRingBuffer() {
        this(SIZE, THRESHOLD);
    }

    /**
     * 
     * @param o
     * @throws MissingBackpressureException
     *             if more onNext are sent than have been requested
     */
    public void onNext(Object o) throws MissingBackpressureException {
        if (count.incrementAndGet() > size) {
            // a rare time where a checkedexception seems helpful ... as an internal safety check that we're handling it correctly
            throw new MissingBackpressureException();
        }
        // we received a requested item
        outstandingRequests.decrementAndGet();
        if (!queue.offer(on.next(o))) {
            throw new IllegalStateException("onNext could not be delivered. There is a bug.");
        }
    }

    public void onCompleted() {
        if (!queue.offer(on.completed())) {
            throw new IllegalStateException("onComplete could not be delivered. Buffer has already received a terminal event.");
        }
    }

    public void onError(Throwable t) {
        if (!queue.offer(on.error(t))) {
            if (t instanceof MissingBackpressureException) {
                throw new IllegalStateException("onError could not be delivered. Missing Backpressure.", t);
            } else {
                throw new IllegalStateException("onError could not be delivered. Buffer has already received a terminal event.", t);
            }
        }
    }

    public int count() {
        return count.get();
    }

    public int available() {
        return size - count.get();
    }

    public int requested() {
        // it can be 0 if onNext are sent without having been requested
        return Math.max(0, outstandingRequests.get());
    }

    public int capacity() {
        return size;
    }

    public Object poll() {
        Object o = queue.poll();
        if (o != null) {
            count.decrementAndGet();
        }
        return o;
    }

    public boolean isCompleted(Object o) {
        return on.isCompleted(o);
    }

    public boolean isError(Object o) {
        return on.isError(o);
    }

    public Throwable asError(Object o) {
        return on.getError(o);
    }

    public void requestIfNeeded(Subscriber<?> s) {
        do {
            int o = outstandingRequests.get();
            int available = available();
            if (o > 0) {
                // oustandingRequests can be <0 if we received an onNext or MissingBackpressureException that decremented below 0
                available = available - o;
            }
            if (available > (size - threshold)) {
                if (outstandingRequests.compareAndSet(o, available + Math.max(0, o))) {
                    s.request(available);
                    return;
                }
            } else {
                // nothing to request
                return;
            }
        } while (true);
    }

}
