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

package io.reactivex.rxjava3.internal.operators.single;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.*;

public final class SingleSubscribeOn<T> extends Single<T> {
    final SingleSource<? extends T> source;

    final Scheduler scheduler;

    public SingleSubscribeOn(SingleSource<? extends T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> observer) {
        final SubscribeOnObserver<T> parent = new SubscribeOnObserver<T>(observer, source);
        observer.onSubscribe(parent);

        Disposable f = scheduler.scheduleDirect(parent);

        parent.task.replace(f);

    }

    static final class SubscribeOnObserver<T>
    extends AtomicReference<Disposable>
    implements SingleObserver<T>, Disposable, Runnable {

        private static final long serialVersionUID = 7000911171163930287L;

        final SingleObserver<? super T> downstream;

        final SequentialDisposable task;

        final SingleSource<? extends T> source;

        SubscribeOnObserver(SingleObserver<? super T> actual, SingleSource<? extends T> source) {
            this.downstream = actual;
            this.source = source;
            this.task = new SequentialDisposable();
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }

        @Override
        public void onSuccess(T value) {
            downstream.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
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

        @Override
        public void run() {
            source.subscribe(this);
        }
    }

}
