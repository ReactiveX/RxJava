/*
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

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.NoSuchElementException;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Consumes the source Publisher and emits its last item or the defaultItem
 * if empty.
 * 
 * @param <T> the value type
 */
public final class FlowableLastSingle<T> extends Single<T> {

    final Publisher<T> source;

    final T defaultItem;

    public FlowableLastSingle(Publisher<T> source, T defaultItem) {
        this.source = source;
        this.defaultItem = defaultItem;
    }

    // TODO fuse back to Flowable

    @Override
    protected void subscribeActual(SingleObserver<? super T> observer) {
        source.subscribe(new LastSubscriber<>(observer, defaultItem));
    }

    static final class LastSubscriber<T> implements FlowableSubscriber<T>, Disposable {

        final SingleObserver<? super T> downstream;

        final T defaultItem;

        Subscription upstream;

        T item;

        LastSubscriber(SingleObserver<? super T> actual, T defaultItem) {
            this.downstream = actual;
            this.defaultItem = defaultItem;
        }

        @Override
        public void dispose() {
            upstream.cancel();
            upstream = SubscriptionHelper.CANCELLED;
        }

        @Override
        public boolean isDisposed() {
            return upstream == SubscriptionHelper.CANCELLED;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            item = t;
        }

        @Override
        public void onError(Throwable t) {
            upstream = SubscriptionHelper.CANCELLED;
            item = null;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            upstream = SubscriptionHelper.CANCELLED;
            T v = item;
            if (v != null) {
                item = null;
                downstream.onSuccess(v);
            } else {
                v = defaultItem;

                if (v != null) {
                    downstream.onSuccess(v);
                } else {
                    downstream.onError(new NoSuchElementException());
                }
            }
        }
    }
}
