/**
 * Copyright 2015 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

/*
 * The code was inspired by the similarly named JCTools class: 
 * https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/atomic
 */

package io.reactivex.internal.queue;

import java.util.concurrent.atomic.AtomicLong;

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
 * @param <E>
 */
public final class SpscArrayQueue<E> extends BaseArrayQueue<E> {
    /** */
    private static final long serialVersionUID = -1296597691183856449L;
    private static final Integer MAX_LOOK_AHEAD_STEP = Integer.getInteger("jctools.spsc.max.lookahead.step", 4096);
    final AtomicLong producerIndex;
    protected long producerLookAhead;
    final AtomicLong consumerIndex;
    final int lookAheadStep;
    public SpscArrayQueue(int capacity) {
        super(capacity);
        this.producerIndex = new AtomicLong();
        this.consumerIndex = new AtomicLong();
        lookAheadStep = Math.min(capacity / 4, MAX_LOOK_AHEAD_STEP);
    }

    @Override
    public boolean offer(E e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }
        // local load of field to avoid repeated loads after volatile reads
        final int mask = this.mask;
        final long index = producerIndex.get();
        final int offset = calcElementOffset(index, mask);
        if (index >= producerLookAhead) {
            int step = lookAheadStep;
            if (null == lvElement(calcElementOffset(index + step, mask))) {// LoadLoad
                producerLookAhead = index + step;
            }
            else if (null != lvElement(offset)){
                return false;
            }
        }
        soProducerIndex(index + 1); // ordered store -> atomic and ordered for size()
        soElement(offset, e); // StoreStore
        return true;
    }

    @Override
    public E poll() {
        final long index = consumerIndex.get();
        final int offset = calcElementOffset(index);
        // local load of field to avoid repeated loads after volatile reads
        final E e = lvElement(offset);// LoadLoad
        if (null == e) {
            return null;
        }
        soConsumerIndex(index + 1); // ordered store -> atomic and ordered for size()
        soElement(offset, null);// StoreStore
        return e;
    }

    @Override
    public E peek() {
        return lvElement(calcElementOffset(consumerIndex.get()));
    }
    
    @Override
    public boolean isEmpty() {
        return producerIndex.get() == consumerIndex.get();
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

    private void soProducerIndex(long newIndex) {
        producerIndex.lazySet(newIndex);
    }

    private void soConsumerIndex(long newIndex) {
        consumerIndex.lazySet(newIndex);
    }
    
    private long lvConsumerIndex() {
        return consumerIndex.get();
    }
    private long lvProducerIndex() {
        return producerIndex.get();
    }

}

