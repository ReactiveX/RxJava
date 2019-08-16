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

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class ObservableUnsubscribeOn<T> extends AbstractObservableWithUpstream<T, T> {
    final Scheduler scheduler;
    public ObservableUnsubscribeOn(ObservableSource<T> source, Scheduler scheduler) {
        super(source);
        this.scheduler = scheduler;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new UnsubscribeObserver<T>(t, scheduler));
    }

    static final class UnsubscribeObserver<T> extends AtomicBoolean implements Observer<T>, Disposable {

        private static final long serialVersionUID = 1015244841293359600L;

        final Observer<? super T> downstream;
        final Scheduler scheduler;

        Disposable upstream;

        UnsubscribeObserver(Observer<? super T> actual, Scheduler scheduler) {
            this.downstream = actual;
            this.scheduler = scheduler;
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
            if (!get()) {
                downstream.onNext(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (get()) {
                RxJavaPlugins.onError(t);
                return;
            }
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (!get()) {
                downstream.onComplete();
            }
        }

        @Override
        public void dispose() {
            if (compareAndSet(false, true)) {
                scheduler.scheduleDirect(new DisposeTask());
            }
        }

        @Override
        public boolean isDisposed() {
            return get();
        }

        final class DisposeTask implements Runnable {
            @Override
            public void run() {
                upstream.dispose();
            }
        }
    }
}
