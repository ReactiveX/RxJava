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

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class ObservableGenerate<T, S> extends Observable<T> {
    final Supplier<S> stateSupplier;
    final BiFunction<S, Emitter<T>, S> generator;
    final Consumer<? super S> disposeState;

    public ObservableGenerate(Supplier<S> stateSupplier, BiFunction<S, Emitter<T>, S> generator,
            Consumer<? super S> disposeState) {
        this.stateSupplier = stateSupplier;
        this.generator = generator;
        this.disposeState = disposeState;
    }

    @Override
    public void subscribeActual(Observer<? super T> observer) {
        S state;

        try {
            state = stateSupplier.get();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, observer);
            return;
        }

        GeneratorDisposable<T, S> gd = new GeneratorDisposable<T, S>(observer, generator, disposeState, state);
        observer.onSubscribe(gd);
        gd.run();
    }

    static final class GeneratorDisposable<T, S>
    implements Emitter<T>, Disposable {

        final Observer<? super T> downstream;
        final BiFunction<S, ? super Emitter<T>, S> generator;
        final Consumer<? super S> disposeState;

        S state;

        volatile boolean cancelled;

        boolean terminate;

        boolean hasNext;

        GeneratorDisposable(Observer<? super T> actual,
                BiFunction<S, ? super Emitter<T>, S> generator,
                Consumer<? super S> disposeState, S initialState) {
            this.downstream = actual;
            this.generator = generator;
            this.disposeState = disposeState;
            this.state = initialState;
        }

        public void run() {
            S s = state;

            if (cancelled) {
                state = null;
                dispose(s);
                return;
            }

            final BiFunction<S, ? super Emitter<T>, S> f = generator;

            for (;;) {

                if (cancelled) {
                    state = null;
                    dispose(s);
                    return;
                }

                hasNext = false;

                try {
                    s = f.apply(s, this);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    state = null;
                    cancelled = true;
                    onError(ex);
                    dispose(s);
                    return;
                }

                if (terminate) {
                    cancelled = true;
                    state = null;
                    dispose(s);
                    return;
                }
            }

        }

        private void dispose(S s) {
            try {
                disposeState.accept(s);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void dispose() {
            cancelled = true;
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        @Override
        public void onNext(T t) {
            if (!terminate) {
                if (hasNext) {
                    onError(new IllegalStateException("onNext already called in this generate turn"));
                } else {
                    if (t == null) {
                        onError(ExceptionHelper.createNullPointerException("onNext called with a null value."));
                    } else {
                        hasNext = true;
                        downstream.onNext(t);
                    }
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (terminate) {
                RxJavaPlugins.onError(t);
            } else {
                if (t == null) {
                    t = ExceptionHelper.createNullPointerException("onError called with a null Throwable.");
                }
                terminate = true;
                downstream.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!terminate) {
                terminate = true;
                downstream.onComplete();
            }
        }
    }
}
