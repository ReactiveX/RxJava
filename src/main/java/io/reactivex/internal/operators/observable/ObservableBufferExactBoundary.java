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
import java.util.Collection;
import java.util.concurrent.Callable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.observers.QueueDrainObserver;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.util.QueueDrainHelper;
import io.reactivex.observers.*;

public final class ObservableBufferExactBoundary<T, U extends Collection<? super T>, B>
extends AbstractObservableWithUpstream<T, U> {
    final ObservableSource<B> boundary;
    final Callable<U> bufferSupplier;

    public ObservableBufferExactBoundary(ObservableSource<T> source, ObservableSource<B> boundary, Callable<U> bufferSupplier) {
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

        final Callable<U> bufferSupplier;
        final ObservableSource<B> boundary;

        Disposable s;

        Disposable other;

        U buffer;

        BufferExactBoundaryObserver(Observer<? super U> actual, Callable<U> bufferSupplier,
                                             ObservableSource<B> boundary) {
            super(actual, new MpscLinkedQueue<U>());
            this.bufferSupplier = bufferSupplier;
            this.boundary = boundary;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;

                U b;

                try {
                    b = ObjectHelper.requireNonNull(bufferSupplier.call(), "The buffer supplied is null");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancelled = true;
                    s.dispose();
                    EmptyDisposable.error(e, actual);
                    return;
                }

                buffer = b;

                BufferBoundaryObserver<T, U, B> bs = new BufferBoundaryObserver<T, U, B>(this);
                other = bs;

                actual.onSubscribe(this);

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
            actual.onError(t);
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
                QueueDrainHelper.drainLoop(queue, actual, false, this, this);
            }
        }

        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                other.dispose();
                s.dispose();

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
                next = ObjectHelper.requireNonNull(bufferSupplier.call(), "The buffer supplied is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                dispose();
                actual.onError(e);
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
            actual.onNext(v);
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
