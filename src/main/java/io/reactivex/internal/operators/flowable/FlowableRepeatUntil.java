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

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.internal.subscriptions.SubscriptionArbiter;

public final class FlowableRepeatUntil<T> extends AbstractFlowableWithUpstream<T, T> {
    final BooleanSupplier until;
    public FlowableRepeatUntil(Flowable<T> source, BooleanSupplier until) {
        super(source);
        this.until = until;
    }

    @Override
    public void subscribeActual(Subscriber<? super T> s) {
        SubscriptionArbiter sa = new SubscriptionArbiter();
        s.onSubscribe(sa);

        RepeatSubscriber<T> rs = new RepeatSubscriber<T>(s, until, sa, source);
        rs.subscribeNext();
    }

    static final class RepeatSubscriber<T> extends AtomicInteger implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -7098360935104053232L;

        final Subscriber<? super T> actual;
        final SubscriptionArbiter sa;
        final Publisher<? extends T> source;
        final BooleanSupplier stop;

        long produced;

        RepeatSubscriber(Subscriber<? super T> actual, BooleanSupplier until, SubscriptionArbiter sa, Publisher<? extends T> source) {
            this.actual = actual;
            this.sa = sa;
            this.source = source;
            this.stop = until;
        }

        @Override
        public void onSubscribe(Subscription s) {
            sa.setSubscription(s);
        }

        @Override
        public void onNext(T t) {
            produced++;
            actual.onNext(t);
        }
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            boolean b;
            try {
                b = stop.getAsBoolean();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }
            if (b) {
                actual.onComplete();
            } else {
                subscribeNext();
            }
        }

        /**
         * Subscribes to the source again via trampolining.
         */
        void subscribeNext() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                for (;;) {
                    if (sa.isCancelled()) {
                        return;
                    }

                    long p = produced;
                    if (p != 0L) {
                        produced = 0L;
                        sa.produced(p);
                    }

                    source.subscribe(this);

                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                }
            }
        }
    }
}
