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

package io.reactivex.internal.subscribers;

import org.reactivestreams.Subscription;

import io.reactivex.FlowableSubscriber;
import io.reactivex.internal.subscriptions.*;

/**
 * Subscriber that communicates with a FullArbiter.
 *
 * @param <T> the value type
 */
public final class FullArbiterSubscriber<T> implements FlowableSubscriber<T> {
    final FullArbiter<T> arbiter;

    Subscription s;

    public FullArbiterSubscriber(FullArbiter<T> arbiter) {
        this.arbiter = arbiter;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validate(this.s, s)) {
            this.s = s;
            arbiter.setSubscription(s);
        }
    }

    @Override
    public void onNext(T t) {
        arbiter.onNext(t, s);
    }

    @Override
    public void onError(Throwable t) {
        arbiter.onError(t, s);
    }

    @Override
    public void onComplete() {
        arbiter.onComplete(s);
    }
}
