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

import java.util.Collection;
import java.util.Objects;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.observers.QueueDrainObserver;
import io.reactivex.rxjava3.internal.queue.MpscLinkedQueue;
import io.reactivex.rxjava3.internal.util.QueueDrainHelper;
import io.reactivex.rxjava3.observers.*;

public final class ObservableBufferExactBoundary<T, U extends Collection<? super T>, B>
extends AbstractObservableWithUpstream<T, U> {
    final ObservableSource<B> boundary;
    final Supplier<U> bufferSupplier;

    public ObservableBufferExactBoundary(ObservableSource<T> source, ObservableSource<B> boundary, Supplier<U> bufferSupplier) {
        super(source);
        this.boundary = boundary;
        this.bufferSupplier = bufferSupplier;
    }

    @Override
    protected void subscribeActual(Observer<? super U> t) {
        source.subscribe(new BufferExactBoundaryObserver<T, U, B>(new SerializedObserver<U>(t), bufferSupplier, boundary));
    }

    static final class BufferExactBoundaryObserver<T, U extends Collection<? super T>, B>
    extends QueueDrainObserver<T, U, U> implements Observer<T>, Disposable {

        final Supplier<U> bufferSupplier;
        final ObservableSource<B> boundary;

        Disposable upstream;

        Disposable other;

        U buffer;

        BufferExactBoundaryObserver(Observer<? super U> actual, Supplier<U> bufferSupplier,
                                             ObservableSource<B> boundary) {
            super(actual, new MpscLinkedQueue<U>());
            this.bufferSupplier = bufferSupplier;
            this.boundary = boundary;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                U b;

                try {
                    b = Objects.requireNonNull(bufferSupplier.get(), "The buffer supplied is null");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancelled = true;
                    d.dispose();
                    EmptyDisposable.error(e, downstream);
                    return;
                }

                buffer = b;

                BufferBoundaryObserver<T, U, B> bs = new BufferBoundaryObserver<T, U, B>(this);
                other = bs;

                downstream.onSubscribe(this);

                if (!cancelled) {
                    boundary.subscribe(bs);
                }
            }
        }

        @Override
        public void onNext(T t) {
            synchronized (this) {
                U b = buffer;
                if (b == null) {
                    return;
                }
                b.add(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            dispose();
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            U b;
            synchronized (this) {
                b = buffer;
                if (b == null) {
                    return;
                }
                buffer = null;
            }
            queue.offer(b);
            done = true;
            if (enter()) {
                QueueDrainHelper.drainLoop(queue, downstream, false, this, this);
            }
        }

        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                other.dispose();
                upstream.dispose();

                if (enter()) {
                    queue.clear();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void next() {

            U next;

            try {
                next = Objects.requireNonNull(bufferSupplier.get(), "The buffer supplied is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                dispose();
                downstream.onError(e);
                return;
            }

            U b;
            synchronized (this) {
                b = buffer;
                if (b == null) {
                    return;
                }
                buffer = next;
            }

            fastPathEmit(b, false, this);
        }

        @Override
        public void accept(Observer<? super U> a, U v) {
            downstream.onNext(v);
        }

    }

    static final class BufferBoundaryObserver<T, U extends Collection<? super T>, B>
    extends DisposableObserver<B> {
        final BufferExactBoundaryObserver<T, U, B> parent;

        BufferBoundaryObserver(BufferExactBoundaryObserver<T, U, B> parent) {
            this.parent = parent;
        }

        @Override
        public void onNext(B t) {
            parent.next();
        }

        @Override
        public void onError(Throwable t) {
            parent.onError(t);
        }

        @Override
        public void onComplete() {
            parent.onComplete();
        }
    }
}
