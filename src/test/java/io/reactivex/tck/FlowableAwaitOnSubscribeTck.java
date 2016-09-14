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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * Intercepts the onSubscribe call and makes sure calls to Subscription methods
 * only happen after the child Subscriber has returned from its onSubscribe method.
 * 
 * <p>This helps with child Subscribers that don't expect a recursive call from
 * onSubscribe into their onNext because, for example, they request immediately from
 * their onSubscribe but don't finish their preparation before that and onNext
 * runs into a half-prepared state. This can happen with non Rx mentality based Subscribers.
 *
 * @param <T> the value type
 */
public final class FlowableAwaitOnSubscribeTck<T> extends Flowable<T> {

    final Publisher<T> source;

    FlowableAwaitOnSubscribeTck(Publisher<T> source) {
        this.source = source;
    }

    public static <T> Flowable<T> wrap(Publisher<T> source) {
        return new FlowableAwaitOnSubscribeTck<T>(ObjectHelper.requireNonNull(source, "source is null"));
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new PublisherPostOnSubscribeSubscriber<T>(s));
    }

    static final class PublisherPostOnSubscribeSubscriber<T>
    extends AtomicReference<Subscription>
    implements Subscriber<T>, Subscription {

        private static final long serialVersionUID = -4850665729904103852L;

        final Subscriber<? super T> actual;

        final AtomicLong requested;

        PublisherPostOnSubscribeSubscriber(Subscriber<? super T> actual) {
            this.actual = actual;
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.get(), s)) {

                actual.onSubscribe(this);

                if (SubscriptionHelper.setOnce(this, s)) {
                    long r = requested.getAndSet(0L);
                    if (r != 0L) {
                        s.request(r);
                    }
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
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }

        @Override
        public void request(long n) {
            Subscription a = get();
            if (a != null) {
                a.request(n);
            } else {
                if (SubscriptionHelper.validate(n)) {
                    BackpressureHelper.add(requested, n);
                    a = get();
                    if (a != null) {
                        long r = requested.getAndSet(0L);
                        if (r != 0L) {
                            a.request(n);
                        }
                    }
                }
            }
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(this);
        }
    }
}
