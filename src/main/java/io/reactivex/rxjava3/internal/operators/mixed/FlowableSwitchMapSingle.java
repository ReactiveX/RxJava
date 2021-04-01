/*
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

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Maps the upstream items into {@link SingleSource}s and switches (subscribes) to the newer ones
 * while disposing the older ones and emits the latest success value, optionally delaying
 * errors from the main source or the inner sources.
 * <p>History: 2.1.11 - experimental
 * @param <T> the upstream value type
 * @param <R> the downstream value type
 * @since 2.2
 */
public final class FlowableSwitchMapSingle<T, R> extends Flowable<R> {

    final Flowable<T> source;

    final Function<? super T, ? extends SingleSource<? extends R>> mapper;

    final boolean delayErrors;

    public FlowableSwitchMapSingle(Flowable<T> source,
            Function<? super T, ? extends SingleSource<? extends R>> mapper,
            boolean delayErrors) {
        this.source = source;
        this.mapper = mapper;
        this.delayErrors = delayErrors;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new SwitchMapSingleSubscriber<>(s, mapper, delayErrors));
    }

    static final class SwitchMapSingleSubscriber<T, R> extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -5402190102429853762L;

        final Subscriber<? super R> downstream;

        final Function<? super T, ? extends SingleSource<? extends R>> mapper;

        final boolean delayErrors;

        final AtomicThrowable errors;

        final AtomicLong requested;

        final AtomicReference<SwitchMapSingleObserver<R>> inner;

        static final SwitchMapSingleObserver<Object> INNER_DISPOSED =
                new SwitchMapSingleObserver<>(null);

        Subscription upstream;

        volatile boolean done;

        volatile boolean cancelled;

        long emitted;

        SwitchMapSingleSubscriber(Subscriber<? super R> downstream,
                Function<? super T, ? extends SingleSource<? extends R>> mapper,
                boolean delayErrors) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.errors = new AtomicThrowable();
            this.requested = new AtomicLong();
            this.inner = new AtomicReference<>();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(upstream, s)) {
                upstream = s;
                downstream.onSubscribe(this);
                s.request(Long.MAX_VALUE);
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
                ss = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null SingleSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.cancel();
                inner.getAndSet((SwitchMapSingleObserver)INNER_DISPOSED);
                onError(ex);
                return;
            }

            SwitchMapSingleObserver<R> observer = new SwitchMapSingleObserver<>(this);

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
            SwitchMapSingleObserver<R> current = inner.getAndSet((SwitchMapSingleObserver)INNER_DISPOSED);
            if (current != null && current != INNER_DISPOSED) {
                current.dispose();
            }
        }

        @Override
        public void request(long n) {
            BackpressureHelper.add(requested, n);
            drain();
        }

        @Override
        public void cancel() {
            cancelled = true;
            upstream.cancel();
            disposeInner();
            errors.tryTerminateAndReport();
        }

        void innerError(SwitchMapSingleObserver<R> sender, Throwable ex) {
            if (inner.compareAndSet(sender, null)) {
                if (errors.tryAddThrowableOrReport(ex)) {
                    if (!delayErrors) {
                        upstream.cancel();
                        disposeInner();
                    }
                    drain();
                }
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Subscriber<? super R> downstream = this.downstream;
            AtomicThrowable errors = this.errors;
            AtomicReference<SwitchMapSingleObserver<R>> inner = this.inner;
            AtomicLong requested = this.requested;
            long emitted = this.emitted;

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
                    SwitchMapSingleObserver<R> current = inner.get();
                    boolean empty = current == null;

                    if (d && empty) {
                        errors.tryTerminateConsumer(downstream);
                        return;
                    }

                    if (empty || current.item == null || emitted == requested.get()) {
                        break;
                    }

                    inner.compareAndSet(current, null);

                    downstream.onNext(current.item);

                    emitted++;
                }

                this.emitted = emitted;
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        static final class SwitchMapSingleObserver<R>
        extends AtomicReference<Disposable> implements SingleObserver<R> {

            private static final long serialVersionUID = 8042919737683345351L;

            final SwitchMapSingleSubscriber<?, R> parent;

            volatile R item;

            SwitchMapSingleObserver(SwitchMapSingleSubscriber<?, R> parent) {
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
