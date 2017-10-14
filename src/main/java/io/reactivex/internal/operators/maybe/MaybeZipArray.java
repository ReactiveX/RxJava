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

package io.reactivex.internal.operators.maybe;

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class MaybeZipArray<T, R> extends Maybe<R> {

    final MaybeSource<? extends T>[] sources;

    final Function<? super Object[], ? extends R> zipper;

    public MaybeZipArray(MaybeSource<? extends T>[] sources, Function<? super Object[], ? extends R> zipper) {
        this.sources = sources;
        this.zipper = zipper;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super R> observer) {
        MaybeSource<? extends T>[] sources = this.sources;
        int n = sources.length;


        if (n == 1) {
            sources[0].subscribe(new MaybeMap.MapMaybeObserver<T, R>(observer, new SingletonArrayFunc()));
            return;
        }

        ZipCoordinator<T, R> parent = new ZipCoordinator<T, R>(observer, n, zipper);

        observer.onSubscribe(parent);

        for (int i = 0; i < n; i++) {
            if (parent.isDisposed()) {
                return;
            }

            MaybeSource<? extends T> source = sources[i];

            if (source == null) {
                parent.innerError(new NullPointerException("One of the sources is null"), i);
                return;
            }
            source.subscribe(parent.observers[i]);
        }
    }

    static final class ZipCoordinator<T, R> extends AtomicInteger implements Disposable {


        private static final long serialVersionUID = -5556924161382950569L;

        final MaybeObserver<? super R> actual;

        final Function<? super Object[], ? extends R> zipper;

        final ZipMaybeObserver<T>[] observers;

        final Object[] values;

        @SuppressWarnings("unchecked")
        ZipCoordinator(MaybeObserver<? super R> observer, int n, Function<? super Object[], ? extends R> zipper) {
            super(n);
            this.actual = observer;
            this.zipper = zipper;
            ZipMaybeObserver<T>[] o = new ZipMaybeObserver[n];
            for (int i = 0; i < n; i++) {
                o[i] = new ZipMaybeObserver<T>(this, i);
            }
            this.observers = o;
            this.values = new Object[n];
        }

        @Override
        public boolean isDisposed() {
            return get() <= 0;
        }

        @Override
        public void dispose() {
            if (getAndSet(0) > 0) {
                for (ZipMaybeObserver<?> d : observers) {
                    d.dispose();
                }
            }
        }

        void innerSuccess(T value, int index) {
            values[index] = value;
            if (decrementAndGet() == 0) {
                R v;

                try {
                    v = ObjectHelper.requireNonNull(zipper.apply(values), "The zipper returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    actual.onError(ex);
                    return;
                }

                actual.onSuccess(v);
            }
        }

        void disposeExcept(int index) {
            ZipMaybeObserver<T>[] observers = this.observers;
            int n = observers.length;
            for (int i = 0; i < index; i++) {
                observers[i].dispose();
            }
            for (int i = index + 1; i < n; i++) {
                observers[i].dispose();
            }
        }

        void innerError(Throwable ex, int index) {
            if (getAndSet(0) > 0) {
                disposeExcept(index);
                actual.onError(ex);
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        void innerComplete(int index) {
            if (getAndSet(0) > 0) {
                disposeExcept(index);
                actual.onComplete();
            }
        }
    }

    static final class ZipMaybeObserver<T>
    extends AtomicReference<Disposable>
    implements MaybeObserver<T> {

        private static final long serialVersionUID = 3323743579927613702L;

        final ZipCoordinator<T, ?> parent;

        final int index;

        ZipMaybeObserver(ZipCoordinator<T, ?> parent, int index) {
            this.parent = parent;
            this.index = index;
        }

        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }

        @Override
        public void onSuccess(T value) {
            parent.innerSuccess(value, index);
        }

        @Override
        public void onError(Throwable e) {
            parent.innerError(e, index);
        }

        @Override
        public void onComplete() {
            parent.innerComplete(index);
        }
    }

    final class SingletonArrayFunc implements Function<T, R> {
        @Override
        public R apply(T t) throws Exception {
            return ObjectHelper.requireNonNull(zipper.apply(new Object[] { t }), "The zipper returned a null value");
        }
    }
}
