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
 * A subscription that allows only a single resource to be assigned.
 * <p>
 * If this subscription is live, no other subscription may be set() and
 * yields an {@link IllegalStateException}.
 * <p>
 * If the unsubscribe has been called, setting a new subscription will
 * unsubscribe it immediately.
 */
public final class SingleAssignmentSubscription implements Subscription {
    /** Holds the current resource. */
    private final AtomicReference<Subscription> current = new AtomicReference<Subscription>();
    /** Sentinel for the unsubscribed state. */
    private static final Subscription SENTINEL = new Subscription() {
        @Override
        public void unsubscribe() {
        }
    };
    /**
     * Returns the current subscription or null if not yet set.
     */
    public Subscription get() {
        Subscription s = current.get();
        if (s == SENTINEL) {
            return Subscriptions.empty();
        }
        return s;
    }
    /**
     * Sets a new subscription if not already set.
     * @param s the new subscription
     * @throws IllegalStateException if this subscription is live and contains
     * another subscription.
     */
    public void set(Subscription s) {
        if (current.compareAndSet(null, s)) {
            return;
        }
        if (current.get() != SENTINEL) {
            throw new IllegalStateException("Subscription already set");
        }
        if (s != null) {
            s.unsubscribe();
        }
    }
    @Override
    public void unsubscribe() {
        Subscription old = current.getAndSet(SENTINEL);
        if (old != null) {
            old.unsubscribe();
        }
    }
    /**
     * Test if this subscription is already unsubscribed.
     */
    public boolean isUnsubscribed() {
        return current.get() == SENTINEL;
    }
    
}
