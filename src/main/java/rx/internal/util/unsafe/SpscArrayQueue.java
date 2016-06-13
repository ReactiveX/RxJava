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
package rx.internal.util.unsafe;

import static rx.internal.util.unsafe.UnsafeAccess.UNSAFE;

abstract class SpscArrayQueueColdField<E> extends ConcurrentCircularArrayQueue<E> {
    private static final Integer MAX_LOOK_AHEAD_STEP = Integer.getInteger("jctools.spsc.max.lookahead.step", 4096);
    protected final int lookAheadStep;
    public SpscArrayQueueColdField(int capacity) {
        super(capacity);
        lookAheadStep = Math.min(capacity/4, MAX_LOOK_AHEAD_STEP);
    }
}
abstract class SpscArrayQueueL1Pad<E> extends SpscArrayQueueColdField<E> {
    long p10, p11, p12, p13, p14, p15, p16;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public SpscArrayQueueL1Pad(int capacity) {
        super(capacity);
    }
}

abstract class SpscArrayQueueProducerFields<E> extends SpscArrayQueueL1Pad<E> {
    protected final static long P_INDEX_OFFSET = UnsafeAccess.addressOf(SpscArrayQueueProducerFields.class, "producerIndex");
    protected long producerIndex;
    protected long producerLookAhead;

    public SpscArrayQueueProducerFields(int capacity) {
        super(capacity);
    }
}

abstract class SpscArrayQueueL2Pad<E> extends SpscArrayQueueProducerFields<E> {
    long p20, p21, p22, p23, p24, p25, p26;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public SpscArrayQueueL2Pad(int capacity) {
        super(capacity);
    }
}

abstract class SpscArrayQueueConsumerField<E> extends SpscArrayQueueL2Pad<E> {
    protected long consumerIndex;
    protected final static long C_INDEX_OFFSET = UnsafeAccess.addressOf(SpscArrayQueueConsumerField.class, "consumerIndex");
    public SpscArrayQueueConsumerField(int capacity) {
        super(capacity);
    }
}

abstract class SpscArrayQueueL3Pad<E> extends SpscArrayQueueConsumerField<E> {
    long p40, p41, p42, p43, p44, p45, p46;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public SpscArrayQueueL3Pad(int capacity) {
        super(capacity);
    }
}

/**
 * A Single-Producer-Single-Consumer queue backed by a pre-allocated buffer.
 * <p>
 * This implementation is a mashup of the <a href="http://sourceforge.net/projects/mc-fastflow/">Fast Flow</a>
 * algorithm with an optimization of the offer method taken from the <a
 * href="http://staff.ustc.edu.cn/~bhua/publications/IJPP_draft.pdf">BQueue</a> algorithm (a variation on Fast
 * Flow), and adjusted to comply with Queue.offer semantics with regards to capacity.<br>
 * For convenience the relevant papers are available in the resources folder:<br>
 * <i>2010 - Pisa - SPSC Queues on Shared Cache Multi-Core Systems.pdf<br>
 * 2012 - Junchang- BQueue- Efficient and Practical Queuing.pdf <br>
 * </i> This implementation is wait free.
 * 
 * @author nitsanw
 * 
 * @param <E>
 */
public final class SpscArrayQueue<E> extends SpscArrayQueueL3Pad<E> {

    public SpscArrayQueue(final int capacity) {
        super(capacity);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation is correct for single producer thread use only.
     */
    @Override
    public boolean offer(final E e) {
        if (e == null) {
            throw new NullPointerException("null elements not allowed");
        }
        // local load of field to avoid repeated loads after volatile reads
        final E[] lElementBuffer = buffer;
        final long index = producerIndex;
        final long offset = calcElementOffset(index);
        if (null != lvElement(lElementBuffer, offset)){
            return false;
        }
        soElement(lElementBuffer, offset, e); // StoreStore
        soProducerIndex(index + 1); // ordered store -> atomic and ordered for size()
        return true;
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * This implementation is correct for single consumer thread use only.
     */
    @Override
    public E poll() {
        final long index = consumerIndex;
        final long offset = calcElementOffset(index);
        // local load of field to avoid repeated loads after volatile reads
        final E[] lElementBuffer = buffer;
        final E e = lvElement(lElementBuffer, offset);// LoadLoad
        if (null == e) {
            return null;
        }
        soElement(lElementBuffer, offset, null);// StoreStore
        soConsumerIndex(index + 1); // ordered store -> atomic and ordered for size()
        return e;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation is correct for single consumer thread use only.
     */
    @Override
    public E peek() {
        return lvElement(calcElementOffset(consumerIndex));
    }

    @Override
    public int size() {
        /*
         * It is possible for a thread to be interrupted or reschedule between the read of the producer and consumer
         * indices, therefore protection is required to ensure size is within valid range. In the event of concurrent
         * polls/offers to this method the size is OVER estimated as we read consumer index BEFORE the producer index.
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
    
    @Override
    public boolean isEmpty() {
        return lvProducerIndex() == lvConsumerIndex();
    }

    private void soProducerIndex(long v) {
        UNSAFE.putOrderedLong(this, P_INDEX_OFFSET, v);
    }

    private void soConsumerIndex(long v) {
        UNSAFE.putOrderedLong(this, C_INDEX_OFFSET, v);
    }
    
    private long lvProducerIndex() {
        return UNSAFE.getLongVolatile(this, P_INDEX_OFFSET);
    }
    
    private long lvConsumerIndex() {
        return UNSAFE.getLongVolatile(this, C_INDEX_OFFSET);
    }
}

