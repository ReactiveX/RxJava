/*
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

package io.reactivex.rxjava3.internal.operators.maybe;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.schedulers.Timed;

/**
 * Measures the time between subscription and the success item emission
 * from the upstream and emits this as a {@link Timed} success value.
 * @param <T> the element type of the sequence
 * @since 3.0.0
 */
public final class MaybeTimeInterval<T> extends Maybe<Timed<T>> {

    final MaybeSource<T> source;

    final TimeUnit unit;

    final Scheduler scheduler;

    final boolean start;

    public MaybeTimeInterval(MaybeSource<T> source, TimeUnit unit, Scheduler scheduler, boolean start) {
        this.source = source;
        this.unit = unit;
        this.scheduler = scheduler;
        this.start = start;
    }

    @Override
    protected void subscribeActual(@NonNull MaybeObserver<? super @NonNull Timed<T>> observer) {
        source.subscribe(new TimeIntervalMaybeObserver<>(observer, unit, scheduler, start));
    }

    static final class TimeIntervalMaybeObserver<T> implements MaybeObserver<T>, Disposable {

        final MaybeObserver<? super Timed<T>> downstream;

        final TimeUnit unit;

        final Scheduler scheduler;

        final long startTime;

        Disposable upstream;

        TimeIntervalMaybeObserver(MaybeObserver<? super Timed<T>> downstream, TimeUnit unit, Scheduler scheduler, boolean start) {
            this.downstream = downstream;
            this.unit = unit;
            this.scheduler = scheduler;
            this.startTime = start ? scheduler.now(unit) : 0L;
        }

        @Override
        public void onSubscribe(@NonNull Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(@NonNull T t) {
            downstream.onSuccess(new Timed<>(t, scheduler.now(unit) - startTime, unit));
        }

        @Override
        public void onError(@NonNull Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }
    }
}
