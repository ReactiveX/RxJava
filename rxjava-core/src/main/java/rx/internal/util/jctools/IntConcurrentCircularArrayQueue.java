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
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/IntConcurrentCircularArrayQueue.java
 */
package rx.internal.util.jctools;

import static rx.internal.util.UnsafeAccess.UNSAFE;
import rx.internal.util.UnsafeAccess;

abstract class IntConcurrentCircularArrayQueueL0Pad {
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p30, p31, p32, p33, p34, p35, p36, p37;
}

public abstract class IntConcurrentCircularArrayQueue extends IntConcurrentCircularArrayQueueL0Pad {
    protected static final int SPARSE_SHIFT = Integer.getInteger("sparse.shift", 0);
    protected static final int BUFFER_PAD = 32;
    private static final long REF_ARRAY_BASE;
    private static final int REF_ELEMENT_SHIFT;
    static {
        final int scale = UnsafeAccess.UNSAFE.arrayIndexScale(int[].class);
        if (4 == scale) {
            REF_ELEMENT_SHIFT = 2 + SPARSE_SHIFT;
        } else if (8 == scale) {
            REF_ELEMENT_SHIFT = 3 + SPARSE_SHIFT;
        } else {
            throw new IllegalStateException("Unknown pointer size");
        }
        // Including the buffer pad in the array base offset
        REF_ARRAY_BASE = UnsafeAccess.UNSAFE.arrayBaseOffset(int[].class)
                + (BUFFER_PAD << (REF_ELEMENT_SHIFT - SPARSE_SHIFT));
    }
    protected final int capacity;
    protected final long mask;
    // @Stable :(
    protected final int[] buffer;

    public IntConcurrentCircularArrayQueue(int capacity) {
        this.capacity = Pow2.findNextPositivePowerOfTwo(capacity);
        mask = this.capacity - 1;
        // pad data on either end with some empty slots.
        buffer = new int[(this.capacity << SPARSE_SHIFT) + BUFFER_PAD * 2];
    }

    public IntConcurrentCircularArrayQueue(IntConcurrentCircularArrayQueue c) {
        this.capacity = c.capacity;
        this.mask = c.mask;
        // pad data on either end with some empty slots.
        this.buffer = c.buffer;
    }

    protected final long calcOffset(long index) {
        return REF_ARRAY_BASE + ((index & mask) << REF_ELEMENT_SHIFT);
    }

    protected final void spElement(long offset, int e) {
        UNSAFE.putInt(buffer, offset, e);
    }

    protected final void soElement(long offset, int e) {
        UNSAFE.putOrderedInt(buffer, offset, e);
    }

    protected final void svElement(long offset, int e) {
        UNSAFE.putIntVolatile(buffer, offset, e);
    }

    protected final int lpElement(long offset) {
        return UNSAFE.getInt(buffer, offset);
    }

    protected final int lvElement(long offset) {
        return UNSAFE.getIntVolatile(buffer, offset);
    }

    protected final void spElement(int[] buffer, long offset, int e) {
        UNSAFE.putInt(buffer, offset, e);
    }

    protected final void soElement(int[] buffer, long offset, int e) {
        UNSAFE.putOrderedInt(buffer, offset, e);
    }

    protected final void svElement(int[] buffer, long offset, int e) {
        UNSAFE.putIntVolatile(buffer, offset, e);
    }

    protected final int lpElement(int[] buffer, long offset) {
        return UNSAFE.getInt(buffer, offset);
    }

    protected final int lvElement(int[] buffer, long offset) {
        return UNSAFE.getIntVolatile(buffer, offset);
    }

}