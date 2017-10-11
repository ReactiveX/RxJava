/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.internal.operators;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import rx.*;
import rx.Scheduler.Worker;
import rx.functions.Action0;
import rx.internal.producers.ProducerArbiter;
import rx.internal.subscriptions.SequentialSubscription;
import rx.plugins.RxJavaHooks;

/**
 * Switches to consuming a fallback Observable if the main source doesn't signal an onNext event
 * within the given time frame after subscription or the previous onNext event.
 *
 * @param <T> the value type
 * @since 1.3.3
 */
public final class OnSubscribeTimeoutTimedWithFallback<T> implements Observable.OnSubscribe<T> {

    final Observable<T> source;

    final long timeout;

    final TimeUnit unit;

    final Scheduler scheduler;

    final Observable<? extends T> fallback;

    public OnSubscribeTimeoutTimedWithFallback(Observable<T> source, long timeout,
            TimeUnit unit, Scheduler scheduler,
            Observable<? extends T> fallback) {
        this.source = source;
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.fallback = fallback;
    }

    @Override
    public void call(Subscriber<? super T> t) {
        TimeoutMainSubscriber<T> parent = new TimeoutMainSubscriber<T>(t, timeout, unit, scheduler.createWorker(), fallback);
        t.add(parent.upstream);
        t.setProducer(parent.arbiter);
        parent.startTimeout(0L);
        source.subscribe(parent);
    }

    static final class TimeoutMainSubscriber<T> extends Subscriber<T> {

        final Subscriber<? super T> actual;

        final long timeout;

        final TimeUnit unit;

        final Worker worker;

        final Observable<? extends T> fallback;

        final ProducerArbiter arbiter;

        final AtomicLong index;

        final SequentialSubscription task;

        final SequentialSubscription upstream;

        long consumed;

        TimeoutMainSubscriber(Subscriber<? super T> actual, long timeout,
                TimeUnit unit, Worker worker,
                Observable<? extends T> fallback) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
            this.fallback = fallback;
            this.arbiter = new ProducerArbiter();
            this.index = new AtomicLong();
            this.task = new SequentialSubscription();
            this.upstream = new SequentialSubscription(this);
            this.add(worker);
            this.add(task);
        }


        @Override
        public void onNext(T t) {
            long idx = index.get();
            if (idx == Long.MAX_VALUE || !index.compareAndSet(idx, idx + 1)) {
                return;
            }

            Subscription s = task.get();
            if (s != null) {
                s.unsubscribe();
            }

            consumed++;

            actual.onNext(t);

            startTimeout(idx + 1);
        }

        void startTimeout(long nextIdx) {
            task.replace(worker.schedule(new TimeoutTask(nextIdx), timeout, unit));
        }

        @Override
        public void onError(Throwable e) {
            if (index.getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.unsubscribe();

                actual.onError(e);

                worker.unsubscribe();
            } else {
                RxJavaHooks.onError(e);
            }
        }

        @Override
        public void onCompleted() {
            if (index.getAndSet(Long.MAX_VALUE) != Long.MAX_VALUE) {
                task.unsubscribe();

                actual.onCompleted();

                worker.unsubscribe();
            }
        }

        @Override
        public void setProducer(Producer p) {
            arbiter.setProducer(p);
        }

        void onTimeout(long idx) {
            if (!index.compareAndSet(idx, Long.MAX_VALUE)) {
                return;
            }

            unsubscribe();

            if (fallback == null) {
                actual.onError(new TimeoutException());
            } else {
                long c = consumed;
                if (c != 0L) {
                    arbiter.produced(c);
                }

                FallbackSubscriber<T> fallbackSubscriber = new FallbackSubscriber<T>(actual, arbiter);

                if (upstream.replace(fallbackSubscriber)) {
                    fallback.subscribe(fallbackSubscriber);
                }
            }
        }

        final class TimeoutTask implements Action0 {

            final long idx;

            TimeoutTask(long idx) {
                this.idx = idx;
            }

            @Override
            public void call() {
                onTimeout(idx);
            }
        }
    }

    static final class FallbackSubscriber<T> extends Subscriber<T> {

        final Subscriber<? super T> actual;

        final ProducerArbiter arbiter;

        FallbackSubscriber(Subscriber<? super T> actual, ProducerArbiter arbiter) {
            this.actual = actual;
            this.arbiter = arbiter;
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }

        @Override
        public void onCompleted() {
            actual.onCompleted();
        }

        @Override
        public void setProducer(Producer p) {
            arbiter.setProducer(p);
        }
    }
}
