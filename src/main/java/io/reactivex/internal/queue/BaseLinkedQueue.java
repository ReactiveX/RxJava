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

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.internal.fuseable.SimpleQueue;

abstract class BaseLinkedQueue<E> implements SimpleQueue<E> {
    private final AtomicReference<LinkedQueueNode<E>> producerNode;
    private final AtomicReference<LinkedQueueNode<E>> consumerNode;
    public BaseLinkedQueue() {
        producerNode = new AtomicReference<LinkedQueueNode<E>>();
        consumerNode = new AtomicReference<LinkedQueueNode<E>>();
    }
    protected final LinkedQueueNode<E> lvProducerNode() {
        return producerNode.get();
    }
    protected final LinkedQueueNode<E> lpProducerNode() {
        return producerNode.get();
    }
    protected final void spProducerNode(LinkedQueueNode<E> node) {
        producerNode.lazySet(node);
    }
    protected final LinkedQueueNode<E> xchgProducerNode(LinkedQueueNode<E> node) {
        return producerNode.getAndSet(node);
    }
    protected final LinkedQueueNode<E> lvConsumerNode() {
        return consumerNode.get();
    }
    
    protected final LinkedQueueNode<E> lpConsumerNode() {
        return consumerNode.get();
    }
    protected final void spConsumerNode(LinkedQueueNode<E> node) {
        consumerNode.lazySet(node);
    }

    /**
     * {@inheritDoc} <br>
     * <p>
     * IMPLEMENTATION NOTES:<br>
     * Queue is empty when producerNode is the same as consumerNode. An alternative implementation would be to observe
     * the producerNode.value is null, which also means an empty queue because only the consumerNode.value is allowed to
     * be null.
     */
    @Override
    public final boolean isEmpty() {
        return lvConsumerNode() == lvProducerNode();
    }
}