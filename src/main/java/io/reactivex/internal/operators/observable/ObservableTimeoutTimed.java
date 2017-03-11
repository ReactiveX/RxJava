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
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.observers.FullArbiterObserver;
import io.reactivex.observers.SerializedObserver;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableTimeoutTimed<T> extends AbstractObservableWithUpstream<T, T> {
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;
    final ObservableSource<? extends T> other;


    static final Disposable NEW_TIMER = new EmptyDisposable();

    public ObservableTimeoutTimed(ObservableSource<T> source,
            long timeout, TimeUnit unit, Scheduler scheduler, ObservableSource<? extends T> other) {
        super(source);
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.other = other;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        if (other == null) {
            source.subscribe(new TimeoutTimedObserver<T>(
                    new SerializedObserver<T>(t), // because errors can race
                    timeout, unit, scheduler.createWorker()));
        } else {
            source.subscribe(new TimeoutTimedOtherObserver<T>(
                    t, // the FullArbiter serializes
                    timeout, unit, scheduler.createWorker(), other));
        }
    }

    static final class TimeoutTimedOtherObserver<T>
    extends AtomicReference<Disposable> implements Observer<T>, Disposable {
        private static final long serialVersionUID = -4619702551964128179L;

        final Observer<? super T> actual;
        final long timeout;
        final TimeUnit unit;
        final Scheduler.Worker worker;
        final ObservableSource<? extends T> other;

        Disposable s;

        final ObserverFullArbiter<T> arbiter;

        volatile long index;

        volatile boolean done;

        TimeoutTimedOtherObserver(Observer<? super T> actual, long timeout, TimeUnit unit, Worker worker,
                ObservableSource<? extends T> other) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
            this.other = other;
            this.arbiter = new ObserverFullArbiter<T>(actual, this, 8);
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                if (arbiter.setDisposable(s)) {
                    actual.onSubscribe(arbiter);

                    scheduleTimeout(0L);
                }
            }

        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            long idx = index + 1;
            index = idx;

            if (arbiter.onNext(t, s)) {
                scheduleTimeout(idx);
            }
        }

        void scheduleTimeout(final long idx) {
            Disposable d = get();
            if (d != null) {
                d.dispose();
            }

            if (compareAndSet(d, NEW_TIMER)) {
                d = worker.schedule(new SubscribeNext(idx), timeout, unit);

                DisposableHelper.replace(this, d);
            }
        }

        void subscribeNext() {
            other.subscribe(new FullArbiterObserver<T>(arbiter));
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            arbiter.onError(t, s);
            worker.dispose();
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            arbiter.onComplete(s);
            worker.dispose();
        }

        @Override
        public void dispose() {
            s.dispose();
            worker.dispose();
        }

        @Override
        public boolean isDisposed() {
            return worker.isDisposed();
        }

        final class SubscribeNext implements Runnable {
            private final long idx;

            SubscribeNext(long idx) {
                this.idx = idx;
            }

            @Override
            public void run() {
                if (idx == index) {
                    done = true;
                    s.dispose();
                    DisposableHelper.dispose(TimeoutTimedOtherObserver.this);

                    subscribeNext();

                    worker.dispose();
                }
            }
        }
    }

    static final class TimeoutTimedObserver<T>
    extends AtomicReference<Disposable>
    implements Observer<T>, Disposable {
        private static final long serialVersionUID = -8387234228317808253L;

        final Observer<? super T> actual;
        final long timeout;
        final TimeUnit unit;
        final Scheduler.Worker worker;

        Disposable s;

        volatile long index;

        volatile boolean done;

        TimeoutTimedObserver(Observer<? super T> actual, long timeout, TimeUnit unit, Worker worker) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                scheduleTimeout(0L);
            }

        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            long idx = index + 1;
            index = idx;

            actual.onNext(t);

            scheduleTimeout(idx);
        }

        void scheduleTimeout(final long idx) {
            Disposable d = get();
            if (d != null) {
                d.dispose();
            }

            if (compareAndSet(d, NEW_TIMER)) {
                d = worker.schedule(new TimeoutTask(idx), timeout, unit);

                DisposableHelper.replace(this, d);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;

            actual.onError(t);
            dispose();
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;

            actual.onComplete();
            dispose();
        }

        @Override
        public void dispose() {
            s.dispose();
            worker.dispose();
        }

        @Override
        public boolean isDisposed() {
            return worker.isDisposed();
        }

        final class TimeoutTask implements Runnable {
            private final long idx;

            TimeoutTask(long idx) {
                this.idx = idx;
            }

            @Override
            public void run() {
                if (idx == index) {
                    done = true;
                    s.dispose();
                    DisposableHelper.dispose(TimeoutTimedObserver.this);

                    actual.onError(new TimeoutException());

                    worker.dispose();
                }
            }
        }
    }

    static final class EmptyDisposable implements Disposable {
        @Override
        public void dispose() { }

        @Override
        public boolean isDisposed() {
            return true;
        }
    }
}
