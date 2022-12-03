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

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Consumer;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.MissingBackpressureException;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.BackpressureHelper;
import io.reactivex.rxjava3.subscribers.SerializedSubscriber;

public final class FlowableSampleTimed<T> extends AbstractFlowableWithUpstream<T, T> {
    final long period;
    final TimeUnit unit;
    final Scheduler scheduler;
    final boolean emitLast;
    final Consumer<? super T> onDropped;

    public FlowableSampleTimed(Flowable<T> source, long period, TimeUnit unit, Scheduler scheduler, boolean emitLast, Consumer<? super T> onDropped) {
        super(source);
        this.period = period;
        this.unit = unit;
        this.scheduler = scheduler;
        this.emitLast = emitLast;
        this.onDropped = onDropped;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        SerializedSubscriber<T> serial = new SerializedSubscriber<>(s);
        if (emitLast) {
            source.subscribe(new SampleTimedEmitLast<>(serial, period, unit, scheduler, onDropped));
        } else {
            source.subscribe(new SampleTimedNoLast<>(serial, period, unit, scheduler, onDropped));
        }
    }

    abstract static class SampleTimedSubscriber<T> extends AtomicReference<T> implements FlowableSubscriber<T>, Subscription, Runnable {

        private static final long serialVersionUID = -3517602651313910099L;

        final Subscriber<? super T> downstream;
        final long period;
        final TimeUnit unit;
        final Scheduler scheduler;
        final Consumer<? super T> onDropped;

        final AtomicLong requested = new AtomicLong();

        final SequentialDisposable timer = new SequentialDisposable();

        Subscription upstream;

        SampleTimedSubscriber(Subscriber<? super T> actual, long period, TimeUnit unit, Scheduler scheduler, Consumer<? super T> onDropped) {
            this.downstream = actual;
            this.period = period;
            this.unit = unit;
            this.scheduler = scheduler;
            this.onDropped = onDropped;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
                timer.replace(scheduler.schedulePeriodicallyDirect(this, period, period, unit));
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            T oldValue = getAndSet(t);
            if (oldValue != null && onDropped != null) {
              try {
                  onDropped.accept(oldValue);
              } catch (Throwable throwable) {
                  Exceptions.throwIfFatal(throwable);
                  cancelTimer();
                  upstream.cancel();
                  downstream.onError(throwable);
              }
            }
        }

        @Override
        public void onError(Throwable t) {
            cancelTimer();
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            cancelTimer();
            complete();
        }

        void cancelTimer() {
            DisposableHelper.dispose(timer);
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
            }
        }

        @Override
        public void cancel() {
            cancelTimer();
            upstream.cancel();
        }

        void emit() {
            T value = getAndSet(null);
            if (value != null) {
                long r = requested.get();
                if (r != 0L) {
                    downstream.onNext(value);
                    BackpressureHelper.produced(requested, 1);
                } else {
                    cancel();
                    downstream.onError(MissingBackpressureException.createDefault());
                }
            }
        }

        abstract void complete();
    }

    static final class SampleTimedNoLast<T> extends SampleTimedSubscriber<T> {

        private static final long serialVersionUID = -7139995637533111443L;

        SampleTimedNoLast(Subscriber<? super T> actual, long period, TimeUnit unit, Scheduler scheduler, Consumer<? super T> onDropped) {
            super(actual, period, unit, scheduler, onDropped);
        }

        @Override
        void complete() {
            downstream.onComplete();
        }

        @Override
        public void run() {
            emit();
        }
    }

    static final class SampleTimedEmitLast<T> extends SampleTimedSubscriber<T> {

        private static final long serialVersionUID = -7139995637533111443L;

        final AtomicInteger wip;

        SampleTimedEmitLast(Subscriber<? super T> actual, long period, TimeUnit unit, Scheduler scheduler, Consumer<? super T> onDropped) {
            super(actual, period, unit, scheduler, onDropped);
            this.wip = new AtomicInteger(1);
        }

        @Override
        void complete() {
            emit();
            if (wip.decrementAndGet() == 0) {
                downstream.onComplete();
            }
        }

        @Override
        public void run() {
            if (wip.incrementAndGet() == 2) {
                emit();
                if (wip.decrementAndGet() == 0) {
                    downstream.onComplete();
                }
            }
        }
    }
}
