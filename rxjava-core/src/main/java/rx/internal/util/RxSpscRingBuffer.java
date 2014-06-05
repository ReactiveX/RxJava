package rx.internal.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.internal.operators.NotificationLite;

/**
 * Ring Buffer customized to Rx use-case.
 * <p>
 * Single-producer-single-consumer.
 */
public class RxSpscRingBuffer {

    // TODO change to some global size like 1024/768
    public static final int DEFAULT_SIZE = 10;
    public static final int DEFAULT_THRESHOLD = 7;

    private static final NotificationLite<Object> on = NotificationLite.instance();

    // TODO replace with non-blocking spsc ringbuffer
    // for example: http://psy-lob-saw.blogspot.com/2013/11/spsc-iv-look-at-bqueue.html
    private final ArrayBlockingQueue<Object> queue;
    private final int size;
    private final int threshold;
    private final AtomicInteger outstandingRequests = new AtomicInteger();
    private final AtomicInteger count = new AtomicInteger();

    public RxSpscRingBuffer(int size, int threshold) {
        queue = new ArrayBlockingQueue<Object>(size + 1); // +1 for onCompleted if onNext fills buffer
        this.size = size;
        this.threshold = threshold;
    }

    public RxSpscRingBuffer() {
        this(DEFAULT_SIZE, DEFAULT_THRESHOLD);
    }

    /**
     * 
     * @param o
     * @throws MissingBackpressureException
     *             if more onNext are sent than have been requested
     */
    public void onNext(Object o) {
        if (count.incrementAndGet() > size) {
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
            throw new IllegalStateException("onError could not be delivered. Buffer has already received a terminal event.");
        }
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

    public void requestIfNeeded(Subscriber<?> s) {
        do {
            int o = outstandingRequests.get();
            int available = available();
            if (o > 0) {
                // oustandingRequests can be <0 if we received an onNext or MissingBackpressureException that decremented below 0
                available = available - o;
            }
            if (available >= threshold) {
                if (outstandingRequests.compareAndSet(o, available)) {
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
