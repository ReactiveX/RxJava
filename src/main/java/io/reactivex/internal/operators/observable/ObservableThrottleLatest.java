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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * Emits the next or latest item when the given time elapses.
 * <p>
 * The operator emits the next item, then starts a timer. When the timer fires,
 * it tries to emit the latest item from upstream. If there was no upstream item,
 * in the meantime, the next upstream item is emitted immediately and the
 * timed process repeats.
 * <p>History: 2.1.14 - experimental
 * @param <T> the upstream and downstream value type
 * @since 2.2
 */
public final class ObservableThrottleLatest<T> extends AbstractObservableWithUpstream<T, T> {

    final long timeout;

    final TimeUnit unit;

    final Scheduler scheduler;

    final boolean emitLast;

    public ObservableThrottleLatest(Observable<T> source,
            long timeout, TimeUnit unit, Scheduler scheduler,
            boolean emitLast) {
        super(source);
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.emitLast = emitLast;
    }

    @Override
    protected void subscribeActual(Observer<? super T> s) {
        source.subscribe(new ThrottleLatestObserver<T>(s, timeout, unit, scheduler.createWorker(), emitLast));
    }

    static final class ThrottleLatestObserver<T>
    extends AtomicInteger
    implements Observer<T>, Disposable, Runnable {

        private static final long serialVersionUID = -8296689127439125014L;

        final Observer<? super T> downstream;

        final long timeout;

        final TimeUnit unit;

        final Scheduler.Worker worker;

        final boolean emitLast;

        final AtomicReference<T> latest;

        Disposable upstream;

        volatile boolean done;
        Throwable error;

        volatile boolean cancelled;

        volatile boolean timerFired;

        boolean timerRunning;

        ThrottleLatestObserver(Observer<? super T> downstream,
                long timeout, TimeUnit unit, Scheduler.Worker worker,
                boolean emitLast) {
            this.downstream = downstream;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
            this.emitLast = emitLast;
            this.latest = new AtomicReference<T>();
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(upstream, s)) {
                upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            latest.set(t);
            drain();
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @Override
        public void dispose() {
            cancelled = true;
            upstream.dispose();
            worker.dispose();
            if (getAndIncrement() == 0) {
                latest.lazySet(null);
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        @Override
        public void run() {
            timerFired = true;
            drain();
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;

            AtomicReference<T> latest = this.latest;
            Observer<? super T> downstream = this.downstream;

            for (;;) {

                for (;;) {
                    if (cancelled) {
                        latest.lazySet(null);
                        return;
                    }

                    boolean d = done;

                    if (d && error != null) {
                        latest.lazySet(null);
                        downstream.onError(error);
                        worker.dispose();
                        return;
                    }

                    T v = latest.get();
                    boolean empty = v == null;

                    if (d) {
                        v = latest.getAndSet(null);
                        if (!empty && emitLast) {
                            downstream.onNext(v);
                        }
                        downstream.onComplete();
                        worker.dispose();
                        return;
                    }

                    if (empty) {
                        if (timerFired) {
                            timerRunning = false;
                            timerFired = false;
                        }
                        break;
                    }

                    if (!timerRunning || timerFired) {
                        v = latest.getAndSet(null);
                        downstream.onNext(v);

                        timerFired = false;
                        timerRunning = true;
                        worker.schedule(this, timeout, unit);
                    } else {
                        break;
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }
}
