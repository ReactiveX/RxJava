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

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.subscribers.SerializedSubscriber;

public final class FlowableDelay<T> extends AbstractFlowableWithUpstream<T, T> {
    final long delay;
    final TimeUnit unit;
    final Scheduler scheduler;
    final boolean delayError;

    public FlowableDelay(Flowable<T> source, long delay, TimeUnit unit, Scheduler scheduler, boolean delayError) {
        super(source);
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
        this.delayError = delayError;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> t) {
        Subscriber<? super T> downstream;
        if (delayError) {
            downstream = t;
        } else {
            downstream = new SerializedSubscriber<>(t);
        }

        Scheduler.Worker w = scheduler.createWorker();

        source.subscribe(new DelaySubscriber<>(downstream, delay, unit, w, delayError));
    }

    static final class DelaySubscriber<T> implements FlowableSubscriber<T>, Subscription {
        final Subscriber<? super T> downstream;
        final long delay;
        final TimeUnit unit;
        final Scheduler.Worker w;
        final boolean delayError;

        Subscription upstream;

        DelaySubscriber(Subscriber<? super T> actual, long delay, TimeUnit unit, Worker w, boolean delayError) {
            super();
            this.downstream = actual;
            this.delay = delay;
            this.unit = unit;
            this.w = w;
            this.delayError = delayError;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(final T t) {
            w.schedule(new OnNext(t), delay, unit);
        }

        @Override
        public void onError(final Throwable t) {
            w.schedule(new OnError(t), delayError ? delay : 0, unit);
        }

        @Override
        public void onComplete() {
            w.schedule(new OnComplete(), delay, unit);
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
            w.dispose();
        }

        final class OnNext implements Runnable {
            private final T t;

            OnNext(T t) {
                this.t = t;
            }

            @Override
            public void run() {
                downstream.onNext(t);
            }
        }

        final class OnError implements Runnable {
            private final Throwable t;

            OnError(Throwable t) {
                this.t = t;
            }

            @Override
            public void run() {
                try {
                    downstream.onError(t);
                } finally {
                    w.dispose();
                }
            }
        }

        final class OnComplete implements Runnable {
            @Override
            public void run() {
                try {
                    downstream.onComplete();
                } finally {
                    w.dispose();
                }
            }
        }
    }
}
