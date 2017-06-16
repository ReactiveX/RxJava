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

package io.reactivex.internal.operators.flowable;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.AtomicThrowable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Maps a sequence of values into CompletableSources and awaits their termination.
 * @param <T> the value type
 */
public final class FlowableFlatMapCompletable<T> extends AbstractFlowableWithUpstream<T, T> {

    final Function<? super T, ? extends CompletableSource> mapper;

    final int maxConcurrency;

    final boolean delayErrors;

    public FlowableFlatMapCompletable(Flowable<T> source,
            Function<? super T, ? extends CompletableSource> mapper, boolean delayErrors,
            int maxConcurrency) {
        super(source);
        this.mapper = mapper;
        this.delayErrors = delayErrors;
        this.maxConcurrency = maxConcurrency;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> observer) {
        source.subscribe(new FlatMapCompletableMainSubscriber<T>(observer, mapper, delayErrors, maxConcurrency));
    }

    static final class FlatMapCompletableMainSubscriber<T> extends BasicIntQueueSubscription<T>
    implements FlowableSubscriber<T> {
        private static final long serialVersionUID = 8443155186132538303L;

        final Subscriber<? super T> actual;

        final AtomicThrowable errors;

        final Function<? super T, ? extends CompletableSource> mapper;

        final boolean delayErrors;

        final CompositeDisposable set;

        final int maxConcurrency;

        Subscription s;

        volatile boolean cancelled;

        FlatMapCompletableMainSubscriber(Subscriber<? super T> observer,
                Function<? super T, ? extends CompletableSource> mapper, boolean delayErrors,
                int maxConcurrency) {
            this.actual = observer;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.errors = new AtomicThrowable();
            this.set = new CompositeDisposable();
            this.maxConcurrency = maxConcurrency;
            this.lazySet(1);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);

                int m = maxConcurrency;
                if (m == Integer.MAX_VALUE) {
                    s.request(Long.MAX_VALUE);
                } else {
                    s.request(m);
                }
            }
        }

        @Override
        public void onNext(T value) {
            CompletableSource cs;

            try {
                cs = ObjectHelper.requireNonNull(mapper.apply(value), "The mapper returned a null CompletableSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.cancel();
                onError(ex);
                return;
            }

            getAndIncrement();

            InnerConsumer inner = new InnerConsumer();

            if (!cancelled && set.add(inner)) {
                cs.subscribe(inner);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (errors.addThrowable(e)) {
                if (delayErrors) {
                    if (decrementAndGet() == 0) {
                        Throwable ex = errors.terminate();
                        actual.onError(ex);
                    } else {
                        if (maxConcurrency != Integer.MAX_VALUE) {
                            s.request(1);
                        }
                    }
                } else {
                    cancel();
                    if (getAndSet(0) > 0) {
                        Throwable ex = errors.terminate();
                        actual.onError(ex);
                    }
                }
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (decrementAndGet() == 0) {
                Throwable ex = errors.terminate();
                if (ex != null) {
                    actual.onError(ex);
                } else {
                    actual.onComplete();
                }
            } else {
                if (maxConcurrency != Integer.MAX_VALUE) {
                    s.request(1);
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            s.cancel();
            set.dispose();
        }

        @Override
        public void request(long n) {
            // ignored, no values emitted
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            return null; // always empty
        }

        @Override
        public boolean isEmpty() {
            return true; // always empty
        }

        @Override
        public void clear() {
            // nothing to clear
        }

        @Override
        public int requestFusion(int mode) {
            return mode & ASYNC;
        }

        void innerComplete(InnerConsumer inner) {
            set.delete(inner);
            onComplete();
        }

        void innerError(InnerConsumer inner, Throwable e) {
            set.delete(inner);
            onError(e);
        }

        final class InnerConsumer extends AtomicReference<Disposable> implements CompletableObserver, Disposable {
            private static final long serialVersionUID = 8606673141535671828L;

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onComplete() {
                innerComplete(this);
            }

            @Override
            public void onError(Throwable e) {
                innerError(this, e);
            }

            @Override
            public void dispose() {
                DisposableHelper.dispose(this);
            }

            @Override
            public boolean isDisposed() {
                return DisposableHelper.isDisposed(get());
            }
        }
    }
}
