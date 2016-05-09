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
package rx;

import rx.internal.util.SubscriptionList;

/**
 * Provides a mechanism for receiving push-based notifications from Observables, and permits manual
 * unsubscribing from these Observables.
 * <p>
 * After a Subscriber calls an {@link Observable}'s {@link Observable#subscribe subscribe} method, the
 * {@link Observable} calls the Subscriber's {@link #onNext} method to emit items. A well-behaved
 * {@link Observable} will call a Subscriber's {@link #onCompleted} method exactly once or the Subscriber's
 * {@link #onError} method exactly once.
 * 
 * @see <a href="http://reactivex.io/documentation/observable.html">ReactiveX documentation: Observable</a>
 * @param <T>
 *          the type of items the Subscriber expects to observe
 */
public abstract class Subscriber<T> implements Observer<T>, Subscription {
    
    // represents requested not set yet
    private static final long NOT_SET = Long.MIN_VALUE;

    private final SubscriptionList subscriptions;
    private final Subscriber<?> subscriber;
    /* protected by `this` */
    private Producer producer;
    /* protected by `this` */
    private long requested = NOT_SET; // default to not set

    protected Subscriber() {
        this(null, false);
    }

    /**
     * Construct a Subscriber by using another Subscriber for backpressure and
     * for holding the subscription list (when <code>this.add(sub)</code> is
     * called this will in fact call <code>subscriber.add(sub)</code>).
     * 
     * @param subscriber
     *            the other Subscriber
     */
    protected Subscriber(Subscriber<?> subscriber) {
        this(subscriber, true);
    }

    /**
     * Construct a Subscriber by using another Subscriber for backpressure and
     * optionally for holding the subscription list (if
     * <code>shareSubscriptions</code> is <code>true</code> then when
     * <code>this.add(sub)</code> is called this will in fact call
     * <code>subscriber.add(sub)</code>).
     * <p>
     * To retain the chaining of subscribers when setting
     * <code>shareSubscriptions</code> to <code>false</code>, add the created
     * instance to {@code subscriber} via {@link #add}.
     * 
     * @param subscriber
     *            the other Subscriber
     * @param shareSubscriptions
     *            {@code true} to share the subscription list in {@code subscriber} with
     *            this instance
     * @since 1.0.6
     */
    protected Subscriber(Subscriber<?> subscriber, boolean shareSubscriptions) {
        this.subscriber = subscriber;
        this.subscriptions = shareSubscriptions && subscriber != null ? subscriber.subscriptions : new SubscriptionList();
    }

    /**
     * Adds a {@link Subscription} to this Subscriber's list of subscriptions if this list is not marked as
     * unsubscribed. If the list <em>is</em> marked as unsubscribed, {@code add} will indicate this by
     * explicitly unsubscribing the new {@code Subscription} as well.
     *
     * @param s
     *            the {@code Subscription} to add
     */
    public final void add(Subscription s) {
        subscriptions.add(s);
    }

    @Override
    public final void unsubscribe() {
        subscriptions.unsubscribe();
    }

    /**
     * Indicates whether this Subscriber has unsubscribed from its list of subscriptions.
     * 
     * @return {@code true} if this Subscriber has unsubscribed from its subscriptions, {@code false} otherwise
     */
    @Override
    public final boolean isUnsubscribed() {
        return subscriptions.isUnsubscribed();
    }

    /**
     * This method is invoked when the Subscriber and Observable have been connected but the Observable has
     * not yet begun to emit items or send notifications to the Subscriber. Override this method to add any
     * useful initialization to your subscription, for instance to initiate backpressure.
     */
    public void onStart() {
        // do nothing by default
    }
    
    /**
     * Request a certain maximum number of emitted items from the Observable this Subscriber is subscribed to.
     * This is a way of requesting backpressure. To disable backpressure, pass {@code Long.MAX_VALUE} to this
     * method.
     * <p>
     * Requests are additive but if a sequence of requests totals more than {@code Long.MAX_VALUE} then 
     * {@code Long.MAX_VALUE} requests will be actioned and the extras <i>may</i> be ignored. Arriving at 
     * {@code Long.MAX_VALUE} by addition of requests cannot be assumed to disable backpressure. For example, 
     * the code below may result in {@code Long.MAX_VALUE} requests being actioned only.
     * 
     * <pre>
     * request(100);
     * request(Long.MAX_VALUE-1);
     * </pre>
     * 
     * @param n the maximum number of items you want the Observable to emit to the Subscriber at this time, or
     *           {@code Long.MAX_VALUE} if you want the Observable to emit items at its own pace
     * @throws IllegalArgumentException
     *             if {@code n} is negative
     */
    protected final void request(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("number requested cannot be negative: " + n);
        } 
        
        // if producer is set then we will request from it
        // otherwise we increase the requested count by n
        Producer producerToRequestFrom = null;
        synchronized (this) {
            if (producer != null) {
                producerToRequestFrom = producer;
            } else {
                addToRequested(n);
                return;
            }
        }
        // after releasing lock (we should not make requests holding a lock)
        producerToRequestFrom.request(n);
    }

    private void addToRequested(long n) {
        if (requested == NOT_SET) {
            requested = n;
        } else { 
            final long total = requested + n;
            // check if overflow occurred
            if (total < 0) {
                requested = Long.MAX_VALUE;
            } else {
                requested = total;
            }
        }
    }
    
    /**
     * If other subscriber is set (by calling constructor
     * {@link #Subscriber(Subscriber)} or
     * {@link #Subscriber(Subscriber, boolean)}) then this method calls
     * <code>setProducer</code> on the other subscriber. If the other subscriber
     * is not set and no requests have been made to this subscriber then
     * <code>p.request(Long.MAX_VALUE)</code> is called. If the other subscriber
     * is not set and some requests have been made to this subscriber then
     * <code>p.request(n)</code> is called where n is the accumulated requests
     * to this subscriber.
     * 
     * @param p
     *            producer to be used by this subscriber or the other subscriber
     *            (or recursively its other subscriber) to make requests from
     */
    public void setProducer(Producer p) {
        long toRequest;
        boolean passToSubscriber = false;
        synchronized (this) {
            toRequest = requested;
            producer = p;
            if (subscriber != null) {
                // middle operator ... we pass through unless a request has been made
                if (toRequest == NOT_SET) {
                    // we pass through to the next producer as nothing has been requested
                    passToSubscriber = true;
                }
            }
        }
        // do after releasing lock
        if (passToSubscriber) {
            subscriber.setProducer(producer);
        } else {
            // we execute the request with whatever has been requested (or Long.MAX_VALUE)
            if (toRequest == NOT_SET) {
                producer.request(Long.MAX_VALUE);
            } else {
                producer.request(toRequest);
            }
        }
    }
}
