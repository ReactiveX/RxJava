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

package io.reactivex.rxjava3.internal.operators.single;

import java.util.NoSuchElementException;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class SingleFromPublisher<T> extends Single<T> {

    final Publisher<? extends T> publisher;

    public SingleFromPublisher(Publisher<? extends T> publisher) {
        this.publisher = publisher;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> observer) {
        publisher.subscribe(new ToSingleObserver<T>(observer));
    }

    static final class ToSingleObserver<T> implements FlowableSubscriber<T>, Disposable {
        final SingleObserver<? super T> downstream;

        Subscription upstream;

        T value;

        boolean done;

        volatile boolean disposed;

        ToSingleObserver(SingleObserver<? super T> downstream) {
            this.downstream = downstream;
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
            if (done) {
                return;
            }
            if (value != null) {
                upstream.cancel();
                done = true;
                this.value = null;
                downstream.onError(new IndexOutOfBoundsException("Too many elements in the Publisher"));
            } else {
                value = t;
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            this.value = null;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            T v = this.value;
            this.value = null;
            if (v == null) {
                downstream.onError(new NoSuchElementException("The source Publisher is empty"));
            } else {
                downstream.onSuccess(v);
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public void dispose() {
            disposed = true;
            upstream.cancel();
        }
    }
}
