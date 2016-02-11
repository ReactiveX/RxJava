/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.operators;

import java.util.Queue;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.exceptions.MissingBackpressureException;
import rx.internal.util.atomic.SpscAtomicArrayQueue;
import rx.internal.util.unsafe.*;

/**
 * Multicasts notifications coming through its input Subscriber view to its
 * client Subscribers via lockstep backpressure mode.
 * 
 * <p>The difference between this class and OperatorPublish is that this
 * class doesn't consume the upstream if there are no child subscribers but
 * waits for them to show up. Plus if the upstream source terminates, late
 * subscribers will be immediately terminated with the same terminal event
 * unlike OperatorPublish which just waits for the next connection.
 * 
 * <p>The class extends AtomicInteger which is the work-in-progress gate
 * for the drain-loop serializing subscriptions and child request changes.
 * 
 * @param <T> the input and output type
 */
public final class OnSubscribePublishMulticast<T> extends AtomicInteger 
implements Observable.OnSubscribe<T>, Observer<T>, Subscription {
    /** */
    private static final long serialVersionUID = -3741892510772238743L;
    /** 
     * The prefetch queue holding onto a fixed amount of items until all
     * all child subscribers have requested something.
     */
    final Queue<T> queue;
    /**
     * The number of items to prefetch from the upstreams source.
     */
    final int prefetch;
    
    /**
     * Delays the error delivery to happen only after all values have been consumed.
     */
    final boolean delayError;
    /**
     * The subscriber that can be 'connected' to the upstream source.
     */
    final ParentSubscriber<T> parent;
    /** Indicates the upstream has completed. */
    volatile boolean done;
    /** 
     * Holds onto the upstream's exception if done is true and this field is non-null.
     * <p>This field must be read after done or if subscribers == TERMINATED to
     * establish a proper happens-before. 
     */
    Throwable error;

    /**
     * Holds the upstream producer if any, set through the parent subscriber.
     */
    volatile Producer producer;
    /**
     * A copy-on-write array of currently subscribed child subscribers' wrapper structure.
     */
    volatile PublishProducer<T>[] subscribers;
    
    /** 
     * Represents an empty array of subscriber wrapper, 
     * helps avoid allocating an empty array all the time. 
     */
    static final PublishProducer<?>[] EMPTY = new PublishProducer[0];

    /**
     * Represents a final state for this class that prevents new subscribers
     * from subscribing to it.
     */
    static final PublishProducer<?>[] TERMINATED = new PublishProducer[0];

    /**
     * Constructor, initializes the fields
     * @param prefetch the prefetch amount, &gt; 0 required
     * @param delayError delay the error delivery after the normal items?
     * @throws IllegalArgumentException if prefetch &lt;= 0
     */
    @SuppressWarnings("unchecked")
    public OnSubscribePublishMulticast(int prefetch, boolean delayError) {
        if (prefetch <= 0) {
            throw new IllegalArgumentException("prefetch > 0 required but it was " + prefetch);
        }
        this.prefetch = prefetch;
        this.delayError = delayError;
        if (UnsafeAccess.isUnsafeAvailable()) {
            this.queue = new SpscArrayQueue<T>(prefetch);
        } else {
            this.queue = new SpscAtomicArrayQueue<T>(prefetch);
        }
        this.subscribers = (PublishProducer<T>[]) EMPTY;
        this.parent = new ParentSubscriber<T>(this);
    }
    
    @Override
    public void call(Subscriber<? super T> t) {
        PublishProducer<T> pp = new PublishProducer<T>(t, this);
        t.add(pp);
        t.setProducer(pp);
        
        if (add(pp)) {
            if (pp.isUnsubscribed()) {
                remove(pp);
            } else {
                drain();
            }
        } else {
            Throwable e = error;
            if (e != null) {
                t.onError(e);
            } else {
                t.onCompleted();
            }
        }
    }
    
    @Override
    public void onNext(T t) {
        if (!queue.offer(t)) {
            parent.unsubscribe();
            
            error = new MissingBackpressureException("Queue full?!");
            done = true;
        }
        drain();
    }
    
    @Override
    public void onError(Throwable e) {
        error = e;
        done = true;
        drain();
    }
    
    @Override
    public void onCompleted() {
        done = true;
        drain();
    }
    
    /**
     * Sets the main producer and issues the prefetch amount.
     * @param p the producer to set
     */
    void setProducer(Producer p) {
        this.producer = p;
        p.request(prefetch);
    }
    
    /**
     * The serialization loop that determines the minimum request of
     * all subscribers and tries to emit as many items from the queue if
     * they are available.
     * 
     * <p>The execution of the drain-loop is guaranteed to be thread-safe.
     */
    void drain() {
        if (getAndIncrement() != 0) {
            return;
        }
        
        final Queue<T> q = queue;
        
        int missed = 0;
        
        for (;;) {
            
            long r = Long.MAX_VALUE;
            PublishProducer<T>[] a = subscribers;
            int n = a.length;
            
            for (PublishProducer<T> inner : a) {
                r = Math.min(r, inner.get());
            }
            
            if (n != 0) {
                long e = 0L;
                
                while (e != r) {
                    boolean d = done;
                    
                    T v = q.poll();
                    
                    boolean empty = v == null;
                    
                    if (checkTerminated(d, empty)) {
                        return;
                    }
                    
                    if (empty) {
                        break;
                    }
                    
                    for (PublishProducer<T> inner : a) {
                        inner.actual.onNext(v);
                    }
                    
                    e++;
                }
                
                if (e == r) {
                    if (checkTerminated(done, q.isEmpty())) {
                        return;
                    }
                }
                
                if (e != 0L) {
                    Producer p = producer;
                    if (p != null) {
                        p.request(e);
                    }
                    for (PublishProducer<T> inner : a) {
                        BackpressureUtils.produced(inner, e);
                    }
                    
                }
            }
            
            missed = addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }
    }
    
    /**
     * Given the current source state, terminates all child subscribers.
     * @param d the source-done indicator
     * @param empty the queue-emptiness indicator
     * @return true if the class reached its terminal state
     */
    boolean checkTerminated(boolean d, boolean empty) {
        if (d) {
            if (delayError) {
                if (empty) {
                    PublishProducer<T>[] a = terminate();
                    Throwable ex = error;
                    if (ex != null) {
                        for (PublishProducer<T> inner : a) {
                            inner.actual.onError(ex);
                        }
                    } else {
                        for (PublishProducer<T> inner : a) {
                            inner.actual.onCompleted();
                        }
                    }
                    return true;
                }
            } else {
                Throwable ex = error;
                if (ex != null) {
                    queue.clear();
                    PublishProducer<T>[] a = terminate();
                    for (PublishProducer<T> inner : a) {
                        inner.actual.onError(ex);
                    }
                    return true;
                } else
                if (empty) {
                    PublishProducer<T>[] a = terminate();
                    for (PublishProducer<T> inner : a) {
                        inner.actual.onCompleted();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Atomically swaps in the terminated state.
     * @return the last set of subscribers before the state change or an empty array
     */
    @SuppressWarnings("unchecked")
    PublishProducer<T>[] terminate() {
        PublishProducer<T>[] a = subscribers;
        if (a != TERMINATED) {
            synchronized (this) {
                a = subscribers;
                if (a != TERMINATED) {
                    subscribers = (PublishProducer<T>[]) TERMINATED;
                }
            }
        }
        return a;
    }
    
    /**
     * Atomically adds the given wrapper of a child Subscriber to the subscribers array.
     * @param inner the wrapper
     * @return true if successful, false if the terminal state has been reached in the meantime
     */
    boolean add(PublishProducer<T> inner) {
        PublishProducer<T>[] a = subscribers;
        if (a == TERMINATED) {
            return false;
        }
        synchronized (this) {
            a = subscribers;
            if (a == TERMINATED) {
                return false;
            }
            
            int n = a.length;
            @SuppressWarnings("unchecked")
            PublishProducer<T>[] b = new PublishProducer[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = inner;
            subscribers = b;
            return true;
        }
    }

    /**
     * Atomically removes the given wrapper, if present, from the subscribers array.
     * @param inner the wrapper to remove
     */
    @SuppressWarnings("unchecked")
    void remove(PublishProducer<T> inner) {
        PublishProducer<T>[] a = subscribers;
        if (a == TERMINATED || a == EMPTY) {
            return;
        }
        synchronized (this) {
            a = subscribers;
            if (a == TERMINATED || a == EMPTY) {
                return;
            }
            
            int j = -1;
            int n = a.length;
            
            for (int i = 0; i < n; i++) {
                if (a[i] == inner) {
                    j = i;
                    break;
                }
            }
            
            if (j < 0) {
                return;
            }
            
            PublishProducer<T>[] b;
            if (n == 1) {
                b = (PublishProducer<T>[])EMPTY;
            } else {
                b = new PublishProducer[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            subscribers = b;
        }
    }

    /**
     * The subscriber that must be used for subscribing to the upstream source.
     * @param <T> the input value type;
     */
    static final class ParentSubscriber<T> extends Subscriber<T> {
        /** The reference to the parent state where the events are forwarded to. */
        final OnSubscribePublishMulticast<T> state;
        
        public ParentSubscriber(OnSubscribePublishMulticast<T> state) {
            super();
            this.state = state;
        }
        
        @Override
        public void onNext(T t) {
            state.onNext(t);
        }
        
        @Override
        public void onError(Throwable e) {
            state.onError(e);
        }
        
        @Override
        public void onCompleted() {
            state.onCompleted();
        }
        
        @Override
        public void setProducer(Producer p) {
            state.setProducer(p);
        }
    }
    
    /**
     * Returns the input subscriber of this class that must be subscribed
     * to the upstream source.
     * @return the subscriber instance
     */
    public Subscriber<T> subscriber() {
        return parent;
    }
    
    @Override
    public void unsubscribe() {
        parent.unsubscribe();
    }
    
    @Override
    public boolean isUnsubscribed() {
        return parent.isUnsubscribed();
    }
    
    /**
     * A Producer and Subscription that wraps a child Subscriber and manages
     * its backpressure requests along with its unsubscription from the parent
     * class.
     * 
     * <p>The class extends AtomicLong that holds onto the child's requested amount.
     * 
     * @param <T> the output value type
     */
    static final class PublishProducer<T> 
    extends AtomicLong
    implements Producer, Subscription {
        /** */
        private static final long serialVersionUID = 960704844171597367L;

        /** The actual subscriber to receive the events. */
        final Subscriber<? super T> actual;
        
        /** The parent object to request draining or removal. */
        final OnSubscribePublishMulticast<T> parent;
        
        /** Makes sure unsubscription happens only once. */
        final AtomicBoolean once;

        public PublishProducer(Subscriber<? super T> actual, OnSubscribePublishMulticast<T> parent) {
            this.actual = actual;
            this.parent = parent;
            this.once = new AtomicBoolean();
        }
        
        @Override
        public void request(long n) {
            if (n < 0) {
                throw new IllegalArgumentException("n >= 0 required but it was " + n);
            } else
            if (n != 0) {
                BackpressureUtils.getAndAddRequest(this, n);
                parent.drain();
            }
        }
        
        @Override
        public boolean isUnsubscribed() {
            return once.get();
        }
        
        @Override
        public void unsubscribe() {
            if (once.compareAndSet(false, true)) {
                parent.remove(this);
            }
        }
    }
}
