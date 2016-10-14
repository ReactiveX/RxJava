package rx.internal.util.unsafe;
import java.util.AbstractQueue;

import rx.internal.util.atomic.LinkedQueueNode;


import static rx.internal.util.unsafe.UnsafeAccess.UNSAFE;

/**
 * Created by kgalligan on 7/9/16.
 */
public abstract class BaseLinkedQueueProducerNodeRef<E> extends BaseLinkedQueuePad0<E> {
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

abstract class BaseLinkedQueuePad0<E> extends AbstractQueue<E>
{
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p30, p31, p32, p33, p34, p35, p36, p37;
}