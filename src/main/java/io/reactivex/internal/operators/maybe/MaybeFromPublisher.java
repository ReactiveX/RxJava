/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.maybe;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Wraps a publisher, expects it to emit 0 or 1 signal or an error.
 *
 * @param <T> the value type
 */
public final class MaybeFromPublisher<T> extends Maybe<T> implements HasUpstreamPublisher<T> {

    final Publisher<T> source;

    public MaybeFromPublisher(Publisher<T> source) {
        this.source = source;
    }

    @Override
    public Publisher<T> source() {
        return source;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new FromPublisherToMaybeObserver<T>(observer));
    }

    static final class FromPublisherToMaybeObserver<T>
    extends AtomicReference<Subscription>
    implements Subscriber<T>, Disposable {

        private static final long serialVersionUID = -8017657973346356002L;

        final MaybeObserver<? super T> actual;

        T value;

        FromPublisherToMaybeObserver(MaybeObserver<? super T> observer) {
            this.actual = observer;
        }

        @Override
        public void dispose() {
            SubscriptionHelper.cancel(this);
        }

        @Override
        public boolean isDisposed() {
            return SubscriptionHelper.isCancelled(get());
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {
                actual.onSubscribe(this);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (!isDisposed()) {
                if (value != null) {
                    get().cancel();
                    onError(new IndexOutOfBoundsException("The source Publisher produced more than one item"));
                } else {
                    value = t;
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!isDisposed()) {
                value = null;
                lazySet(SubscriptionHelper.CANCELLED);
                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!isDisposed()) {
                lazySet(SubscriptionHelper.CANCELLED);
                T v = value;
                if (v == null) {
                    actual.onComplete();
                } else {
                    actual.onSuccess(v);
                }
            }
        }


    }
}
