/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.subscriptions;

import java.util.concurrent.atomic.AtomicReference;

import rx.Subscription;

/**
 * Base class to manage a reference to another subscription in atomic manner
 * and allows callbacks to handle the referenced subscription at the
 * pre-swap and post-swap stages.
 * 
 */
public abstract class AbstractAssignmentSubscription implements Subscription {
    /** The subscription holding the reference. */
    protected final AtomicReference<Subscription> reference = new AtomicReference<Subscription>();
    /** Sentinel for the unsubscribed state. */
    private static final Subscription UNSUBSCRIBED_SENTINEL = new Subscription() {
        @Override
        public void unsubscribe() {
        }
    };
    /** Creates an empty AbstractAssignmentSubscription. */
    public AbstractAssignmentSubscription() {
        
    }
    /**
     * Creates a AbstractAssignmentSubscription with the given subscription
     * as its initial value.
     * 
     * @param s the initial subscription
     */
    public AbstractAssignmentSubscription(Subscription s) {
        this();
        reference.set(s);
    }
    public boolean isUnsubscribed() {
        return reference.get() == UNSUBSCRIBED_SENTINEL;
    }

    @Override
    public void unsubscribe() {
        Subscription s = reference.getAndSet(UNSUBSCRIBED_SENTINEL);
        if (s != null) {
            s.unsubscribe();
        }
    }

    public void setSubscription(Subscription s) {
        do {
            Subscription r = reference.get();
            if (r == UNSUBSCRIBED_SENTINEL) {
                s.unsubscribe();
                return;
            }
            onPreSwap(r);
            if (reference.compareAndSet(r, s)) {
                onPostSwap(r);
                break;
            }
        } while (true);
    }
    /**
     * Override this method to perform logic on a subscription before
     * an attempt is tried to swap it for a new subscription.
     * @param current the current subscription value
     */
    protected void onPreSwap(Subscription current) { }
    /**
     * Override this method to perform actions once a subscription has been
     * swapped to a new one.
     * @param old the old subscription value
     */
    protected void onPostSwap(Subscription old) { }
    
    public Subscription getSubscription() {
        Subscription s = reference.get();
        return s != UNSUBSCRIBED_SENTINEL ? s : Subscriptions.empty();
    }

}
