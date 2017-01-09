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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableDisposeOn extends Completable {

    final CompletableSource source;

    final Scheduler scheduler;

    public CompletableDisposeOn(CompletableSource source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(final CompletableObserver s) {
        source.subscribe(new CompletableObserverImplementation(s, scheduler));
    }

    static final class CompletableObserverImplementation implements CompletableObserver, Disposable, Runnable {
        final CompletableObserver s;

        final Scheduler scheduler;

        Disposable d;

        volatile boolean disposed;

        CompletableObserverImplementation(CompletableObserver s, Scheduler scheduler) {
            this.s = s;
            this.scheduler = scheduler;
        }

        @Override
        public void onComplete() {
            if (disposed) {
                return;
            }
            s.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            if (disposed) {
                RxJavaPlugins.onError(e);
                return;
            }
            s.onError(e);
        }

        @Override
        public void onSubscribe(final Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                s.onSubscribe(this);
            }
        }

        @Override
        public void dispose() {
            disposed = true;
            scheduler.scheduleDirect(this);
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public void run() {
            d.dispose();
            d = DisposableHelper.DISPOSED;
        }
    }

}
