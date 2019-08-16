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

package io.reactivex.rxjava3.internal.operators.completable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.*;

public final class CompletableSubscribeOn extends Completable {
    final CompletableSource source;

    final Scheduler scheduler;

    public CompletableSubscribeOn(CompletableSource source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(final CompletableObserver observer) {

        final SubscribeOnObserver parent = new SubscribeOnObserver(observer, source);
        observer.onSubscribe(parent);

        Disposable f = scheduler.scheduleDirect(parent);

        parent.task.replace(f);

    }

    static final class SubscribeOnObserver
    extends AtomicReference<Disposable>
    implements CompletableObserver, Disposable, Runnable {

        private static final long serialVersionUID = 7000911171163930287L;

        final CompletableObserver downstream;

        final SequentialDisposable task;

        final CompletableSource source;

        SubscribeOnObserver(CompletableObserver actual, CompletableSource source) {
            this.downstream = actual;
            this.source = source;
            this.task = new SequentialDisposable();
        }

        @Override
        public void run() {
            source.subscribe(this);
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
            task.dispose();
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }
    }

}
