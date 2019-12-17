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

package io.reactivex.rxjava3.internal.operators.observable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class ObservableUsing<T, D> extends Observable<T> {
    final Supplier<? extends D> resourceSupplier;
    final Function<? super D, ? extends ObservableSource<? extends T>> sourceSupplier;
    final Consumer<? super D> disposer;
    final boolean eager;

    public ObservableUsing(Supplier<? extends D> resourceSupplier,
            Function<? super D, ? extends ObservableSource<? extends T>> sourceSupplier,
            Consumer<? super D> disposer,
            boolean eager) {
        this.resourceSupplier = resourceSupplier;
        this.sourceSupplier = sourceSupplier;
        this.disposer = disposer;
        this.eager = eager;
    }

    @Override
    public void subscribeActual(Observer<? super T> observer) {
        D resource;

        try {
            resource = resourceSupplier.get();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, observer);
            return;
        }

        ObservableSource<? extends T> source;
        try {
            source = Objects.requireNonNull(sourceSupplier.apply(resource), "The sourceSupplier returned a null ObservableSource");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            try {
                disposer.accept(resource);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptyDisposable.error(new CompositeException(e, ex), observer);
                return;
            }
            EmptyDisposable.error(e, observer);
            return;
        }

        UsingObserver<T, D> us = new UsingObserver<T, D>(observer, resource, disposer, eager);

        source.subscribe(us);
    }

    static final class UsingObserver<T, D> extends AtomicBoolean implements Observer<T>, Disposable {

        private static final long serialVersionUID = 5904473792286235046L;

        final Observer<? super T> downstream;
        final D resource;
        final Consumer<? super D> disposer;
        final boolean eager;

        Disposable upstream;

        UsingObserver(Observer<? super T> actual, D resource, Consumer<? super D> disposer, boolean eager) {
            this.downstream = actual;
            this.resource = resource;
            this.disposer = disposer;
            this.eager = eager;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
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
                if (compareAndSet(false, true)) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable e) {
                        Exceptions.throwIfFatal(e);
                        t = new CompositeException(t, e);
                    }
                }

                upstream.dispose();
                downstream.onError(t);
            } else {
                downstream.onError(t);
                upstream.dispose();
                disposeResource();
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

                upstream.dispose();
                downstream.onComplete();
            } else {
                downstream.onComplete();
                upstream.dispose();
                disposeResource();
            }
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

        @Override
        public boolean isDisposed() {
            return get();
        }

        void disposeResource() {
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
