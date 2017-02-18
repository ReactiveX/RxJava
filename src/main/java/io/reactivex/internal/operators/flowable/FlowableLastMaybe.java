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
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Consumes the source Publisher and emits its last item or completes.
 * 
 * @param <T> the value type
 */
public final class FlowableLastMaybe<T> extends Maybe<T> {

    final Publisher<T> source;

    public FlowableLastMaybe(Publisher<T> source) {
        this.source = source;
    }

    // TODO fuse back to Flowable

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new LastSubscriber<T>(observer));
    }

    static final class LastSubscriber<T> implements FlowableSubscriber<T>, Disposable {

        final MaybeObserver<? super T> actual;

        Subscription s;

        T item;

        LastSubscriber(MaybeObserver<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void dispose() {
            s.cancel();
            s = SubscriptionHelper.CANCELLED;
        }

        @Override
        public boolean isDisposed() {
            return s == SubscriptionHelper.CANCELLED;
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
            item = t;
        }

        @Override
        public void onError(Throwable t) {
            s = SubscriptionHelper.CANCELLED;
            item = null;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            s = SubscriptionHelper.CANCELLED;
            T v = item;
            if (v != null) {
                item = null;
                actual.onSuccess(v);
            } else {
                actual.onComplete();
            }
        }
    }
}
