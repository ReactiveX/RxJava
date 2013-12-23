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

import rx.Subscription;

/**
 * A subscription that allows only a single subscription to be assigned.
 * <p>
 * If this subscription is live, no other subscription may be set and
 * yields an {@link IllegalStateException}.
 * <p>
 * If the unsubscribe has been called, setting a new subscription will
 * unsubscribe it immediately.
 */
public final class SingleAssignmentSubscription extends AbstractAssignmentSubscription {
    /** Creates an empty SingleAssignmentSubscription. */
    public SingleAssignmentSubscription() {
        super();
    }
    /**
     * Creates a SerialSubscription with the given subscription
     * as its initial value.
     * 
     * @param s the initial subscription
     */
    public SingleAssignmentSubscription(Subscription s) {
        super(s);
    }
    @Override
    protected void onPreSwap(Subscription current) {
        if (current != null) {
            throw new IllegalStateException("Can not set subscription more than once!");
        }
    }
    /**
     * Sets the subscription if not already set.
     * 
     * @param s 
     * @deprecated use the common {@link #setSubscription(rx.Subscription)} method instead.
     */
    public void set(Subscription s) {
        setSubscription(s);
    }
}
