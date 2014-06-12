/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Original License: https://github.com/JCTools/JCTools/blob/master/LICENSE
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/SpscArrayQueue.java
 */
package rx.internal.util.jctools;

import java.util.Queue;

abstract class SpscArrayQueueL1Pad<E> extends ConcurrentCircularArrayQueue<E> {
    long p10, p11, p12, p13, p14, p15, p16;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public SpscArrayQueueL1Pad(int capacity) {
        super(capacity);
    }
}

abstract class SpscArrayQueueTailField<E> extends SpscArrayQueueL1Pad<E> {
    protected long tail;
    protected long batchTail;

    public SpscArrayQueueTailField(int capacity) {
        super(capacity);
    }
}

abstract class SpscArrayQueueL2Pad<E> extends SpscArrayQueueTailField<E> {
    long p20, p21, p22, p23, p24, p25, p26;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public SpscArrayQueueL2Pad(int capacity) {
        super(capacity);
    }
}

abstract class SpscArrayQueueHeadField<E> extends SpscArrayQueueL2Pad<E> {
    protected long head;

    public SpscArrayQueueHeadField(int capacity) {
        super(capacity);
    }
}

abstract class SpscArrayQueueL3Pad<E> extends SpscArrayQueueHeadField<E> {
    long p40, p41, p42, p43, p44, p45, p46;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public SpscArrayQueueL3Pad(int capacity) {
        super(capacity);
    }
}

public final class SpscArrayQueue<E> extends SpscArrayQueueL3Pad<E> implements Queue<E> {
    private final static long TAIL_OFFSET;
    private final static long HEAD_OFFSET;
    static {
        try {
            TAIL_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(SpscArrayQueueTailField.class
                    .getDeclaredField("tail"));
            HEAD_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(SpscArrayQueueHeadField.class
                    .getDeclaredField("head"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private final int offerLimit;

    public SpscArrayQueue(final int capacity) {
        super(capacity);
        this.offerLimit = capacity;
    }

    private long getHeadV() {
        return UnsafeAccess.UNSAFE.getLongVolatile(this, HEAD_OFFSET);
    }

    private long getTailV() {
        return UnsafeAccess.UNSAFE.getLongVolatile(this, TAIL_OFFSET);
    }

    @Override
    public boolean offer(final E e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }

        E[] lb = buffer;
        if (tail >= batchTail) {
            if (null != lvElement(lb, calcOffset(tail))) {
                return false;
            }
        }
        soElement(lb, calcOffset(tail), e);
        tail++;

        return true;
    }

    @Override
    public E poll() {
        final long offset = calcOffset(head);
        final E[] lb = buffer;
        final E e = lvElement(lb, offset);
        if (null == e) {
            return null;
        }
        soElement(lb, offset, null);
        head++;
        return e;
    }

    @Override
    public E peek() {
        return lvElement(calcOffset(head));
    }

    @Override
    public int size() {
        // TODO: this is ugly :( the head/tail cannot be counted on to be written out, so must take max
        return (int) (Math.max(getTailV(), tail) - Math.max(getHeadV(), head));
    }
}