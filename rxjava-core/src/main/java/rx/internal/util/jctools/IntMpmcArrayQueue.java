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
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/IntMpmcArrayQueue.java
 */
package rx.internal.util.jctools;

import static rx.internal.util.UnsafeAccess.UNSAFE;

import java.util.Queue;

abstract class IntMpmcArrayQueueL1Pad extends IntConcurrentSequencedCircularArrayQueue {
    long p10, p11, p12, p13, p14, p15, p16;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public IntMpmcArrayQueueL1Pad(int capacity) {
        super(capacity);
    }
}

abstract class IntMpmcArrayQueueTailField extends IntMpmcArrayQueueL1Pad {
    private final static long TAIL_OFFSET;
    static {
        try {
            TAIL_OFFSET = UNSAFE.objectFieldOffset(IntMpmcArrayQueueTailField.class.getDeclaredField("tail"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    private volatile long tail;

    public IntMpmcArrayQueueTailField(int capacity) {
        super(capacity);
    }

    protected final long lvTail() {
        return tail;
    }

    protected final boolean casTail(long expect, long newValue) {
        return UNSAFE.compareAndSwapLong(this, TAIL_OFFSET, expect, newValue);
    }
}

abstract class IntMpmcArrayQueueL2Pad extends IntMpmcArrayQueueTailField {
    long p20, p21, p22, p23, p24, p25, p26;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public IntMpmcArrayQueueL2Pad(int capacity) {
        super(capacity);
    }
}

abstract class IntMpmcArrayQueueHeadField extends IntMpmcArrayQueueL2Pad {
    private final static long HEAD_OFFSET;
    static {
        try {
            HEAD_OFFSET = UNSAFE.objectFieldOffset(IntMpmcArrayQueueHeadField.class.getDeclaredField("head"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    private volatile long head;

    public IntMpmcArrayQueueHeadField(int capacity) {
        super(capacity);
    }

    protected final long lvHead() {
        return head;
    }

    protected final boolean casHead(long expect, long newValue) {
        return UNSAFE.compareAndSwapLong(this, HEAD_OFFSET, expect, newValue);
    }
}

public class IntMpmcArrayQueue extends IntMpmcArrayQueueHeadField {
    long p40, p41, p42, p43, p44, p45, p46;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public IntMpmcArrayQueue(final int capacity) {
        super(Math.max(2, capacity));
    }

    public boolean offer(final int e) {
        if (e < 0) {
            throw new IllegalStateException("only supports positive numbers");
        }
        final long[] lsb = sequenceBuffer;
        long currentTail;
        long pOffset;

        for (;;) {
            currentTail = lvTail();
            pOffset = calcSequenceOffset(currentTail);
            long seq = lvSequenceElement(lsb, pOffset);
            long delta = seq - currentTail;
            if (delta == 0) {
                // this is expected if we see this first time around
                if (casTail(currentTail, currentTail + 1)) {
                    break;
                }
                // failed cas, retry 1
            } else if (delta < 0) {
                // poll has not moved this value forward
                return false;
            } else {
                // another producer beat us
            }
        }
        final long offset = calcOffset(currentTail);
        spElement(offset, e);
        // increment position, seeing this value again should lead to retry 2
        soSequenceElement(lsb, pOffset, currentTail + 1);
        return true;
    }

    public int poll() {
        final long[] lsb = sequenceBuffer;
        long currentHead;
        long pOffset;
        for (;;) {
            currentHead = lvHead();
            pOffset = calcSequenceOffset(currentHead);
            long seq = lvSequenceElement(lsb, pOffset);
            long delta = seq - (currentHead + 1);
            if (delta == 0) {
                if (casHead(currentHead, currentHead + 1)) {
                    break;
                }
                // failed cas, retry 1
            } else if (delta < 0) {
                // queue is empty
                return -1;
            } else {
                // another consumer beat us
            }
        }
        final long offset = calcOffset(currentHead);
        final int[] lb = buffer;
        int e = lvElement(lb, offset);
        spElement(lb, offset, -1);
        soSequenceElement(lsb, pOffset, currentHead + capacity);
        return e;
    }

    public int size() {
        return (int) (lvTail() - lvHead());
    }
    
    public void clear() {
        while (poll() != -1)
            ;
    }
}