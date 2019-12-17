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

package io.reactivex.rxjava3.internal.operators.mixed;

import java.util.Objects;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.util.AtomicThrowable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Maps the upstream items into {@link MaybeSource}s and switches (subscribes) to the newer ones
 * while disposing the older ones and emits the latest success value if available, optionally delaying
 * errors from the main source or the inner sources.
 * <p>History: 2.1.11 - experimental
 * @param <T> the upstream value type
 * @param <R> the downstream value type
 * @since 2.2
 */
public final class ObservableSwitchMapMaybe<T, R> extends Observable<R> {

    final Observable<T> source;

    final Function<? super T, ? extends MaybeSource<? extends R>> mapper;

    final boolean delayErrors;

    public ObservableSwitchMapMaybe(Observable<T> source,
            Function<? super T, ? extends MaybeSource<? extends R>> mapper,
            boolean delayErrors) {
        this.source = source;
        this.mapper = mapper;
        this.delayErrors = delayErrors;
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) {
        if (!ScalarXMapZHelper.tryAsMaybe(source, mapper, observer)) {
            source.subscribe(new SwitchMapMaybeMainObserver<T, R>(observer, mapper, delayErrors));
        }
    }

    static final class SwitchMapMaybeMainObserver<T, R> extends AtomicInteger
    implements Observer<T>, Disposable {

        private static final long serialVersionUID = -5402190102429853762L;

        final Observer<? super R> downstream;

        final Function<? super T, ? extends MaybeSource<? extends R>> mapper;

        final boolean delayErrors;

        final AtomicThrowable errors;

        final AtomicReference<SwitchMapMaybeObserver<R>> inner;

        static final SwitchMapMaybeObserver<Object> INNER_DISPOSED =
                new SwitchMapMaybeObserver<Object>(null);

        Disposable upstream;

        volatile boolean done;

        volatile boolean cancelled;

        SwitchMapMaybeMainObserver(Observer<? super R> downstream,
                Function<? super T, ? extends MaybeSource<? extends R>> mapper,
                boolean delayErrors) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.errors = new AtomicThrowable();
            this.inner = new AtomicReference<SwitchMapMaybeObserver<R>>();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(upstream, d)) {
                upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void onNext(T t) {
            SwitchMapMaybeObserver<R> current = inner.get();
            if (current != null) {
                current.dispose();
            }

            MaybeSource<? extends R> ms;

            try {
                ms = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null MaybeSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.dispose();
                inner.getAndSet((SwitchMapMaybeObserver)INNER_DISPOSED);
                onError(ex);
                return;
            }

            SwitchMapMaybeObserver<R> observer = new SwitchMapMaybeObserver<R>(this);

            for (;;) {
                current = inner.get();
                if (current == INNER_DISPOSED) {
                    break;
                }
                if (inner.compareAndSet(current, observer)) {
                    ms.subscribe(observer);
                    break;
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (errors.tryAddThrowableOrReport(t)) {
                if (!delayErrors) {
                    disposeInner();
                }
                done = true;
                drain();
            }
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        void disposeInner() {
            SwitchMapMaybeObserver<R> current = inner.getAndSet((SwitchMapMaybeObserver)INNER_DISPOSED);
            if (current != null && current != INNER_DISPOSED) {
                current.dispose();
            }
        }

        @Override
        public void dispose() {
            cancelled = true;
            upstream.dispose();
            disposeInner();
            errors.tryTerminateAndReport();
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void innerError(SwitchMapMaybeObserver<R> sender, Throwable ex) {
            if (inner.compareAndSet(sender, null)) {
                if (errors.tryAddThrowableOrReport(ex)) {
                    if (!delayErrors) {
                        upstream.dispose();
                        disposeInner();
                    }
                    drain();
                }
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        void innerComplete(SwitchMapMaybeObserver<R> sender) {
            if (inner.compareAndSet(sender, null)) {
                drain();
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Observer<? super R> downstream = this.downstream;
            AtomicThrowable errors = this.errors;
            AtomicReference<SwitchMapMaybeObserver<R>> inner = this.inner;

            for (;;) {

                for (;;) {
                    if (cancelled) {
                        return;
                    }

                    if (errors.get() != null) {
                        if (!delayErrors) {
                            errors.tryTerminateConsumer(downstream);
                            return;
                        }
                    }

                    boolean d = done;
                    SwitchMapMaybeObserver<R> current = inner.get();
                    boolean empty = current == null;

                    if (d && empty) {
                        errors.tryTerminateConsumer(downstream);
                        return;
                    }

                    if (empty || current.item == null) {
                        break;
                    }

                    inner.compareAndSet(current, null);

                    downstream.onNext(current.item);
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        static final class SwitchMapMaybeObserver<R>
        extends AtomicReference<Disposable> implements MaybeObserver<R> {

            private static final long serialVersionUID = 8042919737683345351L;

            final SwitchMapMaybeMainObserver<?, R> parent;

            volatile R item;

            SwitchMapMaybeObserver(SwitchMapMaybeMainObserver<?, R> parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onSuccess(R t) {
                item = t;
                parent.drain();
            }

            @Override
            public void onError(Throwable e) {
                parent.innerError(this, e);
            }

            @Override
            public void onComplete() {
                parent.innerComplete(this);
            }

            void dispose() {
                DisposableHelper.dispose(this);
            }
        }
    }
}
