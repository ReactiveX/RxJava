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
package io.reactivex.rxjava3.internal.operators.flowable;

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;
import io.reactivex.rxjava3.internal.fuseable.FuseToFlowable;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

import java.util.Objects;

public final class FlowableCollectSingle<T, U> extends Single<U> implements FuseToFlowable<U> {

    final Flowable<T> source;

    final Supplier<? extends U> initialSupplier;
    final BiConsumer<? super U, ? super T> collector;

    public FlowableCollectSingle(Flowable<T> source, Supplier<? extends U> initialSupplier, BiConsumer<? super U, ? super T> collector) {
        this.source = source;
        this.initialSupplier = initialSupplier;
        this.collector = collector;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super U> observer) {
        U u;
        try {
            u = Objects.requireNonNull(initialSupplier.get(), "The initialSupplier returned a null value");
        } catch (Throwable e) {
            EmptyDisposable.error(e, observer);
            return;
        }

        source.subscribe(new CollectSubscriber<T, U>(observer, u, collector));
    }

    @Override
    public Flowable<U> fuseToFlowable() {
        return RxJavaPlugins.onAssembly(new FlowableCollect<T, U>(source, initialSupplier, collector));
    }

    static final class CollectSubscriber<T, U> implements FlowableSubscriber<T>, Disposable {

        final SingleObserver<? super U> downstream;

        final BiConsumer<? super U, ? super T> collector;

        final U u;

        Subscription upstream;

        boolean done;

        CollectSubscriber(SingleObserver<? super U> actual, U u, BiConsumer<? super U, ? super T> collector) {
            this.downstream = actual;
            this.collector = collector;
            this.u = u;
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
            try {
                collector.accept(u, t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                upstream.cancel();
                onError(e);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            upstream = SubscriptionHelper.CANCELLED;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            upstream = SubscriptionHelper.CANCELLED;
            downstream.onSuccess(u);
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
    }
}
