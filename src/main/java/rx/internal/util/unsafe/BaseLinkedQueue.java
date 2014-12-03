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
 */
package rx.internal.util.unsafe;

import java.util.AbstractQueue;
import java.util.Iterator;

abstract class BaseLinkedQueuePad0<E> extends AbstractQueue<E> {
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p30, p31, p32, p33, p34, p35, p36, p37;
}

abstract class BaseLinkedQueueProducerNodeRef<E> extends BaseLinkedQueuePad0<E> {
    protected final static long P_NODE_OFFSET;

    static {
        try {
            P_NODE_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(BaseLinkedQueueProducerNodeRef.class.getDeclaredField("producerNode"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    protected LinkedQueueNode<E> producerNode;
    protected final void spProducerNode(LinkedQueueNode<E> node) {
        producerNode = node;
    }
    
    @SuppressWarnings("unchecked")
    protected final LinkedQueueNode<E> lvProducerNode() {
        return (LinkedQueueNode<E>) UnsafeAccess.UNSAFE.getObjectVolatile(this, P_NODE_OFFSET);
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
    protected final static long C_NODE_OFFSET;

    static {
        try {
            C_NODE_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(BaseLinkedQueueConsumerNodeRef.class.getDeclaredField("consumerNode"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    protected LinkedQueueNode<E> consumerNode;
    protected final void spConsumerNode(LinkedQueueNode<E> node) {
        consumerNode = node;
    }
    
    @SuppressWarnings("unchecked")
    protected final LinkedQueueNode<E> lvConsumerNode() {
        return (LinkedQueueNode<E>) UnsafeAccess.UNSAFE.getObjectVolatile(this, C_NODE_OFFSET);
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
        LinkedQueueNode<E> temp = lvConsumerNode();
        int size = 0;
        while ((temp = temp.lvNext()) != null && size < Integer.MAX_VALUE) {
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