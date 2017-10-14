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
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiFunction;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.subscribers.SerializedSubscriber;

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

        final Subscriber<? super R> actual;
        final BiFunction<? super T, ? super U, ? extends R> combiner;

        final AtomicReference<Subscription> s = new AtomicReference<Subscription>();

        final AtomicLong requested = new AtomicLong();

        final AtomicReference<Subscription> other = new AtomicReference<Subscription>();

        WithLatestFromSubscriber(Subscriber<? super R> actual, BiFunction<? super T, ? super U, ? extends R> combiner) {
            this.actual = actual;
            this.combiner = combiner;
        }
        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(this.s, requested, s);
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                s.get().request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            U u = get();
            if (u != null) {
                R r;
                try {
                    r = ObjectHelper.requireNonNull(combiner.apply(t, u), "The combiner returned a null value");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancel();
                    actual.onError(e);
                    return false;
                }
                actual.onNext(r);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onError(Throwable t) {
            SubscriptionHelper.cancel(other);
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            SubscriptionHelper.cancel(other);
            actual.onComplete();
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(s, requested, n);
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(s);
            SubscriptionHelper.cancel(other);
        }

        public boolean setOther(Subscription o) {
            return SubscriptionHelper.setOnce(other, o);
        }

        public void otherError(Throwable e) {
            SubscriptionHelper.cancel(s);
            actual.onError(e);
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
