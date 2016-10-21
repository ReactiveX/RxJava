/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

public final class FlowableOnErrorReturn<T> extends AbstractFlowableWithUpstream<T, T> {
    final Function<? super Throwable, ? extends T> valueSupplier;
    public FlowableOnErrorReturn(Publisher<T> source, Function<? super Throwable, ? extends T> valueSupplier) {
        super(source);
        this.valueSupplier = valueSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new OnErrorReturnSubscriber<T>(s, valueSupplier));
    }

    static final class OnErrorReturnSubscriber<T> extends AtomicLong
    implements Subscriber<T>, Subscription {

        private static final long serialVersionUID = -3740826063558713822L;
        final Subscriber<? super T> actual;

        final Function<? super Throwable, ? extends T> valueSupplier;

        Subscription s;

        T value;

        long produced;

        static final long COMPLETE_MASK = Long.MIN_VALUE;
        static final long REQUEST_MASK = Long.MAX_VALUE;

        OnErrorReturnSubscriber(Subscriber<? super T> actual, Function<? super Throwable, ? extends T> valueSupplier) {
            this.actual = actual;
            this.valueSupplier = valueSupplier;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            produced++;
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            T v;
            try {
                v = ObjectHelper.requireNonNull(valueSupplier.apply(t), "The valueSupplier returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(new CompositeException(t, ex));
                return;
            }
            complete(v);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }

        void complete(T n) {
            long p = produced;
            if (p != 0L) {
                BackpressureHelper.produced(this, p);
            }

            for (;;) {
                long r = get();
                if ((r & COMPLETE_MASK) != 0) {
                    return;
                }
                if ((r & REQUEST_MASK) != 0) {
                    lazySet(COMPLETE_MASK + 1);
                    actual.onNext(n);
                    actual.onComplete();
                    return;
                }
                value = n;
                if (compareAndSet(0, COMPLETE_MASK)) {
                    return;
                }
            }
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                for (;;) {
                    long r = get();
                    if ((r & COMPLETE_MASK) != 0) {
                        if (compareAndSet(COMPLETE_MASK, COMPLETE_MASK + 1)) {
                            actual.onNext(value);
                            actual.onComplete();
                        }
                        break;
                    }
                    long u = BackpressureHelper.addCap(r, n);
                    if (compareAndSet(r, u)) {
                        s.request(n);
                        break;
                    }
                }
            }
        }

        @Override
        public void cancel() {
            s.cancel();
        }
    }
}