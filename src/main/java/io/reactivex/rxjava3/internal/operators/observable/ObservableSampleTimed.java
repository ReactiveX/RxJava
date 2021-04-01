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

package io.reactivex.rxjava3.internal.operators.observable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.observers.SerializedObserver;

public final class ObservableSampleTimed<T> extends AbstractObservableWithUpstream<T, T> {
    final long period;
    final TimeUnit unit;
    final Scheduler scheduler;

    final boolean emitLast;

    public ObservableSampleTimed(ObservableSource<T> source, long period, TimeUnit unit, Scheduler scheduler, boolean emitLast) {
        super(source);
        this.period = period;
        this.unit = unit;
        this.scheduler = scheduler;
        this.emitLast = emitLast;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        SerializedObserver<T> serial = new SerializedObserver<>(t);
        if (emitLast) {
            source.subscribe(new SampleTimedEmitLast<>(serial, period, unit, scheduler));
        } else {
            source.subscribe(new SampleTimedNoLast<>(serial, period, unit, scheduler));
        }
    }

    abstract static class SampleTimedObserver<T> extends AtomicReference<T> implements Observer<T>, Disposable, Runnable {

        private static final long serialVersionUID = -3517602651313910099L;

        final Observer<? super T> downstream;
        final long period;
        final TimeUnit unit;
        final Scheduler scheduler;

        final AtomicReference<Disposable> timer = new AtomicReference<>();

        Disposable upstream;

        SampleTimedObserver(Observer<? super T> actual, long period, TimeUnit unit, Scheduler scheduler) {
            this.downstream = actual;
            this.period = period;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);

                Disposable task = scheduler.schedulePeriodicallyDirect(this, period, period, unit);
                DisposableHelper.replace(timer, task);
            }
        }

        @Override
        public void onNext(T t) {
            lazySet(t);
        }

        @Override
        public void onError(Throwable t) {
            cancelTimer();
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            cancelTimer();
            complete();
        }

        void cancelTimer() {
            DisposableHelper.dispose(timer);
        }

        @Override
        public void dispose() {
            cancelTimer();
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        void emit() {
            T value = getAndSet(null);
            if (value != null) {
                downstream.onNext(value);
            }
        }

        abstract void complete();
    }

    static final class SampleTimedNoLast<T> extends SampleTimedObserver<T> {

        private static final long serialVersionUID = -7139995637533111443L;

        SampleTimedNoLast(Observer<? super T> actual, long period, TimeUnit unit, Scheduler scheduler) {
            super(actual, period, unit, scheduler);
        }

        @Override
        void complete() {
            downstream.onComplete();
        }

        @Override
        public void run() {
            emit();
        }
    }

    static final class SampleTimedEmitLast<T> extends SampleTimedObserver<T> {

        private static final long serialVersionUID = -7139995637533111443L;

        final AtomicInteger wip;

        SampleTimedEmitLast(Observer<? super T> actual, long period, TimeUnit unit, Scheduler scheduler) {
            super(actual, period, unit, scheduler);
            this.wip = new AtomicInteger(1);
        }

        @Override
        void complete() {
            emit();
            if (wip.decrementAndGet() == 0) {
                downstream.onComplete();
            }
        }

        @Override
        public void run() {
            if (wip.incrementAndGet() == 2) {
                emit();
                if (wip.decrementAndGet() == 0) {
                    downstream.onComplete();
                }
            }
        }
    }
}
