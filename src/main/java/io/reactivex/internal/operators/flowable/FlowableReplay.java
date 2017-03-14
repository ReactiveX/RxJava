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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.internal.subscribers.SubscriberResourceWrapper;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Timed;

public final class FlowableReplay<T> extends ConnectableFlowable<T> implements HasUpstreamPublisher<T>, Disposable {
    /** The source observable. */
    final Flowable<T> source;
    /** Holds the current subscriber that is, will be or just was subscribed to the source observable. */
    final AtomicReference<ReplaySubscriber<T>> current;
    /** A factory that creates the appropriate buffer for the ReplaySubscriber. */
    final Callable<? extends ReplayBuffer<T>> bufferFactory;

    final Publisher<T> onSubscribe;

    @SuppressWarnings("rawtypes")
    static final Callable DEFAULT_UNBOUNDED_FACTORY = new DefaultUnboundedFactory();

    /**
     * Given a connectable observable factory, it multicasts over the generated
     * ConnectableObservable via a selector function.
     * @param <U> the connectable observable type
     * @param <R> the result type
     * @param connectableFactory the factory that returns a ConnectableFlowable for each individual subscriber
     * @param selector the function that receives a Flowable and should return another Flowable that will be subscribed to
     * @return the new Observable instance
     */
    public static <U, R> Flowable<R> multicastSelector(
            final Callable<? extends ConnectableFlowable<U>> connectableFactory,
            final Function<? super Flowable<U>, ? extends Publisher<R>> selector) {
        return Flowable.unsafeCreate(new MultiCastPublisher<R, U>(connectableFactory, selector));
    }

    /**
     * Child Subscribers will observe the events of the ConnectableObservable on the
     * specified scheduler.
     * @param <T> the value type
     * @param co the ConnectableFlowable to wrap
     * @param scheduler the target scheduler
     * @return the new ConnectableObservable instance
     */
    public static <T> ConnectableFlowable<T> observeOn(final ConnectableFlowable<T> co, final Scheduler scheduler) {
        final Flowable<T> observable = co.observeOn(scheduler);
        return RxJavaPlugins.onAssembly(new ConnectableFlowableReplay<T>(co, observable));
    }

    /**
     * Creates a replaying ConnectableObservable with an unbounded buffer.
     * @param <T> the value type
     * @param source the source Publisher to use
     * @return the new ConnectableObservable instance
     */
    @SuppressWarnings("unchecked")
    public static <T> ConnectableFlowable<T> createFrom(Flowable<? extends T> source) {
        return create(source, DEFAULT_UNBOUNDED_FACTORY);
    }

    /**
     * Creates a replaying ConnectableObservable with a size bound buffer.
     * @param <T> the value type
     * @param source the source Flowable to use
     * @param bufferSize the maximum number of elements to hold
     * @return the new ConnectableObservable instance
     */
    public static <T> ConnectableFlowable<T> create(Flowable<T> source,
            final int bufferSize) {
        if (bufferSize == Integer.MAX_VALUE) {
            return createFrom(source);
        }
        return create(source, new ReplayBufferTask<T>(bufferSize));
    }

    /**
     * Creates a replaying ConnectableObservable with a time bound buffer.
     * @param <T> the value type
     * @param source the source Flowable to use
     * @param maxAge the maximum age of entries
     * @param unit the unit of measure of the age amount
     * @param scheduler the target scheduler providing the current time
     * @return the new ConnectableObservable instance
     */
    public static <T> ConnectableFlowable<T> create(Flowable<T> source,
            long maxAge, TimeUnit unit, Scheduler scheduler) {
        return create(source, maxAge, unit, scheduler, Integer.MAX_VALUE);
    }

    /**
     * Creates a replaying ConnectableObservable with a size and time bound buffer.
     * @param <T> the value type
     * @param source the source Flowable to use
     * @param maxAge the maximum age of entries
     * @param unit the unit of measure of the age amount
     * @param scheduler the target scheduler providing the current time
     * @param bufferSize the maximum number of elements to hold
     * @return the new ConnectableFlowable instance
     */
    public static <T> ConnectableFlowable<T> create(Flowable<T> source,
            final long maxAge, final TimeUnit unit, final Scheduler scheduler, final int bufferSize) {
        return create(source, new ScheduledReplayBufferTask<T>(bufferSize, maxAge, unit, scheduler));
    }

    /**
     * Creates a OperatorReplay instance to replay values of the given source observable.
     * @param source the source observable
     * @param bufferFactory the factory to instantiate the appropriate buffer when the observable becomes active
     * @return the connectable observable
     */
    static <T> ConnectableFlowable<T> create(Flowable<T> source,
            final Callable<? extends ReplayBuffer<T>> bufferFactory) {
        // the current connection to source needs to be shared between the operator and its onSubscribe call
        final AtomicReference<ReplaySubscriber<T>> curr = new AtomicReference<ReplaySubscriber<T>>();
        Publisher<T> onSubscribe = new ReplayPublisher<T>(curr, bufferFactory);
        return RxJavaPlugins.onAssembly(new FlowableReplay<T>(onSubscribe, source, curr, bufferFactory));
    }

    private FlowableReplay(Publisher<T> onSubscribe, Flowable<T> source,
            final AtomicReference<ReplaySubscriber<T>> current,
            final Callable<? extends ReplayBuffer<T>> bufferFactory) {
        this.onSubscribe = onSubscribe;
        this.source = source;
        this.current = current;
        this.bufferFactory = bufferFactory;
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
    public void dispose() {
        current.lazySet(null);
    }

    @Override
    public boolean isDisposed() {
        Disposable d = current.get();
        return d == null || d.isDisposed();
    }

    @Override
    public void connect(Consumer<? super Disposable> connection) {
        boolean doConnect;
        ReplaySubscriber<T> ps;
        // we loop because concurrent connect/disconnect and termination may change the state
        for (;;) {
            // retrieve the current subscriber-to-source instance
            ps = current.get();
            // if there is none yet or the current was disposed
            if (ps == null || ps.isDisposed()) {

                ReplayBuffer<T> buf;

                try {
                    buf = bufferFactory.call();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    throw ExceptionHelper.wrapOrThrow(ex);
                }

                // create a new subscriber-to-source
                ReplaySubscriber<T> u = new ReplaySubscriber<T>(buf);
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
         * Disposable as unsafeSubscribe may never return in its own.
         *
         * Note however, that asynchronously disconnecting a running source might leave
         * child-subscribers without any terminal event; ReplaySubject does not have this
         * issue because the cancellation was always triggered by the child-subscribers
         * themselves.
         */
        try {
            connection.accept(ps);
        } catch (Throwable ex) {
            if (doConnect) {
                ps.shouldConnect.compareAndSet(true, false);
            }
            Exceptions.throwIfFatal(ex);
            throw ExceptionHelper.wrapOrThrow(ex);
        }
        if (doConnect) {
            source.subscribe(ps);
        }
    }

    @SuppressWarnings("rawtypes")
    static final class ReplaySubscriber<T>
    extends AtomicReference<Subscription>
    implements FlowableSubscriber<T>, Disposable {
        private static final long serialVersionUID = 7224554242710036740L;
        /** Holds notifications from upstream. */
        final ReplayBuffer<T> buffer;
        /** Indicates this Subscriber received a terminal event. */
        boolean done;

        /** Indicates an empty array of inner subscriptions. */
        static final InnerSubscription[] EMPTY = new InnerSubscription[0];
        /** Indicates a terminated ReplaySubscriber. */
        static final InnerSubscription[] TERMINATED = new InnerSubscription[0];

        /** Tracks the subscribed InnerSubscriptions. */
        final AtomicReference<InnerSubscription<T>[]> subscribers;
        /**
         * Atomically changed from false to true by connect to make sure the
         * connection is only performed by one thread.
         */
        final AtomicBoolean shouldConnect;

        final AtomicInteger management;

        /** Contains the maximum element index the child Subscribers requested so far. Accessed while emitting is true. */
        long maxChildRequested;
        /** Counts the outstanding upstream requests until the producer arrives. */
        long maxUpstreamRequested;

        @SuppressWarnings("unchecked")
        ReplaySubscriber(ReplayBuffer<T> buffer) {
            this.buffer = buffer;
            this.management = new AtomicInteger();
            this.subscribers = new AtomicReference<InnerSubscription<T>[]>(EMPTY);
            this.shouldConnect = new AtomicBoolean();
        }

        @Override
        public boolean isDisposed() {
            return subscribers.get() == TERMINATED;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void dispose() {
            subscribers.set(TERMINATED);
            // unlike OperatorPublish, we can't null out the terminated so
            // late subscribers can still get replay
            // current.compareAndSet(ReplaySubscriber.this, null);
            // we don't care if it fails because it means the current has
            // been replaced in the meantime
            SubscriptionHelper.cancel(this);
        }

        /**
         * Atomically try adding a new InnerSubscription to this Subscriber or return false if this
         * Subscriber was terminated.
         * @param producer the producer to add
         * @return true if succeeded, false otherwise
         */
        @SuppressWarnings("unchecked")
        boolean add(InnerSubscription<T> producer) {
            if (producer == null) {
                throw new NullPointerException();
            }
            // the state can change so we do a CAS loop to achieve atomicity
            for (;;) {
                // get the current producer array
                InnerSubscription<T>[] c = subscribers.get();
                // if this subscriber-to-source reached a terminal state by receiving
                // an onError or onComplete, just refuse to add the new producer
                if (c == TERMINATED) {
                    return false;
                }
                // we perform a copy-on-write logic
                int len = c.length;
                InnerSubscription<T>[] u = new InnerSubscription[len + 1];
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
         * Atomically removes the given InnerSubscription from the subscribers array.
         * @param p the InnerSubscription to remove
         */
        @SuppressWarnings("unchecked")
        void remove(InnerSubscription<T> p) {
            // the state can change so we do a CAS loop to achieve atomicity
            for (;;) {
                // let's read the current subscribers array
                InnerSubscription<T>[] c = subscribers.get();
                int len = c.length;
                // if it is either empty or terminated, there is nothing to remove so we quit
                if (len == 0) {
                    return;
                }
                // let's find the supplied producer in the array
                // although this is O(n), we don't expect too many child subscribers in general
                int j = -1;
                for (int i = 0; i < len; i++) {
                    if (c[i].equals(p)) {
                        j = i;
                        break;
                    }
                }
                // we didn't find it so just quit
                if (j < 0) {
                    return;
                }
                // we do copy-on-write logic here
                InnerSubscription<T>[] u;
                // we don't create a new empty array if producer was the single inhabitant
                // but rather reuse an empty array
                if (len == 1) {
                    u = EMPTY;
                } else {
                    // otherwise, create a new array one less in size
                    u = new InnerSubscription[len - 1];
                    // copy elements being before the given producer
                    System.arraycopy(c, 0, u, 0, j);
                    // copy elements being after the given producer
                    System.arraycopy(c, j + 1, u, j, len - j - 1);
                }
                // try setting this new array as
                if (subscribers.compareAndSet(c, u)) {
                    return;
                }
                // if we failed, it means something else happened
                // (a concurrent add/remove or termination), we need to retry
            }
        }

        @Override
        public void onSubscribe(Subscription p) {
            if (SubscriptionHelper.setOnce(this, p)) {
                manageRequests();
                for (InnerSubscription<T> rp : subscribers.get()) {
                    buffer.replay(rp);
                }
            }
        }

        @Override
        public void onNext(T t) {
            if (!done) {
                buffer.next(t);
                for (InnerSubscription<T> rp : subscribers.get()) {
                    buffer.replay(rp);
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onError(Throwable e) {
            // The observer front is accessed serially as required by spec so
            // no need to CAS in the terminal value
            if (!done) {
                done = true;
                buffer.error(e);
                for (InnerSubscription<T> rp : subscribers.getAndSet(TERMINATED)) {
                    buffer.replay(rp);
                }
            } else {
                RxJavaPlugins.onError(e);
            }
        }
        @SuppressWarnings("unchecked")
        @Override
        public void onComplete() {
            // The observer front is accessed serially as required by spec so
            // no need to CAS in the terminal value
            if (!done) {
                done = true;
                buffer.complete();
                for (InnerSubscription<T> rp : subscribers.getAndSet(TERMINATED)) {
                    buffer.replay(rp);
                }
            }
        }

        /**
         * Coordinates the request amounts of various child Subscribers.
         */
        void manageRequests() {
            if (management.getAndIncrement() != 0) {
                return;
            }
            int missed = 1;
            for (;;) {
                // if the upstream has completed, no more requesting is possible
                if (isDisposed()) {
                    return;
                }

                InnerSubscription<T>[] a = subscribers.get();

                long ri = maxChildRequested;
                long maxTotalRequests = ri;

                for (InnerSubscription<T> rp : a) {
                    maxTotalRequests = Math.max(maxTotalRequests, rp.totalRequested.get());
                }

                long ur = maxUpstreamRequested;
                Subscription p = get();

                long diff = maxTotalRequests - ri;
                if (diff != 0L) {
                    maxChildRequested = maxTotalRequests;
                    if (p != null) {
                        if (ur != 0L) {
                            maxUpstreamRequested = 0L;
                            p.request(ur + diff);
                        } else {
                            p.request(diff);
                        }
                    } else {
                        // collect upstream request amounts until there is a producer for them
                        long u = ur + diff;
                        if (u < 0) {
                            u = Long.MAX_VALUE;
                        }
                        maxUpstreamRequested = u;
                    }
                } else
                // if there were outstanding upstream requests and we have a producer
                if (ur != 0L && p != null) {
                    maxUpstreamRequested = 0L;
                    // fire the accumulated requests
                    p.request(ur);
                }

                missed = management.addAndGet(-missed);
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
    static final class InnerSubscription<T> extends AtomicLong implements Subscription, Disposable {

        private static final long serialVersionUID = -4453897557930727610L;
        /**
         * The parent subscriber-to-source used to allow removing the child in case of
         * child cancellation.
         */
        final ReplaySubscriber<T> parent;
        /** The actual child subscriber. */
        final Subscriber<? super T> child;
        /**
         * Holds an object that represents the current location in the buffer.
         * Guarded by the emitter loop.
         */
        Object index;
        /**
         * Keeps the sum of all requested amounts.
         */
        final AtomicLong totalRequested;
        /** Indicates an emission state. Guarded by this. */
        boolean emitting;
        /** Indicates a missed update. Guarded by this. */
        boolean missed;
        /**
         * Indicates this child has been cancelled: the state is swapped in atomically and
         * will prevent the dispatch() to emit (too many) values to a terminated child subscriber.
         */
        static final long CANCELLED = Long.MIN_VALUE;

        InnerSubscription(ReplaySubscriber<T> parent, Subscriber<? super T> child) {
            this.parent = parent;
            this.child = child;
            this.totalRequested = new AtomicLong();
        }

        @Override
        public void request(long n) {
            // ignore negative requests
            if (SubscriptionHelper.validate(n)) {
                // In general, RxJava doesn't prevent concurrent requests (with each other or with
                // a cancel) so we need a CAS-loop, but we need to handle
                // request overflow and cancelled/not requested state as well.
                for (;;) {
                    // get the current request amount
                    long r = get();
                    // if child called cancel() do nothing
                    if (r == CANCELLED) {
                        return;
                    }
                    // ignore zero requests except any first that sets in zero
                    if (r >= 0L && n == 0) {
                        return;
                    }
                    // otherwise, increase the request count
                    long u = BackpressureHelper.addCap(r, n);

                    // try setting the new request value
                    if (compareAndSet(r, u)) {
                        // increment the total request counter
                        BackpressureHelper.add(totalRequested, n);
                        // if successful, notify the parent dispatcher this child can receive more
                        // elements
                        parent.manageRequests();

                        parent.buffer.replay(this);
                        return;
                    }
                    // otherwise, someone else changed the state (perhaps a concurrent
                    // request or cancellation) so retry
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
        public boolean isDisposed() {
            return get() == CANCELLED;
        }

        @Override
        public void cancel() {
            dispose();
        }

        @Override
        public void dispose() {
            if (getAndSet(CANCELLED) != CANCELLED) {
                // remove this from the parent
                parent.remove(this);
                // After removal, we might have unblocked the other child subscribers:
                // let's assume this child had 0 requested before the cancellation while
                // the others had non-zero. By removing this 'blocking' child, the others
                // are now free to receive events
                parent.manageRequests();
            }
        }
        /**
         * Convenience method to auto-cast the index object.
         * @return the current index object
         */
        @SuppressWarnings("unchecked")
        <U> U index() {
            return (U)index;
        }
    }
    /**
     * The interface for interacting with various buffering logic.
     *
     * @param <T> the value type
     */
    interface ReplayBuffer<T> {
        /**
         * Adds a regular value to the buffer.
         * @param value the next value to store
         */
        void next(T value);
        /**
         * Adds a terminal exception to the buffer.
         * @param e the Throwable instance
         */
        void error(Throwable e);
        /**
         * Adds a completion event to the buffer.
         */
        void complete();
        /**
         * Tries to replay the buffered values to the
         * subscriber inside the output if there
         * is new value and requests available at the
         * same time.
         * @param output the receiver of the events
         */
        void replay(InnerSubscription<T> output);
    }

    /**
     * Holds an unbounded list of events.
     *
     * @param <T> the value type
     */
    static final class UnboundedReplayBuffer<T> extends ArrayList<Object> implements ReplayBuffer<T> {

        private static final long serialVersionUID = 7063189396499112664L;
        /** The total number of events in the buffer. */
        volatile int size;

        UnboundedReplayBuffer(int capacityHint) {
            super(capacityHint);
        }
        @Override
        public void next(T value) {
            add(NotificationLite.next(value));
            size++;
        }

        @Override
        public void error(Throwable e) {
            add(NotificationLite.error(e));
            size++;
        }

        @Override
        public void complete() {
            add(NotificationLite.complete());
            size++;
        }

        @Override
        public void replay(InnerSubscription<T> output) {
            synchronized (output) {
                if (output.emitting) {
                    output.missed = true;
                    return;
                }
                output.emitting = true;
            }
            final Subscriber<? super T> child = output.child;

            for (;;) {
                if (output.isDisposed()) {
                    return;
                }
                int sourceIndex = size;

                Integer destinationIndexObject = output.index();
                int destinationIndex = destinationIndexObject != null ? destinationIndexObject : 0;

                long r = output.get();
                long r0 = r; // NOPMD
                long e = 0L;

                while (r != 0L && destinationIndex < sourceIndex) {
                    Object o = get(destinationIndex);
                    try {
                        if (NotificationLite.accept(o, child)) {
                            return;
                        }
                    } catch (Throwable err) {
                        Exceptions.throwIfFatal(err);
                        output.dispose();
                        if (!NotificationLite.isError(o) && !NotificationLite.isComplete(o)) {
                            child.onError(err);
                        }
                        return;
                    }
                    if (output.isDisposed()) {
                        return;
                    }
                    destinationIndex++;
                    r--;
                    e++;
                }
                if (e != 0L) {
                    output.index = destinationIndex;
                    if (r0 != Long.MAX_VALUE) {
                        output.produced(e);
                    }
                }

                synchronized (output) {
                    if (!output.missed) {
                        output.emitting = false;
                        return;
                    }
                    output.missed = false;
                }
            }
        }
    }

    /**
     * Represents a node in a bounded replay buffer's linked list.
     */
    static final class Node extends AtomicReference<Node> {

        private static final long serialVersionUID = 245354315435971818L;
        final Object value;
        final long index;

        Node(Object value, long index) {
            this.value = value;
            this.index = index;
        }
    }

    /**
     * Base class for bounded buffering with options to specify an
     * enter and leave transforms and custom truncation behavior.
     *
     * @param <T> the value type
     */
    static class BoundedReplayBuffer<T> extends AtomicReference<Node> implements ReplayBuffer<T> {

        private static final long serialVersionUID = 2346567790059478686L;

        Node tail;
        int size;

        long index;

        BoundedReplayBuffer() {
            Node n = new Node(null, 0);
            tail = n;
            set(n);
        }

        /**
         * Add a new node to the linked list.
         * @param n the Node instance to add
         */
        final void addLast(Node n) {
            tail.set(n);
            tail = n;
            size++;
        }
        /**
         * Remove the first node from the linked list.
         */
        final void removeFirst() {
            Node head = get();
            Node next = head.get();
            if (next == null) {
                throw new IllegalStateException("Empty list!");
            }
            size--;
            // can't just move the head because it would retain the very first value
            // can't null out the head's value because of late replayers would see null
            setFirst(next);
        }
        /* test */ final void removeSome(int n) {
            Node head = get();
            while (n > 0) {
                head = head.get();
                n--;
                size--;
            }

            setFirst(head);
        }
        /**
         * Arranges the given node is the new head from now on.
         * @param n the Node instance to set as first
         */
        final void setFirst(Node n) {
            set(n);
        }

        @Override
        public final void next(T value) {
            Object o = enterTransform(NotificationLite.next(value));
            Node n = new Node(o, ++index);
            addLast(n);
            truncate();
        }

        @Override
        public final void error(Throwable e) {
            Object o = enterTransform(NotificationLite.error(e));
            Node n = new Node(o, ++index);
            addLast(n);
            truncateFinal();
        }

        @Override
        public final void complete() {
            Object o = enterTransform(NotificationLite.complete());
            Node n = new Node(o, ++index);
            addLast(n);
            truncateFinal();
        }

        @Override
        public final void replay(InnerSubscription<T> output) {
            synchronized (output) {
                if (output.emitting) {
                    output.missed = true;
                    return;
                }
                output.emitting = true;
            }
            for (;;) {
                if (output.isDisposed()) {
                    return;
                }

                long r = output.get();
                boolean unbounded = r == Long.MAX_VALUE; // NOPMD
                long e = 0L;

                Node node = output.index();
                if (node == null) {
                    node = getHead();
                    output.index = node;

                    BackpressureHelper.add(output.totalRequested, node.index);
                }

                while (r != 0) {
                    Node v = node.get();
                    if (v != null) {
                        Object o = leaveTransform(v.value);
                        try {
                            if (NotificationLite.accept(o, output.child)) {
                                output.index = null;
                                return;
                            }
                        } catch (Throwable err) {
                            Exceptions.throwIfFatal(err);
                            output.index = null;
                            output.dispose();
                            if (!NotificationLite.isError(o) && !NotificationLite.isComplete(o)) {
                                output.child.onError(err);
                            }
                            return;
                        }
                        e++;
                        r--;
                        node = v;
                    } else {
                        break;
                    }
                    if (output.isDisposed()) {
                        return;
                    }
                }

                if (e != 0L) {
                    output.index = node;
                    if (!unbounded) {
                        output.produced(e);
                    }
                }

                synchronized (output) {
                    if (!output.missed) {
                        output.emitting = false;
                        return;
                    }
                    output.missed = false;
                }
            }

        }

        /**
         * Override this to wrap the NotificationLite object into a
         * container to be used later by truncate.
         * @param value the value to transform into the internal representation
         * @return the transformed value
         */
        Object enterTransform(Object value) {
            return value;
        }
        /**
         * Override this to unwrap the transformed value into a
         * NotificationLite object.
         * @param value the input value to transform to the external representation
         * @return the transformed value
         */
        Object leaveTransform(Object value) {
            return value;
        }
        /**
         * Override this method to truncate a non-terminated buffer
         * based on its current properties.
         */
        void truncate() {

        }
        /**
         * Override this method to truncate a terminated buffer
         * based on its properties (i.e., truncate but the very last node).
         */
        void truncateFinal() {

        }
        /* test */ final  void collect(Collection<? super T> output) {
            Node n = getHead();
            for (;;) {
                Node next = n.get();
                if (next != null) {
                    Object o = next.value;
                    Object v = leaveTransform(o);
                    if (NotificationLite.isComplete(v) || NotificationLite.isError(v)) {
                        break;
                    }
                    output.add(NotificationLite.<T>getValue(v));
                    n = next;
                } else {
                    break;
                }
            }
        }
        /* test */ boolean hasError() {
            return tail.value != null && NotificationLite.isError(leaveTransform(tail.value));
        }
        /* test */ boolean hasCompleted() {
            return tail.value != null && NotificationLite.isComplete(leaveTransform(tail.value));
        }

        Node getHead() {
            return get();
        }
    }

    /**
     * A bounded replay buffer implementation with size limit only.
     *
     * @param <T> the value type
     */
    static final class SizeBoundReplayBuffer<T> extends BoundedReplayBuffer<T> {

        private static final long serialVersionUID = -5898283885385201806L;

        final int limit;
        SizeBoundReplayBuffer(int limit) {
            this.limit = limit;
        }

        @Override
        void truncate() {
            // overflow can be at most one element
            if (size > limit) {
                removeFirst();
            }
        }

        // no need for final truncation because values are truncated one by one
    }

    /**
     * Size and time bound replay buffer.
     *
     * @param <T> the buffered value type
     */
    static final class SizeAndTimeBoundReplayBuffer<T> extends BoundedReplayBuffer<T> {

        private static final long serialVersionUID = 3457957419649567404L;
        final Scheduler scheduler;
        final long maxAge;
        final TimeUnit unit;
        final int limit;
        SizeAndTimeBoundReplayBuffer(int limit, long maxAge, TimeUnit unit, Scheduler scheduler) {
            this.scheduler = scheduler;
            this.limit = limit;
            this.maxAge = maxAge;
            this.unit = unit;
        }

        @Override
        Object enterTransform(Object value) {
            return new Timed<Object>(value, scheduler.now(unit), unit);
        }

        @Override
        Object leaveTransform(Object value) {
            return ((Timed<?>)value).value();
        }

        @Override
        void truncate() {
            long timeLimit = scheduler.now(unit) - maxAge;

            Node prev = get();
            Node next = prev.get();

            int e = 0;
            for (;;) {
                if (next != null) {
                    if (size > limit) {
                        e++;
                        size--;
                        prev = next;
                        next = next.get();
                    } else {
                        Timed<?> v = (Timed<?>)next.value;
                        if (v.time() <= timeLimit) {
                            e++;
                            size--;
                            prev = next;
                            next = next.get();
                        } else {
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
            if (e != 0) {
                setFirst(prev);
            }
        }
        @Override
        void truncateFinal() {
            long timeLimit = scheduler.now(unit) - maxAge;

            Node prev = get();
            Node next = prev.get();

            int e = 0;
            for (;;) {
                if (next != null && size > 1) {
                    Timed<?> v = (Timed<?>)next.value;
                    if (v.time() <= timeLimit) {
                        e++;
                        size--;
                        prev = next;
                        next = next.get();
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            if (e != 0) {
                setFirst(prev);
            }
        }

        @Override
        Node getHead() {
            long timeLimit = scheduler.now(unit) - maxAge;
            Node prev = get();
            Node next = prev.get();
            for (;;) {
                if (next == null) {
                    break;
                }
                Timed<?> v = (Timed<?>)next.value;
                if (NotificationLite.isComplete(v.value()) || NotificationLite.isError(v.value())) {
                    break;
                }
                if (v.time() <= timeLimit) {
                    prev = next;
                    next = next.get();
                } else {
                    break;
                }
            }
            return prev;
        }
    }

    static final class MultiCastPublisher<R, U> implements Publisher<R> {
        private final Callable<? extends ConnectableFlowable<U>> connectableFactory;
        private final Function<? super Flowable<U>, ? extends Publisher<R>> selector;

        MultiCastPublisher(Callable<? extends ConnectableFlowable<U>> connectableFactory, Function<? super Flowable<U>, ? extends Publisher<R>> selector) {
            this.connectableFactory = connectableFactory;
            this.selector = selector;
        }

        @Override
        public void subscribe(Subscriber<? super R> child) {
            ConnectableFlowable<U> co;
            try {
                co = ObjectHelper.requireNonNull(connectableFactory.call(), "The connectableFactory returned null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                EmptySubscription.error(e, child);
                return;
            }

            Publisher<R> observable;
            try {
                observable = ObjectHelper.requireNonNull(selector.apply(co), "The selector returned a null Publisher");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                EmptySubscription.error(e, child);
                return;
            }

            final SubscriberResourceWrapper<R> srw = new SubscriberResourceWrapper<R>(child);

            observable.subscribe(srw);

            co.connect(new DisposableConsumer(srw));
        }

        final class DisposableConsumer implements Consumer<Disposable> {
            private final SubscriberResourceWrapper<R> srw;

            DisposableConsumer(SubscriberResourceWrapper<R> srw) {
                this.srw = srw;
            }

            @Override
            public void accept(Disposable r) {
                srw.setResource(r);
            }
        }
    }

    static final class ConnectableFlowableReplay<T> extends ConnectableFlowable<T> {
        private final ConnectableFlowable<T> co;
        private final Flowable<T> observable;

        ConnectableFlowableReplay(ConnectableFlowable<T> co, Flowable<T> observable) {
            this.co = co;
            this.observable = observable;
        }

        @Override
        public void connect(Consumer<? super Disposable> connection) {
            co.connect(connection);
        }

        @Override
        protected void subscribeActual(Subscriber<? super T> s) {
            observable.subscribe(s);
        }
    }

    static final class ReplayBufferTask<T> implements Callable<ReplayBuffer<T>> {
        private final int bufferSize;

        ReplayBufferTask(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        @Override
        public ReplayBuffer<T> call() {
            return new SizeBoundReplayBuffer<T>(bufferSize);
        }
    }

    static final class ScheduledReplayBufferTask<T> implements Callable<ReplayBuffer<T>> {
        private final int bufferSize;
        private final long maxAge;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        ScheduledReplayBufferTask(int bufferSize, long maxAge, TimeUnit unit, Scheduler scheduler) {
            this.bufferSize = bufferSize;
            this.maxAge = maxAge;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public ReplayBuffer<T> call() {
            return new SizeAndTimeBoundReplayBuffer<T>(bufferSize, maxAge, unit, scheduler);
        }
    }

    static final class ReplayPublisher<T> implements Publisher<T> {
        private final AtomicReference<ReplaySubscriber<T>> curr;
        private final Callable<? extends ReplayBuffer<T>> bufferFactory;

        ReplayPublisher(AtomicReference<ReplaySubscriber<T>> curr, Callable<? extends ReplayBuffer<T>> bufferFactory) {
            this.curr = curr;
            this.bufferFactory = bufferFactory;
        }

        @Override
        public void subscribe(Subscriber<? super T> child) {
            // concurrent connection/disconnection may change the state,
            // we loop to be atomic while the child subscribes
            for (;;) {
                // get the current subscriber-to-source
                ReplaySubscriber<T> r = curr.get();
                // if there isn't one
                if (r == null) {
                    ReplayBuffer<T> buf;

                    try {
                        buf = bufferFactory.call();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        throw ExceptionHelper.wrapOrThrow(ex);
                    }
                    // create a new subscriber to source
                    ReplaySubscriber<T> u = new ReplaySubscriber<T>(buf);
                    // let's try setting it as the current subscriber-to-source
                    if (!curr.compareAndSet(null, u)) {
                        // didn't work, maybe someone else did it or the current subscriber
                        // to source has just finished
                        continue;
                    }
                    // we won, let's use it going onwards
                    r = u;
                }

                // create the backpressure-managing producer for this child
                InnerSubscription<T> inner = new InnerSubscription<T>(r, child);
                // the producer has been registered with the current subscriber-to-source so
                // at least it will receive the next terminal event
                // setting the producer will trigger the first request to be considered by
                // the subscriber-to-source.
                child.onSubscribe(inner);
                // we try to add it to the array of subscribers
                // if it fails, no worries because we will still have its buffer
                // so it is going to replay it for us
                r.add(inner);

                if (inner.isDisposed()) {
                    r.remove(inner);
                    return;
                }

                r.manageRequests();

                // trigger the capturing of the current node and total requested
                r.buffer.replay(inner);

                break; // NOPMD
            }
        }
    }

    static final class DefaultUnboundedFactory implements Callable<Object> {
        @Override
        public Object call() {
            return new UnboundedReplayBuffer<Object>(16);
        }
    }
}
