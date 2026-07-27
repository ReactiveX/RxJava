/*
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.rxjava4.internal.operators.flowable;

import java.io.Serial;
import java.util.concurrent.atomic.*;

import static java.util.concurrent.Flow.*;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava4.internal.util.BackpressureHelper;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;

/**
 * An observable which auto-connects to another observable, caches the elements
 * from that observable but allows terminating the connection and completing the cache.
 *
 * @param <T> the source element type
 */
public final class FlowableCache<T> extends AbstractFlowableWithUpstream<T, T> {

    /**
     * The subscription to the source should happen at most once.
     */
    final AtomicBoolean once;

    /**
     * A shared instance of an empty array of subscribers to avoid creating
     * a new empty array when all subscribers cancel.
     */
    @SuppressWarnings("rawtypes")
    static final CacheSubscription[] EMPTY = new CacheSubscription[0];
    /**
     * A shared instance indicating the source has no more events and there
     * is no need to remember subscribers anymore.
     */
    @SuppressWarnings("rawtypes")
    static final CacheSubscription[] TERMINATED = new CacheSubscription[0];

    /**
     * Responsible for caching events from the source and multicasting them to each downstream.
     */
    final Multicaster<T> multicaster;

    /**
     * The first node in a singly linked list. Each node has the capacity to hold a specific number of events, and each
     * points exclusively to the <em>next</em> node (if present). When a new downstream arrives, the subscription is
     * initialized with a reference to the "head" node, and any events present in the linked list are replayed. As
     * events are replayed to the new downstream, its 'node' reference advances through the linked list, discarding each
     * node reference once all events in that node have been replayed. Consequently, once {@code this} instance goes out
     * of scope, the prefix of nodes up to the first node that is still being replayed becomes unreachable and eligible
     * for collection.
     */
    final Node<T> head;

    /**
     * Constructs an empty, non-connected cache.
     * @param source the source to subscribe to for the first incoming subscriber
     * @param capacityHint the number of items expected (reduce allocation frequency)
     */
    public FlowableCache(Flowable<T> source, int capacityHint) {
        super(source);
        this.once = new AtomicBoolean();
        Node<T> n = new Node<>(capacityHint);
        this.head = n;
        this.multicaster = new Multicaster<>(capacityHint, n);
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> t) {
        CacheSubscription<T> consumer = new CacheSubscription<>(t, multicaster, head);
        t.onSubscribe(consumer);
        multicaster.add(consumer);

        if (!once.get() && once.compareAndSet(false, true)) {
            source.subscribe(multicaster);
        } else {
            multicaster.replay(consumer);
        }
    }

    /**
     * Check if this cached observable is connected to its source.
     * @return true if already connected
     */
    /* public */boolean isConnected() {
        return once.get();
    }

    /**
     * Returns true if there are observers subscribed to this observable.
     * @return true if the cache has Subscribers
     */
    /* public */ boolean hasSubscribers() {
        return multicaster.get().length != 0;
    }

    /**
     * Returns the number of events currently cached.
     * @return the number of currently cached event count
     */
    /* public */ long cachedEventCount() {
        return multicaster.size;
    }

    static final class Multicaster<T> extends AtomicReference<CacheSubscription<T>[]>
    implements FlowableSubscriber<T> {

        /** */
        @Serial
        private static final long serialVersionUID = 8514643269016498691L;

        /**
         * The number of items per cached nodes.
         */
        final int capacityHint;

        /**
         * The total number of elements in the list available for reads.
         */
        volatile long size;

        /**
         * The current tail of the linked structure holding the items.
         */
        Node<T> tail;

        /**
         * How many items have been put into the tail node so far.
         */
        int tailOffset;

        /**
         * If the subscribers are {@link #TERMINATED}, this holds the terminal error if not null.
         */
        Throwable error;

        /**
         * True if the source has terminated.
         */
        volatile boolean done;

        @SuppressWarnings("unchecked")
        Multicaster(int capacityHint, Node<T> head) {
            super(EMPTY);
            this.tail = head;
            this.capacityHint = capacityHint;
        }

        /**
         * Atomically adds the consumer to the subscribers copy-on-write array
         * if the source has not yet terminated.
         * @param consumer the consumer to add
         */
        void add(CacheSubscription<T> consumer) {
            for (;;) {
                CacheSubscription<T>[] current = get();
                if (current == TERMINATED) {
                    return;
                }
                int n = current.length;

                @SuppressWarnings("unchecked")
                CacheSubscription<T>[] next = new CacheSubscription[n + 1];
                System.arraycopy(current, 0, next, 0, n);
                next[n] = consumer;

                if (compareAndSet(current, next)) {
                    return;
                }
            }
        }

        /**
         * Atomically removes the consumer from the subscribers copy-on-write array.
         * @param consumer the consumer to remove
         */
        @SuppressWarnings("unchecked")
        void remove(CacheSubscription<T> consumer) {
            for (;;) {
                CacheSubscription<T>[] current = get();
                int n = current.length;
                if (n == 0) {
                    return;
                }

                int j = -1;
                for (int i = 0; i < n; i++) {
                    if (current[i] == consumer) {
                        j = i;
                        break;
                    }
                }

                if (j < 0) {
                    return;
                }
                CacheSubscription<T>[] next;

                if (n == 1) {
                    next = EMPTY;
                } else {
                    next = new CacheSubscription[n - 1];
                    System.arraycopy(current, 0, next, 0, j);
                    System.arraycopy(current, j + 1, next, j, n - j - 1);
                }

                if (compareAndSet(current, next)) {
                    return;
                }
            }
        }

        /**
         * Replays the contents of this cache to the given consumer based on its
         * current state and number of items requested by it.
         * @param consumer the consumer to continue replaying items to
         */
        void replay(CacheSubscription<T> consumer) {
            // make sure there is only one replay going on at a time
            if (consumer.getAndIncrement() != 0) {
                return;
            }

            // see if there were more replay request in the meantime
            int missed = 1;
            // read out state into locals upfront to avoid being re-read due to volatile reads
            long index = consumer.index;
            int offset = consumer.offset;
            Node<T> node = consumer.node;
            AtomicLong requested = consumer.requested;
            Subscriber<? super T> downstream = consumer.downstream;
            int capacity = capacityHint;

            for (;;) {
                // see how many items the consumer has requested in total so far
                long consumerRequested = requested.get();
                // MIN_VALUE indicates a cancelled consumer, we stop replaying
                if (consumerRequested == Long.MIN_VALUE) {
                    // release the node object to avoid leaks through retained consumers
                    consumer.node = null;
                    return;
                }
                // first see if the source has terminated, read order matters!
                boolean sourceDone = done;
                // and if the number of items is the same as this consumer has received
                boolean empty = size == index;

                // if the source is done and we have all items so far, terminate the consumer
                if (sourceDone && empty) {
                    // release the node object to avoid leaks through retained consumers
                    consumer.node = null;
                    // if error is not null then the source failed
                    Throwable ex = error;
                    if (ex != null) {
                        downstream.onError(ex);
                    } else {
                        downstream.onComplete();
                    }
                    return;
                }

                // there are still items not sent to the consumer
                if (!empty && consumerRequested != index) {
                    // if the offset in the current node has reached the node capacity
                    if (offset == capacity) {
                        // switch to the subsequent node
                        node = node.next;
                        // reset the in-node offset
                        offset = 0;
                    }

                    // emit the cached item
                    downstream.onNext(node.values[offset]);

                    // move the node offset forward
                    offset++;
                    // move the total consumed item count forward
                    index++;

                    // retry for the next item/terminal event if any
                    continue;
                }
                // commit the changed references back
                consumer.index = index;
                consumer.offset = offset;
                consumer.node = node;
                // release the changes and see if there were more replay request in the meantime
                missed = consumer.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            int tailOffset = this.tailOffset;
            // if the current tail node is full, create a fresh node
            if (tailOffset == capacityHint) {
                Node<T> n = new Node<>(tailOffset);
                n.values[0] = t;
                this.tailOffset = 1;
                tail.next = n;
                tail = n;
            } else {
                tail.values[tailOffset] = t;
                this.tailOffset = tailOffset + 1;
            }
            size++;
            for (CacheSubscription<T> consumer : get()) {
                replay(consumer);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            error = t;
            done = true;
            // No additional events will arrive, so now we can clear the 'tail' reference
            tail = null;
            for (CacheSubscription<T> consumer : getAndSet(TERMINATED)) {
                replay(consumer);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            // No additional events will arrive, so now we can clear the 'tail' reference
            tail = null;
            for (CacheSubscription<T> consumer : getAndSet(TERMINATED)) {
                replay(consumer);
            }
        }
    }

    /**
     * Hosts the downstream consumer and its current requested and replay states.
     * {@code this} holds the work-in-progress counter for the serialized replay.
     * @param <T> the value type
     */
    static final class CacheSubscription<T> extends AtomicInteger
    implements Subscription {

        @Serial
        private static final long serialVersionUID = 6770240836423125754L;

        final Subscriber<? super T> downstream;

        final Multicaster<T> parent;

        final AtomicLong requested;

        Node<T> node;

        int offset;

        long index;

        /**
         * Constructs a new instance with the actual downstream consumer and
         * the parent multicaster.
         * @param downstream the actual consumer
         * @param parent the parent that holds onto the cached items
         * @param head the first node in the linked list
         */
        CacheSubscription(Subscriber<? super T> downstream, Multicaster<T> parent, Node<T> head) {
            this.downstream = downstream;
            this.parent = parent;
            this.node = head;
            this.requested = new AtomicLong();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.addCancel(requested, n);
                parent.replay(this);
            }
        }

        @Override
        public void cancel() {
            if (requested.getAndSet(Long.MIN_VALUE) != Long.MIN_VALUE) {
                parent.remove(this);
            }
        }
    }

    /**
     * Represents a segment of the cached item list as
     * part of a linked-node-list structure.
     * @param <T> the element type
     */
    static final class Node<T> {

        /**
         * The array of values held by this node.
         */
        final T[] values;

        /**
         * The next node if not null.
         */
        volatile Node<T> next;

        @SuppressWarnings("unchecked")
        Node(int capacityHint) {
            this.values = (T[])new Object[capacityHint];
        }
    }
}
