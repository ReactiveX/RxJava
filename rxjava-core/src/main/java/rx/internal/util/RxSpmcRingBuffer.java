package rx.internal.util;

import java.util.Queue;

import rx.internal.util.jctools.SpmcArrayQueue;

public class RxSpmcRingBuffer extends RxRingBuffer {

    public static final ObjectPool<RxRingBuffer> POOL = new ObjectPool<RxRingBuffer>(0, 1024, 67) {

        @Override
        protected RxRingBuffer createObject() {
            return new RxSpmcRingBuffer();
        }

    };

    public final static RxSpmcRingBuffer getInstance() {
        return (RxSpmcRingBuffer) POOL.borrowObject();
    }

    @Override
    protected Queue<Object> createQueue(int size) {
        return new SpmcArrayQueue<Object>(size);
    }

    @Override
    protected void returnObject(RxRingBuffer b) {
        POOL.returnObject(b);
    }

}
