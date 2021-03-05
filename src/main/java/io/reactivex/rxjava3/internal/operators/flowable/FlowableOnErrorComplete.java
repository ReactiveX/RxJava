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

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Emits an onComplete if the source emits an onError and the predicate returns true for
 * that Throwable.
 * 
 * @param <T> the value type
 * @since 3.0.0
 */
public final class FlowableOnErrorComplete<T> extends AbstractFlowableWithUpstream<T, T> {

    final Predicate<? super Throwable> predicate;

    public FlowableOnErrorComplete(Flowable<T> source,
            Predicate<? super Throwable> predicate) {
        super(source);
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> observer) {
        source.subscribe(new OnErrorCompleteSubscriber<>(observer, predicate));
    }

    public static final class OnErrorCompleteSubscriber<T>
    implements FlowableSubscriber<T>, Subscription {

        final Subscriber<? super T> downstream;

        final Predicate<? super Throwable> predicate;

        Subscription upstream;

        public OnErrorCompleteSubscriber(Subscriber<? super T> actual, Predicate<? super Throwable> predicate) {
            this.downstream = actual;
            this.predicate = predicate;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T value) {
            downstream.onNext(value);
        }

        @Override
        public void onError(Throwable e) {
            boolean b;

            try {
                b = predicate.test(e);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(new CompositeException(e, ex));
                return;
            }

            if (b) {
                downstream.onComplete();
            } else {
                downstream.onError(e);
            }
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void cancel() {
            upstream.cancel();
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }
    }
}
