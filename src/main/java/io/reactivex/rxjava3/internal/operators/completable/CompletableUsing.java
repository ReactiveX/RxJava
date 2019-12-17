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

package io.reactivex.rxjava3.internal.operators.completable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class CompletableUsing<R> extends Completable {

    final Supplier<R> resourceSupplier;
    final Function<? super R, ? extends CompletableSource> completableFunction;
    final Consumer<? super R> disposer;
    final boolean eager;

    public CompletableUsing(Supplier<R> resourceSupplier,
                            Function<? super R, ? extends CompletableSource> completableFunction, Consumer<? super R> disposer,
                            boolean eager) {
        this.resourceSupplier = resourceSupplier;
        this.completableFunction = completableFunction;
        this.disposer = disposer;
        this.eager = eager;
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        R resource;

        try {
            resource = resourceSupplier.get();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, observer);
            return;
        }

        CompletableSource source;

        try {
            source = Objects.requireNonNull(completableFunction.apply(resource), "The completableFunction returned a null CompletableSource");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            if (eager) {
                try {
                    disposer.accept(resource);
                } catch (Throwable exc) {
                    Exceptions.throwIfFatal(exc);
                    EmptyDisposable.error(new CompositeException(ex, exc), observer);
                    return;
                }
            }

            EmptyDisposable.error(ex, observer);

            if (!eager) {
                try {
                    disposer.accept(resource);
                } catch (Throwable exc) {
                    Exceptions.throwIfFatal(exc);
                    RxJavaPlugins.onError(exc);
                }
            }
            return;
        }

        source.subscribe(new UsingObserver<R>(observer, resource, disposer, eager));
    }

    static final class UsingObserver<R>
    extends AtomicReference<Object>
    implements CompletableObserver, Disposable {

        private static final long serialVersionUID = -674404550052917487L;

        final CompletableObserver downstream;

        final Consumer<? super R> disposer;

        final boolean eager;

        Disposable upstream;

        UsingObserver(CompletableObserver actual, R resource, Consumer<? super R> disposer, boolean eager) {
            super(resource);
            this.downstream = actual;
            this.disposer = disposer;
            this.eager = eager;
        }

        @Override
        public void dispose() {
            if (eager) {
                disposeResource();
                upstream.dispose();
                upstream = DisposableHelper.DISPOSED;
            } else {
                upstream.dispose();
                upstream = DisposableHelper.DISPOSED;
                disposeResource();
            }
        }

        @SuppressWarnings("unchecked")
        void disposeResource() {
            Object resource = getAndSet(this);
            if (resource != this) {
                try {
                    disposer.accept((R)resource);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaPlugins.onError(ex);
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onError(Throwable e) {
            upstream = DisposableHelper.DISPOSED;
            if (eager) {
                Object resource = getAndSet(this);
                if (resource != this) {
                    try {
                        disposer.accept((R)resource);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        e = new CompositeException(e, ex);
                    }
                } else {
                    return;
                }
            }

            downstream.onError(e);

            if (!eager) {
                disposeResource();
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onComplete() {
            upstream = DisposableHelper.DISPOSED;
            if (eager) {
                Object resource = getAndSet(this);
                if (resource != this) {
                    try {
                        disposer.accept((R)resource);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        downstream.onError(ex);
                        return;
                    }
                } else {
                    return;
                }
            }

            downstream.onComplete();

            if (!eager) {
                disposeResource();
            }
        }
    }

}
