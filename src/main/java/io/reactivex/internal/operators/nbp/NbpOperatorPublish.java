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

package io.reactivex.internal.operators.nbp;

import java.util.Queue;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import io.reactivex.NbpObservable;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.observables.nbp.NbpConnectableObservable;

/**
 * A connectable observable which shares an underlying source and dispatches source values to subscribers in a backpressure-aware
 * manner. 
 * @param <T> the value type
 */
public final class NbpOperatorPublish<T> extends NbpConnectableObservable<T> {
    /** The source observable. */
    final NbpObservable<? extends T> source;
    /** Holds the current subscriber that is, will be or just was subscribed to the source observable. */
    final AtomicReference<PublishSubscriber<T>> current;
    
    /** The size of the prefetch buffer. */
    final int bufferSize;

    /**
     * Creates a OperatorPublish instance to publish values of the given source observable.
     * @param source the source observable
     * @return the connectable observable
     */
    public static <T> NbpConnectableObservable<T> create(NbpObservable<? extends T> source, int bufferSize) {
        // the current connection to source needs to be shared between the operator and its onSubscribe call
        final AtomicReference<PublishSubscriber<T>> curr = new AtomicReference<>();
        NbpOnSubscribe<T> onSubscribe = new NbpOnSubscribe<T>() {
            @Override
            public void accept(NbpSubscriber<? super T> child) {
                // concurrent connection/disconnection may change the state, 
                // we loop to be atomic while the child subscribes
                for (;;) {
                    // get the current subscriber-to-source
                    PublishSubscriber<T> r = curr.get();
                    // if there isn't one or it is unsubscribed
                    if (r == null || r.isDisposed()) {
                        // create a new subscriber to source
                        PublishSubscriber<T> u = new PublishSubscriber<>(curr, bufferSize);
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
                    InnerProducer<T> inner = new InnerProducer<>(r, child);
                    /*
                     * Try adding it to the current subscriber-to-source, add is atomic in respect 
                     * to other adds and the termination of the subscriber-to-source.
                     */
                    if (!r.add(inner)) {
                        /*
                         * The current PublishSubscriber has been terminated, try with a newer one.
                         */
                        continue;
                        /*
                         * Note: although technically corrent, concurrent disconnects can cause 
                         * unexpected behavior such as child subscribers never receiving anything 
                         * (unless connected again). An alternative approach, similar to 
                         * PublishSubject would be to immediately terminate such child 
                         * subscribers as well:
                         * 
                         * Object term = r.terminalEvent;
                         * if (r.nl.isCompleted(term)) {
                         *     child.onCompleted();
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
                    // the producer has been registered with the current subscriber-to-source so 
                    // at least it will receive the next terminal event
                    // setting the producer will trigger the first request to be considered by 
                    // the subscriber-to-source.
                    child.onSubscribe(inner);
                    break;
                }
            }
        };
        return new NbpOperatorPublish<>(onSubscribe, source, curr, bufferSize);
    }

    public static <T, R> NbpObservable<R> create(final NbpObservable<? extends T> source, 
            final Function<? super NbpObservable<T>, ? extends NbpObservable<R>> selector, int bufferSize) {
        return create(sr -> {
            NbpConnectableObservable<T> op = create(source, bufferSize);
            
            NbpSubscriberResourceWrapper<R, Disposable> srw = new NbpSubscriberResourceWrapper<>(sr, Disposable::dispose);
            
            selector.apply(op).subscribe(srw);
            
            op.connect(srw::setResource);
        });
    }

    private NbpOperatorPublish(NbpOnSubscribe<T> onSubscribe, NbpObservable<? extends T> source, 
            final AtomicReference<PublishSubscriber<T>> current, int bufferSize) {
        super(onSubscribe);
        this.source = source;
        this.current = current;
        this.bufferSize = bufferSize;
    }

    @Override
    public void connect(Consumer<? super Disposable> connection) {
        boolean doConnect = false;
        PublishSubscriber<T> ps;
        // we loop because concurrent connect/disconnect and termination may change the state
        for (;;) {
            // retrieve the current subscriber-to-source instance
            ps = current.get();
            // if there is none yet or the current has unsubscribed
            if (ps == null || ps.isDisposed()) {
                // create a new subscriber-to-source
                PublishSubscriber<T> u = new PublishSubscriber<>(current, bufferSize);
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
         * child-subscribers without any terminal event; PublishSubject does not have this 
         * issue because the unsubscription was always triggered by the child-subscribers 
         * themselves.
         */
        connection.accept(ps);
        if (doConnect) {
            source.subscribe(ps);
        }
    }
    
    @SuppressWarnings("rawtypes")
    static final class PublishSubscriber<T> implements NbpSubscriber<T>, Disposable {
        /** Holds notifications from upstream. */
        final Queue<Object> queue;
        /** Holds onto the current connected PublishSubscriber. */
        final AtomicReference<PublishSubscriber<T>> current;
        /** The prefetch buffer size. */
        final int bufferSize;
        /** Contains either an onCompleted or an onError token from upstream. */
        volatile Object terminalEvent;
        
        /** Indicates an empty array of inner producers. */
        static final InnerProducer[] EMPTY = new InnerProducer[0];
        /** Indicates a terminated PublishSubscriber. */
        static final InnerProducer[] TERMINATED = new InnerProducer[0];
        
        /** Tracks the subscribed producers. */
        final AtomicReference<InnerProducer[]> producers;
        /** 
         * Atomically changed from false to true by connect to make sure the 
         * connection is only performed by one thread. 
         */
        final AtomicBoolean shouldConnect;
        
        /** Guarded by this. */
        boolean emitting;
        /** Guarded by this. */
        boolean missed;
        
        volatile Disposable s;
        static final AtomicReferenceFieldUpdater<PublishSubscriber, Disposable> S =
                AtomicReferenceFieldUpdater.newUpdater(PublishSubscriber.class, Disposable.class, "s");
        
        static final Disposable CANCELLED = () -> { };
        
        public PublishSubscriber(AtomicReference<PublishSubscriber<T>> current, int bufferSize) {
            this.queue = new SpscArrayQueue<>(bufferSize);
            
            this.producers = new AtomicReference<>(EMPTY);
            this.current = current;
            this.shouldConnect = new AtomicBoolean();
            this.bufferSize = bufferSize;
        }
        
        @Override
        public void dispose() {
            if (producers.get() != TERMINATED) {
                InnerProducer[] ps = producers.getAndSet(TERMINATED);
                if (ps != TERMINATED) {
                    current.compareAndSet(PublishSubscriber.this, null);
                    
                    Disposable a = s;
                    if (a != CANCELLED) {
                        a = S.getAndSet(this, CANCELLED);
                        if (a != CANCELLED && a != null) {
                            a.dispose();
                        }
                    }
                }
            }
        }
        
        public boolean isDisposed() {
            return producers.get() == TERMINATED; 
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (!S.compareAndSet(this, null, s)) {
                s.dispose();
                if (this.s != CANCELLED) {
                    SubscriptionHelper.reportSubscriptionSet();
                }
                return;
            }
        }
        
        @Override
        public void onNext(T t) {
            // we expect upstream to honor backpressure requests
            // nl is required because JCTools queue doesn't accept nulls.
            if (!queue.offer(t)) {
                onError(new IllegalStateException("Prefetch queue is full?!"));
            } else {
                // since many things can happen concurrently, we have a common dispatch 
                // loop to act on the current state serially
                dispatch();
            }
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
         * Atomically try adding a new InnerProducer to this Subscriber or return false if this
         * Subscriber was terminated.
         * @param producer the producer to add
         * @return true if succeeded, false otherwise
         */
        boolean add(InnerProducer<T> producer) {
            if (producer == null) {
                throw new NullPointerException();
            }
            // the state can change so we do a CAS loop to achieve atomicity
            for (;;) {
                // get the current producer array
                InnerProducer[] c = producers.get();
                // if this subscriber-to-source reached a terminal state by receiving 
                // an onError or onCompleted, just refuse to add the new producer
                if (c == TERMINATED) {
                    return false;
                }
                // we perform a copy-on-write logic
                int len = c.length;
                InnerProducer[] u = new InnerProducer[len + 1];
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
        void remove(InnerProducer<T> producer) {
            // the state can change so we do a CAS loop to achieve atomicity
            for (;;) {
                // let's read the current producers array
                InnerProducer[] c = producers.get();
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
                InnerProducer[] u;
                // we don't create a new empty array if producer was the single inhabitant
                // but rather reuse an empty array
                if (len == 1) {
                    u = EMPTY;
                } else {
                    // otherwise, create a new array one less in size
                    u = new InnerProducer[len - 1];
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
                        // not yet unsubscribed PublishSubscriber
                        current.compareAndSet(this, null);
                        try {
                            /*
                             * This will swap in a terminated array so add() in OnSubscribe will reject
                             * child subscribers to associate themselves with a terminated and thus
                             * never again emitting chain.
                             * 
                             * Since we atomically change the contents of 'producers' only one
                             * operation wins at a time. If an add() wins before this getAndSet,
                             * its value will be part of the returned array by getAndSet and thus
                             * will receive the terminal notification. Otherwise, if getAndSet wins,
                             * add() will refuse to add the child producer and will trigger the
                             * creation of subscriber-to-source.
                             */
                            for (InnerProducer<?> ip : producers.getAndSet(TERMINATED)) {
                                ip.child.onComplete();
                            }
                        } finally {
                            // we explicitely unsubscribe/disconnect from the upstream
                            // after we sent out the terminal event to child subscribers
                            dispose();
                        }
                        // indicate we reached the terminal state
                        return true;
                    }
                } else {
                    Throwable t = NotificationLite.getError(term);
                    // this will prevent OnSubscribe spinning on a terminated 
                    // but not yet unsubscribed PublishSubscriber
                    current.compareAndSet(this, null);
                    try {
                        // this will swap in a terminated array so add() in OnSubscribe will reject
                        // child subscribers to associate themselves with a terminated and thus
                        // never again emitting chain
                        for (InnerProducer<?> ip : producers.getAndSet(TERMINATED)) {
                            ip.child.onError(t);
                        }
                    } finally {
                        // we explicitely unsubscribe/disconnect from the upstream
                        // after we sent out the terminal event to child subscribers
                        dispose();
                    }
                    // indicate we reached the terminal state
                    return true;
                }
            }
            // there is still work to be done
            return false;
        }
        
        /**
         * The common serialization point of events arriving from upstream and child-subscribers
         * requesting more.
         */
        void dispatch() {
            // standard construct of emitter loop (blocking)
            // if there is an emission going on, indicate that more work needs to be done
            // the exact nature of this work needs to be determined from other data structures
            synchronized (this) {
                if (emitting) {
                    missed = true;
                    return;
                }
                // there was no emission going on, we won and will start emitting
                emitting = true;
                missed = false;
            }
            /*
             * In case an exception is thrown in the loop, we need to set emitting back to false
             * on the way out (the exception will propagate up) so if it bounces back and
             * onError is called, its dispatch() call will have the opportunity to emit it.
             * However, if we want to exit regularly, we will set the emitting to false (+ other operations)
             * atomically so we want to prevent the finally part to accidentally unlock some other
             * emissions happening between the two synchronized blocks.
             */
            boolean skipFinal = false;
            try {
                for (;;) {
                    /*
                     * We need to read terminalEvent before checking the queue for emptyness because
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
                    boolean empty = queue.isEmpty();
                    // if the queue is empty and the terminal event was received, quit
                    // and don't bother restoring emitting to false: no further activity is
                    // possible at this point
                    if (checkTerminated(term, empty)) {
                        skipFinal = true;
                        return;
                    }
                    
                    // We have elements queued. Note that due to the serialization nature of dispatch()
                    // this loop is the only one which can turn a non-empty queue into an empty one
                    // and as such, no need to ask the queue itself again for that.
                    if (!empty) {
                        // We take a snapshot of the current child-subscribers.
                        // Concurrent subscribers may miss this iteration, but it is to be expected
                        @SuppressWarnings("unchecked")
                        InnerProducer<T>[] ps = producers.get();
                        
                        int len = ps.length;
                        // count how many have triggered unsubscription
                        int unsubscribed = 0;
                    
                        // Now find the minimum amount each child-subscriber requested
                        // since we can only emit that much to all of them without violating
                        // backpressure constraints
                        for (InnerProducer<T> ip : ps) {
                            if (ip.cancelled) {
                                unsubscribed++;
                            }
                            // we ignore those with NOT_REQUESTED as if they aren't even there
                        }
                        
                        // it may happen everyone has unsubscribed between here and producers.get()
                        // or we have no subscribers at all to begin with
                        if (len == unsubscribed) {
                            term = terminalEvent;
                            // so let's consume a value from the queue
                            Object v = queue.poll();
                            // or terminate if there was a terminal event and the queue is empty
                            if (checkTerminated(term, v == null)) {
                                skipFinal = true;
                                return;
                            }
                            // and retry emitting to potential new child-subscribers
                            continue;
                        }
                        // if we get here, it means there are non-unsubscribed child-subscribers
                        // and we count the number of emitted values because the queue
                        // may contain less than requested
                        for (;;) {
                            term = terminalEvent;
                            Object v = queue.poll();
                            empty = v == null;
                            // let's check if there is a terminal event and the queue became empty just now
                            if (checkTerminated(term, empty)) {
                                skipFinal = true;
                                return;
                            }
                            // the queue is empty but we aren't terminated yet, finish this emission loop
                            if (empty) {
                                break;
                            }
                            // we need to unwrap potential nulls
                            T value = NotificationLite.getValue(v);
                            // let's emit this value to all child subscribers
                            for (InnerProducer<T> ip : ps) {
                                // if ip.get() is negative, the child has either unsubscribed in the
                                // meantime or hasn't requested anything yet
                                // this eager behavior will skip unsubscribed children in case
                                // multiple values are available in the queue
                                if (!ip.cancelled) {
                                    try {
                                        ip.child.onNext(value);
                                    } catch (Throwable t) {
                                        // we bounce back exceptions and kick out the child subscriber
                                        ip.dispose();
                                        ip.child.onError(t);
                                        continue;
                                    }
                                }
                            }
                        }
                        
                        // if we have requests but not an empty queue after emission
                        // let's try again to see if more requests/child-subscribers are 
                        // ready to receive more
                        if (!empty) {
                            continue;
                        }
                    }
                    
                    // we did what we could: either the queue is empty or child subscribers
                    // haven't requested more (or both), let's try to finish dispatching
                    synchronized (this) {
                        // since missed is changed atomically, if we see it as true
                        // it means some state has changed and we need to loop again
                        // and handle that case
                        if (!missed) {
                            // but if no missed dispatch happened, let's stop emitting
                            emitting = false;
                            // and skip the emitting = false in the finally block as well
                            skipFinal = true;
                            return;
                        }
                        // we acknowledge the missed changes so far
                        missed = false;
                    }
                }
            } finally {
                // unless returned cleanly (i.e., some method above threw)
                if (!skipFinal) {
                    // we stop emitting so the error can propagate back down through onError
                    synchronized (this) {
                        emitting = false;
                    }
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
    static final class InnerProducer<T> implements Disposable {
        /** 
         * The parent subscriber-to-source used to allow removing the child in case of
         * child unsubscription.
         */
        final PublishSubscriber<T> parent;
        /** The actual child subscriber. */
        final NbpSubscriber<? super T> child;
        /** 
         * Indicates this child has been unsubscribed: the state is swapped in atomically and
         * will prevent the dispatch() to emit (too many) values to a terminated child subscriber.
         */
        volatile boolean cancelled;
        
        public InnerProducer(PublishSubscriber<T> parent, NbpSubscriber<? super T> child) {
            this.parent = parent;
            this.child = child;
        }
        
        public boolean isDisposed() {
            return cancelled;
        }
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                // remove this from the parent
                parent.remove(this);
                // After removal, we might have unblocked the other child subscribers:
                // let's assume this child had 0 requested before the unsubscription while
                // the others had non-zero. By removing this 'blocking' child, the others
                // are now free to receive events
                parent.dispatch();
            }
        }
    }
}