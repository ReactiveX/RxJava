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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableUsing<T, D> extends Flowable<T> {
    final Callable<? extends D> resourceSupplier;
    final Function<? super D, ? extends Publisher<? extends T>> sourceSupplier;
    final Consumer<? super D> disposer;
    final boolean eager;

    public FlowableUsing(Callable<? extends D> resourceSupplier,
            Function<? super D, ? extends Publisher<? extends T>> sourceSupplier,
            Consumer<? super D> disposer,
            boolean eager) {
        this.resourceSupplier = resourceSupplier;
        this.sourceSupplier = sourceSupplier;
        this.disposer = disposer;
        this.eager = eager;
    }

    @Override
    public void subscribeActual(Subscriber<? super T> s) {
        D resource;

        try {
            resource = resourceSupplier.call();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptySubscription.error(e, s);
            return;
        }

        Publisher<? extends T> source;
        try {
            source = ObjectHelper.requireNonNull(sourceSupplier.apply(resource), "The sourceSupplier returned a null Publisher");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            try {
                disposer.accept(resource);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptySubscription.error(new CompositeException(e, ex), s);
                return;
            }
            EmptySubscription.error(e, s);
            return;
        }

        UsingSubscriber<T, D> us = new UsingSubscriber<T, D>(s, resource, disposer, eager);

        source.subscribe(us);
    }

    static final class UsingSubscriber<T, D> extends AtomicBoolean implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = 5904473792286235046L;

        final Subscriber<? super T> downstream;
        final D resource;
        final Consumer<? super D> disposer;
        final boolean eager;

        Subscription upstream;

        UsingSubscriber(Subscriber<? super T> actual, D resource, Consumer<? super D> disposer, boolean eager) {
            this.downstream = actual;
            this.resource = resource;
            this.disposer = disposer;
            this.eager = eager;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (eager) {
                Throwable innerError = null;
                if (compareAndSet(false, true)) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable e) {
                        Exceptions.throwIfFatal(e);
                        innerError = e;
                    }
                }

                upstream.cancel();
                if (innerError != null) {
                    downstream.onError(new CompositeException(t, innerError));
                } else {
                    downstream.onError(t);
                }
            } else {
                downstream.onError(t);
                upstream.cancel();
                disposeAfter();
            }
        }

        @Override
        public void onComplete() {
            if (eager) {
                if (compareAndSet(false, true)) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable e) {
                        Exceptions.throwIfFatal(e);
                        downstream.onError(e);
                        return;
                    }
                }

                upstream.cancel();
                downstream.onComplete();
            } else {
                downstream.onComplete();
                upstream.cancel();
                disposeAfter();
            }
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            disposeAfter();
            upstream.cancel();
        }

        void disposeAfter() {
            if (compareAndSet(false, true)) {
                try {
                    disposer.accept(resource);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    // can't call actual.onError unless it is serialized, which is expensive
                    RxJavaPlugins.onError(e);
                }
            }
        }
    }
}
