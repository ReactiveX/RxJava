/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.reactivestreams.*;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.*;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Timed;

public final class OperatorReplay<T> extends ConnectableObservable<T> {
    /** The source observable. */
    final Publisher<? extends T> source;
    /** Holds the current subscriber that is, will be or just was subscribed to the source observable. */
    final AtomicReference<ReplaySubscriber<T>> current;
    /** A factory that creates the appropriate buffer for the ReplaySubscriber. */
    final Supplier<? extends ReplayBuffer<T>> bufferFactory;

    @SuppressWarnings("rawtypes")
    static final Supplier DEFAULT_UNBOUNDED_FACTORY = () -> new UnboundedReplayBuffer<>(16);
    
    /**
     * Given a connectable observable factory, it multicasts over the generated
     * ConnectableObservable via a selector function.
     * @param connectableFactory
     * @param selector
     * @return
     */
    public static <T, U, R> Observable<R> multicastSelector(
            final Supplier<? extends ConnectableObservable<U>> connectableFactory,
            final Function<? super Observable<U>, ? extends Publisher<R>> selector) {
        return Observable.create(child -> {
            ConnectableObservable<U> co;
            Publisher<R> observable;
            try {
                co = connectableFactory.get();
                observable = selector.apply(co);
            } catch (Throwable e) {
                EmptySubscription.error(e, child);
                return;
            }
            
            SubscriberResourceWrapper<R, Disposable> srw = new SubscriberResourceWrapper<>(child, Disposable::dispose);
            
            observable.subscribe(srw);
            
            co.connect(srw::setResource);
        });
    }
    
    /**
     * Child Subscribers will observe the events of the ConnectableObservable on the
     * specified scheduler.
     * @param co
     * @param scheduler
     * @return
     */
    public static <T> ConnectableObservable<T> observeOn(final ConnectableObservable<T> co, final Scheduler scheduler) {
        final Observable<T> observable = co.observeOn(scheduler);
        return new ConnectableObservable<T>(observable) {
            @Override
            public void connect(Consumer<? super Disposable> connection) {
                co.connect(connection);
            }
        };
    }
    
    /**
     * Creates a replaying ConnectableObservable with an unbounded buffer.
     * @param source
     * @return
     */
    // TODO javac 1.8 fails with ambiguity error if this method is called create(), works properly
    @SuppressWarnings("unchecked")
    public static <T> ConnectableObservable<T> createFrom(Observable<? extends T> source) {
        return create(source, DEFAULT_UNBOUNDED_FACTORY);
    }
    
    /**
     * Creates a replaying ConnectableObservable with a size bound buffer.
     * @param source
     * @param bufferSize
     * @return
     */
    public static <T> ConnectableObservable<T> create(Observable<? extends T> source, 
            final int bufferSize) {
        if (bufferSize == Integer.MAX_VALUE) {
            return createFrom(source);
        }
        return create(source, () -> new SizeBoundReplayBuffer<>(bufferSize));
    }

    /**
     * Creates a replaying ConnectableObservable with a time bound buffer.
     * @param source
     * @param maxAge
     * @param unit
     * @param scheduler
     * @return
     */
    public static <T> ConnectableObservable<T> create(Observable<? extends T> source, 
            long maxAge, TimeUnit unit, Scheduler scheduler) {
        return create(source, maxAge, unit, scheduler, Integer.MAX_VALUE);
    }

    /**
     * Creates a replaying ConnectableObservable with a size and time bound buffer.
     * @param source
     * @param maxAge
     * @param unit
     * @param scheduler
     * @param bufferSize
     * @return
     */
    public static <T> ConnectableObservable<T> create(Observable<? extends T> source, 
            long maxAge, TimeUnit unit, final Scheduler scheduler, final int bufferSize) {
        return create(source, () -> new SizeAndTimeBoundReplayBuffer<>(bufferSize, maxAge, unit, scheduler));
    }

    /**
     * Creates a OperatorReplay instance to replay values of the given source observable.
     * @param source the source observable
     * @param bufferFactory the factory to instantiate the appropriate buffer when the observable becomes active
     * @return the connectable observable
     */
    static <T> ConnectableObservable<T> create(Observable<? extends T> source, 
            final Supplier<? extends ReplayBuffer<T>> bufferFactory) {
        // the current connection to source needs to be shared between the operator and its onSubscribe call
        final AtomicReference<ReplaySubscriber<T>> curr = new AtomicReference<>();
        Publisher<T> onSubscribe = new Publisher<T>() {
            @Override
            public void subscribe(Subscriber<? super T> child) {
                // concurrent connection/disconnection may change the state, 
                // we loop to be atomic while the child subscribes
                for (;;) {
                    // get the current subscriber-to-source
                    ReplaySubscriber<T> r = curr.get();
                    // if there isn't one
                    if (r == null) {
                        // create a new subscriber to source
                        ReplaySubscriber<T> u = new ReplaySubscriber<>(curr, bufferFactory.get());
                        // let's try setting it as the current subscriber-to-source
                        if (!curr.compareAndSet(r, u)) {
                            // didn't work, maybe someone else did it or the current subscriber 
                            // to source has just finished
                            continue;
                        }
                        // we won, let's use it going onwards
                        r = u;
                    }
                    
                    // create the backpressure-managing producer for this child
                    InnerSubscription<T> inner = new InnerSubscription<>(r, child);
                    // we try to add it to the array of producers
                    // if it fails, no worries because we will still have its buffer
                    // so it is going to replay it for us
                    r.add(inner);
                    // the producer has been registered with the current subscriber-to-source so 
                    // at least it will receive the next terminal event
                    // setting the producer will trigger the first request to be considered by 
                    // the subscriber-to-source.
                    child.onSubscribe(inner);
                    break;
                }
            }
        };
        return new OperatorReplay<>(onSubscribe, source, curr, bufferFactory);
    }
    private OperatorReplay(Publisher<T> onSubscribe, Observable<? extends T> source, 
            final AtomicReference<ReplaySubscriber<T>> current,
            final Supplier<? extends ReplayBuffer<T>> bufferFactory) {
        super(onSubscribe);
        this.source = source;
        this.current = current;
        this.bufferFactory = bufferFactory;
    }

    @Override
    public void connect(Consumer<? super Disposable> connection) {
        boolean doConnect = false;
        ReplaySubscriber<T> ps;
        // we loop because concurrent connect/disconnect and termination may change the state
        for (;;) {
            // retrieve the current subscriber-to-source instance
            ps = current.get();
            // if there is none yet or the current has unsubscribed
            if (ps == null || ps.isDisposed()) {
                // create a new subscriber-to-source
                ReplaySubscriber<T> u = new ReplaySubscriber<>(current, bufferFactory.get());
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
            break;
        }
        /* 
         * Notify the callback that we have a (new) connection which it can unsubscribe
         * but since ps is unique to a connection, multiple calls to connect() will return the
         * same Subscription and even if there was a connect-disconnect-connect pair, the older
         * references won't disconnect the newer connection.
         * Synchronous source consumers have the opportunity to disconnect via unsubscribe on the
         * Subscription as unsafeSubscribe may never return in its own.
         * 
         * Note however, that asynchronously disconnecting a running source might leave 
         * child-subscribers without any terminal event; ReplaySubject does not have this 
         * issue because the unsubscription was always triggered by the child-subscribers 
         * themselves.
         */
        connection.accept(ps);
        if (doConnect) {
            source.subscribe(ps);
        }
    }
    
    @SuppressWarnings("rawtypes")
    static final class ReplaySubscriber<T> implements Subscriber<T>, Disposable {
        /** Holds notifications from upstream. */
        final ReplayBuffer<T> buffer;
        /** The notification-lite factory. */
        /** Contains either an onCompleted or an onError token from upstream. */
        boolean done;
        
        /** Indicates an empty array of inner producers. */
        static final InnerSubscription[] EMPTY = new InnerSubscription[0];
        /** Indicates a terminated ReplaySubscriber. */
        static final InnerSubscription[] TERMINATED = new InnerSubscription[0];
        
        /** Tracks the subscribed producers. */
        final AtomicReference<InnerSubscription[]> producers;
        /** 
         * Atomically changed from false to true by connect to make sure the 
         * connection is only performed by one thread. 
         */
        final AtomicBoolean shouldConnect;
        
        /** Guarded by this. */
        boolean emitting;
        /** Guarded by this. */
        boolean missed;
        
        
        /** Contains the maximum element index the child Subscribers requested so far. Accessed while emitting is true. */
        long maxChildRequested;
        /** Counts the outstanding upstream requests until the producer arrives. */
        long maxUpstreamRequested;
        /** The upstream producer. */
        volatile Subscription subscription;
        
        volatile boolean cancelled;
        
        public ReplaySubscriber(AtomicReference<ReplaySubscriber<T>> current,
                ReplayBuffer<T> buffer) {
            this.buffer = buffer;
            
            this.producers = new AtomicReference<>(EMPTY);
            this.shouldConnect = new AtomicBoolean();
        }
        
        boolean isDisposed() {
            return cancelled;
        }
        
        @Override
        public void dispose() {
            producers.getAndSet(TERMINATED);
            // unlike OperatorPublish, we can't null out the terminated so
            // late subscribers can still get replay
            // current.compareAndSet(ReplaySubscriber.this, null);
            // we don't care if it fails because it means the current has 
            // been replaced in the meantime
            subscription.cancel();
        }

        /**
         * Atomically try adding a new InnerProducer to this Subscriber or return false if this
         * Subscriber was terminated.
         * @param producer the producer to add
         * @return true if succeeded, false otherwise
         */
        boolean add(InnerSubscription<T> producer) {
            if (producer == null) {
                throw new NullPointerException();
            }
            // the state can change so we do a CAS loop to achieve atomicity
            for (;;) {
                // get the current producer array
                InnerSubscription[] c = producers.get();
                // if this subscriber-to-source reached a terminal state by receiving 
                // an onError or onCompleted, just refuse to add the new producer
                if (c == TERMINATED) {
                    return false;
                }
                // we perform a copy-on-write logic
                int len = c.length;
                InnerSubscription[] u = new InnerSubscription[len + 1];
                System.arraycopy(c, 0, u, 0, len);
                u[len] = producer;
                // try setting the producers array
                if (producers.compareAndSet(c, u)) {
                    return true;
                }
                // if failed, some other operation succeded (another add, remove or termination)
                // so retry
            }
        }
        
        /**
         * Atomically removes the given producer from the producers array.
         * @param producer the producer to remove
         */
        void remove(InnerSubscription<T> producer) {
            // the state can change so we do a CAS loop to achieve atomicity
            for (;;) {
                // let's read the current producers array
                InnerSubscription[] c = producers.get();
                // if it is either empty or terminated, there is nothing to remove so we quit
                if (c == EMPTY || c == TERMINATED) {
                    return;
                }
                // let's find the supplied producer in the array
                // although this is O(n), we don't expect too many child subscribers in general
                int j = -1;
                int len = c.length;
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
                InnerSubscription[] u;
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
                if (producers.compareAndSet(c, u)) {
                    return;
                }
                // if we failed, it means something else happened
                // (a concurrent add/remove or termination), we need to retry
            }
        }
        
        @Override
        public void onSubscribe(Subscription p) {
            Subscription p0 = subscription;
            if (p0 != null) {
                RxJavaPlugins.onError(new IllegalStateException("Only a single producer can be set on a Subscriber."));
                return;
            }
            subscription = p;
            manageRequests();
            replay();
        }
        
        @Override
        public void onNext(T t) {
            if (!done) {
                buffer.next(t);
                replay();
            }
        }
        @Override
        public void onError(Throwable e) {
            // The observer front is accessed serially as required by spec so
            // no need to CAS in the terminal value
            if (!done) {
                done = true;
                try {
                    buffer.error(e);
                    replay();
                } finally {
                    dispose(); // expectation of testIssue2191
                }
            }
        }
        @Override
        public void onComplete() {
            // The observer front is accessed serially as required by spec so
            // no need to CAS in the terminal value
            if (!done) {
                done = true;
                try {
                    buffer.complete();
                    replay();
                } finally {
                    dispose();
                }
            }
        }
        
        /**
         * Coordinates the request amounts of various child Subscribers.
         */
        void manageRequests() {
            // if the upstream has completed, no more requesting is possible
            if (isDisposed()) {
                return;
            }
            synchronized (this) {
                if (emitting) {
                    missed = true;
                    return;
                }
                emitting = true;
            }
            for (;;) {
                // if the upstream has completed, no more requesting is possible
                if (isDisposed()) {
                    return;
                }
                
                @SuppressWarnings("unchecked")
                InnerSubscription<T>[] a = producers.get();
                
                long ri = maxChildRequested;
                long maxTotalRequests = ri;

                for (InnerSubscription<T> rp : a) {
                    maxTotalRequests = Math.max(maxTotalRequests, rp.totalRequested.get());
                }
                
                long ur = maxUpstreamRequested;
                Subscription p = subscription;

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
                
                synchronized (this) {
                    if (!missed) {
                        emitting = false;
                        return;
                    }
                    missed = false;
                }
            }
        }
        
        /**
         * Tries to replay the buffer contents to all known subscribers.
         */
        void replay() {
            @SuppressWarnings("unchecked")
            InnerSubscription<T>[] a = producers.get();
            for (InnerSubscription<T> rp : a) {
                buffer.replay(rp);
            }
        }
    }
    /**
     * A Producer and Subscription that manages the request and unsubscription state of a
     * child subscriber in thread-safe manner.
     * We use AtomicLong as a base class to save on extra allocation of an AtomicLong and also
     * save the overhead of the AtomicIntegerFieldUpdater.
     * @param <T> the value type
     */
    static final class InnerSubscription<T> extends AtomicLong implements Subscription, Disposable {
        /** */
        private static final long serialVersionUID = -4453897557930727610L;
        /** 
         * The parent subscriber-to-source used to allow removing the child in case of
         * child unsubscription.
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
         * Indicates this child has been unsubscribed: the state is swapped in atomically and
         * will prevent the dispatch() to emit (too many) values to a terminated child subscriber.
         */
        static final long UNSUBSCRIBED = Long.MIN_VALUE;
        
        public InnerSubscription(ReplaySubscriber<T> parent, Subscriber<? super T> child) {
            this.parent = parent;
            this.child = child;
            this.totalRequested = new AtomicLong();
        }
        
        @Override
        public void request(long n) {
            // ignore negative requests
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            // In general, RxJava doesn't prevent concurrent requests (with each other or with
            // an unsubscribe) so we need a CAS-loop, but we need to handle
            // request overflow and unsubscribed/not requested state as well.
            for (;;) {
                // get the current request amount
                long r = get();
                // if child called unsubscribe() do nothing
                if (r == UNSUBSCRIBED) {
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
                    // if successful, notify the parent dispacher this child can receive more
                    // elements
                    parent.manageRequests();
                    
                    parent.buffer.replay(this);
                    return;
                }
                // otherwise, someone else changed the state (perhaps a concurrent 
                // request or unsubscription so retry
            }
        }
        
        /**
         * Indicate that values have been emitted to this child subscriber by the dispatch() method.
         * @param n the number of items emitted
         * @return the updated request value (may indicate how much can be produced or a terminal state)
         */
        public long produced(long n) {
            // we don't allow producing zero or less: it would be a bug in the operator
            if (n <= 0) {
                throw new IllegalArgumentException("Cant produce zero or less");
            }
            for (;;) {
                // get the current request value
                long r = get();
                // if the child has unsubscribed, simply return and indicate this
                if (r == UNSUBSCRIBED) {
                    return UNSUBSCRIBED;
                }
                // reduce the requested amount
                long u = r - n;
                // if the new amount is less than zero, we have a bug in this operator
                if (u < 0) {
                    throw new IllegalStateException("More produced (" + n + ") than requested (" + r + ")");
                }
                // try updating the request value
                if (compareAndSet(r, u)) {
                    // and return the udpated value
                    return u;
                }
                // otherwise, some concurrent activity happened and we need to retry
            }
        }
        
        public boolean isDisposed() {
            return get() == UNSUBSCRIBED;
        }
        
        @Override
        public void cancel() {
            dispose();
        }
        
        @Override
        public void dispose() {
            long r = get();
            // let's see if we are unsubscribed
            if (r != UNSUBSCRIBED) {
                // if not, swap in the terminal state, this is idempotent
                // because other methods using CAS won't overwrite this value,
                // concurrent calls to unsubscribe will atomically swap in the same
                // terminal value
                r = getAndSet(UNSUBSCRIBED);
                // and only one of them will see a non-terminated value before the swap
                if (r != UNSUBSCRIBED) {
                    // remove this from the parent
                    parent.remove(this);
                    // After removal, we might have unblocked the other child subscribers:
                    // let's assume this child had 0 requested before the unsubscription while
                    // the others had non-zero. By removing this 'blocking' child, the others
                    // are now free to receive events
                    parent.manageRequests();
                }
            }
        }
        /**
         * Convenience method to auto-cast the index object.
         * @return
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
         * @param value
         */
        void next(T value);
        /**
         * Adds a terminal exception to the buffer
         * @param e
         */
        void error(Throwable e);
        /**
         * Adds a completion event to the buffer
         */
        void complete();
        /**
         * Tries to replay the buffered values to the
         * subscriber inside the output if there
         * is new value and requests available at the
         * same time.
         * @param output
         */
        void replay(InnerSubscription<T> output);
    }
    
    /**
     * Holds an unbounded list of events.
     *
     * @param <T> the value type
     */
    static final class UnboundedReplayBuffer<T> extends ArrayList<Object> implements ReplayBuffer<T> {
        /** */
        private static final long serialVersionUID = 7063189396499112664L;
        /** The total number of events in the buffer. */
        volatile int size;
        
        public UnboundedReplayBuffer(int capacityHint) {
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
                
                Integer destIndexObject = output.index();
                int destIndex = destIndexObject != null ? destIndexObject.intValue() : 0;
                
                long r = output.get();
                long r0 = r;
                long e = 0L;
                
                while (r != 0L && destIndex < sourceIndex) {
                    Object o = get(destIndex);
                    try {
                        if (NotificationLite.accept(o, child)) {
                            return;
                        }
                    } catch (Throwable err) {
                        output.dispose();
                        if (!NotificationLite.isError(o) && !NotificationLite.isComplete(o)) {
                            child.onError(err);
                        }
                        return;
                    }
                    if (output.isDisposed()) {
                        return;
                    }
                    destIndex++;
                    r--;
                    e++;
                }
                if (e != 0L) {
                    output.index = destIndex;
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
     *
     * @param <T> the contained value type
     */
    static final class Node extends AtomicReference<Node> {
        /** */
        private static final long serialVersionUID = 245354315435971818L;
        final Object value;
        public Node(Object value) {
            this.value = value;
        }
    }
    
    /**
     * Base class for bounded buffering with options to specify an
     * enter and leave transforms and custom truncation behavior.
     *
     * @param <T> the value type
     */
    static class BoundedReplayBuffer<T> extends AtomicReference<Node> implements ReplayBuffer<T> {
        /** */
        private static final long serialVersionUID = 2346567790059478686L;
        
        Node tail;
        int size;
        
        public BoundedReplayBuffer() {
            Node n = new Node(null);
            tail = n;
            set(n);
        }
        
        /**
         * Add a new node to the linked list.
         * @param n
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
         * @param n
         */
        final void setFirst(Node n) {
            set(n);
        }
        
        @Override
        public final void next(T value) {
            Object o = enterTransform(NotificationLite.next(value));
            Node n = new Node(o);
            addLast(n);
            truncate();
        }

        @Override
        public final void error(Throwable e) {
            Object o = enterTransform(NotificationLite.error(e));
            Node n = new Node(o);
            addLast(n);
            truncateFinal();
        }

        @Override
        public final void complete() {
            Object o = enterTransform(NotificationLite.complete());
            Node n = new Node(o);
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
                long r0 = r;
                long e = 0L;
                
                Node node = output.index();
                if (node == null) {
                    node = get();
                    output.index = node;
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
                            output.index = null;
                            output.dispose();
                            if (!NotificationLite.isError(o) && !NotificationLite.isComplete(o)) {
                                output.child.onError(err);
                            }
                            return;
                        }
                        e++;
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
        
        /**
         * Override this to wrap the NotificationLite object into a
         * container to be used later by truncate.
         * @param value
         * @return
         */
        Object enterTransform(Object value) {
            return value;
        }
        /**
         * Override this to unwrap the transformed value into a
         * NotificationLite object.
         * @param value
         * @return
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
            Node n = get();
            for (;;) {
                Node next = n.get();
                if (next != null) {
                    Object o = next.value;
                    Object v = leaveTransform(o);
                    if (NotificationLite.isComplete(v) || NotificationLite.isError(v)) {
                        break;
                    }
                    output.add(NotificationLite.getValue(v));
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
    }
    
    /**
     * A bounded replay buffer implementation with size limit only.
     *
     * @param <T> the value type
     */
    static final class SizeBoundReplayBuffer<T> extends BoundedReplayBuffer<T> {
        /** */
        private static final long serialVersionUID = -5898283885385201806L;
        
        final int limit;
        public SizeBoundReplayBuffer(int limit) {
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
        /** */
        private static final long serialVersionUID = 3457957419649567404L;
        final Scheduler scheduler;
        final long maxAge;
        final TimeUnit unit;
        final int limit;
        public SizeAndTimeBoundReplayBuffer(int limit, long maxAge, TimeUnit unit, Scheduler scheduler) {
            this.scheduler = scheduler;
            this.limit = limit;
            this.maxAge = maxAge;
            this.unit = unit;
        }
        
        @Override
        Object enterTransform(Object value) {
            return new Timed<>(value, scheduler.now(unit), unit);
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
    }
}