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

package io.reactivex.internal.operators.observable;

import io.reactivex.internal.functions.ObjectHelper;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.disposables.*;

public final class ObservableBuffer<T, U extends Collection<? super T>> extends AbstractObservableWithUpstream<T, U> {
    final int count;
    final int skip;
    final Callable<U> bufferSupplier;

    public ObservableBuffer(ObservableSource<T> source, int count, int skip, Callable<U> bufferSupplier) {
        super(source);
        this.count = count;
        this.skip = skip;
        this.bufferSupplier = bufferSupplier;
    }

    @Override
    protected void subscribeActual(Observer<? super U> t) {
        if (skip == count) {
            BufferExactObserver<T, U> bes = new BufferExactObserver<T, U>(t, count, bufferSupplier);
            if (bes.createBuffer()) {
                source.subscribe(bes);
            }
        } else {
            source.subscribe(new BufferSkipObserver<T, U>(t, count, skip, bufferSupplier));
        }
    }

    static final class BufferExactObserver<T, U extends Collection<? super T>> implements Observer<T>, Disposable {
        final Observer<? super U> downstream;
        final int count;
        final Callable<U> bufferSupplier;
        U buffer;

        int size;

        Disposable upstream;

        BufferExactObserver(Observer<? super U> actual, int count, Callable<U> bufferSupplier) {
            this.downstream = actual;
            this.count = count;
            this.bufferSupplier = bufferSupplier;
        }

        boolean createBuffer() {
            U b;
            try {
                b = ObjectHelper.requireNonNull(bufferSupplier.call(), "Empty buffer supplied");
            } catch (Throwable t) {
                Exceptions.throwIfFatal(t);
                buffer = null;
                if (upstream == null) {
                    EmptyDisposable.error(t, downstream);
                } else {
                    upstream.dispose();
                    downstream.onError(t);
                }
                return false;
            }

            buffer = b;

            return true;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void onNext(T t) {
            U b = buffer;
            if (b != null) {
                b.add(t);

                if (++size >= count) {
                    downstream.onNext(b);

                    size = 0;
                    createBuffer();
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            buffer = null;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            U b = buffer;
            if (b != null) {
                buffer = null;
                if (!b.isEmpty()) {
                    downstream.onNext(b);
                }
                downstream.onComplete();
            }
        }
    }

    static final class BufferSkipObserver<T, U extends Collection<? super T>>
    extends AtomicBoolean implements Observer<T>, Disposable {

        private static final long serialVersionUID = -8223395059921494546L;
        final Observer<? super U> downstream;
        final int count;
        final int skip;
        final Callable<U> bufferSupplier;

        Disposable upstream;

        final ArrayDeque<U> buffers;

        long index;

        BufferSkipObserver(Observer<? super U> actual, int count, int skip, Callable<U> bufferSupplier) {
            this.downstream = actual;
            this.count = count;
            this.skip = skip;
            this.bufferSupplier = bufferSupplier;
            this.buffers = new ArrayDeque<U>();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void onNext(T t) {
            if (index++ % skip == 0) {
                U b;

                try {
                    b = ObjectHelper.requireNonNull(bufferSupplier.call(), "The bufferSupplier returned a null collection. Null values are generally not allowed in 2.x operators and sources.");
                } catch (Throwable e) {
                    buffers.clear();
                    upstream.dispose();
                    downstream.onError(e);
                    return;
                }

                buffers.offer(b);
            }

            Iterator<U> it = buffers.iterator();
            while (it.hasNext()) {
                U b = it.next();
                b.add(t);
                if (count <= b.size()) {
                    it.remove();

                    downstream.onNext(b);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            buffers.clear();
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            while (!buffers.isEmpty()) {
                downstream.onNext(buffers.poll());
            }
            downstream.onComplete();
        }
    }
}
