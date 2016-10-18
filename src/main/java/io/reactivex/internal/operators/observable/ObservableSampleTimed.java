/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.observers.SerializedObserver;

public final class ObservableSampleTimed<T> extends AbstractObservableWithUpstream<T, T> {
    final long period;
    final TimeUnit unit;
    final Scheduler scheduler;

    public ObservableSampleTimed(ObservableSource<T> source, long period, TimeUnit unit, Scheduler scheduler) {
        super(source);
        this.period = period;
        this.unit = unit;
        this.scheduler = scheduler;
    }


    @Override
    public void subscribeActual(Observer<? super T> t) {
        SerializedObserver<T> serial = new SerializedObserver<T>(t);
        source.subscribe(new SampleTimedObserver<T>(serial, period, unit, scheduler));
    }

    static final class SampleTimedObserver<T> extends AtomicReference<T> implements Observer<T>, Disposable, Runnable {

        private static final long serialVersionUID = -3517602651313910099L;

        final Observer<? super T> actual;
        final long period;
        final TimeUnit unit;
        final Scheduler scheduler;

        final AtomicReference<Disposable> timer = new AtomicReference<Disposable>();

        Disposable s;

        SampleTimedObserver(Observer<? super T> actual, long period, TimeUnit unit, Scheduler scheduler) {
            this.actual = actual;
            this.period = period;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);

                Disposable d = scheduler.schedulePeriodicallyDirect(this, period, period, unit);
                DisposableHelper.replace(timer, d);
            }
        }

        @Override
        public void onNext(T t) {
            lazySet(t);
        }

        @Override
        public void onError(Throwable t) {
            cancelTimer();
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            cancelTimer();
            actual.onComplete();
        }

        void cancelTimer() {
            DisposableHelper.dispose(timer);
        }

        @Override
        public void dispose() {
            cancelTimer();
            s.dispose();
        }

        @Override public boolean isDisposed() {
            return s.isDisposed();
        }

        @Override
        public void run() {
            T value = getAndSet(null);
            if (value != null) {
                actual.onNext(value);
            }
        }
    }
}
