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
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.subscribers.SerializedSubscriber;

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

        final Subscriber<? super T> actual;
        final Publisher<?> sampler;

        final AtomicLong requested = new AtomicLong();

        final AtomicReference<Subscription> other = new AtomicReference<Subscription>();

        Subscription s;

        SamplePublisherSubscriber(Subscriber<? super T> actual, Publisher<?> other) {
            this.actual = actual;
            this.sampler = other;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
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
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            SubscriptionHelper.cancel(other);
            completeMain();
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
            s.cancel();
        }

        public void error(Throwable e) {
            s.cancel();
            actual.onError(e);
        }

        public void complete() {
            s.cancel();
            completeOther();
        }

        void emit() {
            T value = getAndSet(null);
            if (value != null) {
                long r = requested.get();
                if (r != 0L) {
                    actual.onNext(value);
                    BackpressureHelper.produced(requested, 1);
                } else {
                    cancel();
                    actual.onError(new MissingBackpressureException("Couldn't emit value due to lack of requests!"));
                }
            }
        }

        abstract void completeMain();

        abstract void completeOther();

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
        void completeMain() {
            actual.onComplete();
        }

        @Override
        void completeOther() {
            actual.onComplete();
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
        void completeMain() {
            done = true;
            if (wip.getAndIncrement() == 0) {
                emit();
                actual.onComplete();
            }
        }

        @Override
        void completeOther() {
            done = true;
            if (wip.getAndIncrement() == 0) {
                emit();
                actual.onComplete();
            }
        }

        @Override
        void run() {
            if (wip.getAndIncrement() == 0) {
                do {
                    boolean d = done;
                    emit();
                    if (d) {
                        actual.onComplete();
                        return;
                    }
                } while (wip.decrementAndGet() != 0);
            }
        }
    }
}
