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
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/SpscLinkedQueue.java
 */
package rx.internal.util.unsafe;

import rx.internal.util.atomic.LinkedQueueNode;



/**
 * This is a weakened version of the MPSC algorithm as presented <a
 * href="http://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue"> on 1024
 * Cores</a> by D. Vyukov. The original has been adapted to Java and it's quirks with regards to memory model and
 * layout:
 * <ol>
 * <li>Use inheritance to ensure no false sharing occurs between producer/consumer node reference fields.
 * <li>As this is an SPSC we have no need for XCHG, an ordered store is enough.
 * </ol>
 * The queue is initialized with a stub node which is set to both the producer and consumer node references. From this
 * point follow the notes on offer/poll.
 * 
 * @author nitsanw
 * 
 * @param <E>
 */
public final class SpscLinkedQueue<E> extends BaseLinkedQueue<E> {

    public SpscLinkedQueue() {
        spProducerNode(new LinkedQueueNode<E>());
        spConsumerNode(producerNode);
        consumerNode.soNext(null); // this ensures correct construction: StoreStore
    }

    /**
     * {@inheritDoc} <br>
     * 
     * IMPLEMENTATION NOTES:<br>
     * Offer is allowed from a SINGLE thread.<br>
     * Offer allocates a new node (holding the offered value) and:
     * <ol>
     * <li>Sets that node as the producerNode.next
     * <li>Sets the new node as the producerNode
     * </ol>
     * From this follows that producerNode.next is always null and for all other nodes node.next is not null.
     * 
     * @see MessagePassingQueue#offer(Object)
     * @see java.util.Queue#offer(java.lang.Object)
     */
    @Override
    public boolean offer(final E nextValue) {
        if (nextValue == null) {
            throw new NullPointerException("null elements not allowed");
        }
        final LinkedQueueNode<E> nextNode = new LinkedQueueNode<E>(nextValue);
        producerNode.soNext(nextNode);
        producerNode = nextNode;
        return true;
    }

    /**
     * {@inheritDoc} <br>
     * 
     * IMPLEMENTATION NOTES:<br>
     * Poll is allowed from a SINGLE thread.<br>
     * Poll reads the next node from the consumerNode and:
     * <ol>
     * <li>If it is null, the queue is empty.
     * <li>If it is not null set it as the consumer node and return it's now evacuated value.
     * </ol>
     * This means the consumerNode.value is always null, which is also the starting point for the queue. Because null
     * values are not allowed to be offered this is the only node with it's value set to null at any one time.
     * 
     */
    @Override
    public E poll() {
        final LinkedQueueNode<E> nextNode = consumerNode.lvNext();
        if (nextNode != null) {
            // we have to null out the value because we are going to hang on to the node
            final E nextValue = nextNode.getAndNullValue();
            consumerNode = nextNode;
            return nextValue;
        }
        return null;
    }

    @Override
    public E peek() {
        final LinkedQueueNode<E> nextNode = consumerNode.lvNext();
        if (nextNode != null) {
            return nextNode.lpValue();
        } else {
            return null;
        }
    }
}