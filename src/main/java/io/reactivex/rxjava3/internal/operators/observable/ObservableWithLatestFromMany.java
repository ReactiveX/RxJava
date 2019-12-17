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

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Combines a main sequence of values with the latest from multiple other sequences via
 * a selector function.
 *
 * @param <T> the main sequence's type
 * @param <R> the output type
 */
public final class ObservableWithLatestFromMany<T, R> extends AbstractObservableWithUpstream<T, R> {

    @Nullable
    final ObservableSource<?>[] otherArray;

    @Nullable
    final Iterable<? extends ObservableSource<?>> otherIterable;

    @NonNull
    final Function<? super Object[], R> combiner;

    public ObservableWithLatestFromMany(@NonNull ObservableSource<T> source, @NonNull ObservableSource<?>[] otherArray, @NonNull Function<? super Object[], R> combiner) {
        super(source);
        this.otherArray = otherArray;
        this.otherIterable = null;
        this.combiner = combiner;
    }

    public ObservableWithLatestFromMany(@NonNull ObservableSource<T> source, @NonNull Iterable<? extends ObservableSource<?>> otherIterable, @NonNull Function<? super Object[], R> combiner) {
        super(source);
        this.otherArray = null;
        this.otherIterable = otherIterable;
        this.combiner = combiner;
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) {
        ObservableSource<?>[] others = otherArray;
        int n = 0;
        if (others == null) {
            others = new ObservableSource[8];

            try {
                for (ObservableSource<?> p : otherIterable) {
                    if (n == others.length) {
                        others = Arrays.copyOf(others, n + (n >> 1));
                    }
                    others[n++] = p;
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptyDisposable.error(ex, observer);
                return;
            }

        } else {
            n = others.length;
        }

        if (n == 0) {
            new ObservableMap<T, R>(source, new SingletonArrayFunc()).subscribeActual(observer);
            return;
        }

        WithLatestFromObserver<T, R> parent = new WithLatestFromObserver<T, R>(observer, combiner, n);
        observer.onSubscribe(parent);
        parent.subscribe(others, n);

        source.subscribe(parent);
    }

    static final class WithLatestFromObserver<T, R>
    extends AtomicInteger
    implements Observer<T>, Disposable {

        private static final long serialVersionUID = 1577321883966341961L;

        final Observer<? super R> downstream;

        final Function<? super Object[], R> combiner;

        final WithLatestInnerObserver[] observers;

        final AtomicReferenceArray<Object> values;

        final AtomicReference<Disposable> upstream;

        final AtomicThrowable error;

        volatile boolean done;

        WithLatestFromObserver(Observer<? super R> actual, Function<? super Object[], R> combiner, int n) {
            this.downstream = actual;
            this.combiner = combiner;
            WithLatestInnerObserver[] s = new WithLatestInnerObserver[n];
            for (int i = 0; i < n; i++) {
                s[i] = new WithLatestInnerObserver(this, i);
            }
            this.observers = s;
            this.values = new AtomicReferenceArray<Object>(n);
            this.upstream = new AtomicReference<Disposable>();
            this.error = new AtomicThrowable();
        }

        void subscribe(ObservableSource<?>[] others, int n) {
            WithLatestInnerObserver[] observers = this.observers;
            AtomicReference<Disposable> upstream = this.upstream;
            for (int i = 0; i < n; i++) {
                if (DisposableHelper.isDisposed(upstream.get()) || done) {
                    return;
                }
                others[i].subscribe(observers[i]);
            }
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this.upstream, d);
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            AtomicReferenceArray<Object> ara = values;
            int n = ara.length();
            Object[] objects = new Object[n + 1];
            objects[0] = t;

            for (int i = 0; i < n; i++) {
                Object o = ara.get(i);
                if (o == null) {
                    // no latest, skip this value
                    return;
                }
                objects[i + 1] = o;
            }

            R v;

            try {
                v = Objects.requireNonNull(combiner.apply(objects), "combiner returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                dispose();
                onError(ex);
                return;
            }

            HalfSerializer.onNext(downstream, v, this, error);
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            cancelAllBut(-1);
            HalfSerializer.onError(downstream, t, this, error);
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                cancelAllBut(-1);
                HalfSerializer.onComplete(downstream, this, error);
            }
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(upstream.get());
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(upstream);
            for (WithLatestInnerObserver observer : observers) {
                observer.dispose();
            }
        }

        void innerNext(int index, Object o) {
            values.set(index, o);
        }

        void innerError(int index, Throwable t) {
            done = true;
            DisposableHelper.dispose(upstream);
            cancelAllBut(index);
            HalfSerializer.onError(downstream, t, this, error);
        }

        void innerComplete(int index, boolean nonEmpty) {
            if (!nonEmpty) {
                done = true;
                cancelAllBut(index);
                HalfSerializer.onComplete(downstream, this, error);
            }
        }

        void cancelAllBut(int index) {
            WithLatestInnerObserver[] observers = this.observers;
            for (int i = 0; i < observers.length; i++) {
                if (i != index) {
                    observers[i].dispose();
                }
            }
        }
    }

    static final class WithLatestInnerObserver
    extends AtomicReference<Disposable>
    implements Observer<Object> {

        private static final long serialVersionUID = 3256684027868224024L;

        final WithLatestFromObserver<?, ?> parent;

        final int index;

        boolean hasValue;

        WithLatestInnerObserver(WithLatestFromObserver<?, ?> parent, int index) {
            this.parent = parent;
            this.index = index;
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }

        @Override
        public void onNext(Object t) {
            if (!hasValue) {
                hasValue = true;
            }
            parent.innerNext(index, t);
        }

        @Override
        public void onError(Throwable t) {
            parent.innerError(index, t);
        }

        @Override
        public void onComplete() {
            parent.innerComplete(index, hasValue);
        }

        public void dispose() {
            DisposableHelper.dispose(this);
        }
    }

    final class SingletonArrayFunc implements Function<T, R> {
        @Override
        public R apply(T t) throws Throwable {
            return Objects.requireNonNull(combiner.apply(new Object[] { t }), "The combiner returned a null value");
        }
    }
}
