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
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/atomic/BaseLinkedQueue.java
 */
package rx.internal.util.unsafe;

import static rx.internal.util.unsafe.UnsafeAccess.UNSAFE;

import java.util.*;

import rx.internal.util.atomic.LinkedQueueNode;

abstract class BaseLinkedQueuePad0<E> extends AbstractQueue<E> {
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p30, p31, p32, p33, p34, p35, p36, p37;
}

abstract class BaseLinkedQueueProducerNodeRef<E> extends BaseLinkedQueuePad0<E> {
    protected final static long P_NODE_OFFSET = UnsafeAccess.addressOf(BaseLinkedQueueProducerNodeRef.class, "producerNode");

    protected LinkedQueueNode<E> producerNode;
    protected final void spProducerNode(LinkedQueueNode<E> node) {
        producerNode = node;
    }
    
    @SuppressWarnings("unchecked")
    protected final LinkedQueueNode<E> lvProducerNode() {
        return (LinkedQueueNode<E>) UNSAFE.getObjectVolatile(this, P_NODE_OFFSET);
    }
    
    protected final LinkedQueueNode<E> lpProducerNode() {
        return producerNode;
    }
}

abstract class BaseLinkedQueuePad1<E> extends BaseLinkedQueueProducerNodeRef<E> {
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p30, p31, p32, p33, p34, p35, p36, p37;
}

abstract class BaseLinkedQueueConsumerNodeRef<E> extends BaseLinkedQueuePad1<E> {
    protected final static long C_NODE_OFFSET = UnsafeAccess.addressOf(BaseLinkedQueueConsumerNodeRef.class, "consumerNode");
    protected LinkedQueueNode<E> consumerNode;
    protected final void spConsumerNode(LinkedQueueNode<E> node) {
        consumerNode = node;
    }
    
    @SuppressWarnings("unchecked")
    protected final LinkedQueueNode<E> lvConsumerNode() {
        return (LinkedQueueNode<E>) UNSAFE.getObjectVolatile(this, C_NODE_OFFSET);
    }
    
    protected final LinkedQueueNode<E> lpConsumerNode() {
        return consumerNode;
    }
}

/**
 * A base data structure for concurrent linked queues.
 * 
 * @author nitsanw
 * 
 * @param <E>
 */
abstract class BaseLinkedQueue<E> extends BaseLinkedQueueConsumerNodeRef<E> {
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p30, p31, p32, p33, p34, p35, p36, p37;


    @Override
    public final Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc} <br>
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * This is an O(n) operation as we run through all the nodes and count them.<br>
     * 
     * @see java.util.Queue#size()
     */
    @Override
    public final int size() {
        // Read consumer first, this is important because if the producer is node is 'older' than the consumer the
        // consumer may overtake it (consume past it). This will lead to an infinite loop below.
        LinkedQueueNode<E> chaserNode = lvConsumerNode();
        final LinkedQueueNode<E> producerNode = lvProducerNode();
        int size = 0;
        // must chase the nodes all the way to the producer node, but there's no need to chase a moving target.
        while (chaserNode != producerNode && size < Integer.MAX_VALUE) {
            LinkedQueueNode<E> next;
            while((next = chaserNode.lvNext()) == null);
            chaserNode = next;
            size++;
        }
        return size;
    }
    
    /**
     * {@inheritDoc} <br>
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * Queue is empty when producerNode is the same as consumerNode. An alternative implementation would be to observe
     * the producerNode.value is null, which also means an empty queue because only the consumerNode.value is allowed to
     * be null.
     * 
     * @see MessagePassingQueue#isEmpty()
     */
    @Override
    public final boolean isEmpty() {
        return lvConsumerNode() == lvProducerNode();
    }
}