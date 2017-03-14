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
package io.reactivex.flowable;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.Flowable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * Creates {@link Flowable} of a number of items followed by either an error or
 * completion. Cancellation has no effect on preventing emissions until the
 * currently outstanding requests have been met.
 * @param <T> the value type
 */
public final class Burst<T> extends Flowable<T> {

    final List<T> items;
    final Throwable error;

    Burst(Throwable error, List<T> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items cannot be empty");
        }
        for (T item : items) {
            if (item == null) {
                throw new IllegalArgumentException("items cannot include null");
            }
        }
        this.error = error;
        this.items = items;
    }

    @Override
    protected void subscribeActual(final Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new BurstSubscription(subscriber));

    }

    @SuppressWarnings("unchecked")
    public static <T> Builder<T> item(T item) {
        return items(item);
    }

    public static <T> Builder<T> items(T... items) {
        return new Builder<T>(Arrays.asList(items));
    }

    final class BurstSubscription implements Subscription {
        private final Subscriber<? super T> subscriber;
        final Queue<T> q = new ConcurrentLinkedQueue<T>(items);
        final AtomicLong requested = new AtomicLong();
        volatile boolean cancelled;

        BurstSubscription(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            if (cancelled) {
                // required by reactive-streams-jvm 3.6
                return;
            }
            if (SubscriptionHelper.validate(n)) {
                // just for testing, don't care about perf
                // so no attempt made to reduce volatile reads
                if (BackpressureHelper.add(requested, n) == 0) {
                    if (q.isEmpty()) {
                        return;
                    }
                    while (!q.isEmpty() && requested.get() > 0) {
                        T item = q.poll();
                        requested.decrementAndGet();
                        subscriber.onNext(item);
                    }
                    if (q.isEmpty()) {
                        if (error != null) {
                            subscriber.onError(error);
                        } else {
                            subscriber.onComplete();
                        }
                    }
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

    public static final class Builder<T> {

        private final List<T> items;
        private Throwable error;

        Builder(List<T> items) {
            this.items = items;
        }

        public Flowable<T> error(Throwable e) {
            this.error = e;
            return create();
        }

        public Flowable<T> create() {
            return new Burst<T>(error, items);
        }

    }

}
