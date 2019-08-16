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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.MissingBackpressureException;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.BackpressureHelper;
import io.reactivex.rxjava3.subscribers.SerializedSubscriber;

public final class FlowableSamplePublisher<T> extends Flowable<T> {
    final Publisher<T> source;
    final Publisher<?> other;

    final boolean emitLast;

    public FlowableSamplePublisher(Publisher<T> source, Publisher<?> other, boolean emitLast) {
        this.source = source;
        this.other = other;
        this.emitLast = emitLast;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        SerializedSubscriber<T> serial = new SerializedSubscriber<T>(s);
        if (emitLast) {
            source.subscribe(new SampleMainEmitLast<T>(serial, other));
        } else {
            source.subscribe(new SampleMainNoLast<T>(serial, other));
        }
    }

    abstract static class SamplePublisherSubscriber<T> extends AtomicReference<T> implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -3517602651313910099L;

        final Subscriber<? super T> downstream;
        final Publisher<?> sampler;

        final AtomicLong requested = new AtomicLong();

        final AtomicReference<Subscription> other = new AtomicReference<Subscription>();

        Subscription upstream;

        SamplePublisherSubscriber(Subscriber<? super T> actual, Publisher<?> other) {
            this.downstream = actual;
            this.sampler = other;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
                if (other.get() == null) {
                    sampler.subscribe(new SamplerSubscriber<T>(this));
                    s.request(Long.MAX_VALUE);
                }
            }

        }

        @Override
        public void onNext(T t) {
            lazySet(t);
        }

        @Override
        public void onError(Throwable t) {
            SubscriptionHelper.cancel(other);
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            SubscriptionHelper.cancel(other);
            completion();
        }

        void setOther(Subscription o) {
            SubscriptionHelper.setOnce(other, o, Long.MAX_VALUE);
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
            }
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(other);
            upstream.cancel();
        }

        public void error(Throwable e) {
            upstream.cancel();
            downstream.onError(e);
        }

        public void complete() {
            upstream.cancel();
            completion();
        }

        void emit() {
            T value = getAndSet(null);
            if (value != null) {
                long r = requested.get();
                if (r != 0L) {
                    downstream.onNext(value);
                    BackpressureHelper.produced(requested, 1);
                } else {
                    cancel();
                    downstream.onError(new MissingBackpressureException("Couldn't emit value due to lack of requests!"));
                }
            }
        }

        abstract void completion();

        abstract void run();
    }

    static final class SamplerSubscriber<T> implements FlowableSubscriber<Object> {
        final SamplePublisherSubscriber<T> parent;
        SamplerSubscriber(SamplePublisherSubscriber<T> parent) {
            this.parent = parent;

        }

        @Override
        public void onSubscribe(Subscription s) {
            parent.setOther(s);
        }

        @Override
        public void onNext(Object t) {
            parent.run();
        }

        @Override
        public void onError(Throwable t) {
            parent.error(t);
        }

        @Override
        public void onComplete() {
            parent.complete();
        }
    }

    static final class SampleMainNoLast<T> extends SamplePublisherSubscriber<T> {

        private static final long serialVersionUID = -3029755663834015785L;

        SampleMainNoLast(Subscriber<? super T> actual, Publisher<?> other) {
            super(actual, other);
        }

        @Override
        void completion() {
            downstream.onComplete();
        }

        @Override
        void run() {
            emit();
        }
    }

    static final class SampleMainEmitLast<T> extends SamplePublisherSubscriber<T> {

        private static final long serialVersionUID = -3029755663834015785L;

        final AtomicInteger wip;

        volatile boolean done;

        SampleMainEmitLast(Subscriber<? super T> actual, Publisher<?> other) {
            super(actual, other);
            this.wip = new AtomicInteger();
        }

        @Override
        void completion() {
            done = true;
            if (wip.getAndIncrement() == 0) {
                emit();
                downstream.onComplete();
            }
        }

        @Override
        void run() {
            if (wip.getAndIncrement() == 0) {
                do {
                    boolean d = done;
                    emit();
                    if (d) {
                        downstream.onComplete();
                        return;
                    }
                } while (wip.decrementAndGet() != 0);
            }
        }
    }
}
