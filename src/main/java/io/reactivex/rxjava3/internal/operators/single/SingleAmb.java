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

package io.reactivex.rxjava3.internal.operators.single;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class SingleAmb<T> extends Single<T> {
    private final SingleSource<? extends T>[] sources;
    private final Iterable<? extends SingleSource<? extends T>> sourcesIterable;

    public SingleAmb(SingleSource<? extends T>[] sources, Iterable<? extends SingleSource<? extends T>> sourcesIterable) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void subscribeActual(final SingleObserver<? super T> observer) {
        SingleSource<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new SingleSource[8];
            try {
                for (SingleSource<? extends T> element : sourcesIterable) {
                    if (element == null) {
                        EmptyDisposable.error(new NullPointerException("One of the sources is null"), observer);
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
                EmptyDisposable.error(e, observer);
                return;
            }
        } else {
            count = sources.length;
        }

        final AtomicBoolean winner = new AtomicBoolean();
        final CompositeDisposable set = new CompositeDisposable();

        observer.onSubscribe(set);

        for (int i = 0; i < count; i++) {
            SingleSource<? extends T> s1 = sources[i];
            if (set.isDisposed()) {
                return;
            }

            if (s1 == null) {
                set.dispose();
                Throwable e = new NullPointerException("One of the sources is null");
                if (winner.compareAndSet(false, true)) {
                    observer.onError(e);
                } else {
                    RxJavaPlugins.onError(e);
                }
                return;
            }

            s1.subscribe(new AmbSingleObserver<T>(observer, set, winner));
        }
    }

    static final class AmbSingleObserver<T> implements SingleObserver<T> {

        final CompositeDisposable set;

        final SingleObserver<? super T> downstream;

        final AtomicBoolean winner;

        Disposable upstream;

        AmbSingleObserver(SingleObserver<? super T> observer, CompositeDisposable set, AtomicBoolean winner) {
            this.downstream = observer;
            this.set = set;
            this.winner = winner;
        }

        @Override
        public void onSubscribe(Disposable d) {
            this.upstream = d;
            set.add(d);
        }

        @Override
        public void onSuccess(T value) {
            if (winner.compareAndSet(false, true)) {
                set.delete(upstream);
                set.dispose();
                downstream.onSuccess(value);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (winner.compareAndSet(false, true)) {
                set.delete(upstream);
                set.dispose();
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }
    }

}
