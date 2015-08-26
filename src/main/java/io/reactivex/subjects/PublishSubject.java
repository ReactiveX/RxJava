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
package io.reactivex.subjects;

import java.util.concurrent.atomic.*;
import java.util.function.IntFunction;

import org.reactivestreams.*;

import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * A Subject that multicasts events to Subscribers that are currently subscribed to it.
 * 
 * <p>The subject does not coordinate backpressure for its subscribers and implements a weaker onSubscribe which
 * calls requests Long.MAX_VALUE from the incoming Subscriptions. This makes it possible to subscribe the PublishSubject
 * to multiple sources (note on serialization though) unlike the standard contract on Subscriber. Child subscribers, however, are not overflown but receive an
 * IllegalStateException in case their requested amount is zero.
 * 
 * <p>The implementation of onXXX methods are technically thread-safe but non-serialized calls
 * to them may lead to undefined state in the currently subscribed Subscribers.
 * 
 * <p>Due to the nature Observables are constructed, the PublishSubject can't be instantiated through
 * {@code new} but must be created via the {@link #create()} method.
 *
 * @param <T> the value type multicast to Subscribers.
 */
public final class PublishSubject<T> extends Subject<T, T> {
    
    /**
     * Constructs a PublishSubject.
     * @return the new PublishSubject
     */
    public static <T> PublishSubject<T> create() {
        State<T> state = new State<>();
        return new PublishSubject<>(state);
    }
    
    /** Holds the terminal event and manages the array of subscribers. */
    final State<T> state;
    /** 
     * Indicates the subject has been terminated. It is checked in the onXXX methods in
     * a relaxed matter: concurrent calls may not properly see it (which shouldn't happen if
     * the reactive-streams contract is held).
     */
    boolean done;
    
    protected PublishSubject(State<T> state) {
        super(state);
        this.state = state;
    }
    
    @Override
    public void onSubscribe(Subscription s) {
        if (done) {
            s.cancel();
            return;
        }
        // PublishSubject doesn't bother with request coordination.
        s.request(Long.MAX_VALUE);
    }
    
    @Override
    public void onNext(T t) {
        if (done) {
            return;
        }
        for (PublishSubscriber<T> s : state.subscribers()) {
            s.onNext(t);
        }
    }
    
    @Override
    public void onError(Throwable t) {
        if (done) {
            return;
        }
        done = true;
        for (PublishSubscriber<T> s : state.terminate(t)) {
            s.onError(t);
        }
    }
    
    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        done = true;
        for (PublishSubscriber<T> s : state.terminate()) {
            s.onComplete();
        }
    }
    
    @Override
    public boolean hasSubscribers() {
        return state.subscribers().length != 0;
    }
    
    @Override
    public boolean hasValue() {
        return false;
    }
    
    @Override
    public T getValue() {
        return null;
    }
    
    @Override
    public T[] getValues(T[] array) {
        if (array.length != 0) {
            array[0] = null;
        }
        return array;
    }
    
    @Override
    public Throwable getThrowable() {
        Object o = state.get();
        if (o == State.COMPLETE) {
            return null;
        }
        return (Throwable)o;
    }
    
    @Override
    public boolean hasThrowable() {
        Object o = state.get();
        return o != null && o != State.COMPLETE;
    }
    
    @Override
    public boolean hasComplete() {
        return state.get() == State.COMPLETE;
    }
    
    /**
     * Contains the state of the Subject, including the currently subscribed clients and the 
     * terminal value.
     *
     * @param <T> the value type of the events
     */
    @SuppressWarnings("rawtypes")
    static final class State<T> extends AtomicReference<Object> implements Publisher<T>, IntFunction<PublishSubscriber[]> {
        /** */
        private static final long serialVersionUID = -2699311989055418316L;
        /** The completion token. */
        static final Object COMPLETE = new Object();
        
        /** The terminated indicator for the subscribers array. */
        static final PublishSubscriber[] TERMINATED = new PublishSubscriber[0];
        /** An empty subscribers array to avoid allocating it all the time. */
        static final PublishSubscriber[] EMPTY = new PublishSubscriber[0];

        /** The array of currently subscribed subscribers. */
        @SuppressWarnings("unchecked")
        volatile PublishSubscriber<T>[] subscribers = EMPTY;
        
        /** Field updater for subscribers. */
        static final AtomicReferenceFieldUpdater<State, PublishSubscriber[]> SUBSCRIBERS =
                AtomicReferenceFieldUpdater.newUpdater(State.class, PublishSubscriber[].class, "subscribers");
        
        @Override
        public void subscribe(Subscriber<? super T> t) {
            PublishSubscriber<T> ps = new PublishSubscriber<>(t, this);
            t.onSubscribe(ps);
            if (ps.cancelled == 0) {
                if (add(ps)) {
                    // if cancellation happened while a successful add, the remove() didn't work
                    // so we need to do it again
                    if (ps.cancelled != 0) {
                        remove(ps);
                    }
                } else {
                    Object o = get();
                    if (o == COMPLETE) {
                        ps.onComplete();
                    } else {
                        ps.onError((Throwable)o);
                    }
                }
            }
        }
        
        /**
         * @return the array of currently subscribed subscribers
         */
        PublishSubscriber<T>[] subscribers() {
            return subscribers;
        }
        
        /**
         * Atomically swaps in the terminal state with a completion indicator and returns
         * the last array of subscribers.
         * @return the last array of subscribers
         */
        PublishSubscriber<T>[] terminate() {
            return terminate(COMPLETE);
        }
        
        /**
         * Atomically swaps in the terminal state with a completion indicator and returns
         * the last array of subscribers.
         * @param event the terminal state value to set
         * @return the last array of subscribers
         */
        @SuppressWarnings("unchecked")
        PublishSubscriber<T>[] terminate(Object event) {
            if (compareAndSet(null, event)) {
                return TerminalAtomicsHelper.terminate(SUBSCRIBERS, this, TERMINATED);
            }
            return TERMINATED;
        }
        
        /**
         * Tries to add the given subscriber to the subscribers array atomically
         * or returns false if the subject has terminated.
         * @param ps the subscriber to add
         * @return true if successful, false if the subject has terminated
         */
        boolean add(PublishSubscriber<T> ps) {
            return TerminalAtomicsHelper.add(SUBSCRIBERS, this, ps, TERMINATED, this);
        }
        
        /**
         * Atomically removes the given subscriber if it is subscribed to the subject.
         * @param ps the subject to remove
         */
        void remove(PublishSubscriber<T> ps) {
            TerminalAtomicsHelper.remove(SUBSCRIBERS, this, ps, EMPTY, TERMINATED, this);
        }
        
        @Override
        public PublishSubscriber[] apply(int value) {
            return new PublishSubscriber[value];
        }
    }
    
    /**
     * Wraps the actual subscriber, tracks its requests and makes cancellation
     * to remove itself from the current subscribers array.
     *
     * @param <T> the value type
     */
    static final class PublishSubscriber<T> extends AtomicLong implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = 3562861878281475070L;
        /** The actual subscriber. */
        final Subscriber<? super T> actual;
        /** The subject state. */
        final State<T> state;

        /** Indicates the cancelled state if not zero. */
        volatile int cancelled;
        /** Field updater for cancelled. */
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<PublishSubscriber> CANCELLED =
                AtomicIntegerFieldUpdater.newUpdater(PublishSubscriber.class, "cancelled");
        
        /**
         * Constructs a PublishSubscriber, wraps the actual subscriber and the state.
         * @param actual the actual subscriber
         * @param state the state
         */
        public PublishSubscriber(Subscriber<? super T> actual, State<T> state) {
            this.actual = actual;
            this.state = state;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            // not called because requests are handled locally and cancel is forwared to state
        }
        
        @Override
        public void onNext(T t) {
            long r = get();
            if (r != 0L) {
                actual.onNext(t);
                if (r != Long.MAX_VALUE) {
                    decrementAndGet();
                }
            } else {
                cancel();
                actual.onError(new IllegalStateException("Could not emit value due to lack of requests"));
            }
        }
        
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            if (n <= 0) {
                // can't really call onError here because request could be async in respect to other onXXX calls
                RxJavaPlugins.onError(new IllegalStateException("n > 0 required but it was " + n));
                return;
            }
            BackpressureHelper.add(this, n);
        }
        
        @Override
        public void cancel() {
            if (cancelled == 0 && CANCELLED.compareAndSet(this, 0, 1)) {
                state.remove(this);
            }
        }
    }
}
