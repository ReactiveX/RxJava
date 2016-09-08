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
package rx.subscriptions;

import rx.Subscription;
import rx.internal.subscriptions.SequentialSubscription;

/**
 * Represents a subscription whose underlying subscription can be swapped for another subscription which causes
 * the previous underlying subscription to be unsubscribed.
 */
public final class SerialSubscription implements Subscription {

    final SequentialSubscription state = new SequentialSubscription();

    @Override
    public boolean isUnsubscribed() {
        return state.isUnsubscribed();
    }

    @Override
    public void unsubscribe() {
        state.unsubscribe();
    }

    /**
     * Sets the underlying subscription. If the {@code MultipleAssignmentSubscription} is already unsubscribed,
     * setting a new subscription causes the new subscription to also be immediately unsubscribed.
     *
     * @param s the {@link Subscription} to set
     * @throws IllegalArgumentException if {@code s} is {@code null}
     */
    public void set(Subscription s) {
        if (s == null) {
            throw new IllegalArgumentException("Subscription can not be null");
        }
        state.update(s);
    }

    /**
     * Gets the underlying subscription.
     *
     * @return the {@link Subscription} that underlies the {@code MultipleAssignmentSubscription}
     */
    public Subscription get() {
        return state.current();
    }

}
