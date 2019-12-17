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

package io.reactivex.rxjava3.internal.operators.maybe;

import java.util.Iterator;
import java.util.Objects;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.observers.BasicQueueDisposable;

/**
 * Maps a success value into an Iterable and streams it back as a Flowable.
 *
 * @param <T> the source value type
 * @param <R> the element type of the Iterable
 */
public final class MaybeFlatMapIterableObservable<T, R> extends Observable<R> {

    final MaybeSource<T> source;

    final Function<? super T, ? extends Iterable<? extends R>> mapper;

    public MaybeFlatMapIterableObservable(MaybeSource<T> source,
            Function<? super T, ? extends Iterable<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) {
        source.subscribe(new FlatMapIterableObserver<T, R>(observer, mapper));
    }

    static final class FlatMapIterableObserver<T, R>
    extends BasicQueueDisposable<R>
    implements MaybeObserver<T> {

        final Observer<? super R> downstream;

        final Function<? super T, ? extends Iterable<? extends R>> mapper;

        Disposable upstream;

        volatile Iterator<? extends R> it;

        volatile boolean cancelled;

        boolean outputFused;

        FlatMapIterableObserver(Observer<? super R> actual,
                Function<? super T, ? extends Iterable<? extends R>> mapper) {
            this.downstream = actual;
            this.mapper = mapper;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            Observer<? super R> a = downstream;

            Iterator<? extends R> iterator;
            boolean has;
            try {
                iterator = mapper.apply(value).iterator();

                has = iterator.hasNext();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                a.onError(ex);
                return;
            }

            if (!has) {
                a.onComplete();
                return;
            }

            this.it = iterator;

            if (outputFused) {
                a.onNext(null);
                a.onComplete();
                return;
            }

            for (;;) {
                if (cancelled) {
                    return;
                }

                R v;

                try {
                    v = iterator.next();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    a.onError(ex);
                    return;
                }

                a.onNext(v);

                if (cancelled) {
                    return;
                }

                boolean b;

                try {
                    b = iterator.hasNext();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    a.onError(ex);
                    return;
                }

                if (!b) {
                    a.onComplete();
                    return;
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            upstream = DisposableHelper.DISPOSED;
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void dispose() {
            cancelled = true;
            upstream.dispose();
            upstream = DisposableHelper.DISPOSED;
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & ASYNC) != 0) {
                outputFused = true;
                return ASYNC;
            }
            return NONE;
        }

        @Override
        public void clear() {
            it = null;
        }

        @Override
        public boolean isEmpty() {
            return it == null;
        }

        @Nullable
        @Override
        public R poll() throws Exception {
            Iterator<? extends R> iterator = it;

            if (iterator != null) {
                R v = Objects.requireNonNull(iterator.next(), "The iterator returned a null value");
                if (!iterator.hasNext()) {
                    it = null;
                }
                return v;
            }
            return null;
        }

    }
}
