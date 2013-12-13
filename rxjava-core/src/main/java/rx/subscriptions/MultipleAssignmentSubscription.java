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

import rx.Observable;
import rx.Subscription;

/**
 * Subscription that can be checked for status such as in a loop inside an {@link Observable} to exit the loop if unsubscribed.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.multipleassignmentdisposable">Rx.Net equivalent MultipleAssignmentDisposable</a>
 */
public class MultipleAssignmentSubscription implements Subscription {
    /** The subscription holding the reference. */
    protected final AtomicReference<Subscription> reference = new AtomicReference<Subscription>();
    /** Sentinel for the unsubscribed state. */
    private static final Subscription UNSUBSCRIBED_SENTINEL = new Subscription() {
        @Override
        public void unsubscribe() {
        }
    };
    /** Creates an empty MultipleAssignmentSubscription. */
    public MultipleAssignmentSubscription() {
        
    }
    /**
     * Creates a MultipleAssignmentSubscription with the given subscription
     * as its initial value.
     * 
     * @param s the initial subscription
     */
    public MultipleAssignmentSubscription(Subscription s) {
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
            onCurrentSubscription(r);
            if (reference.compareAndSet(r, s)) {
                onSubscriptionSwapped(r);
                break;
            }
        } while (true);
    }
    /**
     * Override this method to perform logic on a subscription before
     * an attempt is tried to swap it for a new subscription.
     * @param current the current subscription value
     */
    protected void onCurrentSubscription(Subscription current) { }
    /**
     * Override this method to perform actions once a subscription has been
     * swapped to a new one.
     * @param old the old subscription value
     */
    protected void onSubscriptionSwapped(Subscription old) { }
    
    public Subscription getSubscription() {
        Subscription s = reference.get();
        return s != UNSUBSCRIBED_SENTINEL ? s : Subscriptions.empty();
    }

}
