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
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/atomic/SpscUnboundedAtomicArrayQueue.java
 */
package rx.internal.util.atomic;

import java.util.*;
import java.util.concurrent.atomic.*;

import rx.internal.util.unsafe.*;

public class SpscUnboundedAtomicArrayQueue<E> extends AbstractQueue<E> implements QueueProgressIndicators {
    static final int MAX_LOOK_AHEAD_STEP = Integer.getInteger("jctools.spsc.max.lookahead.step", 4096);
    protected final AtomicLong producerIndex;
    protected int producerLookAheadStep;
    protected long producerLookAhead;
    protected int producerMask;
    protected AtomicReferenceArray<Object> producerBuffer;
    protected int consumerMask;
    protected AtomicReferenceArray<Object> consumerBuffer;
    protected final AtomicLong consumerIndex;
    private static final Object HAS_NEXT = new Object();

    public SpscUnboundedAtomicArrayQueue(final int bufferSize) {
        int p2capacity = Pow2.roundToPowerOfTwo(bufferSize);
        int mask = p2capacity - 1;
        AtomicReferenceArray<Object> buffer = new AtomicReferenceArray<Object>(p2capacity + 1);
        producerBuffer = buffer;
        producerMask = mask;
        adjustLookAheadStep(p2capacity);
        consumerBuffer = buffer;
        consumerMask = mask;
        producerLookAhead = mask - 1; // we know it's all empty to start with
        producerIndex = new AtomicLong();
        consumerIndex = new AtomicLong();
        soProducerIndex(0L);
    }

    @Override
    public final Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation is correct for single producer thread use only.
     */
    @Override
    public final boolean offer(final E e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }
        // local load of field to avoid repeated loads after volatile reads
        final AtomicReferenceArray<Object> buffer = producerBuffer;
        final long index = lpProducerIndex();
        final int mask = producerMask;
        final int offset = calcWrappedOffset(index, mask);
        if (index < producerLookAhead) {
            return writeToQueue(buffer, e, index, offset);
        } else {
            final int lookAheadStep = producerLookAheadStep;
            // go around the buffer or resize if full (unless we hit max capacity)
            int lookAheadElementOffset = calcWrappedOffset(index + lookAheadStep, mask);
            if (null == lvElement(buffer, lookAheadElementOffset)) {// LoadLoad
                producerLookAhead = index + lookAheadStep - 1; // joy, there's plenty of room
                return writeToQueue(buffer, e, index, offset);
            } else if (null != lvElement(buffer, calcWrappedOffset(index + 1, mask))) { // buffer is not full
                return writeToQueue(buffer, e, index, offset);
            } else {
                resize(buffer, index, offset, e, mask); // add a buffer and link old to new
                return true;
            }
        }
    }

    private boolean writeToQueue(final AtomicReferenceArray<Object> buffer, final E e, final long index, final int offset) {
        soProducerIndex(index + 1);// this ensures atomic write of long on 32bit platforms
        soElement(buffer, offset, e);// StoreStore
        return true;
    }

    private void resize(final AtomicReferenceArray<Object> oldBuffer, final long currIndex, final int offset, final E e,
            final long mask) {
        final int capacity = oldBuffer.length();
        final AtomicReferenceArray<Object> newBuffer = new AtomicReferenceArray<Object>(capacity);
        producerBuffer = newBuffer;
        producerLookAhead = currIndex + mask - 1;
        soProducerIndex(currIndex + 1);// this ensures correctness on 32bit platforms
        soElement(newBuffer, offset, e);// StoreStore
        soNext(oldBuffer, newBuffer);
        soElement(oldBuffer, offset, HAS_NEXT); // new buffer is visible after element is
                                                                 // inserted
    }

    private void soNext(AtomicReferenceArray<Object> curr, AtomicReferenceArray<Object> next) {
        soElement(curr, calcDirectOffset(curr.length() - 1), next);
    }
    @SuppressWarnings("unchecked")
    private AtomicReferenceArray<Object> lvNext(AtomicReferenceArray<Object> curr) {
        return (AtomicReferenceArray<Object>)lvElement(curr, calcDirectOffset(curr.length() - 1));
    }
    /**
     * {@inheritDoc}
     * <p>
     * This implementation is correct for single consumer thread use only.
     */
    @SuppressWarnings("unchecked")
    @Override
    public final E poll() {
        // local load of field to avoid repeated loads after volatile reads
        final AtomicReferenceArray<Object> buffer = consumerBuffer;
        final long index = lpConsumerIndex();
        final int mask = consumerMask;
        final int offset = calcWrappedOffset(index, mask);
        final Object e = lvElement(buffer, offset);// LoadLoad
        boolean isNextBuffer = e == HAS_NEXT;
        if (null != e && !isNextBuffer) {
            soConsumerIndex(index + 1);// this ensures correctness on 32bit platforms
            soElement(buffer, offset, null);// StoreStore
            return (E) e;
        } else if (isNextBuffer) {
            return newBufferPoll(lvNext(buffer), index, mask);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private E newBufferPoll(AtomicReferenceArray<Object> nextBuffer, final long index, final int mask) {
        consumerBuffer = nextBuffer;
        final int offsetInNew = calcWrappedOffset(index, mask);
        final E n = (E) lvElement(nextBuffer, offsetInNew);// LoadLoad
        if (null == n) {
            return null;
        } else {
            soConsumerIndex(index + 1);// this ensures correctness on 32bit platforms
            soElement(nextBuffer, offsetInNew, null);// StoreStore
            return n;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation is correct for single consumer thread use only.
     */
    @SuppressWarnings("unchecked")
    @Override
    public final E peek() {
        final AtomicReferenceArray<Object> buffer = consumerBuffer;
        final long index = lpConsumerIndex();
        final int mask = consumerMask;
        final int offset = calcWrappedOffset(index, mask);
        final Object e = lvElement(buffer, offset);// LoadLoad
        if (e == HAS_NEXT) {
            return newBufferPeek(lvNext(buffer), index, mask);
        }

        return (E) e;
    }

    @SuppressWarnings("unchecked")
    private E newBufferPeek(AtomicReferenceArray<Object> nextBuffer, final long index, final int mask) {
        consumerBuffer = nextBuffer;
        final int offsetInNew = calcWrappedOffset(index, mask);
        return (E) lvElement(nextBuffer, offsetInNew);// LoadLoad
    }

    @Override
    public final int size() {
        /*
         * It is possible for a thread to be interrupted or reschedule between the read of the producer and
         * consumer indices, therefore protection is required to ensure size is within valid range. In the
         * event of concurrent polls/offers to this method the size is OVER estimated as we read consumer
         * index BEFORE the producer index.
         */
        long after = lvConsumerIndex();
        while (true) {
            final long before = after;
            final long currentProducerIndex = lvProducerIndex();
            after = lvConsumerIndex();
            if (before == after) {
                return (int) (currentProducerIndex - after);
            }
        }
    }

    private void adjustLookAheadStep(int capacity) {
        producerLookAheadStep = Math.min(capacity / 4, MAX_LOOK_AHEAD_STEP);
    }

    private long lvProducerIndex() {
        return producerIndex.get();
    }

    private long lvConsumerIndex() {
        return consumerIndex.get();
    }

    private long lpProducerIndex() {
        return producerIndex.get();
    }

    private long lpConsumerIndex() {
        return consumerIndex.get();
    }

    private void soProducerIndex(long v) {
        producerIndex.lazySet(v);
    }

    private void soConsumerIndex(long v) {
        consumerIndex.lazySet(v);
    }

    private static final int calcWrappedOffset(long index, int mask) {
        return calcDirectOffset((int)index & mask);
    }
    private static final int calcDirectOffset(int index) {
        return index;
    }
    private static final void soElement(AtomicReferenceArray<Object> buffer, int offset, Object e) {
        buffer.lazySet(offset, e);
    }

    private static final <E> Object lvElement(AtomicReferenceArray<Object> buffer, int offset) {
        return buffer.get(offset);
    }
    
    @Override
    public long currentProducerIndex() {
        return lvProducerIndex();
    }
    
    @Override
    public long currentConsumerIndex() {
        return lvConsumerIndex();
    }
}