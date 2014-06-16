package rx.internal.util;

import java.util.Queue;

import rx.internal.util.jctools.SpscArrayQueue;

public class RxSpscRingBuffer extends RxRingBuffer {

    public static final ObjectPool<RxRingBuffer> POOL = new ObjectPool<RxRingBuffer>(0, 1024, 67) {

        @Override
        protected RxRingBuffer createObject() {
            return new RxSpscRingBuffer();
        }

    };

    public final static RxSpscRingBuffer getInstance() {
        return (RxSpscRingBuffer) POOL.borrowObject();
    }

    @Override
    protected Queue<Object> createQueue(int size) {
        return new SpscArrayQueue<Object>(size);
    }

    @Override
    protected void returnObject(RxRingBuffer b) {
        POOL.returnObject(b);
    }

}
