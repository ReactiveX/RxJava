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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * Subscribes to the source Flowable on the specified Scheduler and makes
 * sure downstream requests are scheduled there as well.
 *
 * @param <T> the value type emitted
 */
public final class FlowableSubscribeOn<T> extends AbstractFlowableWithUpstream<T , T> {

    final Scheduler scheduler;

    final boolean nonScheduledRequests;

    public FlowableSubscribeOn(Flowable<T> source, Scheduler scheduler, boolean nonScheduledRequests) {
        super(source);
        this.scheduler = scheduler;
        this.nonScheduledRequests = nonScheduledRequests;
    }

    @Override
    public void subscribeActual(final Subscriber<? super T> s) {
        Scheduler.Worker w = scheduler.createWorker();
        final SubscribeOnSubscriber<T> sos = new SubscribeOnSubscriber<T>(s, w, source, nonScheduledRequests);
        s.onSubscribe(sos);

        w.schedule(sos);
    }

    static final class SubscribeOnSubscriber<T> extends AtomicReference<Thread>
    implements FlowableSubscriber<T>, Subscription, Runnable {

        private static final long serialVersionUID = 8094547886072529208L;

        final Subscriber<? super T> actual;

        final Scheduler.Worker worker;

        final AtomicReference<Subscription> s;

        final AtomicLong requested;

        final boolean nonScheduledRequests;

        Publisher<T> source;

        SubscribeOnSubscriber(Subscriber<? super T> actual, Scheduler.Worker worker, Publisher<T> source, boolean requestOn) {
            this.actual = actual;
            this.worker = worker;
            this.source = source;
            this.s = new AtomicReference<Subscription>();
            this.requested = new AtomicLong();
            this.nonScheduledRequests = !requestOn;
        }

        @Override
        public void run() {
            lazySet(Thread.currentThread());
            Publisher<T> src = source;
            source = null;
            src.subscribe(this);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this.s, s)) {
                long r = requested.getAndSet(0L);
                if (r != 0L) {
                    requestUpstream(r, s);
                }
            }
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
            worker.dispose();
        }

        @Override
        public void onComplete() {
            actual.onComplete();
            worker.dispose();
        }

        @Override
        public void request(final long n) {
            if (SubscriptionHelper.validate(n)) {
                Subscription s = this.s.get();
                if (s != null) {
                    requestUpstream(n, s);
                } else {
                    BackpressureHelper.add(requested, n);
                    s = this.s.get();
                    if (s != null) {
                        long r = requested.getAndSet(0L);
                        if (r != 0L) {
                            requestUpstream(r, s);
                        }
                    }
                }
            }
        }

        void requestUpstream(final long n, final Subscription s) {
            if (nonScheduledRequests || Thread.currentThread() == get()) {
                s.request(n);
            } else {
                worker.schedule(new Request(s, n));
            }
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(s);
            worker.dispose();
        }

        static final class Request implements Runnable {
            private final Subscription s;
            private final long n;

            Request(Subscription s, long n) {
                this.s = s;
                this.n = n;
            }

            @Override
            public void run() {
                s.request(n);
            }
        }
    }
}
