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
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;
import io.reactivex.rxjava3.internal.fuseable.QueueDisposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Utility classes to work with scalar-sourced XMap operators (where X == { flat, concat, switch }).
 */
public final class ObservableScalarXMap {

    /** Utility class. */
    private ObservableScalarXMap() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Tries to subscribe to a possibly Supplier source's mapped ObservableSource.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param source the source ObservableSource
     * @param observer the subscriber
     * @param mapper the function mapping a scalar value into an ObservableSource
     * @return true if successful, false if the caller should continue with the regular path.
     */
    @SuppressWarnings("unchecked")
    public static <T, R> boolean tryScalarXMapSubscribe(ObservableSource<T> source,
            Observer<? super R> observer,
            Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        if (source instanceof Supplier) {
            T t;

            try {
                t = ((Supplier<T>)source).get();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptyDisposable.error(ex, observer);
                return true;
            }

            if (t == null) {
                EmptyDisposable.complete(observer);
                return true;
            }

            ObservableSource<? extends R> r;

            try {
                r = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null ObservableSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptyDisposable.error(ex, observer);
                return true;
            }

            if (r instanceof Supplier) {
                R u;

                try {
                    u = ((Supplier<R>)r).get();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    EmptyDisposable.error(ex, observer);
                    return true;
                }

                if (u == null) {
                    EmptyDisposable.complete(observer);
                    return true;
                }
                ScalarDisposable<R> sd = new ScalarDisposable<>(observer, u);
                observer.onSubscribe(sd);
                sd.run();
            } else {
                r.subscribe(observer);
            }

            return true;
        }
        return false;
    }

    /**
     * Maps a scalar value into an Observable and emits its values.
     *
     * @param <T> the scalar value type
     * @param <U> the output value type
     * @param value the scalar value to map
     * @param mapper the function that gets the scalar value and should return
     * an ObservableSource that gets streamed
     * @return the new Observable instance
     */
    public static <T, U> Observable<U> scalarXMap(T value,
            Function<? super T, ? extends ObservableSource<? extends U>> mapper) {
        return RxJavaPlugins.onAssembly(new ScalarXMapObservable<>(value, mapper));
    }

    /**
     * Maps a scalar value to an ObservableSource and subscribes to it.
     *
     * @param <T> the scalar value type
     * @param <R> the mapped ObservableSource's element type.
     */
    static final class ScalarXMapObservable<T, R> extends Observable<R> {

        final T value;

        final Function<? super T, ? extends ObservableSource<? extends R>> mapper;

        ScalarXMapObservable(T value,
                Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
            this.value = value;
            this.mapper = mapper;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void subscribeActual(Observer<? super R> observer) {
            ObservableSource<? extends R> other;
            try {
                other = Objects.requireNonNull(mapper.apply(value), "The mapper returned a null ObservableSource");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                EmptyDisposable.error(e, observer);
                return;
            }
            if (other instanceof Supplier) {
                R u;

                try {
                    u = ((Supplier<R>)other).get();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    EmptyDisposable.error(ex, observer);
                    return;
                }

                if (u == null) {
                    EmptyDisposable.complete(observer);
                    return;
                }
                ScalarDisposable<R> sd = new ScalarDisposable<>(observer, u);
                observer.onSubscribe(sd);
                sd.run();
            } else {
                other.subscribe(observer);
            }
        }
    }

    /**
     * Represents a Disposable that signals one onNext followed by an onComplete.
     *
     * @param <T> the value type
     */
    public static final class ScalarDisposable<T>
    extends AtomicInteger
    implements QueueDisposable<T>, Runnable {

        private static final long serialVersionUID = 3880992722410194083L;

        final Observer<? super T> observer;

        final T value;

        static final int START = 0;
        static final int FUSED = 1;
        static final int ON_NEXT = 2;
        static final int ON_COMPLETE = 3;

        public ScalarDisposable(Observer<? super T> observer, T value) {
            this.observer = observer;
            this.value = value;
        }

        @Override
        public boolean offer(T value) {
            throw new UnsupportedOperationException("Should not be called!");
        }

        @Override
        public boolean offer(T v1, T v2) {
            throw new UnsupportedOperationException("Should not be called!");
        }

        @Nullable
        @Override
        public T poll() {
            if (get() == FUSED) {
                lazySet(ON_COMPLETE);
                return value;
            }
            return null;
        }

        @Override
        public boolean isEmpty() {
            return get() != FUSED;
        }

        @Override
        public void clear() {
            lazySet(ON_COMPLETE);
        }

        @Override
        public void dispose() {
            set(ON_COMPLETE);
        }

        @Override
        public boolean isDisposed() {
            return get() == ON_COMPLETE;
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & SYNC) != 0) {
                lazySet(FUSED);
                return SYNC;
            }
            return NONE;
        }

        @Override
        public void run() {
            if (get() == START && compareAndSet(START, ON_NEXT)) {
                observer.onNext(value);
                if (get() == ON_NEXT) {
                    lazySet(ON_COMPLETE);
                    observer.onComplete();
                }
            }
        }
    }
}
