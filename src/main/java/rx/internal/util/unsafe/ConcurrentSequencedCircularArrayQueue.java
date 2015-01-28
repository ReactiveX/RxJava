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
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/ConcurrentSequencedCircularArrayQueue.java
 */
package rx.internal.util.unsafe;

import static rx.internal.util.unsafe.UnsafeAccess.UNSAFE;

public abstract class ConcurrentSequencedCircularArrayQueue<E> extends ConcurrentCircularArrayQueue<E> {
    private static final long ARRAY_BASE;
    private static final int ELEMENT_SHIFT;
    static {
        final int scale = UnsafeAccess.UNSAFE.arrayIndexScale(long[].class);
        if (8 == scale) {
            ELEMENT_SHIFT = 3 + SPARSE_SHIFT;
        } else {
            throw new IllegalStateException("Unexpected long[] element size");
        }
        // Including the buffer pad in the array base offset
        ARRAY_BASE = UnsafeAccess.UNSAFE.arrayBaseOffset(long[].class) + (BUFFER_PAD << (ELEMENT_SHIFT - SPARSE_SHIFT));
    }
    protected final long[] sequenceBuffer;

    public ConcurrentSequencedCircularArrayQueue(int capacity) {
        super(capacity);
        int actualCapacity = (int) (this.mask + 1);
        // pad data on either end with some empty slots.
        sequenceBuffer = new long[(actualCapacity << SPARSE_SHIFT) + BUFFER_PAD * 2];
        for (long i = 0; i < actualCapacity; i++) {
            soSequence(sequenceBuffer, calcSequenceOffset(i), i);
        }
    }

    protected final long calcSequenceOffset(long index) {
        return ARRAY_BASE + ((index & mask) << ELEMENT_SHIFT);
    }

    protected final void soSequence(long[] buffer, long offset, long e) {
        UNSAFE.putOrderedLong(buffer, offset, e);
    }

    protected final long lvSequence(long[] buffer, long offset) {
        return UNSAFE.getLongVolatile(buffer, offset);
    }

}
