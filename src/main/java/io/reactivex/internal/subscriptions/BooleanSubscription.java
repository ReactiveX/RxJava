/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.reactivex.internal.subscriptions;

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Subscription;

/**
 * Subscription implementation that ignores request but remembers the cancellation
 * which can be checked via isCancelled.
 */
public final class BooleanSubscription extends AtomicBoolean implements Subscription {

    private static final long serialVersionUID = -8127758972444290902L;

    @Override
    public void request(long n) {
        SubscriptionHelper.validate(n);
    }

    @Override
    public void cancel() {
        lazySet(true);
    }

    /**
     * Returns true if this BooleanSubscription has been cancelled.
     * @return true if this BooleanSubscription has been cancelled
     */
    public boolean isCancelled() {
        return get();
    }

    @Override
    public String toString() {
        return "BooleanSubscription(cancelled=" + get() + ")";
    }
}
