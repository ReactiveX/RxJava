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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableTimeoutTimed<T> extends AbstractFlowableWithUpstream<T, T> {
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;
    final Publisher<? extends T> other;

    public FlowableTimeoutTimed(Flowable<T> source,
            long timeout, TimeUnit unit, Scheduler scheduler, Publisher<? extends T> other) {
        super(source);
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (other == null) {
            TimeoutSubscriber<T> parent = new TimeoutSubscriber<T>(s, timeout, unit, scheduler.createWorker());
            s.onSubscribe(parent);
            parent.startTimeout(0L);
            source.subscribe(parent);
        } else {
            TimeoutFallbackSubscriber<T> parent = new TimeoutFallbackSubscriber<T>(s, timeout, unit, scheduler.createWorker(), other);
            s.onSubscribe(parent);
            parent.startTimeout(0L);
            source.subscribe(parent);
        }
    }

    static final class TimeoutSubscriber<T> extends AtomicLong
    implements FlowableSubscriber<T>, Subscription, TimeoutSupport {

        private static final long serialVersionUID = 3764492702657003550L;

        final Subscriber<? super T> actual;

        final long timeout;

        final TimeUnit unit;

        final Scheduler.Worker worker;

        final SequentialDisposable task;

        final AtomicReference<Subscription> upstream;

        final AtomicLong requested;

        TimeoutSubscriber(Subscriber<? super T> actual, long timeout, TimeUnit unit, Scheduler.Worker worker) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
            this.task = new SequentialDisposable();
            this.upstream = new AtomicReference<Subscription>();
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(upstream, requested, s);
        }

        @Override
        public void onNext(T t) {
            long idx = get();
            if (idx == Long.MAX_VALUE || !compareAndSet(idx, idx + 1)) {
                return;
            }

            task.get().dispose();

            actual.onNext(t);

            startTimeout(idx + 1);
        }

        void startTimeout(long nextIndex) {
            task.replace(worker.schedule(new TimeoutTask(nextIndex, this), timeout, unit));
        }

        @Override
        public void onError(Throwable t) {
            if (getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onError(t);

                worker.dispose();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onComplete();

                worker.dispose();
            }
        }

        @Override
        public void onTimeout(long idx) {
            if (compareAndSet(idx, Long.MAX_VALUE)) {
                SubscriptionHelper.cancel(upstream);

                actual.onError(new TimeoutException());

                worker.dispose();
            }
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(upstream, requested, n);
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(upstream);
            worker.dispose();
        }
    }


    static final class TimeoutTask implements Runnable {

        final TimeoutSupport parent;

        final long idx;

        TimeoutTask(long idx, TimeoutSupport parent) {
            this.idx = idx;
            this.parent = parent;
        }

        @Override
        public void run() {
            parent.onTimeout(idx);
        }
    }

    static final class TimeoutFallbackSubscriber<T> extends SubscriptionArbiter
    implements FlowableSubscriber<T>, TimeoutSupport {

        private static final long serialVersionUID = 3764492702657003550L;

        final Subscriber<? super T> actual;

        final long timeout;

        final TimeUnit unit;

        final Scheduler.Worker worker;

        final SequentialDisposable task;

        final AtomicReference<Subscription> upstream;

        final AtomicLong index;

        long consumed;

        Publisher<? extends T> fallback;

        TimeoutFallbackSubscriber(Subscriber<? super T> actual, long timeout, TimeUnit unit,
                Scheduler.Worker worker, Publisher<? extends T> fallback) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
            this.fallback = fallback;
            this.task = new SequentialDisposable();
            this.upstream = new AtomicReference<Subscription>();
            this.index = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(upstream, s)) {
                setSubscription(s);
            }
        }

        @Override
        public void onNext(T t) {
            long idx = index.get();
            if (idx == Long.MAX_VALUE || !index.compareAndSet(idx, idx + 1)) {
                return;
            }

            task.get().dispose();

            consumed++;

            actual.onNext(t);

            startTimeout(idx + 1);
        }

        void startTimeout(long nextIndex) {
            task.replace(worker.schedule(new TimeoutTask(nextIndex, this), timeout, unit));
        }

        @Override
        public void onError(Throwable t) {
            if (index.getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onError(t);

                worker.dispose();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (index.getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.dispose();

                actual.onComplete();

                worker.dispose();
            }
        }

        @Override
        public void onTimeout(long idx) {
            if (index.compareAndSet(idx, Long.MAX_VALUE)) {
                SubscriptionHelper.cancel(upstream);

                long c = consumed;
                if (c != 0L) {
                    produced(c);
                }

                Publisher<? extends T> f = fallback;
                fallback = null;

                f.subscribe(new FallbackSubscriber<T>(actual, this));

                worker.dispose();
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            worker.dispose();
        }
    }

    static final class FallbackSubscriber<T> implements FlowableSubscriber<T> {

        final Subscriber<? super T> actual;

        final SubscriptionArbiter arbiter;

        FallbackSubscriber(Subscriber<? super T> actual, SubscriptionArbiter arbiter) {
            this.actual = actual;
            this.arbiter = arbiter;
        }

        @Override
        public void onSubscribe(Subscription s) {
            arbiter.setSubscription(s);
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }
    }

    interface TimeoutSupport {

        void onTimeout(long idx);

    }
}
