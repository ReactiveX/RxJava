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
 * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Observable">RxJava Wiki: Observable</a>
 * @param <T>
 *          the type of items the Subscriber expects to observe
 */
public abstract class Subscriber<T> implements Observer<T>, Subscription {

    private final SubscriptionList cs;
    private final Subscriber<?> op;
    /* protected by `this` */
    private Producer p;
    /* protected by `this` */
    private long requested = Long.MIN_VALUE; // default to not set

    protected Subscriber() {
        this.op = null;
        this.cs = new SubscriptionList();
    }

    protected Subscriber(Subscriber<?> op) {
        this.op = op;
        this.cs = op.cs;
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
        cs.add(s);
    }

    @Override
    public final void unsubscribe() {
        cs.unsubscribe();
    }

    /**
     * Indicates whether this Subscriber has unsubscribed from its list of subscriptions.
     * 
     * @return {@code true} if this Subscriber has unsubscribed from its subscriptions, {@code false} otherwise
     */
    @Override
    public final boolean isUnsubscribed() {
        return cs.isUnsubscribed();
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
     *
     * @param n the maximum number of items you want the Observable to emit to the Subscriber at this time, or
     *           {@code Long.MAX_VALUE} if you want the Observable to emit items at its own pace
     */
    protected final void request(long n) {
        Producer shouldRequest = null;
        synchronized (this) {
            if (p != null) {
                shouldRequest = p;
            } else {
                requested = n;
            }
        }
        // after releasing lock
        if (shouldRequest != null) {
            shouldRequest.request(n);
        }
    }

    /**
     * @warn javadoc description missing
     * @warn param producer not described
     * @param producer
     */
    public void setProducer(Producer producer) {
        long toRequest;
        boolean setProducer = false;
        synchronized (this) {
            toRequest = requested;
            p = producer;
            if (op != null) {
                // middle operator ... we pass thru unless a request has been made
                if (toRequest == Long.MIN_VALUE) {
                    // we pass-thru to the next producer as nothing has been requested
                    setProducer = true;
                }

            }
        }
        // do after releasing lock
        if (setProducer) {
            op.setProducer(p);
        } else {
            // we execute the request with whatever has been requested (or Long.MAX_VALUE)
            if (toRequest == Long.MIN_VALUE) {
                p.request(Long.MAX_VALUE);
            } else {
                p.request(toRequest);
            }
        }
    }
}
