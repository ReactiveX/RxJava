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

package io.reactivex.internal.operators.flowable;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.subscriptions.*;

public final class FlowableCount<T> extends AbstractFlowableWithUpstream<T, Long> {

    public FlowableCount(Flowable<T> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Subscriber<? super Long> s) {
        source.subscribe(new CountSubscriber(s));
    }

    static final class CountSubscriber extends DeferredScalarSubscription<Long>
    implements FlowableSubscriber<Object> {


        private static final long serialVersionUID = 4973004223787171406L;

        Subscription s;

        long count;

        CountSubscriber(Subscriber<? super Long> actual) {
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
        public void onNext(Object t) {
            count++;
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            complete(count);
        }

        @Override
        public void cancel() {
            super.cancel();
            s.cancel();
        }
    }
}
