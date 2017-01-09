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

package io.reactivex.internal.operators.single;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.plugins.RxJavaPlugins;

public final class SingleAmb<T> extends Single<T> {
    private final SingleSource<? extends T>[] sources;
    private final Iterable<? extends SingleSource<? extends T>> sourcesIterable;

    public SingleAmb(SingleSource<? extends T>[] sources, Iterable<? extends SingleSource<? extends T>> sourcesIterable) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void subscribeActual(final SingleObserver<? super T> s) {
        SingleSource<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new SingleSource[8];
            try {
                for (SingleSource<? extends T> element : sourcesIterable) {
                    if (element == null) {
                        EmptyDisposable.error(new NullPointerException("One of the sources is null"), s);
                        return;
                    }
                    if (count == sources.length) {
                        SingleSource<? extends T>[] b = new SingleSource[count + (count >> 2)];
                        System.arraycopy(sources, 0, b, 0, count);
                        sources = b;
                    }
                    sources[count++] = element;
                }
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                EmptyDisposable.error(e, s);
                return;
            }
        } else {
            count = sources.length;
        }

        final CompositeDisposable set = new CompositeDisposable();

        AmbSingleObserver<T> shared = new AmbSingleObserver<T>(s, set);
        s.onSubscribe(set);

        for (int i = 0; i < count; i++) {
            SingleSource<? extends T> s1 = sources[i];
            if (shared.get()) {
                return;
            }

            if (s1 == null) {
                set.dispose();
                Throwable e = new NullPointerException("One of the sources is null");
                if (shared.compareAndSet(false, true)) {
                    s.onError(e);
                } else {
                    RxJavaPlugins.onError(e);
                }
                return;
            }

            s1.subscribe(shared);
        }
    }

    static final class AmbSingleObserver<T> extends AtomicBoolean implements SingleObserver<T> {

        private static final long serialVersionUID = -1944085461036028108L;

        final CompositeDisposable set;

        final SingleObserver<? super T> s;

        AmbSingleObserver(SingleObserver<? super T> s, CompositeDisposable set) {
            this.s = s;
            this.set = set;
        }

        @Override
        public void onSubscribe(Disposable d) {
            set.add(d);
        }

        @Override
        public void onSuccess(T value) {
            if (compareAndSet(false, true)) {
                set.dispose();
                s.onSuccess(value);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (compareAndSet(false, true)) {
                set.dispose();
                s.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }
    }

}
