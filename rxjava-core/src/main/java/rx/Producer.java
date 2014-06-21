package rx;

import rx.internal.util.RxRingBuffer;

public interface Producer {

    /**
     * Size of Backpressure Buffers. An Observable producing less data than this can skip using a Producer.
     */
    public static final int BUFFER_SIZE = RxRingBuffer.SIZE;

    public void request(int n);

}
