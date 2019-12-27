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

package io.reactivex.rxjava3.internal.jdk8;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Maps the upstream values onto {@link Stream}s and emits their items in order to the downstream.
 *
 * @param <T> the upstream element type
 * @param <R> the inner {@code Stream} and result element type
 * @since 3.0.0
 */
public final class ObservableFlatMapStream<T, R> extends Observable<R> {

    final Observable<T> source;

    final Function<? super T, ? extends Stream<? extends R>> mapper;

    public ObservableFlatMapStream(Observable<T> source, Function<? super T, ? extends Stream<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) {
        if (source instanceof Supplier) {
            Stream<? extends R> stream = null;
            try {
                @SuppressWarnings("unchecked")
                T t = ((Supplier<T>)source).get();
                if (t != null) {
                    stream = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Stream");
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptyDisposable.error(ex, observer);
                return;
            }

            if (stream != null) {
                ObservableFromStream.subscribeStream(observer, stream);
            } else {
                EmptyDisposable.complete(observer);
            }
        } else {
            source.subscribe(new FlatMapStreamObserver<>(observer, mapper));
        }
    }

    static final class FlatMapStreamObserver<T, R> extends AtomicInteger
    implements Observer<T>, Disposable {

        private static final long serialVersionUID = -5127032662980523968L;

        final Observer<? super R> downstream;

        final Function<? super T, ? extends Stream<? extends R>> mapper;

        Disposable upstream;

        volatile boolean disposed;

        boolean done;

        FlatMapStreamObserver(Observer<? super R> downstream, Function<? super T, ? extends Stream<? extends R>> mapper) {
            this.downstream = downstream;
            this.mapper = mapper;
        }

        @Override
        public void onSubscribe(@NonNull Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(@NonNull T t) {
            if (done) {
                return;
            }
            try {
                try (Stream<? extends R> stream = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Stream")) {
                    Iterator<? extends R> it = stream.iterator();
                    while (it.hasNext()) {
                        if (disposed) {
                            done = true;
                            break;
                        }
                        R value = Objects.requireNonNull(it.next(), "The Stream's Iterator.next returned a null value");
                        if (disposed) {
                            done = true;
                            break;
                        }
                        downstream.onNext(value);
                        if (disposed) {
                            done = true;
                            break;
                        }
                    }
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.dispose();
                onError(ex);
            }
        }

        @Override
        public void onError(@NonNull Throwable e) {
            if (done) {
                RxJavaPlugins.onError(e);
            } else {
                done = true;
                downstream.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                downstream.onComplete();
            }
        }

        @Override
        public void dispose() {
            disposed = true;
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
