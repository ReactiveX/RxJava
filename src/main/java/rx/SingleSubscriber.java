/**
 * Copyright 2015 Netflix, Inc.
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

import rx.annotations.Beta;
import rx.internal.util.SubscriptionList;

/**
 * Provides a mechanism for receiving push-based notifications.
 * <p>
 * After a SingleSubscriber calls a {@link Single}'s {@link Single#subscribe subscribe} method, the
 * {@code Single} calls the SingleSubscriber's {@link #onSuccess} and {@link #onError} methods to provide
 * notifications. A well-behaved {@code Single} will call a SingleSubscriber's {@link #onSuccess} method exactly
 * once or the SingleSubscriber's {@link #onError} method exactly once.
 * 
 * @see <a href="http://reactivex.io/documentation/observable.html">ReactiveX documentation: Observable</a>
 * @param <T>
 *          the type of item the SingleSubscriber expects to observe
 * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
 */
@Beta
public abstract class SingleSubscriber<T> implements Subscription {

    private final SubscriptionList cs = new SubscriptionList();
    
    /**
     * Notifies the SingleSubscriber with a single item and that the {@link Single} has finished sending
     * push-based notifications.
     * <p>
     * The {@link Single} will not call this method if it calls {@link #onError}.
     * 
     * @param value
     *          the item emitted by the Single
     */
    public abstract void onSuccess(T value);

    /**
     * Notifies the SingleSubscriber that the {@link Single} has experienced an error condition.
     * <p>
     * If the {@link Single} calls this method, it will not thereafter call {@link #onSuccess}.
     * 
     * @param error
     *          the exception encountered by the Single
     */
    public abstract void onError(Throwable error);
    
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
}
