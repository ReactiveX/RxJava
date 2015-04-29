package rx.internal.operators;

import java.util.concurrent.atomic.AtomicInteger;

import rx.*;

/**
 * A producer that holds a single value until it is requested and emits it followed by an onCompleted. 
 */
public final class SingleDelayedProducer<T> extends AtomicInteger implements Producer {
    /** */
    private static final long serialVersionUID = 4721551710164477552L;
    /** The actual child. */
    final Subscriber<? super T> child;
    /** The value to emit, acquired and released by compareAndSet. */
    T value;
    /** State flag: request() called with positive value. */
    static final int REQUESTED = 1;
    /** State flag: set() called. */
    static final int SET = 2;
    /**
     * Constructs a SingleDelayedProducer with the given child as output.
     * @param child the subscriber to emit the value and completion events
     */
    public SingleDelayedProducer(Subscriber<? super T> child) {
        this.child = child;
    }
    @Override
    public void request(long n) {
        if (n > 0) {
            for (;;) {
                int s = get();
                // if already requested
                if ((s & REQUESTED) != 0) {
                    break;
                }
                int u = s | REQUESTED;
                if (compareAndSet(s, u)) {
                    if ((s & SET) != 0) {
                        emit();
                    }
                    break;
                }
            }
        }
    }
    /**
     * Sets the value to be emitted and emits it if there was a request.
     * Should be called only once and from a single thread
     * @param value the value to set and possibly emit
     */
    public void set(T value) {
        for (;;) {
            int s = get();
            // if already set
            if ((s & SET) != 0) {
                break;
            }
            int u = s | SET;
            this.value = value;
            if (compareAndSet(s, u)) {
                if ((s & REQUESTED) != 0) {
                    emit();
                }
                break;
            }
        }
    }
    /** 
     * Emits the set value if the child is not unsubscribed and bounces back
     * exceptions caught from child.onNext.
     */
    void emit() {
        try {
            T v = value;
            value = null; // do not hold onto the value
            if (child.isUnsubscribed()) {
                return;
            }
            child.onNext(v);
        } catch (Throwable t) {
            child.onError(t);
            return;
        }
        child.onCompleted();
    }
}