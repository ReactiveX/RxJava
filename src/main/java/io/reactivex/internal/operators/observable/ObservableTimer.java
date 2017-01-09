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
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;

public final class ObservableTimer extends Observable<Long> {
    final Scheduler scheduler;
    final long delay;
    final TimeUnit unit;
    public ObservableTimer(long delay, TimeUnit unit, Scheduler scheduler) {
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    public void subscribeActual(Observer<? super Long> s) {
        TimerObserver ios = new TimerObserver(s);
        s.onSubscribe(ios);

        Disposable d = scheduler.scheduleDirect(ios, delay, unit);

        ios.setResource(d);
    }

    static final class TimerObserver extends AtomicReference<Disposable>
    implements Disposable, Runnable {

        private static final long serialVersionUID = -2809475196591179431L;

        final Observer<? super Long> actual;

        TimerObserver(Observer<? super Long> actual) {
            this.actual = actual;
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return get() == DisposableHelper.DISPOSED;
        }

        @Override
        public void run() {
            if (!isDisposed()) {
                actual.onNext(0L);
                lazySet(EmptyDisposable.INSTANCE);
                actual.onComplete();
            }
        }

        public void setResource(Disposable d) {
            DisposableHelper.trySet(this, d);
        }
    }
}
