 /**
  * Copyright 2013 Netflix, Inc.
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
package rx.subscriptions;

import java.util.concurrent.atomic.AtomicReference;
import rx.Subscription;

/**
 * A subscription that holds another subscription and
 * allows swapping it in compare-and-swap style and does
 * not unsubscribe any replaced values by default.
 * <p>
 * Overloads are provided to perform the unsubscription on
 * the old value if required.
 */
public class ForwardSubscription implements Subscription {
    /** The atomic reference. */
    final AtomicReference<Subscription> reference = new AtomicReference<Subscription>();
    /** The unsubscription sentinel. */
    private static final Subscription UNSUBSCRIBE_SENTINEL = new Subscription() {
        @Override
        public void unsubscribe() {
        }
    };
    /**
     * Creates an empty ForwardSubscription.
     */
    public ForwardSubscription() {
        
    }
    /**
     * Creates a ForwardSubscription with the initial subscription.
     * @param initial the initial subscription
     */
    public ForwardSubscription(Subscription initial) {
        reference.set(initial);
    }
    /**
     * Returns true if this subscription has been unsubscribed.
     * @return true if this subscription has been unsubscribed 
     */
    public boolean isUnsubscribed() {
        return reference.get() == UNSUBSCRIBE_SENTINEL;
    }
    /**
     * Returns the current maintained subscription.
     * @return the current maintained subscription
     */
    public Subscription getSubscription() {
        Subscription s = reference.get();
        if (s == UNSUBSCRIBE_SENTINEL) {
            return Subscriptions.empty();
        }
        return s;
    }
    /**
     * Atomically replace the current subscription but
     * don't unsubscribe the old value.
     * @param newValue the new subscription to set
     */
    public void setSubscription(Subscription newValue) {
        setSubscription(newValue, false);
    }
    /**
     * Atomically replace the current subscription and
     * unsubscribe the old value id required.
     * @param newValue the new subscription to set
     */
    public void setSubscription(Subscription newValue, boolean unsubscribeOld) {
        Subscription s = replace(newValue);
        if (unsubscribeOld && s != null) {
            s.unsubscribe();
        }
    }
    /**
     * Atomically replace a new subscription and return the old one.
     * <p>
     * If this subscription is unsubscribed, the newValue subscription
     * is unsubscribed and an empty subscription is returned.
     * @param newValue the new subscription
     * @return the old subscription or empty if this ForwardSubscription is unsubscribed
     */
    public Subscription replace(Subscription newValue) {
        do {
            Subscription old = reference.get();
            if (old == UNSUBSCRIBE_SENTINEL) {
                if (newValue != null) {
                    newValue.unsubscribe();
                }
                return Subscriptions.empty();
            }
            if (reference.compareAndSet(old, newValue)) {
                return old;
            }
        } while (true);
    }
    /**
     * Atomically change the subscription only if it is the expected value
     * but don't unsubscribe the old value.
     * If this subscription is unsubscribed, the newValue is immediately
     * unsubscribed.
     * @param expected the expected subscription
     * @param newValue the new subscription
     * @return true if successfully replaced, false if this
     * subscription is unsubscribed or it didn't contain 
     * the expected subscription.
     */
    public boolean compareExchange(Subscription expected, Subscription newValue) {
        return compareExchange(expected, newValue, false);
    }
    /**
     * Atomically change the subscription only if it is the expected value
     * and unsubscribe the old one if required.
     * @param expected the expected subscription
     * @param newValue the new subscription
     * @param unsubscribeOld indicates to unsubscribe the old subscription if the exchange succeeded.
     * @return true if successfully replaced, false if this
     * subscription is unsubscribed or it didn't contain 
     * the expected subscription.
     */
    public boolean compareExchange(Subscription expected, Subscription newValue, boolean unsubscribeOld) {
        do {
            Subscription old = reference.get();
            if (old == UNSUBSCRIBE_SENTINEL) {
                if (newValue != null) {
                    newValue.unsubscribe();
                }
                return false;
            }
            if (old != expected) {
                return false;
            }
            if (reference.compareAndSet(old, newValue)) {
                if (unsubscribeOld && old != null) {
                    old.unsubscribe();
                }
                return true;
            }
        } while (true);
    }
    @Override
    public void unsubscribe() {
        Subscription s = reference.getAndSet(UNSUBSCRIBE_SENTINEL);
        if (s != null) {
            s.unsubscribe();
        }
    }
    
}