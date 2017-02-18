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

public final class FlowableOnBackpressureLatest<T> extends AbstractFlowableWithUpstream<T, T> {

    public FlowableOnBackpressureLatest(Flowable<T> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new BackpressureLatestSubscriber<T>(s));
    }

    static final class BackpressureLatestSubscriber<T> extends AtomicInteger implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = 163080509307634843L;

        final Subscriber<? super T> actual;

        Subscription s;

        volatile boolean done;
        Throwable error;

        volatile boolean cancelled;

        final AtomicLong requested = new AtomicLong();

        final AtomicReference<T> current = new AtomicReference<T>();

        BackpressureLatestSubscriber(Subscriber<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            current.lazySet(t);
            drain();
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                s.cancel();

                if (getAndIncrement() == 0) {
                    current.lazySet(null);
                }
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            final Subscriber<? super T> a = actual;
            int missed = 1;
            final AtomicLong r = requested;
            final AtomicReference<T> q = current;

            for (;;) {
                long e = 0L;

                while (e != r.get()) {
                    boolean d = done;
                    T v = q.getAndSet(null);
                    boolean empty = v == null;

                    if (checkTerminated(d, empty, a, q)) {
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(v);

                    e++;
                }

                if (e == r.get() && checkTerminated(done, q.get() == null, a, q)) {
                    return;
                }

                if (e != 0L) {
                    BackpressureHelper.produced(r, e);
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        boolean checkTerminated(boolean d, boolean empty, Subscriber<?> a, AtomicReference<T> q) {
            if (cancelled) {
                q.lazySet(null);
                return true;
            }

            if (d) {
                Throwable e = error;
                if (e != null) {
                    q.lazySet(null);
                    a.onError(e);
                    return true;
                } else
                if (empty) {
                    a.onComplete();
                    return true;
                }
            }

            return false;
        }
    }
}
