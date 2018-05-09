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
package io.reactivex.internal.operators.flowable;

import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.SerializedSubscriber;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

public final class FlowableThrottleAndSample<T> extends AbstractFlowableWithUpstream<T, T> {

    final long windowDuration;
    final TimeUnit unit;
    final Scheduler scheduler;

    final boolean emitLast;

    public FlowableThrottleAndSample(@NonNull final Flowable<T> source, final long windowDuration,
                                     final TimeUnit unit, final Scheduler scheduler, final boolean emitLast) {
        super(source);
        this.windowDuration = windowDuration;
        this.unit = unit;
        this.scheduler = scheduler;
        this.emitLast = emitLast;
    }

    @Override
    protected void subscribeActual(final Subscriber<? super T> s) {
        final SerializedSubscriber<T> serial = new SerializedSubscriber<T>(s);
        if (emitLast) {
            source.subscribe(new ThrottleAndSampleEmitLast<T>(serial, windowDuration, unit, scheduler.createWorker()));
        } else {
            source.subscribe(new ThrottleAndSampleNoLast<T>(serial, windowDuration, unit, scheduler.createWorker()));
        }
    }

    abstract static class ThrottleAndSampleSubscriber<T> extends AtomicReference<T> implements FlowableSubscriber<T>, Subscription, Runnable {

        static final long serialVersionUID = -7130465637537281443L;

        final Subscriber<? super T> actual;
        final long windowDuration;
        final TimeUnit unit;
        final Scheduler.Worker worker;

        final AtomicInteger timerInProgress;
        final SequentialDisposable timer;

        final AtomicLong requested = new AtomicLong();

        Subscription subscription;

        boolean isDone;

        ThrottleAndSampleSubscriber(final Subscriber<? super T> actual, long windowDuration,
                                    final TimeUnit unit, final Scheduler.Worker worker) {
            this.actual = actual;
            this.windowDuration = windowDuration;
            this.unit = unit;
            this.worker = worker;
            this.timerInProgress = new AtomicInteger(0);
            this.timer = new SequentialDisposable(Disposables.disposed());
        }

        @Override
        public void onSubscribe(final Subscription subscription) {
            if (SubscriptionHelper.validate(this.subscription, subscription)) {
                this.subscription = subscription;
                actual.onSubscribe(this);
                subscription.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(final T value) {
            set(value);
            if (timerInProgress.incrementAndGet() == 1) {
                emit(getAndSet(null));
                timer.update(worker.schedule(this, windowDuration, unit));
            }
        }

        protected void emitFromRun() {
            timer.update(Disposables.disposed());
            if (timerInProgress.decrementAndGet() != 0) {
                emit(getAndSet(null));
                timer.replace(worker.schedule(this, windowDuration, unit));
                timerInProgress.set(1);
            }
        }

        @Override
        public void onError(final Throwable t) {
            if (isDone) {
                RxJavaPlugins.onError(t);
                return;
            }
            isDone = true;
            cancelTimer();
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (isDone) {
                return;
            }
            isDone = true;
            cancelTimer();
            complete();
        }

        @Override
        public void request(final long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
            }
        }

        @Override
        public void cancel() {
            cancelTimer();
            subscription.cancel();
        }

        private void cancelTimer() {
            DisposableHelper.dispose(timer);
            worker.dispose();
        }

        void emit(@NonNull final T value) {
            long r = requested.get();
            if (r != 0L) {
                actual.onNext(value);
                BackpressureHelper.produced(requested, 1);
            } else {
                cancel();
                actual.onError(new MissingBackpressureException("Couldn't emit value due to lack of requests!"));
            }
        }

        abstract void complete();
    }

    static final class ThrottleAndSampleNoLast<T> extends ThrottleAndSampleSubscriber<T> {

        private static final long serialVersionUID = -7139995637537281443L;

        ThrottleAndSampleNoLast(final Subscriber<? super T> actual, final long windowDuration,
                                final TimeUnit unit, final Scheduler.Worker worker) {
            super(actual, windowDuration, unit, worker);
        }

        @Override
        public void run() {
            emitFromRun();
        }

        @Override
        void complete() {
            actual.onComplete();
        }
    }

    static final class ThrottleAndSampleEmitLast<T> extends ThrottleAndSampleSubscriber<T> {

        private static final long serialVersionUID = -7139995637537281443L;

        final AtomicInteger wip;

        ThrottleAndSampleEmitLast(final Subscriber<? super T> actual, final long windowDuration,
                                  final TimeUnit unit, final Scheduler.Worker worker) {
            super(actual, windowDuration, unit, worker);
            this.wip = new AtomicInteger(1);
        }

        @Override
        void complete() {
            if (wip.decrementAndGet() == 0) {
                final T value = getAndSet(null);
                if (value != null) {
                    emit(value);
                }
                actual.onComplete();
            }
        }

        @Override
        public void run() {
            if (wip.incrementAndGet() == 2) {
                emitFromRun();
                if (wip.decrementAndGet() == 0) {
                    actual.onComplete();
                }
            }
        }
    }
}
