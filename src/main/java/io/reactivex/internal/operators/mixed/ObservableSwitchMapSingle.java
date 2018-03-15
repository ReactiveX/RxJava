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

package io.reactivex.internal.operators.mixed;

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.annotations.Experimental;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.util.AtomicThrowable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Maps the upstream items into {@link SingleSource}s and switches (subscribes) to the newer ones
 * while disposing the older ones and emits the latest success value if available, optionally delaying
 * errors from the main source or the inner sources.
 *
 * @param <T> the upstream value type
 * @param <R> the downstream value type
 * @since 2.1.11 - experimental
 */
@Experimental
public final class ObservableSwitchMapSingle<T, R> extends Observable<R> {

    final Observable<T> source;

    final Function<? super T, ? extends SingleSource<? extends R>> mapper;

    final boolean delayErrors;

    public ObservableSwitchMapSingle(Observable<T> source,
            Function<? super T, ? extends SingleSource<? extends R>> mapper,
            boolean delayErrors) {
        this.source = source;
        this.mapper = mapper;
        this.delayErrors = delayErrors;
    }

    @Override
    protected void subscribeActual(Observer<? super R> s) {
        if (!ScalarXMapZHelper.tryAsSingle(source, mapper, s)) {
            source.subscribe(new SwitchMapSingleMainObserver<T, R>(s, mapper, delayErrors));
        }
    }

    static final class SwitchMapSingleMainObserver<T, R> extends AtomicInteger
    implements Observer<T>, Disposable {

        private static final long serialVersionUID = -5402190102429853762L;

        final Observer<? super R> downstream;

        final Function<? super T, ? extends SingleSource<? extends R>> mapper;

        final boolean delayErrors;

        final AtomicThrowable errors;

        final AtomicReference<SwitchMapSingleObserver<R>> inner;

        static final SwitchMapSingleObserver<Object> INNER_DISPOSED =
                new SwitchMapSingleObserver<Object>(null);

        Disposable upstream;

        volatile boolean done;

        volatile boolean cancelled;

        SwitchMapSingleMainObserver(Observer<? super R> downstream,
                Function<? super T, ? extends SingleSource<? extends R>> mapper,
                boolean delayErrors) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.errors = new AtomicThrowable();
            this.inner = new AtomicReference<SwitchMapSingleObserver<R>>();
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(upstream, s)) {
                upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void onNext(T t) {
            SwitchMapSingleObserver<R> current = inner.get();
            if (current != null) {
                current.dispose();
            }

            SingleSource<? extends R> ss;

            try {
                ss = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null SingleSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.dispose();
                inner.getAndSet((SwitchMapSingleObserver)INNER_DISPOSED);
                onError(ex);
                return;
            }

            SwitchMapSingleObserver<R> observer = new SwitchMapSingleObserver<R>(this);

            for (;;) {
                current = inner.get();
                if (current == INNER_DISPOSED) {
                    break;
                }
                if (inner.compareAndSet(current, observer)) {
                    ss.subscribe(observer);
                    break;
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (errors.addThrowable(t)) {
                if (!delayErrors) {
                    disposeInner();
                }
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        void disposeInner() {
            SwitchMapSingleObserver<R> current = inner.getAndSet((SwitchMapSingleObserver)INNER_DISPOSED);
            if (current != null && current != INNER_DISPOSED) {
                current.dispose();
            }
        }

        @Override
        public void dispose() {
            cancelled = true;
            upstream.dispose();
            disposeInner();
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void innerError(SwitchMapSingleObserver<R> sender, Throwable ex) {
            if (inner.compareAndSet(sender, null)) {
                if (errors.addThrowable(ex)) {
                    if (!delayErrors) {
                        upstream.dispose();
                        disposeInner();
                    }
                    drain();
                    return;
                }
            }
            RxJavaPlugins.onError(ex);
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Observer<? super R> downstream = this.downstream;
            AtomicThrowable errors = this.errors;
            AtomicReference<SwitchMapSingleObserver<R>> inner = this.inner;

            for (;;) {

                for (;;) {
                    if (cancelled) {
                        return;
                    }

                    if (errors.get() != null) {
                        if (!delayErrors) {
                            Throwable ex = errors.terminate();
                            downstream.onError(ex);
                            return;
                        }
                    }

                    boolean d = done;
                    SwitchMapSingleObserver<R> current = inner.get();
                    boolean empty = current == null;

                    if (d && empty) {
                        Throwable ex = errors.terminate();
                        if (ex != null) {
                            downstream.onError(ex);
                        } else {
                            downstream.onComplete();
                        }
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

        static final class SwitchMapSingleObserver<R>
        extends AtomicReference<Disposable> implements SingleObserver<R> {

            private static final long serialVersionUID = 8042919737683345351L;

            final SwitchMapSingleMainObserver<?, R> parent;

            volatile R item;

            SwitchMapSingleObserver(SwitchMapSingleMainObserver<?, R> parent) {
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

            void dispose() {
                DisposableHelper.dispose(this);
            }
        }
    }
}
