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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableTimeoutTimed<T> extends AbstractObservableWithUpstream<T, T> {
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;
    final ObservableSource<? extends T> other;

    public ObservableTimeoutTimed(Observable<T> source,
            long timeout, TimeUnit unit, Scheduler scheduler, ObservableSource<? extends T> other) {
        super(source);
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Observer<? super T> s) {
        if (other == null) {
            TimeoutObserver<T> parent = new TimeoutObserver<T>(s, timeout, unit, scheduler.createWorker());
            s.onSubscribe(parent);
            parent.startTimeout(0L);
            source.subscribe(parent);
        } else {
            TimeoutFallbackObserver<T> parent = new TimeoutFallbackObserver<T>(s, timeout, unit, scheduler.createWorker(), other);
            s.onSubscribe(parent);
            parent.startTimeout(0L);
            source.subscribe(parent);
        }
    }

    static final class TimeoutObserver<T> extends AtomicLong
    implements Observer<T>, Disposable, TimeoutSupport {

        private static final long serialVersionUID = 3764492702657003550L;

        final Observer<? super T> actual;

        final long timeout;

        final TimeUnit unit;

        final Scheduler.Worker worker;

        final SequentialDisposable task;

        final AtomicReference<Disposable> upstream;

        TimeoutObserver(Observer<? super T> actual, long timeout, TimeUnit unit, Scheduler.Worker worker) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
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

            task.get().dispose();

            actual.onNext(t);

            startTimeout(idx + 1);
        }

        void startTimeout(long nextIndex) {
            task.replace(worker.schedule(new TimeoutTask(nextIndex, this), timeout, unit));
        }

        @Override
        public void onError(Throwable t) {
            if (getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onError(t);

                worker.dispose();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onComplete();

                worker.dispose();
            }
        }

        @Override
        public void onTimeout(long idx) {
            if (compareAndSet(idx, Long.MAX_VALUE)) {
                DisposableHelper.dispose(upstream);

                actual.onError(new TimeoutException());

                worker.dispose();
            }
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(upstream);
            worker.dispose();
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(upstream.get());
        }
    }


    static final class TimeoutTask implements Runnable {

        final TimeoutSupport parent;

        final long idx;

        TimeoutTask(long idx, TimeoutSupport parent) {
            this.idx = idx;
            this.parent = parent;
        }

        @Override
        public void run() {
            parent.onTimeout(idx);
        }
    }

    static final class TimeoutFallbackObserver<T> extends AtomicReference<Disposable>
    implements Observer<T>, Disposable, TimeoutSupport {

        private static final long serialVersionUID = 3764492702657003550L;

        final Observer<? super T> actual;

        final long timeout;

        final TimeUnit unit;

        final Scheduler.Worker worker;

        final SequentialDisposable task;

        final AtomicLong index;

        final AtomicReference<Disposable> upstream;

        ObservableSource<? extends T> fallback;

        TimeoutFallbackObserver(Observer<? super T> actual, long timeout, TimeUnit unit,
                Scheduler.Worker worker, ObservableSource<? extends T> fallback) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
            this.fallback = fallback;
            this.task = new SequentialDisposable();
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

            task.get().dispose();

            actual.onNext(t);

            startTimeout(idx + 1);
        }

        void startTimeout(long nextIndex) {
            task.replace(worker.schedule(new TimeoutTask(nextIndex, this), timeout, unit));
        }

        @Override
        public void onError(Throwable t) {
            if (index.getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onError(t);

                worker.dispose();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (index.getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onComplete();

                worker.dispose();
            }
        }

        @Override
        public void onTimeout(long idx) {
            if (index.compareAndSet(idx, Long.MAX_VALUE)) {
                DisposableHelper.dispose(upstream);

                ObservableSource<? extends T> f = fallback;
                fallback = null;

                f.subscribe(new FallbackObserver<T>(actual, this));

                worker.dispose();
            }
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(upstream);
            DisposableHelper.dispose(this);
            worker.dispose();
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }
    }

    static final class FallbackObserver<T> implements Observer<T> {

        final Observer<? super T> actual;

        final AtomicReference<Disposable> arbiter;

        FallbackObserver(Observer<? super T> actual, AtomicReference<Disposable> arbiter) {
            this.actual = actual;
            this.arbiter = arbiter;
        }

        @Override
        public void onSubscribe(Disposable s) {
            DisposableHelper.replace(arbiter, s);
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }
    }

    interface TimeoutSupport {

        void onTimeout(long idx);

    }
}
