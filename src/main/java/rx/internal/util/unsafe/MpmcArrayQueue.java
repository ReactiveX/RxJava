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
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/MpmcArrayQueue.java
 */
package rx.internal.util.unsafe;

import static rx.internal.util.unsafe.UnsafeAccess.UNSAFE;

abstract class MpmcArrayQueueL1Pad<E> extends ConcurrentSequencedCircularArrayQueue<E> {
    long p10, p11, p12, p13, p14, p15, p16;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public MpmcArrayQueueL1Pad(int capacity) {
        super(capacity);
    }
}

abstract class MpmcArrayQueueProducerField<E> extends MpmcArrayQueueL1Pad<E> {
    private final static long P_INDEX_OFFSET = UnsafeAccess.addressOf(MpmcArrayQueueProducerField.class, "producerIndex");
    private volatile long producerIndex;

    public MpmcArrayQueueProducerField(int capacity) {
        super(capacity);
    }

    protected final long lvProducerIndex() {
        return producerIndex;
    }

    protected final boolean casProducerIndex(long expect, long newValue) {
        return UNSAFE.compareAndSwapLong(this, P_INDEX_OFFSET, expect, newValue);
    }
}

abstract class MpmcArrayQueueL2Pad<E> extends MpmcArrayQueueProducerField<E> {
    long p20, p21, p22, p23, p24, p25, p26;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public MpmcArrayQueueL2Pad(int capacity) {
        super(capacity);
    }
}

abstract class MpmcArrayQueueConsumerField<E> extends MpmcArrayQueueL2Pad<E> {
    private final static long C_INDEX_OFFSET = UnsafeAccess.addressOf(MpmcArrayQueueConsumerField.class, "consumerIndex");
    private volatile long consumerIndex;

    public MpmcArrayQueueConsumerField(int capacity) {
        super(capacity);
    }

    protected final long lvConsumerIndex() {
        return consumerIndex;
    }

    protected final boolean casConsumerIndex(long expect, long newValue) {
        return UNSAFE.compareAndSwapLong(this, C_INDEX_OFFSET, expect, newValue);
    }
}

/**
 * A Multi-Producer-Multi-Consumer queue based on a {@link ConcurrentCircularArrayQueue}. This implies that
 * any and all threads may call the offer/poll/peek methods and correctness is maintained. <br>
 * This implementation follows patterns documented on the package level for False Sharing protection.<br>
 * The algorithm for offer/poll is an adaptation of the one put forward by D. Vyukov (See <a
 * href="http://www.1024cores.net/home/lock-free-algorithms/queues/bounded-mpmc-queue">here</a>). The original
 * algorithm uses an array of structs which should offer nice locality properties but is sadly not possible in
 * Java (waiting on Value Types or similar). The alternative explored here utilizes 2 arrays, one for each
 * field of the struct. There is a further alternative in the experimental project which uses iteration phase
 * markers to achieve the same algo and is closer structurally to the original, but sadly does not perform as
 * well as this implementation.<br>
 * Tradeoffs to keep in mind:
 * <ol>
 * <li>Padding for false sharing: counter fields and queue fields are all padded as well as either side of
 * both arrays. We are trading memory to avoid false sharing(active and passive).
 * <li>2 arrays instead of one: The algorithm requires an extra array of longs matching the size of the
 * elements array. This is doubling/tripling the memory allocated for the buffer.
 * <li>Power of 2 capacity: Actual elements buffer (and sequence buffer) is the closest power of 2 larger or
 * equal to the requested capacity.
 * </ol>
 * 
 * @param <E>
 *            type of the element stored in the {@link java.util.Queue}
 */
public class MpmcArrayQueue<E> extends MpmcArrayQueueConsumerField<E> {
    long p40, p41, p42, p43, p44, p45, p46;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public MpmcArrayQueue(final int capacity) {
        super(Math.max(2, capacity));
    }

    @Override
    public boolean offer(final E e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }

        // local load of field to avoid repeated loads after volatile reads
        final long capacity = mask + 1;
        final long[] lSequenceBuffer = sequenceBuffer;
        long currentProducerIndex;
        long seqOffset;
        long cIndex = Long.MAX_VALUE;// start with bogus value, hope we don't need it
        while (true) {
            currentProducerIndex = lvProducerIndex(); // LoadLoad
            seqOffset = calcSequenceOffset(currentProducerIndex);
            final long seq = lvSequence(lSequenceBuffer, seqOffset); // LoadLoad
            final long delta = seq - currentProducerIndex;

            if (delta == 0) {
                // this is expected if we see this first time around
                if (casProducerIndex(currentProducerIndex, currentProducerIndex + 1)) {
                    // Successful CAS: full barrier
                    break;
                }
                // failed cas, retry 1
            } else if (delta < 0 && // poll has not moved this value forward
                    currentProducerIndex - capacity <= cIndex && // test against cached cIndex
                    currentProducerIndex - capacity <= (cIndex = lvConsumerIndex())) { // test against latest cIndex
                // Extra check required to ensure [Queue.offer == false iff queue is full]
                return false;
            }

            // another producer has moved the sequence by one, retry 2
        }

        // on 64bit(no compressed oops) JVM this is the same as seqOffset
        final long elementOffset = calcElementOffset(currentProducerIndex);
        spElement(elementOffset, e);

        // increment sequence by 1, the value expected by consumer
        // (seeing this value from a producer will lead to retry 2)
        soSequence(lSequenceBuffer, seqOffset, currentProducerIndex + 1); // StoreStore

        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because return null indicates queue is empty we cannot simply rely on next element visibility for poll
     * and must test producer index when next element is not visible.
     */
    @Override
    public E poll() {
        // local load of field to avoid repeated loads after volatile reads
        final long[] lSequenceBuffer = sequenceBuffer;
        long currentConsumerIndex;
        long seqOffset;
        long pIndex = -1; // start with bogus value, hope we don't need it
        while (true) {
            currentConsumerIndex = lvConsumerIndex();// LoadLoad
            seqOffset = calcSequenceOffset(currentConsumerIndex);
            final long seq = lvSequence(lSequenceBuffer, seqOffset);// LoadLoad
            final long delta = seq - (currentConsumerIndex + 1);

            if (delta == 0) {
                if (casConsumerIndex(currentConsumerIndex, currentConsumerIndex + 1)) {
                    // Successful CAS: full barrier
                    break;
                }
                // failed cas, retry 1
            } else if (delta < 0 && // slot has not been moved by producer
                    currentConsumerIndex >= pIndex && // test against cached pIndex
                    currentConsumerIndex == (pIndex = lvProducerIndex())) { // update pIndex if we must
                // strict empty check, this ensures [Queue.poll() == null iff isEmpty()]
                return null;
            }

            // another consumer beat us and moved sequence ahead, retry 2
        }

        // on 64bit(no compressed oops) JVM this is the same as seqOffset
        final long offset = calcElementOffset(currentConsumerIndex);
        final E e = lpElement(offset);
        spElement(offset, null);

        // Move sequence ahead by capacity, preparing it for next offer
        // (seeing this value from a consumer will lead to retry 2)
        soSequence(lSequenceBuffer, seqOffset, currentConsumerIndex + mask + 1);// StoreStore

        return e;
    }

    @Override
    public E peek() {
        long currConsumerIndex;
        E e;
        do {
            currConsumerIndex = lvConsumerIndex();
            // other consumers may have grabbed the element, or queue might be empty
            e = lpElement(calcElementOffset(currConsumerIndex));
            // only return null if queue is empty
        } while (e == null && currConsumerIndex != lvProducerIndex());
        return e;
    }

    @Override
    public int size() {
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

    @Override
    public boolean isEmpty() {
        // Order matters!
        // Loading consumer before producer allows for producer increments after consumer index is read.
        // This ensures this method is conservative in it's estimate. Note that as this is an MPMC there is
        // nothing we can do to make this an exact method.
        return (lvConsumerIndex() == lvProducerIndex());
    }
}
