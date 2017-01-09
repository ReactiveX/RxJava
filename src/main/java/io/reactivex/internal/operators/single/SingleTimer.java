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

package io.reactivex.internal.operators.single;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * Signals a {@code 0L} after the specified delay.
 */
public final class SingleTimer extends Single<Long> {

    final long delay;
    final TimeUnit unit;
    final Scheduler scheduler;

    public SingleTimer(long delay, TimeUnit unit, Scheduler scheduler) {
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super Long> s) {
        TimerDisposable parent = new TimerDisposable(s);
        s.onSubscribe(parent);
        parent.setFuture(scheduler.scheduleDirect(parent, delay, unit));
    }

    static final class TimerDisposable extends AtomicReference<Disposable> implements Disposable, Runnable {

        private static final long serialVersionUID = 8465401857522493082L;
        final SingleObserver<? super Long> actual;

        TimerDisposable(final SingleObserver<? super Long> actual) {
            this.actual = actual;
        }

        @Override
        public void run() {
            actual.onSuccess(0L);
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        void setFuture(Disposable d) {
            DisposableHelper.replace(this, d);
        }
    }
}
