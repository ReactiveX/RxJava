package rx.internal.util;

import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.MissingBackpressureException;

public interface RxRingBuffer extends Subscription {
    
    public static final int SIZE = 1024; //power of 2
    public static final int THRESHOLD = 256;
    /**
     * Directly emit to `child.onNext` while also decrementing the request counters used by `requestIfNeeded`.
     * 
     * @param o
     * @param child
     */
    public abstract void emitWithoutQueue(Object o, Observer child);

    /**
     * 
     * @param o
     * @throws MissingBackpressureException
     *             if more onNext are sent than have been requested
     */
    public abstract void onNext(Object o) throws MissingBackpressureException;

    public abstract void onCompleted();

    public abstract void onError(Throwable t);

    public abstract int count();

    public abstract int available();

    public abstract int requested();

    public abstract int capacity();

    public abstract Object poll();

    public abstract boolean isCompleted(Object o);

    public abstract boolean isError(Object o);

    public abstract void accept(Object o, Observer child);

    public abstract Throwable asError(Object o);

    public abstract void requestIfNeeded(Subscriber<?> s);

    public abstract boolean isUnsubscribed();

}