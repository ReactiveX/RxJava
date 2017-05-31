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

import java.util.Iterator;

import io.reactivex.*;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.observers.BasicIntQueueDisposable;

/**
 * Maps a success value into an Iterable and streams it back as an Observable.
 *
 * @param <T> the source value type
 * @param <R> the element type of the Iterable
 */
public final class SingleFlatMapIterableObservable<T, R> extends Observable<R> {

    final SingleSource<T> source;

    final Function<? super T, ? extends Iterable<? extends R>> mapper;

    public SingleFlatMapIterableObservable(SingleSource<T> source,
            Function<? super T, ? extends Iterable<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Observer<? super R> s) {
        source.subscribe(new FlatMapIterableObserver<T, R>(s, mapper));
    }

    static final class FlatMapIterableObserver<T, R>
    extends BasicIntQueueDisposable<R>
    implements SingleObserver<T> {

        private static final long serialVersionUID = -8938804753851907758L;

        final Observer<? super R> actual;

        final Function<? super T, ? extends Iterable<? extends R>> mapper;

        Disposable d;

        volatile Iterator<? extends R> it;

        volatile boolean cancelled;

        boolean outputFused;

        FlatMapIterableObserver(Observer<? super R> actual,
                Function<? super T, ? extends Iterable<? extends R>> mapper) {
            this.actual = actual;
            this.mapper = mapper;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            Observer<? super R> a = actual;
            Iterator<? extends R> iterator;
            boolean has;
            try {
                iterator = mapper.apply(value).iterator();

                has = iterator.hasNext();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(ex);
                return;
            }

            if (!has) {
                a.onComplete();
                return;
            }

            if (outputFused) {
                it = iterator;
                a.onNext(null);
                a.onComplete();
            } else {
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
        }

        @Override
        public void onError(Throwable e) {
            d = DisposableHelper.DISPOSED;
            actual.onError(e);
        }

        @Override
        public void dispose() {
            cancelled = true;
            d.dispose();
            d = DisposableHelper.DISPOSED;
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
                R v = ObjectHelper.requireNonNull(iterator.next(), "The iterator returned a null value");
                if (!iterator.hasNext()) {
                    it = null;
                }
                return v;
            }
            return null;
        }

    }
}
