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

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

public final class CompletableObserveOn extends Completable {

    final CompletableSource source;

    final Scheduler scheduler;
    public CompletableObserveOn(CompletableSource source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(final CompletableObserver s) {
        source.subscribe(new ObserveOnCompletableObserver(s, scheduler));
    }

    static final class ObserveOnCompletableObserver
    extends AtomicReference<Disposable>
    implements CompletableObserver, Disposable, Runnable {


        private static final long serialVersionUID = 8571289934935992137L;

        final CompletableObserver actual;

        final Scheduler scheduler;

        Throwable error;

        ObserveOnCompletableObserver(CompletableObserver actual, Scheduler scheduler) {
            this.actual = actual;
            this.scheduler = scheduler;
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(this, d)) {
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onError(Throwable e) {
            this.error = e;
            DisposableHelper.replace(this, scheduler.scheduleDirect(this));
        }

        @Override
        public void onComplete() {
            DisposableHelper.replace(this, scheduler.scheduleDirect(this));
        }

        @Override
        public void run() {
            Throwable ex = error;
            if (ex != null) {
                error = null;
                actual.onError(ex);
            } else {
                actual.onComplete();
            }
        }
    }

}
