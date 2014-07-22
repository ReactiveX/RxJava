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
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/MpscLinkedQueue7.java
 */
/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * Original License: https://github.com/aweisberg/MpscLinkedBlockingQueue/blob/master/LICENSE
 * Original location: https://github.com/aweisberg/MpscLinkedBlockingQueue/blob/master/src/MpscLinkedQueue.java
 */
/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package rx.internal.util.unsafe;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static rx.internal.util.unsafe.UnsafeAccess.UNSAFE;

abstract class MpscLinkedBlockingQueuePad0<E> implements BlockingQueue<E> {
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p30, p31, p32, p33, p34, p35, p36, p37;
}

abstract class MpscLinkedBlockingQueueProducerNodeRef<E> extends MpscLinkedBlockingQueuePad0<E> {
    protected final static long P_NODE_OFFSET;

    static {
        try {
            P_NODE_OFFSET = UNSAFE.objectFieldOffset(MpscLinkedBlockingQueueProducerNodeRef.class.getDeclaredField("producerNode"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    protected volatile LinkedQueueNode<E> producerNode;

    @SuppressWarnings("unchecked")
    protected final LinkedQueueNode<E> xchgProducerNode(LinkedQueueNode<E> newVal) {
        Object oldVal;
        do {
            oldVal = producerNode;
        } while(!UNSAFE.compareAndSwapObject(this, P_NODE_OFFSET, oldVal, newVal));
        return (LinkedQueueNode<E>) oldVal;
    }
}

abstract class MpscLinkedBlockingQueuePad1<E> extends MpscLinkedBlockingQueueProducerNodeRef<E> {
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p30, p31, p32, p33, p34, p35, p36, p37;
}

abstract class MpscLinkedBlockingQueueConsumerNodeRef<E> extends MpscLinkedBlockingQueuePad1<E> {
    protected LinkedQueueNode<E> consumerNode;
}

abstract class MpscLinkedBlockingQueuePad2<E> extends MpscLinkedBlockingQueueConsumerNodeRef<E> {
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p30, p31, p32, p33, p34, p35, p36, p37;
}

abstract class MpscLinkedBlockingQueueWaiterStorage<E> extends MpscLinkedBlockingQueuePad2<E> {
    protected volatile Thread waiter;
}

/**
 * This is a direct Java port of the MPSC algorithm as presented <a
 * href="http://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue"> on 1024
 * Cores</a> by D. Vyukov. The original has been adapted to Java and it's quirks with regards to memory model and
 * layout:
 * <ol>
 * <li>Use inheritance to ensure no false sharing occurs between producer/consumer node reference fields.
 * <li>Use {@link java.util.concurrent.atomic.AtomicReferenceFieldUpdater} to provide XCHG functionality to the best of the JDK ability.
 * </ol>
 * The queue is initialized with a stub node which is set to both the producer and consumer node references. From this
 * point follow the notes on offer/poll.
 *
 * @author nitsanw
 *
 * @param <E>
 */
public final class MpscLinkedBlockingQueue<E> extends MpscLinkedBlockingQueueWaiterStorage<E> {
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p30, p31, p32, p33, p34, p35, p36, p37;

    public MpscLinkedBlockingQueue() {
        consumerNode = new LinkedQueueNode<E>();
        producerNode = consumerNode;// this ensures correct construction: StoreLoad
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    /**
     * {@inheritDoc} <br>
     *
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
     * @see java.util.Queue#offer(Object)
     */
    @Override
    public boolean offer(final E nextValue) {
        if (nextValue == null) {
            throw new NullPointerException("null elements not allowed");
        }
        final LinkedQueueNode<E> nextNode = new LinkedQueueNode<E>(nextValue);
        final LinkedQueueNode<E> prevProducerNode = xchgProducerNode(nextNode);
        // Should a producer thread get interrupted here the chain WILL be broken until that thread is resumed
        // and completes the store in prev.next.
        prevProducerNode.soNext(nextNode); // StoreStore

        final Thread waiterLocal = waiter;
        if (waiterLocal != null) {
            waiter = null;
            LockSupport.unpark(waiterLocal);
        }

        return true;
    }

    @Override
    public E remove() {
        E value = poll();
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;
    }

    @Override
    public void put(E e) throws InterruptedException {
        offer(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        offer(e);
        return true;
    }

    @Override
    public E take() throws InterruptedException {
        return awaitNotEmpty(false, 0);
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return awaitNotEmpty(true, unit.toNanos(timeout));
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o: c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {
        for (;;) {
            if (poll() == null) {
                break;
            }
        }
    }

    @Override
    public boolean contains(Object o) {
        LinkedQueueNode<E> n = peekNode();
        for (;;) {
            if (n == null) {
                break;
            }
            if (o.equals(n.lpValue())) {
                return true;
            }
            n = n.lvNext();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private LinkedQueueNode<E> peekNode() {
        return consumerNode.lvNext();
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) throw new NullPointerException();
        if (c == this) throw new IllegalArgumentException();
        int transferred = 0;
        E val;
        while (transferred <= maxElements && (val = poll()) != null) {
            c.add(val);
            transferred++;
        }
        return transferred;
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
     * @see java.util.Queue#poll()
     */
    @Override
    public E poll() {
        E e = tryPoll();
        if(e == null && !isEmpty()) {
            // Spin wait for the element to appear. This buggers up wait freedom.
            do {
                e = tryPoll();
            } while (e == null);
        }
        return e;
    }

    @Override
    public E element() {
        final E next = tryPeek();
        if (next == null) {
            throw new NoSuchElementException();
        }
        return next;
    }

    public E tryPoll() {
        LinkedQueueNode<E> nextNode = consumerNode.lvNext();
        if (nextNode != null) {
            // we have to null out the value because we are going to hang on to the node
            final E nextValue = nextNode.evacuateValue();
            consumerNode = nextNode;
            return nextValue;
        }
        return null;
    }

    @Override
    public E peek() {
        E e;
        // if the queue is truly empty these 2 are the same. Sadly this means we spin on the producer field...
        while ((e = tryPeek()) == null && producerNode != consumerNode) {
            // spin
        }
        return e;
    }

    public E tryPeek() {
        LinkedQueueNode<E> nextNode = consumerNode.lvNext();
        if (nextNode != null) {
            return nextNode.lpValue();
        }
        return null;
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc} <br>
     *
     * IMPLEMENTATION NOTES:<br>
     * This is an O(n) operation as we run through all the nodes and count them.<br>
     *
     * @see java.util.Queue#size()
     */
    @Override
    public int size() {
        LinkedQueueNode<E> temp = consumerNode;
        int size = 0;
        while ((temp = temp.lvNext()) != null && size < Integer.MAX_VALUE) {
            size++;
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        return consumerNode == producerNode;
    }

    /**
     * The number of times to spin (with randomly interspersed calls
     * to Thread.yield) on multiprocessor before blocking when a node
     * is apparently the first waiter in the queue.  See above for
     * explanation. Must be a power of two. The value is empirically
     * derived -- it works pretty well across a variety of processors,
     * numbers of CPUs, and OSes.
     */
    private static final int FRONT_SPINS   = 1 << 7;

    /**
     * The number of times to spin before blocking when a node is
     * preceded by another node that is apparently spinning.  Also
     * serves as an increment to FRONT_SPINS on phase changes, and as
     * base average frequency for yielding during spins. Must be a
     * power of two.
     */
    private static final int CHAINED_SPINS = FRONT_SPINS >>> 1;

    private E awaitNotEmpty(boolean timed, long nanos) throws InterruptedException {
        long lastTime = timed ? System.nanoTime() : 0L;
        Thread w = Thread.currentThread();
        int spins = -1; // initialized after first item and cancel checks
        ThreadLocalRandom randomYields = null; // bound if needed

        for (;;) {
            E retval = poll();
            if (retval != null) return retval;

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            if (timed && nanos <= 0) {
                return null;
            }

            if (spins < 0) { // establish spins at/near front
                spins = FRONT_SPINS;
                randomYields = ThreadLocalRandom.current();
            } else if (spins > 0) { // spin
                --spins;
                if (randomYields != null && randomYields.nextInt(CHAINED_SPINS) == 0) {
                    Thread.yield(); // occasionally yield
                }
            } else if (waiter == null) {
                waiter = w; // request unpark then recheck
            } else if (timed) {
                long now = System.nanoTime();
                if ((nanos -= now - lastTime) > 0) {
                    LockSupport.parkNanos(this, nanos);
                }
                lastTime = now;
            } else {
                LockSupport.park(this);
            }
        }
    }
}
