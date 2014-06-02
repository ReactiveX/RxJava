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

import rx.subscriptions.SubscriptionList;
import rx.subscriptions.CompositeSubscription;

/**
 * Provides a mechanism for receiving push-based notifications.
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

    protected Subscriber(SubscriptionList cs) {
        if (cs == null) {
            throw new IllegalArgumentException("The CompositeSubscription can not be null");
        }
        this.cs = cs;
    }
    
    @Deprecated
    protected Subscriber(CompositeSubscription cs) {
        this(new SubscriptionList());
        add(cs);
    }

    protected Subscriber() {
        this(new SubscriptionList());
    }

    protected Subscriber(Subscriber<?> op) {
        this(op.cs);
    }

    /**
     * Registers an unsubscribe callback.
     *
     * @warn param "s" undescribed
     */
    public final void add(Subscription s) {
        cs.add(s);
    }

    @Override
    public final void unsubscribe() {
        cs.unsubscribe();
    }

    /**
     * Indicates whether this Subscriber has unsubscribed from its Observable.
     * 
     * @return {@code true} if this Subscriber has unsubscribed from its Observable, {@code false} otherwise
     */
    public final boolean isUnsubscribed() {
        return cs.isUnsubscribed();
    }
}
