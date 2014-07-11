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

import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import rx.internal.util.MpscPaddedQueue.Node;

abstract class MpscLinkedQueuePad0<E> {
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p30, p31, p32, p33, p34, p35, p36, p37;
}

abstract class MpscLinkedQueueHeadRef<E> extends MpscLinkedQueuePad0<E> {
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<MpscLinkedQueueHeadRef, Node> UPDATER =
        newUpdater(MpscLinkedQueueHeadRef.class, Node.class, "headRef");
    private volatile Node<E> headRef;

    protected final Node<E> headRef() {
        return headRef;
    }
    protected final void headRef(Node<E> val) {
        headRef = val;
    }
    protected final void lazySetHeadRef(Node<E> newVal) {
        UPDATER.lazySet(this, newVal);
    }
}

abstract class MpscLinkedQueuePad1<E> extends MpscLinkedQueueHeadRef<E> {
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p30, p31, p32, p33, p34, p35, p36, p37;
}

abstract class MpscLinkedQueueTailRef<E> extends MpscLinkedQueuePad1<E> {
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<MpscLinkedQueueTailRef, Node> UPDATER =
        newUpdater(MpscLinkedQueueTailRef.class, Node.class, "tailRef");
    private volatile Node<E> tailRef;
    protected final Node<E> tailRef() {
        return tailRef;
    }
    protected final void tailRef(Node<E> val) {
        tailRef = val;
    }
    @SuppressWarnings("unchecked")
    protected final Node<E> getAndSetTailRef(Node<E> newVal) {
        return (Node<E>) UPDATER.getAndSet(this, newVal);
    }
}
/**
 * A multiple-producer single consumer queue implementation with padded reference to tail to avoid cache-line
 * thrashing. Based on Netty's <a href='https://github.com/netty/netty/blob/master/common/src/main/java/io/netty/util/internal/MpscLinkedQueue.java'>MpscQueue implementation</a>
 * but using {@code AtomicReferenceFieldUpdater} instead of {@code Unsafe}.<br>
 * Original algorithm presented <a
 * href="http://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue"> on 1024
 * Cores</a> by D. Vyukov.<br>
 * Data structure modified to avoid false sharing between head and tail references as per implementation of
 * MpscLinkedQueue on <a href="https://github.com/JCTools/JCTools">JCTools project</a>.
 * 
 * @param <E> the element type
 */
public final class MpscPaddedQueue<E> extends MpscLinkedQueueTailRef<E> {
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p30, p31, p32, p33, p34, p35, p36, p37;
    /**
     * Initializes the empty queue.
     */
    public MpscPaddedQueue() {
        Node<E> stub = new Node<E>(null);
        headRef(stub);
        tailRef(stub);
    }

    /**
     * Offer a new value.
     *
     * @param v the value to offer
     */
    public void offer(E v) {
        Node<E> n = new Node<E>(v);
        getAndSetTailRef(n).next(n);
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
        lazySetHeadRef(n);
        return v;
    }
    
    /**
     * Check if there is a node available without changing anything.
     * @return
     */
    private Node<E> peekNode() {
        for (;;) {
            Node<E> t = headRef();
            Node<E> n = t.next();
            if (n != null || headRef() == t) {
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

    /**
     * Regular node with value and reference to the next node.
     */
    static final class Node<E> {
        E value;
        @SuppressWarnings(value = "rawtypes")
        static final AtomicReferenceFieldUpdater<Node, Node> TAIL_UPDATER = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "next");
        private volatile Node<E> next;

        Node(E value) {
            this.value = value;
        }

        void next(Node<E> newNext) {
            TAIL_UPDATER.lazySet(this, newNext);
        }

        Node<E> next() {
            return next;
        }
    }
    
}
