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
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/SpmcArrayQueue.java
 */
package rx.internal.util.jctools;

import java.util.Queue;

import rx.internal.util.UnsafeAccess;

abstract class SpmcArrayQueueL1Pad<E> extends ConcurrentCircularArrayQueue<E> {
    long p10, p11, p12, p13, p14, p15, p16;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public SpmcArrayQueueL1Pad(int capacity) {
        super(capacity);
    }
}

abstract class SpmcArrayQueueTailField<E> extends SpmcArrayQueueL1Pad<E> {
    protected final static long TAIL_OFFSET;
    static {
        try {
            TAIL_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(SpmcArrayQueueTailField.class
                    .getDeclaredField("tail"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    private volatile long tail;

    protected final long lvTail() {
        return tail;
    }

    protected final void soTail(long v) {
        UnsafeAccess.UNSAFE.putOrderedLong(this, TAIL_OFFSET, v);
    }

    public SpmcArrayQueueTailField(int capacity) {
        super(capacity);
    }
}

abstract class SpmcArrayQueueL2Pad<E> extends SpmcArrayQueueTailField<E> {
    long p20, p21, p22, p23, p24, p25, p26;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public SpmcArrayQueueL2Pad(int capacity) {
        super(capacity);
    }
}

abstract class SpmcArrayQueueHeadField<E> extends SpmcArrayQueueL2Pad<E> {
    protected final static long HEAD_OFFSET;
    static {
        try {
            HEAD_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(SpmcArrayQueueHeadField.class
                    .getDeclaredField("head"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    private volatile long head;

    public SpmcArrayQueueHeadField(int capacity) {
        super(capacity);
    }

    protected final long lvHead() {
        return head;
    }

    protected final boolean casHead(long expect, long newValue) {
        return UnsafeAccess.UNSAFE.compareAndSwapLong(this, HEAD_OFFSET, expect, newValue);
    }
}

abstract class SpmcArrayQueueMidPad<E> extends SpmcArrayQueueHeadField<E> {
    long p20, p21, p22, p23, p24, p25, p26;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public SpmcArrayQueueMidPad(int capacity) {
        super(capacity);
    }
}

abstract class SpmcArrayQueueTailCacheField<E> extends SpmcArrayQueueMidPad<E> {
    private volatile long tailCache;

    public SpmcArrayQueueTailCacheField(int capacity) {
        super(capacity);
    }

    protected final long lvTailCache() {
        return tailCache;
    }

    protected final void svTailCache(long v) {
        tailCache = v;
    }
}

abstract class SpmcArrayQueueL3Pad<E> extends SpmcArrayQueueTailCacheField<E> {
    long p40, p41, p42, p43, p44, p45, p46;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public SpmcArrayQueueL3Pad(int capacity) {
        super(capacity);
    }
}

public final class SpmcArrayQueue<E> extends SpmcArrayQueueL3Pad<E> implements Queue<E> {

    public SpmcArrayQueue(final int capacity) {
        super(capacity);
    }

    @Override
    public boolean offer(final E e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }
        final E[] lb = buffer;
        final long currTail = lvTail();
        final long offset = calcOffset(currTail);
        if (null != lvElement(lb, offset)) {
            return false;
        }
        spElement(lb, offset, e);
        // single producer, so store ordered is valid. It is also required to correctly publish the element
        // and for the consumers to pick up the tail value.
        soTail(currTail + 1);
        return true;
    }

    @Override
    public E poll() {
        long currentHead;
        final long currTailCache = lvTailCache();
        do {
            currentHead = lvHead();
            if (currentHead >= currTailCache) {
                long currTail = lvTail();
                if (currentHead >= currTail) {
                    return null;
                } else {
                    svTailCache(currTail);
                }
            }
        } while (!casHead(currentHead, currentHead + 1));
        // consumers are gated on latest visible tail, and so can't see a null value in the queue or overtake
        // and wrap to hit same location.
        final long offset = calcOffset(currentHead);
        final E[] lb = buffer;
        // load plain, element happens before it's index becomes visible
        final E e = lpElement(lb, offset);
        // store ordered, make sure nulling out is visible. Producer is waiting for this value.
        soElement(lb, offset, null);
        return e;
    }
    
    @Override
    public E peek() {
        return lvElement(calcOffset(lvHead()));
    }
    @Override
    public int size() {
        return (int) (lvTail() - lvHead());
    }
}