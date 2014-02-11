package rx.util;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Single-producer-single-consumer queue (only thread-safe for 1 producer thread with 1 consumer thread).
 * 
 * This supports an interrupt() being called externally rather than needing to interrupt the thread. This allows
 * unsubscribe behavior when this queue is being used.
 * 
 * @param <E>
 */
public class InterruptibleBlockingQueue<E> {

    private final Semaphore semaphore;
    private volatile boolean interrupted = false;

    private final E[] buffer;

    private AtomicLong tail = new AtomicLong();
    private AtomicLong head = new AtomicLong();
    private final int capacity;
    private final int mask;

    @SuppressWarnings("unchecked")
    public InterruptibleBlockingQueue(final int size) {
        this.semaphore = new Semaphore(size);
        this.capacity = size;
        this.mask = size - 1;
        buffer = (E[]) new Object[size];
    }

    /**
     * Used to unsubscribe and interrupt the producer if blocked in put()
     */
    public void interrupt() {
        interrupted = true;
        semaphore.release();
    }

    public void addBlocking(final E e) throws InterruptedException {
        if (interrupted) {
            throw new InterruptedException("Interrupted by Unsubscribe");
        }
        semaphore.acquire();
        if (interrupted) {
            throw new InterruptedException("Interrupted by Unsubscribe");
        }
        if (e == null) {
            throw new IllegalArgumentException("Can not put null");
        }

        if (offer(e)) {
            return;
        } else {
            throw new IllegalStateException("Queue is full");
        }
    }

    private boolean offer(final E e) {
        final long _t = tail.get();
        if (_t - head.get() == capacity) {
            // queue is full
            return false;
        }
        int index = (int) (_t & mask);
        buffer[index] = e;
        // move the tail forward
        tail.lazySet(_t + 1);

        return true;
    }

    public E poll() {
        if (interrupted) {
            return null;
        }
        final long _h = head.get();
        if (tail.get() == _h) {
            // nothing available
            return null;
        }
        int index = (int) (_h & mask);

        // fetch the item
        E v = buffer[index];
        // allow GC to happen
        buffer[index] = null;
        // increment and signal we're done
        head.lazySet(_h + 1);
        if (v != null) {
            semaphore.release();
        }
        return v;
    }

    public int size()
    {
        int size;
        do
        {
            final long currentHead = head.get();
            final long currentTail = tail.get();
            size = (int) (currentTail - currentHead);
        } while (size > buffer.length);

        return size;
    }

}
