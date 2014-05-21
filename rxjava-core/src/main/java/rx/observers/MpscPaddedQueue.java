/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.observers;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A multiple-producer single consumer queue implementation with padded reference
 * to tail to avoid cache-line thrashing.
 * Based on Netty's <a href='https://github.com/netty/netty/blob/master/common/src/main/java/io/netty/util/internal/MpscLinkedQueue.java'>MpscQueue implementation</a> but using AtomicReferenceFieldUpdater
 * instead of Unsafe.
 * @param <E> the element type
 */
public final class MpscPaddedQueue<E> extends AtomicReference<MpscPaddedQueue.Node<E>> {
    @SuppressWarnings(value = "rawtypes")
    static final AtomicReferenceFieldUpdater<PaddedNode, Node> TAIL_UPDATER = AtomicReferenceFieldUpdater.newUpdater(PaddedNode.class, Node.class, "tail");
    /** */
    private static final long serialVersionUID = 1L;
    /** The padded tail reference. */
    final PaddedNode<E> tail;
    /**
     * Initializes the empty queue.
     */
    public MpscPaddedQueue() {
        Node<E> first = new Node<E>(null);
        tail = new PaddedNode<E>();
        tail.tail = first;
        set(first);
    }
    /**
     * Offer a new value.
     * @param v the value to offer
     */
    public void offer(E v) {
        Node<E> n = new Node<E>(v);
        getAndSet(n).set(n);
    }

    /**
     * @return Poll a value from the head of the queue or return null if the queue is empty.
     */
    public E poll() {
        Node<E> n = peekNode();
        if (n == null) {
            return null;
        }
        E v = n.value;
        n.value = null; // do not retain this value as the node still stays in the queue
        TAIL_UPDATER.lazySet(tail, n);
        return v;
    }
    /**
     * Check if there is a node available without changing anything.
     */
    private Node<E> peekNode() {
        for (;;) {
            @SuppressWarnings(value = "unchecked")
            Node<E> t = TAIL_UPDATER.get(tail);
            Node<E> n = t.get();
            if (n != null || get() == t) {
                return n;
            }
        }
    }
    /**
     * Clears the queue.
     */
    public void clear() {
        for (;;) {
            if (poll() == null) {
                break;
            }
        }
    }
    /** Class that contains a Node reference padded around to fit a typical cache line. */
    static final class PaddedNode<E> {
        /** Padding, public to prevent optimizing it away. */
        public int p1;
        volatile Node<E> tail;
        /** Padding, public to prevent optimizing it away. */
        public long p2;
        /** Padding, public to prevent optimizing it away. */
        public long p3;
        /** Padding, public to prevent optimizing it away. */
        public long p4;
        /** Padding, public to prevent optimizing it away. */
        public long p5;
        /** Padding, public to prevent optimizing it away. */
        public long p6;
    }
    /**
     * Regular node with value and reference to the next node.
     */
    static final class Node<E> {

        E value;
        @SuppressWarnings(value = "rawtypes")
        static final AtomicReferenceFieldUpdater<Node, Node> TAIL_UPDATER = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "tail");
        volatile Node<E> tail;

        public Node(E value) {
            this.value = value;
        }

        public void set(Node<E> newTail) {
            TAIL_UPDATER.lazySet(this, newTail);
        }

        @SuppressWarnings(value = "unchecked")
        public Node<E> get() {
            return TAIL_UPDATER.get(this);
        }
    }
    
}
