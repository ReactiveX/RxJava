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

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.operators.observable.ObservableTimeoutTimed.TimeoutSupport;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableTimeout<T, U, V> extends AbstractObservableWithUpstream<T, T> {
    final ObservableSource<U> firstTimeoutIndicator;
    final Function<? super T, ? extends ObservableSource<V>> itemTimeoutIndicator;
    final ObservableSource<? extends T> other;

    public ObservableTimeout(
            Observable<T> source,
            ObservableSource<U> firstTimeoutIndicator,
            Function<? super T, ? extends ObservableSource<V>> itemTimeoutIndicator,
            ObservableSource<? extends T> other) {
        super(source);
        this.firstTimeoutIndicator = firstTimeoutIndicator;
        this.itemTimeoutIndicator = itemTimeoutIndicator;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Observer<? super T> s) {
        if (other == null) {
            TimeoutObserver<T> parent = new TimeoutObserver<T>(s, itemTimeoutIndicator);
            s.onSubscribe(parent);
            parent.startFirstTimeout(firstTimeoutIndicator);
            source.subscribe(parent);
        } else {
            TimeoutFallbackObserver<T> parent = new TimeoutFallbackObserver<T>(s, itemTimeoutIndicator, other);
            s.onSubscribe(parent);
            parent.startFirstTimeout(firstTimeoutIndicator);
            source.subscribe(parent);
        }
    }

    interface TimeoutSelectorSupport extends TimeoutSupport {
        void onTimeoutError(long idx, Throwable ex);
    }

    static final class TimeoutObserver<T> extends AtomicLong
    implements Observer<T>, Disposable, TimeoutSelectorSupport {

        private static final long serialVersionUID = 3764492702657003550L;

        final Observer<? super T> actual;

        final Function<? super T, ? extends ObservableSource<?>> itemTimeoutIndicator;

        final SequentialDisposable task;

        final AtomicReference<Disposable> upstream;

        TimeoutObserver(Observer<? super T> actual, Function<? super T, ? extends ObservableSource<?>> itemTimeoutIndicator) {
            this.actual = actual;
            this.itemTimeoutIndicator = itemTimeoutIndicator;
            this.task = new SequentialDisposable();
            this.upstream = new AtomicReference<Disposable>();
        }

        @Override
        public void onSubscribe(Disposable s) {
            DisposableHelper.setOnce(upstream, s);
        }

        @Override
        public void onNext(T t) {
            long idx = get();
            if (idx == Long.MAX_VALUE || !compareAndSet(idx, idx + 1)) {
                return;
            }

            Disposable d = task.get();
            if (d != null) {
                d.dispose();
            }

            actual.onNext(t);

            ObservableSource<?> itemTimeoutObservableSource;

            try {
                itemTimeoutObservableSource = ObjectHelper.requireNonNull(
                        itemTimeoutIndicator.apply(t),
                        "The itemTimeoutIndicator returned a null ObservableSource.");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.get().dispose();
                getAndSet(Long.MAX_VALUE);
                actual.onError(ex);
                return;
            }

            TimeoutConsumer consumer = new TimeoutConsumer(idx + 1, this);
            if (task.replace(consumer)) {
                itemTimeoutObservableSource.subscribe(consumer);
            }
        }

        void startFirstTimeout(ObservableSource<?> firstTimeoutIndicator) {
            if (firstTimeoutIndicator != null) {
                TimeoutConsumer consumer = new TimeoutConsumer(0L, this);
                if (task.replace(consumer)) {
                    firstTimeoutIndicator.subscribe(consumer);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onComplete();
            }
        }

        @Override
        public void onTimeout(long idx) {
            if (compareAndSet(idx, Long.MAX_VALUE)) {
                DisposableHelper.dispose(upstream);

                actual.onError(new TimeoutException());
            }
        }

        @Override
        public void onTimeoutError(long idx, Throwable ex) {
            if (compareAndSet(idx, Long.MAX_VALUE)) {
                DisposableHelper.dispose(upstream);

                actual.onError(ex);
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(upstream);
            task.dispose();
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(upstream.get());
        }
    }

    static final class TimeoutFallbackObserver<T>
    extends AtomicReference<Disposable>
    implements Observer<T>, Disposable, TimeoutSelectorSupport {

        private static final long serialVersionUID = -7508389464265974549L;

        final Observer<? super T> actual;

        final Function<? super T, ? extends ObservableSource<?>> itemTimeoutIndicator;

        final SequentialDisposable task;

        final AtomicLong index;

        final AtomicReference<Disposable> upstream;

        ObservableSource<? extends T> fallback;

        TimeoutFallbackObserver(Observer<? super T> actual,
                Function<? super T, ? extends ObservableSource<?>> itemTimeoutIndicator,
                        ObservableSource<? extends T> fallback) {
            this.actual = actual;
            this.itemTimeoutIndicator = itemTimeoutIndicator;
            this.task = new SequentialDisposable();
            this.fallback = fallback;
            this.index = new AtomicLong();
            this.upstream = new AtomicReference<Disposable>();
        }

        @Override
        public void onSubscribe(Disposable s) {
            DisposableHelper.setOnce(upstream, s);
        }

        @Override
        public void onNext(T t) {
            long idx = index.get();
            if (idx == Long.MAX_VALUE || !index.compareAndSet(idx, idx + 1)) {
                return;
            }

            Disposable d = task.get();
            if (d != null) {
                d.dispose();
            }

            actual.onNext(t);

            ObservableSource<?> itemTimeoutObservableSource;

            try {
                itemTimeoutObservableSource = ObjectHelper.requireNonNull(
                        itemTimeoutIndicator.apply(t),
                        "The itemTimeoutIndicator returned a null ObservableSource.");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.get().dispose();
                index.getAndSet(Long.MAX_VALUE);
                actual.onError(ex);
                return;
            }

            TimeoutConsumer consumer = new TimeoutConsumer(idx + 1, this);
            if (task.replace(consumer)) {
                itemTimeoutObservableSource.subscribe(consumer);
            }
        }

        void startFirstTimeout(ObservableSource<?> firstTimeoutIndicator) {
            if (firstTimeoutIndicator != null) {
                TimeoutConsumer consumer = new TimeoutConsumer(0L, this);
                if (task.replace(consumer)) {
                    firstTimeoutIndicator.subscribe(consumer);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (index.getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onError(t);

                task.dispose();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (index.getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onComplete();

                task.dispose();
            }
        }

        @Override
        public void onTimeout(long idx) {
            if (index.compareAndSet(idx, Long.MAX_VALUE)) {
                DisposableHelper.dispose(upstream);

                ObservableSource<? extends T> f = fallback;
                fallback = null;

                f.subscribe(new ObservableTimeoutTimed.FallbackObserver<T>(actual, this));
            }
        }

        @Override
        public void onTimeoutError(long idx, Throwable ex) {
            if (index.compareAndSet(idx, Long.MAX_VALUE)) {
                DisposableHelper.dispose(this);

                actual.onError(ex);
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(upstream);
            DisposableHelper.dispose(this);
            task.dispose();
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }
    }

    static final class TimeoutConsumer extends AtomicReference<Disposable>
    implements Observer<Object>, Disposable {

        private static final long serialVersionUID = 8708641127342403073L;

        final TimeoutSelectorSupport parent;

        final long idx;

        TimeoutConsumer(long idx, TimeoutSelectorSupport parent) {
            this.idx = idx;
            this.parent = parent;
        }

        @Override
        public void onSubscribe(Disposable s) {
            DisposableHelper.setOnce(this, s);
        }

        @Override
        public void onNext(Object t) {
            Disposable upstream = get();
            if (upstream != DisposableHelper.DISPOSED) {
                upstream.dispose();
                lazySet(DisposableHelper.DISPOSED);
                parent.onTimeout(idx);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (get() != DisposableHelper.DISPOSED) {
                lazySet(DisposableHelper.DISPOSED);
                parent.onTimeoutError(idx, t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (get() != DisposableHelper.DISPOSED) {
                lazySet(DisposableHelper.DISPOSED);
                parent.onTimeout(idx);
            }
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(this.get());
        }
    }

}
