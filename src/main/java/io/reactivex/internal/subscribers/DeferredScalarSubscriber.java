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

import org.reactivestreams.*;

import io.reactivex.FlowableSubscriber;
import io.reactivex.internal.subscriptions.*;

/**
 * A subscriber, extending a DeferredScalarSubscription,
 *  that is unbounded-in and can generate 0 or 1 resulting value.
 * @param <T> the input value type
 * @param <R> the output value type
 */
public abstract class DeferredScalarSubscriber<T, R> extends DeferredScalarSubscription<R>
implements FlowableSubscriber<T> {

    private static final long serialVersionUID = 2984505488220891551L;

    /** The upstream subscription. */
    protected Subscription s;

    /** Can indicate if there was at least on onNext call. */
    protected boolean hasValue;

    /**
     * Creates a DeferredScalarSubscriber instance and wraps a downstream Subscriber.
     * @param actual the downstream subscriber, not null (not verified)
     */
    public DeferredScalarSubscriber(Subscriber<? super R> actual) {
        super(actual);
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validate(this.s, s)) {
            this.s = s;

            actual.onSubscribe(this);

            s.request(Long.MAX_VALUE);
        }
    }

    @Override
    public void onError(Throwable t) {
        value = null;
        actual.onError(t);
    }

    @Override
    public void onComplete() {
        if (hasValue) {
            complete(value);
        } else {
            actual.onComplete();
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        s.cancel();
    }
}
