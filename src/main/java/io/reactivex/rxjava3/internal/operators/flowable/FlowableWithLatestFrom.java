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

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.Objects;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.internal.fuseable.ConditionalSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.subscribers.SerializedSubscriber;

public final class FlowableWithLatestFrom<T, U, R> extends AbstractFlowableWithUpstream<T, R> {
    final BiFunction<? super T, ? super U, ? extends R> combiner;
    final Publisher<? extends U> other;
    public FlowableWithLatestFrom(Flowable<T> source, BiFunction<? super T, ? super U, ? extends R> combiner, Publisher<? extends U> other) {
        super(source);
        this.combiner = combiner;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        final SerializedSubscriber<R> serial = new SerializedSubscriber<R>(s);
        final WithLatestFromSubscriber<T, U, R> wlf = new WithLatestFromSubscriber<T, U, R>(serial, combiner);

        serial.onSubscribe(wlf);

        other.subscribe(new FlowableWithLatestSubscriber(wlf));

        source.subscribe(wlf);
    }

    static final class WithLatestFromSubscriber<T, U, R> extends AtomicReference<U>
    implements ConditionalSubscriber<T>, Subscription {

        private static final long serialVersionUID = -312246233408980075L;

        final Subscriber<? super R> downstream;

        final BiFunction<? super T, ? super U, ? extends R> combiner;

        final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

        final AtomicLong requested = new AtomicLong();

        final AtomicReference<Subscription> other = new AtomicReference<Subscription>();

        WithLatestFromSubscriber(Subscriber<? super R> actual, BiFunction<? super T, ? super U, ? extends R> combiner) {
            this.downstream = actual;
            this.combiner = combiner;
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(this.upstream, requested, s);
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                upstream.get().request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            U u = get();
            if (u != null) {
                R r;
                try {
                    r = Objects.requireNonNull(combiner.apply(t, u), "The combiner returned a null value");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancel();
                    downstream.onError(e);
                    return false;
                }
                downstream.onNext(r);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onError(Throwable t) {
            SubscriptionHelper.cancel(other);
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            SubscriptionHelper.cancel(other);
            downstream.onComplete();
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(upstream, requested, n);
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(upstream);
            SubscriptionHelper.cancel(other);
        }

        public boolean setOther(Subscription o) {
            return SubscriptionHelper.setOnce(other, o);
        }

        public void otherError(Throwable e) {
            SubscriptionHelper.cancel(upstream);
            downstream.onError(e);
        }
    }

    final class FlowableWithLatestSubscriber implements FlowableSubscriber<U> {
        private final WithLatestFromSubscriber<T, U, R> wlf;

        FlowableWithLatestSubscriber(WithLatestFromSubscriber<T, U, R> wlf) {
            this.wlf = wlf;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (wlf.setOther(s)) {
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(U t) {
            wlf.lazySet(t);
        }

        @Override
        public void onError(Throwable t) {
            wlf.otherError(t);
        }

        @Override
        public void onComplete() {
            // nothing to do, the wlf will complete on its own pace
        }
    }
}
