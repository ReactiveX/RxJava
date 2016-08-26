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
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * A Subject that multicasts events to Subscribers that are currently subscribed to it.
 * 
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.PublishSubject.png" alt="">
 * 
 * <p>The subject does not coordinate backpressure for its subscribers and implements a weaker onSubscribe which
 * calls requests Long.MAX_VALUE from the incoming Subscriptions. This makes it possible to subscribe the PublishSubject
 * to multiple sources (note on serialization though) unlike the standard contract on Subscriber. Child subscribers, however, are not overflown but receive an
 * IllegalStateException in case their requested amount is zero.
 * 
 * <p>The implementation of onXXX methods are technically thread-safe but non-serialized calls
 * to them may lead to undefined state in the currently subscribed Subscribers.
 * 
 * <p>Due to the nature Flowables are constructed, the PublishProcessor can't be instantiated through
 * {@code new} but must be created via the {@link #create()} method.
 *
 * Example usage:
 * <p>
 * <pre> {@code

  PublishProcessor<Object> processor = PublishProcessor.create();
  // subscriber1 will receive all onNext and onCompleted events
  processor.subscribe(subscriber1);
  processor.onNext("one");
  processor.onNext("two");
  // subscriber2 will only receive "three" and onCompleted
  processor.subscribe(subscriber2);
  processor.onNext("three");
  processor.onComplete();

  } </pre>
 * @param <T> the value type multicast to Subscribers.
 */
public final class PublishProcessor<T> extends FlowableProcessor<T> {
    
    /** Holds the terminal event and manages the array of subscribers. */
    final State<T> state;
    /** 
     * Indicates the subject has been terminated. It is checked in the onXXX methods in
     * a relaxed matter: concurrent calls may not properly see it (which shouldn't happen if
     * the reactive-streams contract is held).
     */
    boolean done;
    
    /**
     * Constructs a PublishProcessor.
     * @param <T> the value type
     * @return the new PublishSubject
     */
    public static <T> PublishProcessor<T> create() {
        return new PublishProcessor<T>();
    }
    
    protected PublishProcessor() {
        this.state = new State<T>();
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        state.subscribe(s);
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
            if (add(ps)) {
                // if cancellation happened while a successful add, the remove() didn't work
                // so we need to do it again
                if (ps.isCancelled()) {
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
                return subscribers.getAndSet(TERMINATED);
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
            for (;;) {
                PublishSubscriber<T>[] a = subscribers.get();
                if (a == TERMINATED) {
                    return false;
                }
                
                int n = a.length;
                PublishSubscriber<T>[] b = apply(n + 1);
                System.arraycopy(a, 0, b, 0, n);
                b[n] = ps;
                
                if (subscribers.compareAndSet(a, b)) {
                    return true;
                }
            }
        }
        
        /**
         * Atomically removes the given subscriber if it is subscribed to the subject.
         * @param ps the subject to remove
         */
        @SuppressWarnings("unchecked")
        void remove(PublishSubscriber<T> ps) {
            for (;;) {
                PublishSubscriber<T>[] a = subscribers.get();
                if (a == TERMINATED || a == EMPTY) {
                    return;
                }
                
                int n = a.length;
                int j = -1;
                for (int i = 0; i < n; i++) {
                    if (a[i] == ps) {
                        j = i;
                        break;
                    }
                }
                
                if (j < 0) {
                    return;
                }
                
                PublishSubscriber<T>[] b;
                
                if (n == 1) {
                    b = EMPTY;
                } else {
                    b = apply(n - 1);
                    System.arraycopy(a, 0, b, 0, j);
                    System.arraycopy(a, j + 1, b, j, n - j - 1);
                }
                if (subscribers.compareAndSet(a, b)) {
                    return;
                }
            }
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
    static final class PublishSubscriber<T> extends AtomicLong implements Subscription {
        /** */
        private static final long serialVersionUID = 3562861878281475070L;
        /** The actual subscriber. */
        final Subscriber<? super T> actual;
        /** The subject state. */
        final State<T> state;
        
        /**
         * Constructs a PublishSubscriber, wraps the actual subscriber and the state.
         * @param actual the actual subscriber
         * @param state the state
         */
        public PublishSubscriber(Subscriber<? super T> actual, State<T> state) {
            this.actual = actual;
            this.state = state;
        }
        
        public void onNext(T t) {
            long r = get();
            if (r == Long.MIN_VALUE) {
                return;
            }
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
        
        public void onError(Throwable t) {
            if (get() != Long.MIN_VALUE) {
                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }
        
        public void onComplete() {
            if (get() != Long.MIN_VALUE) {
                actual.onComplete();
            }
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.addCancel(this, n);
            }
        }
        
        @Override
        public void cancel() {
            if (getAndSet(Long.MIN_VALUE) != Long.MIN_VALUE) {
                state.remove(this);
            }
        }
        
        public boolean isCancelled() {
            return get() == Long.MIN_VALUE;
        }
    }
}
