/**
 * Copyright 2016 Netflix, Inc.
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
package io.reactivex.processors;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.functions.IntFunction;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
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
public final class PublishProcessor<T> extends FlowProcessor<T, T> {
    
    /**
     * Constructs a PublishSubject.
     * @param <T> the value type
     * @return the new PublishSubject
     */
    public static <T> PublishProcessor<T> create() {
        State<T> state = new State<T>();
        return new PublishProcessor<T>(state);
    }
    
    /** Holds the terminal event and manages the array of subscribers. */
    final State<T> state;
    /** 
     * Indicates the subject has been terminated. It is checked in the onXXX methods in
     * a relaxed matter: concurrent calls may not properly see it (which shouldn't happen if
     * the reactive-streams contract is held).
     */
    boolean done;
    
    protected PublishProcessor(State<T> state) {
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
        if (t == null) {
            onError(new NullPointerException());
            return;
        }
        for (PublishSubscriber<T> s : state.subscribers()) {
            s.onNext(t);
        }
    }
    
    @Override
    public void onError(Throwable t) {
        if (done) {
            RxJavaPlugins.onError(t);
            return;
        }
        done = true;
        if (t == null) {
            t = new NullPointerException();
        }
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
    static final class State<T> extends AtomicReference<Object> 
    implements Publisher<T>, IntFunction<PublishSubscriber<T>[]> {
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
        final AtomicReference<PublishSubscriber<T>[]> subscribers = new AtomicReference<PublishSubscriber<T>[]>(EMPTY);
        
        @Override
        public void subscribe(Subscriber<? super T> t) {
            PublishSubscriber<T> ps = new PublishSubscriber<T>(t, this);
            t.onSubscribe(ps);
            if (!ps.cancelled.get()) {
                if (add(ps)) {
                    // if cancellation happened while a successful add, the remove() didn't work
                    // so we need to do it again
                    if (ps.cancelled.get()) {
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
            return subscribers.get();
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
                return TerminalAtomicsHelper.terminate(subscribers, TERMINATED);
            }
            return TERMINATED;
        }
        
        /**
         * Tries to add the given subscriber to the subscribers array atomically
         * or returns false if the subject has terminated.
         * @param ps the subscriber to add
         * @return true if successful, false if the subject has terminated
         */
        @SuppressWarnings("unchecked")
        boolean add(PublishSubscriber<T> ps) {
            return TerminalAtomicsHelper.add(subscribers, ps, TERMINATED, this);
        }
        
        /**
         * Atomically removes the given subscriber if it is subscribed to the subject.
         * @param ps the subject to remove
         */
        @SuppressWarnings("unchecked")
        void remove(PublishSubscriber<T> ps) {
            TerminalAtomicsHelper.remove(subscribers, ps, TERMINATED, EMPTY, this);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public PublishSubscriber<T>[] apply(int value) {
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

        final AtomicBoolean cancelled = new AtomicBoolean();
        
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
                actual.onError(new MissingBackpressureException("Could not emit value due to lack of requests"));
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
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            BackpressureHelper.add(this, n);
        }
        
        @Override
        public void cancel() {
            if (!cancelled.get() && cancelled.compareAndSet(false, true)) {
                state.remove(this);
            }
        }
    }
}
