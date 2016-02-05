/**
 * Copyright 2016 Netflix, Inc.
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

/**
 * A multi-producer single consumer unbounded queue.
 * @param <T> the contained value type
 */
public final class MpscLinkedQueue<T> extends BaseLinkedQueue<T> {

    public MpscLinkedQueue() {
        super();
        LinkedQueueNode<T> node = new LinkedQueueNode<T>();
        spConsumerNode(node);
        xchgProducerNode(node);// this ensures correct construction: StoreLoad
    }
    /**
     * {@inheritDoc} <br>
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * Offer is allowed from multiple threads.<br>
     * Offer allocates a new node and:
     * <ol>
     * <li>Swaps it atomically with current producer node (only one producer 'wins')
     * <li>Sets the new node as the node following from the swapped producer node
     * </ol>
     * This works because each producer is guaranteed to 'plant' a new node and link the old node. No 2 producers can
     * get the same producer node as part of XCHG guarantee.
     * 
     * @see java.util.Queue#offer(java.lang.Object)
     */
    @Override
    public final boolean offer(final T nextValue) {
        final LinkedQueueNode<T> nextNode = new LinkedQueueNode<T>(nextValue);
        final LinkedQueueNode<T> prevProducerNode = xchgProducerNode(nextNode);
        // Should a producer thread get interrupted here the chain WILL be broken until that thread is resumed
        // and completes the store in prev.next.
        prevProducerNode.soNext(nextNode); // StoreStore
        return true;
    }

    /**
     * {@inheritDoc} <br>
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * Poll is allowed from a SINGLE thread.<br>
     * Poll reads the next node from the consumerNode and:
     * <ol>
     * <li>If it is null, the queue is assumed empty (though it might not be).
     * <li>If it is not null set it as the consumer node and return it's now evacuated value.
     * </ol>
     * This means the consumerNode.value is always null, which is also the starting point for the queue. Because null
     * values are not allowed to be offered this is the only node with it's value set to null at any one time.
     * 
     * @see java.util.Queue#poll()
     */
    @Override
    public final T poll() {
        LinkedQueueNode<T> currConsumerNode = lpConsumerNode(); // don't load twice, it's alright
        LinkedQueueNode<T> nextNode = currConsumerNode.lvNext();
        if (nextNode != null) {
            // we have to null out the value because we are going to hang on to the node
            final T nextValue = nextNode.getAndNullValue();
            spConsumerNode(nextNode);
            return nextValue;
        }
        else if (currConsumerNode != lvProducerNode()) {
            // spin, we are no longer wait free
            while((nextNode = currConsumerNode.lvNext()) == null);
            // got the next node...
            
            // we have to null out the value because we are going to hang on to the node
            final T nextValue = nextNode.getAndNullValue();
            spConsumerNode(nextNode);
            return nextValue;
        }
        return null;
    }

    @Override
    public final T peek() {
        LinkedQueueNode<T> currConsumerNode = lpConsumerNode(); // don't load twice, it's alright
        LinkedQueueNode<T> nextNode = currConsumerNode.lvNext();
        if (nextNode != null) {
            return nextNode.lpValue();
        } else 
        if (currConsumerNode != lvProducerNode()) {
            // spin, we are no longer wait free
            while ((nextNode = currConsumerNode.lvNext()) == null);
            // got the next node...
            return nextNode.lpValue();
        }
        return null;
    }
}
