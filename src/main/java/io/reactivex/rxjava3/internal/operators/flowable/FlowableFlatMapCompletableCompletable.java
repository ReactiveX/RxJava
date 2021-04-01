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

import java.util.Objects;
import java.util.concurrent.atomic.*;

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.fuseable.FuseToFlowable;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.AtomicThrowable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Maps a sequence of values into CompletableSources and awaits their termination.
 * @param <T> the value type
 */
public final class FlowableFlatMapCompletableCompletable<T> extends Completable implements FuseToFlowable<T> {

    final Flowable<T> source;

    final Function<? super T, ? extends CompletableSource> mapper;

    final int maxConcurrency;

    final boolean delayErrors;

    public FlowableFlatMapCompletableCompletable(Flowable<T> source,
            Function<? super T, ? extends CompletableSource> mapper, boolean delayErrors,
            int maxConcurrency) {
        this.source = source;
        this.mapper = mapper;
        this.delayErrors = delayErrors;
        this.maxConcurrency = maxConcurrency;
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        source.subscribe(new FlatMapCompletableMainSubscriber<>(observer, mapper, delayErrors, maxConcurrency));
    }

    @Override
    public Flowable<T> fuseToFlowable() {
        return RxJavaPlugins.onAssembly(new FlowableFlatMapCompletable<>(source, mapper, delayErrors, maxConcurrency));
    }

    static final class FlatMapCompletableMainSubscriber<T> extends AtomicInteger
    implements FlowableSubscriber<T>, Disposable {
        private static final long serialVersionUID = 8443155186132538303L;

        final CompletableObserver downstream;

        final AtomicThrowable errors;

        final Function<? super T, ? extends CompletableSource> mapper;

        final boolean delayErrors;

        final CompositeDisposable set;

        final int maxConcurrency;

        Subscription upstream;

        volatile boolean disposed;

        FlatMapCompletableMainSubscriber(CompletableObserver observer,
                Function<? super T, ? extends CompletableSource> mapper, boolean delayErrors,
                int maxConcurrency) {
            this.downstream = observer;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.errors = new AtomicThrowable();
            this.set = new CompositeDisposable();
            this.maxConcurrency = maxConcurrency;
            this.lazySet(1);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);

                int m = maxConcurrency;
                if (m == Integer.MAX_VALUE) {
                    s.request(Long.MAX_VALUE);
                } else {
                    s.request(m);
                }
            }
        }

        @Override
        public void onNext(T value) {
            CompletableSource cs;

            try {
                cs = Objects.requireNonNull(mapper.apply(value), "The mapper returned a null CompletableSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.cancel();
                onError(ex);
                return;
            }

            getAndIncrement();

            InnerObserver inner = new InnerObserver();

            if (!disposed && set.add(inner)) {
                cs.subscribe(inner);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (errors.tryAddThrowableOrReport(e)) {
                if (delayErrors) {
                    if (decrementAndGet() == 0) {
                        errors.tryTerminateConsumer(downstream);
                    } else {
                        if (maxConcurrency != Integer.MAX_VALUE) {
                            upstream.request(1);
                        }
                    }
                } else {
                    disposed = true;
                    upstream.cancel();
                    set.dispose();
                    errors.tryTerminateConsumer(downstream);
                }
            }
        }

        @Override
        public void onComplete() {
            if (decrementAndGet() == 0) {
                errors.tryTerminateConsumer(downstream);
            } else {
                if (maxConcurrency != Integer.MAX_VALUE) {
                    upstream.request(1);
                }
            }
        }

        @Override
        public void dispose() {
            disposed = true;
            upstream.cancel();
            set.dispose();
            errors.tryTerminateAndReport();
        }

        @Override
        public boolean isDisposed() {
            return set.isDisposed();
        }

        void innerComplete(InnerObserver inner) {
            set.delete(inner);
            onComplete();
        }

        void innerError(InnerObserver inner, Throwable e) {
            set.delete(inner);
            onError(e);
        }

        final class InnerObserver extends AtomicReference<Disposable> implements CompletableObserver, Disposable {
            private static final long serialVersionUID = 8606673141535671828L;

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onComplete() {
                innerComplete(this);
            }

            @Override
            public void onError(Throwable e) {
                innerError(this, e);
            }

            @Override
            public void dispose() {
                DisposableHelper.dispose(this);
            }

            @Override
            public boolean isDisposed() {
                return DisposableHelper.isDisposed(get());
            }
        }
    }
}
