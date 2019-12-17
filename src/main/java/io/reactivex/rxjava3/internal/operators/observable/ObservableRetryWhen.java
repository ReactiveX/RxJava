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

import java.util.Objects;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.subjects.*;

/**
 * Repeatedly subscribe to a source if a handler ObservableSource signals an item.
 *
 * @param <T> the value type
 */
public final class ObservableRetryWhen<T> extends AbstractObservableWithUpstream<T, T> {

    final Function<? super Observable<Throwable>, ? extends ObservableSource<?>> handler;

    public ObservableRetryWhen(ObservableSource<T> source, Function<? super Observable<Throwable>, ? extends ObservableSource<?>> handler) {
        super(source);
        this.handler = handler;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        Subject<Throwable> signaller = PublishSubject.<Throwable>create().toSerialized();

        ObservableSource<?> other;

        try {
            other = Objects.requireNonNull(handler.apply(signaller), "The handler returned a null ObservableSource");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, observer);
            return;
        }

        RepeatWhenObserver<T> parent = new RepeatWhenObserver<T>(observer, signaller, source);
        observer.onSubscribe(parent);

        other.subscribe(parent.inner);

        parent.subscribeNext();
    }

    static final class RepeatWhenObserver<T> extends AtomicInteger implements Observer<T>, Disposable {

        private static final long serialVersionUID = 802743776666017014L;

        final Observer<? super T> downstream;

        final AtomicInteger wip;

        final AtomicThrowable error;

        final Subject<Throwable> signaller;

        final InnerRepeatObserver inner;

        final AtomicReference<Disposable> upstream;

        final ObservableSource<T> source;

        volatile boolean active;

        RepeatWhenObserver(Observer<? super T> actual, Subject<Throwable> signaller, ObservableSource<T> source) {
            this.downstream = actual;
            this.signaller = signaller;
            this.source = source;
            this.wip = new AtomicInteger();
            this.error = new AtomicThrowable();
            this.inner = new InnerRepeatObserver();
            this.upstream = new AtomicReference<Disposable>();
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.replace(this.upstream, d);
        }

        @Override
        public void onNext(T t) {
            HalfSerializer.onNext(downstream, t, this, error);
        }

        @Override
        public void onError(Throwable e) {
            DisposableHelper.replace(upstream, null);
            active = false;
            signaller.onNext(e);
        }

        @Override
        public void onComplete() {
            DisposableHelper.dispose(inner);
            HalfSerializer.onComplete(downstream, this, error);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(upstream.get());
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(upstream);
            DisposableHelper.dispose(inner);
        }

        void innerNext() {
            subscribeNext();
        }

        void innerError(Throwable ex) {
            DisposableHelper.dispose(upstream);
            HalfSerializer.onError(downstream, ex, this, error);
        }

        void innerComplete() {
            DisposableHelper.dispose(upstream);
            HalfSerializer.onComplete(downstream, this, error);
        }

        void subscribeNext() {
            if (wip.getAndIncrement() == 0) {

                do {
                    if (isDisposed()) {
                        return;
                    }

                    if (!active) {
                        active = true;
                        source.subscribe(this);
                    }
                } while (wip.decrementAndGet() != 0);
            }
        }

        final class InnerRepeatObserver extends AtomicReference<Disposable> implements Observer<Object> {

            private static final long serialVersionUID = 3254781284376480842L;

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onNext(Object t) {
                innerNext();
            }

            @Override
            public void onError(Throwable e) {
                innerError(e);
            }

            @Override
            public void onComplete() {
                innerComplete();
            }
        }
    }
}
