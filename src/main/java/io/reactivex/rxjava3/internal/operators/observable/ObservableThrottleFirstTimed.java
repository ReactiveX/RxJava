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

package io.reactivex.rxjava3.internal.operators.observable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.observers.SerializedObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class ObservableThrottleFirstTimed<T> extends AbstractObservableWithUpstream<T, T> {
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;

    public ObservableThrottleFirstTimed(ObservableSource<T> source,
            long timeout, TimeUnit unit, Scheduler scheduler) {
        super(source);
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new DebounceTimedObserver<T>(
                new SerializedObserver<T>(t),
                timeout, unit, scheduler.createWorker()));
    }

    static final class DebounceTimedObserver<T>
    extends AtomicReference<Disposable>
    implements Observer<T>, Disposable, Runnable {
        private static final long serialVersionUID = 786994795061867455L;

        final Observer<? super T> downstream;
        final long timeout;
        final TimeUnit unit;
        final Scheduler.Worker worker;

        Disposable upstream;

        volatile boolean gate;

        boolean done;

        DebounceTimedObserver(Observer<? super T> actual, long timeout, TimeUnit unit, Worker worker) {
            this.downstream = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!gate && !done) {
                gate = true;

                downstream.onNext(t);

                Disposable d = get();
                if (d != null) {
                    d.dispose();
                }
                DisposableHelper.replace(this, worker.schedule(this, timeout, unit));
            }
        }

        @Override
        public void run() {
            gate = false;
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
            } else {
                done = true;
                downstream.onError(t);
                worker.dispose();
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                downstream.onComplete();
                worker.dispose();
            }
        }

        @Override
        public void dispose() {
            upstream.dispose();
            worker.dispose();
        }

        @Override
        public boolean isDisposed() {
            return worker.isDisposed();
        }
    }
}
