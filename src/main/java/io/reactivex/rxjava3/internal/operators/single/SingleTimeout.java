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

package io.reactivex.rxjava3.internal.operators.single;

import static io.reactivex.rxjava3.internal.util.ExceptionHelper.timeoutMessage;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class SingleTimeout<T> extends Single<T> {

    final SingleSource<T> source;

    final long timeout;

    final TimeUnit unit;

    final Scheduler scheduler;

    final SingleSource<? extends T> other;

    public SingleTimeout(SingleSource<T> source, long timeout, TimeUnit unit, Scheduler scheduler,
                         SingleSource<? extends T> other) {
        this.source = source;
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.other = other;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> observer) {

        TimeoutMainObserver<T> parent = new TimeoutMainObserver<T>(observer, other, timeout, unit);
        observer.onSubscribe(parent);

        DisposableHelper.replace(parent.task, scheduler.scheduleDirect(parent, timeout, unit));

        source.subscribe(parent);
    }

    static final class TimeoutMainObserver<T> extends AtomicReference<Disposable>
    implements SingleObserver<T>, Runnable, Disposable {

        private static final long serialVersionUID = 37497744973048446L;

        final SingleObserver<? super T> downstream;

        final AtomicReference<Disposable> task;

        final TimeoutFallbackObserver<T> fallback;

        SingleSource<? extends T> other;

        final long timeout;

        final TimeUnit unit;

        static final class TimeoutFallbackObserver<T> extends AtomicReference<Disposable>
        implements SingleObserver<T> {

            private static final long serialVersionUID = 2071387740092105509L;
            final SingleObserver<? super T> downstream;

            TimeoutFallbackObserver(SingleObserver<? super T> downstream) {
                this.downstream = downstream;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onSuccess(T t) {
                downstream.onSuccess(t);
            }

            @Override
            public void onError(Throwable e) {
                downstream.onError(e);
            }
        }

        TimeoutMainObserver(SingleObserver<? super T> actual, SingleSource<? extends T> other, long timeout, TimeUnit unit) {
            this.downstream = actual;
            this.other = other;
            this.timeout = timeout;
            this.unit = unit;
            this.task = new AtomicReference<Disposable>();
            if (other != null) {
                this.fallback = new TimeoutFallbackObserver<T>(actual);
            } else {
                this.fallback = null;
            }
        }

        @Override
        public void run() {
            Disposable d = get();
            if (d != DisposableHelper.DISPOSED && compareAndSet(d, DisposableHelper.DISPOSED)) {
                if (d != null) {
                    d.dispose();
                }
                SingleSource<? extends T> other = this.other;
                if (other == null) {
                    downstream.onError(new TimeoutException(timeoutMessage(timeout, unit)));
                } else {
                    this.other = null;
                    other.subscribe(fallback);
                }
            }
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }

        @Override
        public void onSuccess(T t) {
            Disposable d = get();
            if (d != DisposableHelper.DISPOSED && compareAndSet(d, DisposableHelper.DISPOSED)) {
                DisposableHelper.dispose(task);
                downstream.onSuccess(t);
            }
        }

        @Override
        public void onError(Throwable e) {
            Disposable d = get();
            if (d != DisposableHelper.DISPOSED && compareAndSet(d, DisposableHelper.DISPOSED)) {
                DisposableHelper.dispose(task);
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
            DisposableHelper.dispose(task);
            if (fallback != null) {
                DisposableHelper.dispose(fallback);
            }
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }
    }
}
