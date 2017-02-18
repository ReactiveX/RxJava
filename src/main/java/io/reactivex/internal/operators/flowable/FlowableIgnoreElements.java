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
import io.reactivex.annotations.Nullable;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class FlowableIgnoreElements<T> extends AbstractFlowableWithUpstream<T, T> {

    public FlowableIgnoreElements(Flowable<T> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(final Subscriber<? super T> t) {
        source.subscribe(new IgnoreElementsSubscriber<T>(t));
    }

    static final class IgnoreElementsSubscriber<T> implements FlowableSubscriber<T>, QueueSubscription<T> {
        final Subscriber<? super T> actual;

        Subscription s;

        IgnoreElementsSubscriber(Subscriber<? super T> actual) {
            this.actual = actual;
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
        public void onNext(T t) {
            // deliberately ignored
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }

        @Override
        public boolean offer(T e) {
            throw new UnsupportedOperationException("Should not be called!");
        }

        @Override
        public boolean offer(T v1, T v2) {
            throw new UnsupportedOperationException("Should not be called!");
        }

        @Nullable
        @Override
        public T poll() {
            return null; // empty, always
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void clear() {
            // always empty
        }

        @Override
        public void request(long n) {
            // never emits a value
        }

        @Override
        public void cancel() {
            s.cancel();
        }

        @Override
        public int requestFusion(int mode) {
            return mode & ASYNC;
        }
    }
}
