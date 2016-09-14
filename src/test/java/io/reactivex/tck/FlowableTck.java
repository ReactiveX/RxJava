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

package io.reactivex.tck;

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;

/**
 * Helper Flowable that makes sure invalid requests from downstream are reported
 * as onErrors (instead of RxJavaPlugins.onError). Since requests can be
 * async to  the onXXX method calls, we need half-serialization to ensure correct
 * operations.
 *
 * @param <T> the value type
 */
public final class FlowableTck<T> extends Flowable<T> {

    /**
     * Wraps a given Publisher and makes sure invalid requests trigger an onError(IllegalArgumentException).
     * @param <T> the value type
     * @param source the source to wrap, not null
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> wrap(Publisher<T> source) {
        return new FlowableTck<T>(ObjectHelper.requireNonNull(source, "source is null"));
    }

    final Publisher<T> source;

    FlowableTck(Publisher<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new TckSubscriber<T>(s));
    }

    static final class TckSubscriber<T>
    extends AtomicInteger
    implements Subscriber<T>, Subscription {

        private static final long serialVersionUID = -4945028590049415624L;

        final Subscriber<? super T> actual;

        final AtomicThrowable error;

        Subscription s;

        TckSubscriber(Subscriber<? super T> actual) {
            this.actual = actual;
            this.error = new AtomicThrowable();
        }


        @Override
        public void request(long n) {
            if (n <= 0) {
                s.cancel();
                onError(new IllegalArgumentException("§3.9 violated: positive request amount required but it was " + n));
            } else {
                s.request(n);
            }
        }

        @Override
        public void cancel() {
            s.cancel();
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
            HalfSerializer.onNext(actual, t, this, error);
        }

        @Override
        public void onError(Throwable t) {
            HalfSerializer.onError(actual, t, this, error);
        }

        @Override
        public void onComplete() {
            HalfSerializer.onComplete(actual, this, error);
        }
    }
}
