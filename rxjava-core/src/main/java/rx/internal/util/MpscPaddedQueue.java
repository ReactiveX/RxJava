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
package rx.internal.util;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A multiple-producer single consumer queue implementation with padded reference to tail to avoid cache-line
 * thrashing. Based on Netty's <a href='https://github.com/netty/netty/blob/master/common/src/main/java/io/netty/util/internal/MpscLinkedQueue.java'>MpscQueue implementation</a>
 * but using {@code AtomicReferenceFieldUpdater} instead of {@code Unsafe}.
 *
 * @param <E> the element type
 */
public final class MpscPaddedQueue<E> extends AtomicReference<MpscPaddedQueue.Node<E>> {
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
        tail.node = first;
        set(first);
    }

    /**
     * Offer a new value.
     *
     * @param v the value to offer
     */
    public void offer(E v) {
        Node<E> n = new Node<E>(v);
        getAndSet(n).set(n);
    }

    /**
     * @warn method description missing
     * @return Poll a value from the head of the queue or return null if the queue is empty.
     */
    public E poll() {
        Node<E> n = peekNode();
        if (n == null) {
            return null;
        }
        E v = n.value;
        n.value = null; // do not retain this value as the node still stays in the queue
        tail.lazySet(n);
        return v;
    }

    /**
     * Check if there is a node available without changing anything.
     * @return
     */
    private Node<E> peekNode() {
        for (;;) {
            @SuppressWarnings(value = "unchecked")
            Node<E> t = tail.node;
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
    /** The front-padded node class housing the actual value. */
    static abstract class PaddedNodeBase<E> extends FrontPadding {
        private static final long serialVersionUID = 2L;
        volatile Node<E> node;
        @SuppressWarnings(value = "rawtypes")
        static final AtomicReferenceFieldUpdater<PaddedNodeBase, Node> NODE_UPDATER = AtomicReferenceFieldUpdater.newUpdater(PaddedNodeBase.class, Node.class, "node");
        public void lazySet(Node<E> newValue) {
            NODE_UPDATER.lazySet(this, newValue);
        }
    }
    /** Post-padding of the padded node base class.  */
    static final class PaddedNode<E> extends PaddedNodeBase<E> {
        private static final long serialVersionUID = 3L;
        /** Padding. */
        public transient long p16, p17, p18, p19, p20, p21, p22;      // 56 bytes (the remaining 8 is in the base)
        /** Padding. */
        public transient long p24, p25, p26, p27, p28, p29, p30, p31; // 64 bytes
    }

    /**
     * Regular node with value and reference to the next node.
     */
    static final class Node<E> implements Serializable {
        private static final long serialVersionUID = 4L;
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
