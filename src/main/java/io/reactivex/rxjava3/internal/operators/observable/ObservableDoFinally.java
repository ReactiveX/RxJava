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

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.fuseable.QueueDisposable;
import io.reactivex.rxjava3.internal.observers.BasicIntQueueDisposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Execute an action after an onError, onComplete or a dispose event.
 * <p>History: 2.0.1 - experimental
 * @param <T> the value type
 * @since 2.1
 */
public final class ObservableDoFinally<T> extends AbstractObservableWithUpstream<T, T> {

    final Action onFinally;

    public ObservableDoFinally(ObservableSource<T> source, Action onFinally) {
        super(source);
        this.onFinally = onFinally;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new DoFinallyObserver<T>(observer, onFinally));
    }

    static final class DoFinallyObserver<T> extends BasicIntQueueDisposable<T> implements Observer<T> {

        private static final long serialVersionUID = 4109457741734051389L;

        final Observer<? super T> downstream;

        final Action onFinally;

        Disposable upstream;

        QueueDisposable<T> qd;

        boolean syncFused;

        DoFinallyObserver(Observer<? super T> actual, Action onFinally) {
            this.downstream = actual;
            this.onFinally = onFinally;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                if (d instanceof QueueDisposable) {
                    this.qd = (QueueDisposable<T>)d;
                }

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
            runFinally();
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
            runFinally();
        }

        @Override
        public void dispose() {
            upstream.dispose();
            runFinally();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public int requestFusion(int mode) {
            QueueDisposable<T> qd = this.qd;
            if (qd != null && (mode & BOUNDARY) == 0) {
                int m = qd.requestFusion(mode);
                if (m != NONE) {
                    syncFused = m == SYNC;
                }
                return m;
            }
            return NONE;
        }

        @Override
        public void clear() {
            qd.clear();
        }

        @Override
        public boolean isEmpty() {
            return qd.isEmpty();
        }

        @Nullable
        @Override
        public T poll() throws Throwable {
            T v = qd.poll();
            if (v == null && syncFused) {
                runFinally();
            }
            return v;
        }

        void runFinally() {
            if (compareAndSet(0, 1)) {
                try {
                    onFinally.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaPlugins.onError(ex);
                }
            }
        }
    }
}
