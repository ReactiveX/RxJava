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

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscribers.FullArbiterSubscriber;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.SerializedSubscriber;

public final class FlowableTimeoutTimed<T> extends AbstractFlowableWithUpstream<T, T> {
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;
    final Publisher<? extends T> other;

    static final Disposable NEW_TIMER = new EmptyDispose();

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
            source.subscribe(new TimeoutTimedSubscriber<T>(
                    new SerializedSubscriber<T>(s), // because errors can race
                    timeout, unit, scheduler.createWorker()));
        } else {
            source.subscribe(new TimeoutTimedOtherSubscriber<T>(
                    s, // the FullArbiter serializes
                    timeout, unit, scheduler.createWorker(), other));
        }
    }

    static final class TimeoutTimedOtherSubscriber<T> implements FlowableSubscriber<T>, Disposable {
        final Subscriber<? super T> actual;
        final long timeout;
        final TimeUnit unit;
        final Scheduler.Worker worker;
        final Publisher<? extends T> other;

        Subscription s;

        final FullArbiter<T> arbiter;

        Disposable timer;

        volatile long index;

        volatile boolean done;

        TimeoutTimedOtherSubscriber(Subscriber<? super T> actual, long timeout, TimeUnit unit, Worker worker,
                Publisher<? extends T> other) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
            this.other = other;
            this.arbiter = new FullArbiter<T>(actual, this, 8);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                if (arbiter.setSubscription(s)) {
                    actual.onSubscribe(arbiter);

                    scheduleTimeout(0L);
                }
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            long idx = index + 1;
            index = idx;

            if (arbiter.onNext(t, s)) {
                scheduleTimeout(idx);
            }
        }

        void scheduleTimeout(final long idx) {
            if (timer != null) {
                timer.dispose();
            }

            timer = worker.schedule(new TimeoutTask(idx), timeout, unit);
        }

        void subscribeNext() {
            other.subscribe(new FullArbiterSubscriber<T>(arbiter));
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            arbiter.onError(t, s);
            worker.dispose();
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            arbiter.onComplete(s);
            worker.dispose();
        }

        @Override
        public void dispose() {
            s.cancel();
            worker.dispose();
        }

        @Override
        public boolean isDisposed() {
            return worker.isDisposed();
        }

        final class TimeoutTask implements Runnable {
            private final long idx;

            TimeoutTask(long idx) {
                this.idx = idx;
            }

            @Override
            public void run() {
                if (idx == index) {
                    done = true;
                    s.cancel();
                    worker.dispose();

                    subscribeNext();

                }
            }
        }
    }

    static final class TimeoutTimedSubscriber<T> implements FlowableSubscriber<T>, Disposable, Subscription {
        final Subscriber<? super T> actual;
        final long timeout;
        final TimeUnit unit;
        final Scheduler.Worker worker;

        Subscription s;

        Disposable timer;

        volatile long index;

        volatile boolean done;

        TimeoutTimedSubscriber(Subscriber<? super T> actual, long timeout, TimeUnit unit, Worker worker) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                scheduleTimeout(0L);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            long idx = index + 1;
            index = idx;

            actual.onNext(t);

            scheduleTimeout(idx);
        }

        void scheduleTimeout(final long idx) {
            if (timer != null) {
                timer.dispose();
            }

            timer = worker.schedule(new TimeoutTask(idx), timeout, unit);
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;

            actual.onError(t);
            worker.dispose();
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;

            actual.onComplete();
            worker.dispose();
        }

        @Override
        public void dispose() {
            s.cancel();
            worker.dispose();
        }

        @Override
        public boolean isDisposed() {
            return worker.isDisposed();
        }

        @Override
        public void request(long n) {
            s.request(n);
        }

        @Override
        public void cancel() {
            dispose();
        }

        final class TimeoutTask implements Runnable {
            private final long idx;

            TimeoutTask(long idx) {
                this.idx = idx;
            }

            @Override
            public void run() {
                if (idx == index) {
                    done = true;
                    dispose();

                    actual.onError(new TimeoutException());
                }
            }
        }
    }

    static final class EmptyDispose implements Disposable {
        @Override
        public void dispose() { }

        @Override
        public boolean isDisposed() {
            return true;
        }
    }


}
