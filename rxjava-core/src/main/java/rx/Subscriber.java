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
import rx.subscriptions.CompositeSubscription;

/**
 * Provides a mechanism for receiving push-based notifications from Observables, and permits manual
 * unsubscribing from these Observables.
 * <p>
 * After a Subscriber calls an {@link Observable}'s {@link Observable#subscribe subscribe} method, the
 * {@link Observable} calls the Subscriber's {@link #onNext} method to emit items. A well-behaved
 * {@link Observable} will call a Subscriber's {@link #onCompleted} method exactly once or the Subscriber's
 * {@link #onError} method exactly once.
 * 
 * @see <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki: Observable</a>
 * @param <T>
 *          the type of items the Subscriber expects to observe
 */
public abstract class Subscriber<T> implements Observer<T>, Subscription {

    private final SubscriptionList cs;
    private final Subscriber<?> op;
    /* protected by `this` */
    private volatile Producer p;
    /* protected by `this` */
    private volatile int requested = -1; // default to infinite

    @Deprecated
    protected Subscriber(CompositeSubscription cs) {
        this.op = null;
        this.cs = new SubscriptionList();
        add(cs);
    }

    protected Subscriber() {
        this.op = null;
        this.cs = new SubscriptionList();
    }
    
    protected Subscriber(int bufferRequest) {
        this.op = null;
        this.cs = new SubscriptionList();
        request(bufferRequest);
    }

    protected Subscriber(Subscriber<?> op) {
        this.op = op;
        this.cs = op.cs;
    }
    
    protected Subscriber(Subscriber<?> op, int bufferRequest) {
        this.op = op;
        this.cs = op.cs;
        request(bufferRequest);
    }

    /**
     * Adds a {@link Subscription} to this Subscriber's list of subscriptions if this list is not marked as
     * unsubscribed. If the list <em>is</em> marked as unsubscribed, {@code add} will indicate this by
     * explicitly unsubscribing the new {@code Subscription} as well.
     *
     * @param s the {@code Subscription} to add
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
    public final boolean isUnsubscribed() {
        return cs.isUnsubscribed();
    }

    public final void request(int n) {
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

    protected Producer onSetProducer(Producer producer) {
        return producer;
    }

    public final void setProducer(Producer producer) {
        producer = onSetProducer(producer);
        int toRequest = requested;
        boolean setProducer = false;
        synchronized (this) {
            p = producer;
            if (op != null) {
                // middle operator ... we pass thru unless a request has been made
                if (requested >= 0) {
                    // if we have a positive value we will run as that means this operator has expressed interest
                    toRequest = requested;
                } else {
                    // we pass-thru to the next producer as it has not been set
                    setProducer = true;
                }

            }
        }
        // do after releasing lock
        if (setProducer) {
            op.setProducer(p);
        } else {
            // we execute the request with whatever has been requested (or -1)
            p.request(toRequest);
        }
    }
}
