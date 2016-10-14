package rx.internal.util.unsafe;
import java.util.AbstractQueue;

import rx.internal.util.atomic.LinkedQueueNode;


import static rx.internal.util.unsafe.UnsafeAccess.UNSAFE;

/**
 * Created by kgalligan on 7/9/16.
 */
public abstract class BaseLinkedQueueConsumerNodeRef<E> extends BaseLinkedQueuePad1<E> {
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

abstract class BaseLinkedQueuePad1<E> extends BaseLinkedQueueProducerNodeRef<E> {
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p30, p31, p32, p33, p34, p35, p36, p37;
}

