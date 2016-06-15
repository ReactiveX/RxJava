/**
 * Copyright 2014 Netflix, Inc.
 * 
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
 */
package rx.internal.operators;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable;
import rx.exceptions.*;
import rx.functions.*;
import rx.internal.util.OpenHashSet;
import rx.observables.ConnectableObservable;
import rx.schedulers.Timestamped;
import rx.subscriptions.Subscriptions;

public final class OperatorReplay<T> extends ConnectableObservable<T> {
    /** The source observable. */
    final Observable<? extends T> source;
    /** Holds the current subscriber that is, will be or just was subscribed to the source observable. */
    final AtomicReference<ReplaySubscriber<T>> current;
    /** A factory that creates the appropriate buffer for the ReplaySubscriber. */
    final Func0<? extends ReplayBuffer<T>> bufferFactory;

    @SuppressWarnings("rawtypes")
    static final Func0 DEFAULT_UNBOUNDED_FACTORY = new Func0() {
        @Override
        public Object call() {
            return new UnboundedReplayBuffer<Object>(16);
        }
    };
    
    /**
     * Given a connectable observable factory, it multicasts over the generated
     * ConnectableObservable via a selector function.
     * @param <T> the upstream's value type
     * @param <U> the intermediate value type of the ConnectableObservable
     * @param <R> the final value type provided by the selector function
     * @param connectableFactory the factory that returns a ConnectableObservable instance
     * @param selector the function applied on the ConnectableObservable and returns the Observable
     * the downstream will subscribe to.
     * @return the Observable multicasting over a transformation of a ConnectableObserable
     */
    public static <T, U, R> Observable<R> multicastSelector(
            final Func0<? extends ConnectableObservable<U>> connectableFactory,
            final Func1<? super Observable<U>, ? extends Observable<R>> selector) {
        return Observable.create(new OnSubscribe<R>() {
            @Override
            public void call(final Subscriber<? super R> child) {
                ConnectableObservable<U> co;
                Observable<R> observable;
                try {
                    co = connectableFactory.call();
                    observable = selector.call(co);
                } catch (Throwable e) {
                    Exceptions.throwOrReport(e, child);
                    return;
                }
                
                observable.subscribe(child);
                
                co.connect(new Action1<Subscription>() {
                    @Override
                    public void call(Subscription t) {
                        child.add(t);
                    }
                });
            }
        });
    }
    
    /**
     * Child Subscribers will observe the events of the ConnectableObservable on the
     * specified scheduler.
     * @param <T> the value type
     * @param co the ConnectableObservable to schedule on the specified scheduler
     * @param scheduler the target Scheduler instance
     * @return the ConnectableObservable instance that is observed on the specified scheduler
     */
    public static <T> ConnectableObservable<T> observeOn(final ConnectableObservable<T> co, final Scheduler scheduler) {
        final Observable<T> observable = co.observeOn(scheduler);
        OnSubscribe<T> onSubscribe = new OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> child) {
                // apply observeOn and prevent calling onStart() again
                observable.unsafeSubscribe(new Subscriber<T>(child) {
                    @Override
                    public void onNext(T t) {
                        child.onNext(t);
                    }
                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }
                    @Override
                    public void onCompleted() {
                        child.onCompleted();
                    }
                });
            }
        };
        return new ConnectableObservable<T>(onSubscribe) {
            @Override
            public void connect(Action1<? super Subscription> connection) {
                co.connect(connection);
            }
        };
    }
    
    /**
     * Creates a replaying ConnectableObservable with an unbounded buffer.
     * @param <T> the value type
     * @param source the source Observable
     * @return the replaying ConnectableObservable
     */
    @SuppressWarnings("unchecked")
    public static <T> ConnectableObservable<T> create(Observable<? extends T> source) {
        return create(source, DEFAULT_UNBOUNDED_FACTORY);
    }
    
    /**
     * Creates a replaying ConnectableObservable with a size bound buffer.
     * @param <T> the value type
     * @param source the source Observable
     * @param bufferSize the maximum number of elements buffered
     * @return the replaying ConnectableObservable
     */
    @SuppressWarnings("cast")
    public static <T> ConnectableObservable<T> create(Observable<? extends T> source, 
            final int bufferSize) {
        if (bufferSize == Integer.MAX_VALUE) {
            return (ConnectableObservable<T>)create(source);
        }
        return create(source, new Func0<ReplayBuffer<T>>() {
            @Override
            public ReplayBuffer<T> call() {
                return new SizeBoundReplayBuffer<T>(bufferSize);
            }
        });
    }

    /**
     * Creates a replaying ConnectableObservable with a time bound buffer.
     * @param <T> the value type
     * @param source the source Observable
     * @param maxAge the maximum age (exclusive) of each item when timestamped with the given scheduler
     * @param unit the time unit of the maximum age
     * @param scheduler the scheduler providing the notion of current time
     * @return the replaying ConnectableObservable
     */
    @SuppressWarnings("cast")
    public static <T> ConnectableObservable<T> create(Observable<? extends T> source, 
            long maxAge, TimeUnit unit, Scheduler scheduler) {
        return (ConnectableObservable<T>)create(source, maxAge, unit, scheduler, Integer.MAX_VALUE);
    }

    /**
     * Creates a replaying ConnectableObservable with a size and time bound buffer.
     * @param <T> the value type
     * @param source the source Observable
     * @param maxAge the maximum age (exclusive) of each item when timestamped with the given scheduler
     * @param unit the time unit of the maximum age
     * @param scheduler the scheduler providing the notion of current time
     * @param bufferSize the maximum number of elements buffered
     * @return the replaying ConnectableObservable
     */
    public static <T> ConnectableObservable<T> create(Observable<? extends T> source, 
            long maxAge, TimeUnit unit, final Scheduler scheduler, final int bufferSize) {
        final long maxAgeInMillis = unit.toMillis(maxAge);
        return create(source, new Func0<ReplayBuffer<T>>() {
            @Override
            public ReplayBuffer<T> call() {
                return new SizeAndTimeBoundReplayBuffer<T>(bufferSize, maxAgeInMillis, scheduler);
            }
        });
    }

    /**
     * Creates a OperatorReplay instance to replay values of the given source observable.
     * @param source the source observable
     * @param bufferFactory the factory to instantiate the appropriate buffer when the observable becomes active
     * @return the connectable observable
     */
    static <T> ConnectableObservable<T> create(Observable<? extends T> source, 
            final Func0<? extends ReplayBuffer<T>> bufferFactory) {
        // the current connection to source needs to be shared between the operator and its onSubscribe call
        final AtomicReference<ReplaySubscriber<T>> curr = new AtomicReference<ReplaySubscriber<T>>();
        OnSubscribe<T> onSubscribe = new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> child) {
                // concurrent connection/disconnection may change the state, 
                // we loop to be atomic while the child subscribes
                for (;;) {
                    // get the current subscriber-to-source
                    ReplaySubscriber<T> r = curr.get();
                    // if there isn't one
                    if (r == null) {
                        // create a new subscriber to source
                        ReplaySubscriber<T> u = new ReplaySubscriber<T>(curr, bufferFactory.call());
                        // perform extra initialization to avoid 'this' to escape during construction
                        u.init();
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
                    InnerProducer<T> inner = new InnerProducer<T>(r, child);
                    // we try to add it to the array of producers
                    // if it fails, no worries because we will still have its buffer
                    // so it is going to replay it for us
                    r.add(inner);
                    // the producer has been registered with the current subscriber-to-source so 
                    // at least it will receive the next terminal event
                    child.add(inner);
                    
                    // pin the head of the buffer here, shouldn't affect anything else
                    r.buffer.replay(inner);
                    
                    // setting the producer will trigger the first request to be considered by 
                    // the subscriber-to-source.
                    child.setProducer(inner);
                    break;
                }
            }
        };
        return new OperatorReplay<T>(onSubscribe, source, curr, bufferFactory);
    }
    private OperatorReplay(OnSubscribe<T> onSubscribe, Observable<? extends T> source, 
            final AtomicReference<ReplaySubscriber<T>> current,
            final Func0<? extends ReplayBuffer<T>> bufferFactory) {
        super(onSubscribe);
        this.source = source;
        this.current = current;
        this.bufferFactory = bufferFactory;
    }

    @Override
    public void connect(Action1<? super Subscription> connection) {
        boolean doConnect = false;
        ReplaySubscriber<T> ps;
        // we loop because concurrent connect/disconnect and termination may change the state
        for (;;) {
            // retrieve the current subscriber-to-source instance
            ps = current.get();
            // if there is none yet or the current has unsubscribed
            if (ps == null || ps.isUnsubscribed()) {
                // create a new subscriber-to-source
                ReplaySubscriber<T> u = new ReplaySubscriber<T>(current, bufferFactory.call());
                // initialize out the constructor to avoid 'this' to escape
                u.init();
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
        connection.call(ps);
        if (doConnect) {
            source.unsafeSubscribe(ps);
        }
    }
    
    @SuppressWarnings("rawtypes")
    static final class ReplaySubscriber<T> extends Subscriber<T> implements Subscription {
        /** Holds notifications from upstream. */
        final ReplayBuffer<T> buffer;
        /** The notification-lite factory. */
        final NotificationLite<T> nl;
        /** Contains either an onCompleted or an onError token from upstream. */
        boolean done;
        
        /** Indicates an empty array of inner producers. */
        static final InnerProducer[] EMPTY = new InnerProducer[0];
        /** Indicates a terminated ReplaySubscriber. */
        static final InnerProducer[] TERMINATED = new InnerProducer[0];
        
        /** Indicates no further InnerProducers are accepted. */
        volatile boolean terminated;
        /** Tracks the subscribed producers. Guarded by itself. */
        final OpenHashSet<InnerProducer<T>> producers;
        /** Contains a copy of the producers. Modified only from the source side. */
        InnerProducer<T>[] producersCache;
        /** Contains number of modifications to the producers set.*/
        volatile long producersVersion;
        /** Contains the number of modifications that the producersCache holds. */
        long producersCacheVersion;
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
        volatile Producer producer;
        
        /** The queue that holds producers with request changes that need to be coordinated. */
        List<InnerProducer<T>> coordinationQueue;
        /** Indicate that all request amounts should be considered. */
        boolean coordinateAll;
        
        @SuppressWarnings("unchecked")
        public ReplaySubscriber(AtomicReference<ReplaySubscriber<T>> current,
                ReplayBuffer<T> buffer) {
            this.buffer = buffer;
            
            this.nl = NotificationLite.instance();
            this.producers = new OpenHashSet<InnerProducer<T>>();
            this.producersCache = EMPTY;
            this.shouldConnect = new AtomicBoolean();
            // make sure the source doesn't produce values until the child subscribers
            // expressed their request amounts
            this.request(0);
        }
        /** Should be called after the constructor finished to setup nulling-out the current reference. */
        void init() {
            add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    if (!terminated) {
                        synchronized (producers) {
                            if (!terminated) {
                                producers.terminate();
                                producersVersion++;
                                terminated = true;
                            }
                        }
                    }
                    // unlike OperatorPublish, we can't null out the terminated so
                    // late subscribers can still get replay
                    // current.compareAndSet(ReplaySubscriber.this, null);
                    // we don't care if it fails because it means the current has 
                    // been replaced in the meantime
                }
            }));
        }
        /**
         * Atomically try adding a new InnerProducer to this Subscriber or return false if this
         * Subscriber was terminated.
         * @param producer the producer to add
         * @return true if succeeded, false otherwise
         */
        boolean add(InnerProducer<T> producer) {
            if (producer == null) {
                throw new NullPointerException();
            }
            if (terminated) {
                return false;
            }
            synchronized (producers) {
                if (terminated) {
                    return false;
                }
                
                producers.add(producer);
                producersVersion++;
            }
            return true;
        }
        
        /**
         * Atomically removes the given producer from the producers array.
         * @param producer the producer to remove
         */
        void remove(InnerProducer<T> producer) {
            if (terminated) {
                return;
            }
            synchronized (producers) {
                if (terminated) {
                    return;
                }
                producers.remove(producer);
                producersVersion++;
            }
        }
        
        @Override
        public void setProducer(Producer p) {
            Producer p0 = producer;
            if (p0 != null) {
                throw new IllegalStateException("Only a single producer can be set on a Subscriber.");
            }
            producer = p;
            manageRequests(null);
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
                    unsubscribe(); // expectation of testIssue2191
                }
            }
        }
        @Override
        public void onCompleted() {
            // The observer front is accessed serially as required by spec so
            // no need to CAS in the terminal value
            if (!done) {
                done = true;
                try {
                    buffer.complete();
                    replay();
                } finally {
                    unsubscribe();
                }
            }
        }
        
        /**
         * Coordinates the request amounts of various child Subscribers.
         */
        void manageRequests(InnerProducer<T> inner) {
            // if the upstream has completed, no more requesting is possible
            if (isUnsubscribed()) {
                return;
            }
            synchronized (this) {
                if (emitting) {
                    if (inner != null) {
                        List<InnerProducer<T>> q = coordinationQueue;
                        if (q == null) {
                            q = new ArrayList<InnerProducer<T>>();
                            coordinationQueue = q;
                        }
                        q.add(inner);
                    } else {
                        coordinateAll = true;
                    }
                    missed = true;
                    return;
                }
                emitting = true;
            }
            
            long ri = maxChildRequested;
            long maxTotalRequested;
            
            if (inner != null) {
                maxTotalRequested = Math.max(ri, inner.totalRequested.get());
            } else {
                maxTotalRequested = ri;

                InnerProducer<T>[] a = copyProducers();
                for (InnerProducer<T> rp : a) {
                    if (rp != null) {
                        maxTotalRequested = Math.max(maxTotalRequested, rp.totalRequested.get());
                    }
                }
                
            }
            makeRequest(maxTotalRequested, ri);
            
            for (;;) {
                // if the upstream has completed, no more requesting is possible
                if (isUnsubscribed()) {
                    return;
                }
                
                List<InnerProducer<T>> q;
                boolean all;
                synchronized (this) {
                    if (!missed) {
                        emitting = false;
                        return;
                    }
                    missed = false;
                    q = coordinationQueue;
                    coordinationQueue = null;
                    all = coordinateAll;
                    coordinateAll = false;
                }
                
                ri = maxChildRequested;
                maxTotalRequested = ri;

                if (q != null) {
                    for (InnerProducer<T> rp : q) {
                        maxTotalRequested = Math.max(maxTotalRequested, rp.totalRequested.get());
                    }
                } 
                
                if (all) {
                    InnerProducer<T>[] a = copyProducers();
                    for (InnerProducer<T> rp : a) {
                        if (rp != null) {
                            maxTotalRequested = Math.max(maxTotalRequested, rp.totalRequested.get());
                        }
                    }
                }
                
                makeRequest(maxTotalRequested, ri);
            }
        }
        
        InnerProducer<T>[] copyProducers() {
            synchronized (producers) {
                Object[] a = producers.values();
                int n = a.length;
                @SuppressWarnings("unchecked")
                InnerProducer<T>[] result = new InnerProducer[n];
                System.arraycopy(a, 0, result, 0, n);
                return result;
            }
        }
        
        void makeRequest(long maxTotalRequests, long previousTotalRequests) {
            long ur = maxUpstreamRequested;
            Producer p = producer;

            long diff = maxTotalRequests - previousTotalRequests;
            if (diff != 0) {
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
        }
        
        /**
         * Tries to replay the buffer contents to all known subscribers.
         */
        @SuppressWarnings("unchecked")
        void replay() {
            InnerProducer<T>[] pc = producersCache;
            if (producersCacheVersion != producersVersion) {
                synchronized (producers) {
                    pc = producersCache;
                    // if the producers hasn't changed do nothing
                    // otherwise make a copy of the current set of producers
                    Object[] a = producers.values();
                    int n = a.length;
                    if (pc.length != n) {
                        pc = new InnerProducer[n];
                        producersCache = pc;
                    }
                    System.arraycopy(a, 0, pc, 0, n);
                    producersCacheVersion = producersVersion;
                }
            }
            ReplayBuffer<T> b = buffer;
            for (InnerProducer<T> rp : pc) {
                if (rp != null) {
                    b.replay(rp);
                }
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
    static final class InnerProducer<T> extends AtomicLong implements Producer, Subscription {
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
        
        public InnerProducer(ReplaySubscriber<T> parent, Subscriber<? super T> child) {
            this.parent = parent;
            this.child = child;
            this.totalRequested = new AtomicLong();
        }
        
        @Override
        public void request(long n) {
            // ignore negative requests
            if (n < 0) {
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
                long u = r + n;
                // and check for long overflow
                if (u < 0) {
                    // cap at max value, which is essentially unlimited
                    u = Long.MAX_VALUE;
                }
                // try setting the new request value
                if (compareAndSet(r, u)) {
                    // increment the total request counter
                    addTotalRequested(n);
                    // if successful, notify the parent dispatcher this child can receive more
                    // elements
                    parent.manageRequests(this);
                    
                    parent.buffer.replay(this);
                    return;
                }
                // otherwise, someone else changed the state (perhaps a concurrent 
                // request or unsubscription so retry
            }
        }
        
        /**
         * Increments the total requested amount.
         * @param n the additional request amount
         */
        void addTotalRequested(long n) {
            for (;;) {
                long r = totalRequested.get();
                long u = r + n;
                if (u < 0) {
                    u = Long.MAX_VALUE;
                }
                if (totalRequested.compareAndSet(r, u)) {
                    return;
                }
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
                    // and return the updated value
                    return u;
                }
                // otherwise, some concurrent activity happened and we need to retry
            }
        }
        
        @Override
        public boolean isUnsubscribed() {
            return get() == UNSUBSCRIBED;
        }
        @Override
        public void unsubscribe() {
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
                    parent.manageRequests(this);
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
        void replay(InnerProducer<T> output);
    }
    
    /**
     * Holds an unbounded list of events.
     *
     * @param <T> the value type
     */
    static final class UnboundedReplayBuffer<T> extends ArrayList<Object> implements ReplayBuffer<T> {
        /** */
        private static final long serialVersionUID = 7063189396499112664L;
        final NotificationLite<T> nl;
        /** The total number of events in the buffer. */
        volatile int size;
        
        public UnboundedReplayBuffer(int capacityHint) {
            super(capacityHint);
            nl = NotificationLite.instance();
        }
        @Override
        public void next(T value) {
            add(nl.next(value));
            size++;
        }

        @Override
        public void error(Throwable e) {
            add(nl.error(e));
            size++;
        }

        @Override
        public void complete() {
            add(nl.completed());
            size++;
        }

        @Override
        public void replay(InnerProducer<T> output) {
            synchronized (output) {
                if (output.emitting) {
                    output.missed = true;
                    return;
                }
                output.emitting = true;
            }
            for (;;) {
                if (output.isUnsubscribed()) {
                    return;
                }
                int sourceIndex = size;
                
                Integer destIndexObject = output.index();
                int destIndex = destIndexObject != null ? destIndexObject : 0;
                
                long r = output.get();
                long r0 = r;
                long e = 0L;
                
                while (r != 0L && destIndex < sourceIndex) {
                    Object o = get(destIndex);
                    try {
                        if (nl.accept(output.child, o)) {
                            return;
                        }
                    } catch (Throwable err) {
                        Exceptions.throwIfFatal(err);
                        output.unsubscribe();
                        if (!nl.isError(o) && !nl.isCompleted(o)) {
                            output.child.onError(OnErrorThrowable.addValueAsLastCause(err, nl.getValue(o)));
                        }
                        return;
                    }
                    if (output.isUnsubscribed()) {
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
     */
    static final class Node extends AtomicReference<Node> {
        /** */
        private static final long serialVersionUID = 245354315435971818L;
        
        /** The contained value. */
        final Object value;
        /** The absolute index of the value. */
        final long index;
        
        public Node(Object value, long index) {
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
        /** */
        private static final long serialVersionUID = 2346567790059478686L;
        final NotificationLite<T> nl;
        
        Node tail;
        int size;
        
        /** The total number of received values so far. */
        long index;
        
        public BoundedReplayBuffer() {
            nl = NotificationLite.instance();
            Node n = new Node(null, 0);
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
            Object o = enterTransform(nl.next(value));
            Node n = new Node(o, ++index);
            addLast(n);
            truncate();
        }

        @Override
        public final void error(Throwable e) {
            Object o = enterTransform(nl.error(e));
            Node n = new Node(o, ++index);
            addLast(n);
            truncateFinal();
        }

        @Override
        public final void complete() {
            Object o = enterTransform(nl.completed());
            Node n = new Node(o, ++index);
            addLast(n);
            truncateFinal();
        }

        @Override
        public final void replay(InnerProducer<T> output) {
            synchronized (output) {
                if (output.emitting) {
                    output.missed = true;
                    return;
                }
                output.emitting = true;
            }
            for (;;) {
                if (output.isUnsubscribed()) {
                    return;
                }

                long r = output.get();
                boolean unbounded = r == Long.MAX_VALUE;
                long e = 0L;
                
                Node node = output.index();
                if (node == null) {
                    node = get();
                    output.index = node;
                    
                    /*
                     * Since this is a latecommer, fix its total requested amount
                     * as if it got all the values up to the node.index
                     */
                    output.addTotalRequested(node.index);
                }

                if (output.isUnsubscribed()) {
                    return;
                }

                while (r != 0) {
                    Node v = node.get();
                    if (v != null) {
                        Object o = leaveTransform(v.value);
                        try {
                            if (nl.accept(output.child, o)) {
                                output.index = null;
                                return;
                            }
                        } catch (Throwable err) {
                            output.index = null;
                            Exceptions.throwIfFatal(err);
                            output.unsubscribe();
                            if (!nl.isError(o) && !nl.isCompleted(o)) {
                                output.child.onError(OnErrorThrowable.addValueAsLastCause(err, nl.getValue(o)));
                            }
                            return;
                        }
                        e++;
                        r--;
                        node = v;
                    } else {
                        break;
                    }
                    if (output.isUnsubscribed()) {
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
                    if (nl.isCompleted(v) || nl.isError(v)) {
                        break;
                    }
                    output.add(nl.getValue(v));
                    n = next;
                } else {
                    break;
                }
            }
        }
        /* test */ boolean hasError() {
            return tail.value != null && nl.isError(leaveTransform(tail.value));
        }
        /* test */ boolean hasCompleted() {
            return tail.value != null && nl.isCompleted(leaveTransform(tail.value));
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
        final long maxAgeInMillis;
        final int limit;
        public SizeAndTimeBoundReplayBuffer(int limit, long maxAgeInMillis, Scheduler scheduler) {
            this.scheduler = scheduler;
            this.limit = limit;
            this.maxAgeInMillis = maxAgeInMillis;
        }
        
        @Override
        Object enterTransform(Object value) {
            return new Timestamped<Object>(scheduler.now(), value);
        }
        
        @Override
        Object leaveTransform(Object value) {
            return ((Timestamped<?>)value).getValue();
        }
        
        @Override
        void truncate() {
            long timeLimit = scheduler.now() - maxAgeInMillis;
            
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
                        Timestamped<?> v = (Timestamped<?>)next.value;
                        if (v.getTimestampMillis() <= timeLimit) {
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
            long timeLimit = scheduler.now() - maxAgeInMillis;
            
            Node prev = get();
            Node next = prev.get();
            
            int e = 0;
            for (;;) {
                if (next != null && size > 1) {
                    Timestamped<?> v = (Timestamped<?>)next.value;
                    if (v.getTimestampMillis() <= timeLimit) {
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