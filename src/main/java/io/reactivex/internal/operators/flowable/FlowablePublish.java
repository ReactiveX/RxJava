/**
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

package io.reactivex.internal.operators.flowable;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * A connectable observable which shares an underlying source and dispatches source values to subscribers in a backpressure-aware
 * manner.
 * @param <T> the value type
 */
public final class FlowablePublish<T> extends ConnectableFlowable<T> implements HasUpstreamPublisher<T> {
    /**
     * Indicates this child has been cancelled: the state is swapped in atomically and
     * will prevent the dispatch() to emit (too many) values to a terminated child subscriber.
     */
    static final long CANCELLED = Long.MIN_VALUE;

    /** The source observable. */
    final Flowable<T> source;
    /** Holds the current subscriber that is, will be or just was subscribed to the source observable. */
    final AtomicReference<PublishSubscriber<T>> current;

    /** The size of the prefetch buffer. */
    final int bufferSize;

    final Publisher<T> onSubscribe;

    /**
     * Creates a OperatorPublish instance to publish values of the given source observable.
     * @param <T> the source value type
     * @param source the source observable
     * @param bufferSize the size of the prefetch buffer
     * @return the connectable observable
     */
    public static <T> ConnectableFlowable<T> create(Flowable<T> source, final int bufferSize) {
        // the current connection to source needs to be shared between the operator and its onSubscribe call
        final AtomicReference<PublishSubscriber<T>> curr = new AtomicReference<PublishSubscriber<T>>();
        Publisher<T> onSubscribe = new FlowablePublisher<T>(curr, bufferSize);
        return RxJavaPlugins.onAssembly(new FlowablePublish<T>(onSubscribe, source, curr, bufferSize));
    }

    private FlowablePublish(Publisher<T> onSubscribe, Flowable<T> source,
            final AtomicReference<PublishSubscriber<T>> current, int bufferSize) {
        this.onSubscribe = onSubscribe;
        this.source = source;
        this.current = current;
        this.bufferSize = bufferSize;
    }

    @Override
    public Publisher<T> source() {
        return source;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        onSubscribe.subscribe(s);
    }

    @Override
    public void connect(Consumer<? super Disposable> connection) {
        boolean doConnect;
        PublishSubscriber<T> ps;
        // we loop because concurrent connect/disconnect and termination may change the state
        for (;;) {
            // retrieve the current subscriber-to-source instance
            ps = current.get();
            // if there is none yet or the current has been disposed
            if (ps == null || ps.isDisposed()) {
                // create a new subscriber-to-source
                PublishSubscriber<T> u = new PublishSubscriber<T>(current, bufferSize);
                // try setting it as the current subscriber-to-source
                if (!current.compareAndSet(ps, u)) {
                    // did not work, perhaps a new subscriber arrived
                    // and created a new subscriber-to-source as well, retry
                    continue;
                }
                ps = u;
            }
            // if connect() was called concurrently, only one of them should actually
            // connect to the source
            doConnect = !ps.shouldConnect.get() && ps.shouldConnect.compareAndSet(false, true);
            break; // NOPMD
        }
        /*
         * Notify the callback that we have a (new) connection which it can dispose
         * but since ps is unique to a connection, multiple calls to connect() will return the
         * same Subscription and even if there was a connect-disconnect-connect pair, the older
         * references won't disconnect the newer connection.
         * Synchronous source consumers have the opportunity to disconnect via dispose on the
         * Disposable as subscribe() may never return on its own.
         *
         * Note however, that asynchronously disconnecting a running source might leave
         * child subscribers without any terminal event; PublishProcessor does not have this
         * issue because the cancellation was always triggered by the child subscribers
         * themselves.
         */
        try {
            connection.accept(ps);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            throw ExceptionHelper.wrapOrThrow(ex);
        }
        if (doConnect) {
            source.subscribe(ps);
        }
    }

    @SuppressWarnings("rawtypes")
    static final class PublishSubscriber<T>
    extends AtomicInteger
    implements FlowableSubscriber<T>, Disposable {
        private static final long serialVersionUID = -202316842419149694L;

        /** Indicates an empty array of inner subscribers. */
        static final InnerSubscriber[] EMPTY = new InnerSubscriber[0];
        /** Indicates a terminated PublishSubscriber. */
        static final InnerSubscriber[] TERMINATED = new InnerSubscriber[0];

        /** Holds onto the current connected PublishSubscriber. */
        final AtomicReference<PublishSubscriber<T>> current;
        /** The prefetch buffer size. */
        final int bufferSize;

        /** Tracks the subscribed InnerSubscribers. */
        final AtomicReference<InnerSubscriber[]> subscribers;
        /**
         * Atomically changed from false to true by connect to make sure the
         * connection is only performed by one thread.
         */
        final AtomicBoolean shouldConnect;

        final AtomicReference<Subscription> s = new AtomicReference<Subscription>();

        /** Contains either an onComplete or an onError token from upstream. */
        volatile Object terminalEvent;

        int sourceMode;

        /** Holds notifications from upstream. */
        volatile SimpleQueue<T> queue;

        PublishSubscriber(AtomicReference<PublishSubscriber<T>> current, int bufferSize) {
            this.subscribers = new AtomicReference<InnerSubscriber[]>(EMPTY);
            this.current = current;
            this.shouldConnect = new AtomicBoolean();
            this.bufferSize = bufferSize;
        }

        @Override
        public void dispose() {
            if (subscribers.get() != TERMINATED) {
                InnerSubscriber[] ps = subscribers.getAndSet(TERMINATED);
                if (ps != TERMINATED) {
                    current.compareAndSet(PublishSubscriber.this, null);
                    SubscriptionHelper.cancel(s);
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return subscribers.get() == TERMINATED;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this.s, s)) {
                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<T> qs = (QueueSubscription<T>) s;

                    int m = qs.requestFusion(QueueSubscription.ANY);
                    if (m == QueueSubscription.SYNC) {
                        sourceMode = m;
                        queue = qs;
                        terminalEvent = NotificationLite.complete();
                        dispatch();
                        return;
                    }
                    if (m == QueueSubscription.ASYNC) {
                        sourceMode = m;
                        queue = qs;
                        s.request(bufferSize);
                        return;
                    }
                }

                queue = new SpscArrayQueue<T>(bufferSize);

                s.request(bufferSize);
            }
        }

        @Override
        public void onNext(T t) {
            // we expect upstream to honor backpressure requests
            if (sourceMode == QueueSubscription.NONE && !queue.offer(t)) {
                onError(new MissingBackpressureException("Prefetch queue is full?!"));
                return;
            }
            // since many things can happen concurrently, we have a common dispatch
            // loop to act on the current state serially
            dispatch();
        }
        @Override
        public void onError(Throwable e) {
            // The observer front is accessed serially as required by spec so
            // no need to CAS in the terminal value
            if (terminalEvent == null) {
                terminalEvent = NotificationLite.error(e);
                // since many things can happen concurrently, we have a common dispatch
                // loop to act on the current state serially
                dispatch();
            } else {
                RxJavaPlugins.onError(e);
            }
        }
        @Override
        public void onComplete() {
            // The observer front is accessed serially as required by spec so
            // no need to CAS in the terminal value
            if (terminalEvent == null) {
                terminalEvent = NotificationLite.complete();
                // since many things can happen concurrently, we have a common dispatch loop
                // to act on the current state serially
                dispatch();
            }
        }

        /**
         * Atomically try adding a new InnerSubscriber to this Subscriber or return false if this
         * Subscriber was terminated.
         * @param producer the producer to add
         * @return true if succeeded, false otherwise
         */
        boolean add(InnerSubscriber<T> producer) {
            // the state can change so we do a CAS loop to achieve atomicity
            for (;;) {
                // get the current producer array
                InnerSubscriber[] c = subscribers.get();
                // if this subscriber-to-source reached a terminal state by receiving
                // an onError or onComplete, just refuse to add the new producer
                if (c == TERMINATED) {
                    return false;
                }
                // we perform a copy-on-write logic
                int len = c.length;
                InnerSubscriber[] u = new InnerSubscriber[len + 1];
                System.arraycopy(c, 0, u, 0, len);
                u[len] = producer;
                // try setting the subscribers array
                if (subscribers.compareAndSet(c, u)) {
                    return true;
                }
                // if failed, some other operation succeeded (another add, remove or termination)
                // so retry
            }
        }

        /**
         * Atomically removes the given InnerSubscriber from the subscribers array.
         * @param producer the producer to remove
         */
        void remove(InnerSubscriber<T> producer) {
            // the state can change so we do a CAS loop to achieve atomicity
            for (;;) {
                // let's read the current subscribers array
                InnerSubscriber[] c = subscribers.get();
                int len = c.length;
                // if it is either empty or terminated, there is nothing to remove so we quit
                if (len == 0) {
                    break;
                }
                // let's find the supplied producer in the array
                // although this is O(n), we don't expect too many child subscribers in general
                int j = -1;
                for (int i = 0; i < len; i++) {
                    if (c[i].equals(producer)) {
                        j = i;
                        break;
                    }
                }
                // we didn't find it so just quit
                if (j < 0) {
                    return;
                }
                // we do copy-on-write logic here
                InnerSubscriber[] u;
                // we don't create a new empty array if producer was the single inhabitant
                // but rather reuse an empty array
                if (len == 1) {
                    u = EMPTY;
                } else {
                    // otherwise, create a new array one less in size
                    u = new InnerSubscriber[len - 1];
                    // copy elements being before the given producer
                    System.arraycopy(c, 0, u, 0, j);
                    // copy elements being after the given producer
                    System.arraycopy(c, j + 1, u, j, len - j - 1);
                }
                // try setting this new array as
                if (subscribers.compareAndSet(c, u)) {
                    break;
                }
                // if we failed, it means something else happened
                // (a concurrent add/remove or termination), we need to retry
            }
        }

        /**
         * Perform termination actions in case the source has terminated in some way and
         * the queue has also become empty.
         * @param term the terminal event (a NotificationLite.error or completed)
         * @param empty set to true if the queue is empty
         * @return true if there is indeed a terminal condition
         */
        boolean checkTerminated(Object term, boolean empty) {
            // first of all, check if there is actually a terminal event
            if (term != null) {
                // is it a completion event (impl. note, this is much cheaper than checking for isError)
                if (NotificationLite.isComplete(term)) {
                    // but we also need to have an empty queue
                    if (empty) {
                        // this will prevent OnSubscribe spinning on a terminated but
                        // not yet cancelled PublishSubscriber
                        current.compareAndSet(this, null);
                        /*
                         * This will swap in a terminated array so add() in OnSubscribe will reject
                         * child subscribers to associate themselves with a terminated and thus
                         * never again emitting chain.
                         *
                         * Since we atomically change the contents of 'subscribers' only one
                         * operation wins at a time. If an add() wins before this getAndSet,
                         * its value will be part of the returned array by getAndSet and thus
                         * will receive the terminal notification. Otherwise, if getAndSet wins,
                         * add() will refuse to add the child producer and will trigger the
                         * creation of subscriber-to-source.
                         */
                        for (InnerSubscriber<?> ip : subscribers.getAndSet(TERMINATED)) {
                            ip.child.onComplete();
                        }
                        // indicate we reached the terminal state
                        return true;
                    }
                } else {
                    Throwable t = NotificationLite.getError(term);
                    // this will prevent OnSubscribe spinning on a terminated
                    // but not yet cancelled PublishSubscriber
                    current.compareAndSet(this, null);
                    // this will swap in a terminated array so add() in OnSubscribe will reject
                    // child subscribers to associate themselves with a terminated and thus
                    // never again emitting chain
                    InnerSubscriber[] a = subscribers.getAndSet(TERMINATED);
                    if (a.length != 0) {
                        for (InnerSubscriber<?> ip : a) {
                            ip.child.onError(t);
                        }
                    } else {
                        RxJavaPlugins.onError(t);
                    }
                    // indicate we reached the terminal state
                    return true;
                }
            }
            // there is still work to be done
            return false;
        }

        /**
         * The common serialization point of events arriving from upstream and child subscribers
         * requesting more.
         */
        void dispatch() {
            // standard construct of queue-drain
            // if there is an emission going on, indicate that more work needs to be done
            // the exact nature of this work needs to be determined from other data structures
            if (getAndIncrement() != 0) {
                return;
            }
            int missed = 1;
            for (;;) {
                /*
                 * We need to read terminalEvent before checking the queue for emptiness because
                 * all enqueue happens before setting the terminal event.
                 * If it were the other way around, when the emission is paused between
                 * checking isEmpty and checking terminalEvent, some other thread might
                 * have produced elements and set the terminalEvent and we'd quit emitting
                 * prematurely.
                 */
                Object term = terminalEvent;
                /*
                 * See if the queue is empty; since we need this information multiple
                 * times later on, we read it one.
                 * Although the queue can become non-empty in the mean time, we will
                 * detect it through the missing flag and will do another iteration.
                 */
                SimpleQueue<T> q = queue;

                boolean empty = q == null || q.isEmpty();
                // if the queue is empty and the terminal event was received, quit
                // and don't bother restoring emitting to false: no further activity is
                // possible at this point
                if (checkTerminated(term, empty)) {
                    return;
                }

                // We have elements queued. Note that due to the serialization nature of dispatch()
                // this loop is the only one which can turn a non-empty queue into an empty one
                // and as such, no need to ask the queue itself again for that.
                if (!empty) {
                    // We take a snapshot of the current child subscribers.
                    // Concurrent subscribers may miss this iteration, but it is to be expected
                    @SuppressWarnings("unchecked")
                    InnerSubscriber<T>[] ps = subscribers.get();

                    int len = ps.length;
                    // Let's assume everyone requested the maximum value.
                    long maxRequested = Long.MAX_VALUE;
                    // count how many have triggered cancellation
                    int cancelled = 0;

                    // Now find the minimum amount each child-subscriber requested
                    // since we can only emit that much to all of them without violating
                    // backpressure constraints
                    for (InnerSubscriber<T> ip : ps) {
                        long r = ip.get();
                        // if there is one child subscriber that hasn't requested yet
                        // we can't emit anything to anyone
                        if (r >= 0L) {
                            maxRequested = Math.min(maxRequested, r);
                        } else
                        // cancellation is indicated by a special value
                        if (r == CANCELLED) {
                            cancelled++;
                        }
                        // we ignore those with NOT_REQUESTED as if they aren't even there
                    }

                    // it may happen everyone has cancelled between here and subscribers.get()
                    // or we have no subscribers at all to begin with
                    if (len == cancelled) {
                        term = terminalEvent;
                        // so let's consume a value from the queue
                        T v;

                        try {
                            v = q.poll();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            s.get().cancel();
                            term = NotificationLite.error(ex);
                            terminalEvent = term;
                            v = null;
                        }
                        // or terminate if there was a terminal event and the queue is empty
                        if (checkTerminated(term, v == null)) {
                            return;
                        }
                        // otherwise, just ask for a new value
                        if (sourceMode != QueueSubscription.SYNC) {
                            s.get().request(1);
                        }
                        // and retry emitting to potential new child subscribers
                        continue;
                    }
                    // if we get here, it means there are non-cancelled child subscribers
                    // and we count the number of emitted values because the queue
                    // may contain less than requested
                    int d = 0;
                    while (d < maxRequested) {
                        term = terminalEvent;
                        T v;

                        try {
                            v = q.poll();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            s.get().cancel();
                            term = NotificationLite.error(ex);
                            terminalEvent = term;
                            v = null;
                        }

                        empty = v == null;
                        // let's check if there is a terminal event and the queue became empty just now
                        if (checkTerminated(term, empty)) {
                            return;
                        }
                        // the queue is empty but we aren't terminated yet, finish this emission loop
                        if (empty) {
                            break;
                        }
                        // we need to unwrap potential nulls
                        T value = NotificationLite.getValue(v);
                        // let's emit this value to all child subscribers
                        for (InnerSubscriber<T> ip : ps) {
                            // if ip.get() is negative, the child has either cancelled in the
                            // meantime or hasn't requested anything yet
                            // this eager behavior will skip cancelled children in case
                            // multiple values are available in the queue
                            if (ip.get() > 0L) {
                                ip.child.onNext(value);
                                // indicate this child has received 1 element
                                ip.produced(1);
                            }
                        }
                        // indicate we emitted one element
                        d++;
                    }

                    // if we did emit at least one element, request more to replenish the queue
                    if (d > 0) {
                        if (sourceMode != QueueSubscription.SYNC) {
                            s.get().request(d);
                        }
                    }
                    // if we have requests but not an empty queue after emission
                    // let's try again to see if more requests/child subscribers are
                    // ready to receive more
                    if (maxRequested != 0L && !empty) {
                        continue;
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }
    /**
     * A Subscription that manages the request and cancellation state of a
     * child subscriber in thread-safe manner.
     * @param <T> the value type
     */
    static final class InnerSubscriber<T> extends AtomicLong implements Subscription {

        private static final long serialVersionUID = -4453897557930727610L;
        /** The actual child subscriber. */
        final Subscriber<? super T> child;
        /**
         * The parent subscriber-to-source used to allow removing the child in case of
         * child cancellation.
         */
        volatile PublishSubscriber<T> parent;

        InnerSubscriber(Subscriber<? super T> child) {
            this.child = child;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.addCancel(this, n);
                PublishSubscriber<T> p = parent;
                if (p != null) {
                    p.dispatch();
                }
            }
        }

        /**
         * Indicate that values have been emitted to this child subscriber by the dispatch() method.
         * @param n the number of items emitted
         * @return the updated request value (may indicate how much can be produced or a terminal state)
         */
        public long produced(long n) {
            return BackpressureHelper.producedCancel(this, n);
        }

        @Override
        public void cancel() {
            long r = get();
            // let's see if we are cancelled
            if (r != CANCELLED) {
                // if not, swap in the terminal state, this is idempotent
                // because other methods using CAS won't overwrite this value,
                // concurrent calls to cancel will atomically swap in the same
                // terminal value
                r = getAndSet(CANCELLED);
                // and only one of them will see a non-terminated value before the swap
                if (r != CANCELLED) {
                    PublishSubscriber<T> p = parent;
                    if (p != null) {
                        // remove this from the parent
                        p.remove(this);
                        // After removal, we might have unblocked the other child subscribers:
                        // let's assume this child had 0 requested before the cancellation while
                        // the others had non-zero. By removing this 'blocking' child, the others
                        // are now free to receive events
                        p.dispatch();
                    }
                }
            }
        }
    }

    static final class FlowablePublisher<T> implements Publisher<T> {
        private final AtomicReference<PublishSubscriber<T>> curr;
        private final int bufferSize;

        FlowablePublisher(AtomicReference<PublishSubscriber<T>> curr, int bufferSize) {
            this.curr = curr;
            this.bufferSize = bufferSize;
        }

        @Override
        public void subscribe(Subscriber<? super T> child) {
            // create the backpressure-managing producer for this child
            InnerSubscriber<T> inner = new InnerSubscriber<T>(child);
            child.onSubscribe(inner);
            // concurrent connection/disconnection may change the state,
            // we loop to be atomic while the child subscribes
            for (;;) {
                // get the current subscriber-to-source
                PublishSubscriber<T> r = curr.get();
                // if there isn't one or it is cancelled/disposed
                if (r == null || r.isDisposed()) {
                    // create a new subscriber to source
                    PublishSubscriber<T> u = new PublishSubscriber<T>(curr, bufferSize);
                    // let's try setting it as the current subscriber-to-source
                    if (!curr.compareAndSet(r, u)) {
                        // didn't work, maybe someone else did it or the current subscriber
                        // to source has just finished
                        continue;
                    }
                    // we won, let's use it going onwards
                    r = u;
                }

                /*
                 * Try adding it to the current subscriber-to-source, add is atomic in respect
                 * to other adds and the termination of the subscriber-to-source.
                 */
                if (r.add(inner)) {
                    if (inner.get() == CANCELLED) {
                        r.remove(inner);
                    } else {
                        inner.parent = r;
                    }
                    r.dispatch();
                    break; // NOPMD
                }
                /*
                 * The current PublishSubscriber has been terminated, try with a newer one.
                 */
                /*
                 * Note: although technically correct, concurrent disconnects can cause
                 * unexpected behavior such as child subscribers never receiving anything
                 * (unless connected again). An alternative approach, similar to
                 * PublishProcessor would be to immediately terminate such child
                 * subscribers as well:
                 *
                 * Object term = r.terminalEvent;
                 * if (r.nl.isCompleted(term)) {
                 *     child.onComplete();
                 * } else {
                 *     child.onError(r.nl.getError(term));
                 * }
                 * return;
                 *
                 * The original concurrent behavior was non-deterministic in this regard as well.
                 * Allowing this behavior, however, may introduce another unexpected behavior:
                 * after disconnecting a previous connection, one might not be able to prepare
                 * a new connection right after a previous termination by subscribing new child
                 * subscribers asynchronously before a connect call.
                 */
            }
        }
    }
}
