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

package io.reactivex.internal.operators.completable;

import java.util.concurrent.TimeUnit;

import io.reactivex.*;
import io.reactivex.disposables.*;

public final class CompletableDelay extends Completable {

    final CompletableSource source;

    final long delay;

    final TimeUnit unit;

    final Scheduler scheduler;

    final boolean delayError;

    public CompletableDelay(CompletableSource source, long delay, TimeUnit unit, Scheduler scheduler, boolean delayError) {
        this.source = source;
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
        this.delayError = delayError;
    }

    @Override
    protected void subscribeActual(final CompletableObserver s) {
        final CompositeDisposable set = new CompositeDisposable();

        source.subscribe(new Delay(set, s));
    }

    final class Delay implements CompletableObserver {

        private final CompositeDisposable set;
        final CompletableObserver s;

        Delay(CompositeDisposable set, CompletableObserver s) {
            this.set = set;
            this.s = s;
        }

        @Override
        public void onComplete() {
            set.add(scheduler.scheduleDirect(new OnComplete(), delay, unit));
        }

        @Override
        public void onError(final Throwable e) {
            set.add(scheduler.scheduleDirect(new OnError(e), delayError ? delay : 0, unit));
        }

        @Override
        public void onSubscribe(Disposable d) {
            set.add(d);
            s.onSubscribe(set);
        }

        final class OnComplete implements Runnable {
            @Override
            public void run() {
                s.onComplete();
            }
        }

        final class OnError implements Runnable {
            private final Throwable e;

            OnError(Throwable e) {
                this.e = e;
            }

            @Override
            public void run() {
                s.onError(e);
            }
        }
    }
}
