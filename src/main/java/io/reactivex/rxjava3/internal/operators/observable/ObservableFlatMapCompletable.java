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

package io.reactivex.rxjava3.internal.operators.observable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.observers.BasicIntQueueDisposable;
import io.reactivex.rxjava3.internal.util.AtomicThrowable;

/**
 * Maps a sequence of values into CompletableSources and awaits their termination.
 * @param <T> the value type
 */
public final class ObservableFlatMapCompletable<T> extends AbstractObservableWithUpstream<T, T> {

    final Function<? super T, ? extends CompletableSource> mapper;

    final boolean delayErrors;

    public ObservableFlatMapCompletable(ObservableSource<T> source,
            Function<? super T, ? extends CompletableSource> mapper, boolean delayErrors) {
        super(source);
        this.mapper = mapper;
        this.delayErrors = delayErrors;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new FlatMapCompletableMainObserver<>(observer, mapper, delayErrors));
    }

    static final class FlatMapCompletableMainObserver<T> extends BasicIntQueueDisposable<T>
    implements Observer<T> {
        private static final long serialVersionUID = 8443155186132538303L;

        final Observer<? super T> downstream;

        final AtomicThrowable errors;

        final Function<? super T, ? extends CompletableSource> mapper;

        final boolean delayErrors;

        final CompositeDisposable set;

        Disposable upstream;

        volatile boolean disposed;

        FlatMapCompletableMainObserver(Observer<? super T> observer, Function<? super T, ? extends CompletableSource> mapper, boolean delayErrors) {
            this.downstream = observer;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.errors = new AtomicThrowable();
            this.set = new CompositeDisposable();
            this.lazySet(1);
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T value) {
            CompletableSource cs;

            try {
                cs = Objects.requireNonNull(mapper.apply(value), "The mapper returned a null CompletableSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.dispose();
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
                    }
                } else {
                    disposed = true;
                    upstream.dispose();
                    set.dispose();
                    errors.tryTerminateConsumer(downstream);
                }
            }
        }

        @Override
        public void onComplete() {
            if (decrementAndGet() == 0) {
                errors.tryTerminateConsumer(downstream);
            }
        }

        @Override
        public void dispose() {
            disposed = true;
            upstream.dispose();
            set.dispose();
            errors.tryTerminateAndReport();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Nullable
        @Override
        public T poll() {
            return null; // always empty
        }

        @Override
        public boolean isEmpty() {
            return true; // always empty
        }

        @Override
        public void clear() {
            // nothing to clear
        }

        @Override
        public int requestFusion(int mode) {
            return mode & ASYNC;
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
