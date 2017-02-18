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
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.processors.*;
import io.reactivex.subscribers.SerializedSubscriber;

public final class FlowableRepeatWhen<T> extends AbstractFlowableWithUpstream<T, T> {
    final Function<? super Flowable<Object>, ? extends Publisher<?>> handler;

    public FlowableRepeatWhen(Flowable<T> source,
            Function<? super Flowable<Object>, ? extends Publisher<?>> handler) {
        super(source);
        this.handler = handler;
    }

    @Override
    public void subscribeActual(Subscriber<? super T> s) {

        SerializedSubscriber<T> z = new SerializedSubscriber<T>(s);

        FlowableProcessor<Object> processor = UnicastProcessor.<Object>create(8).toSerialized();

        Publisher<?> when;

        try {
            when = ObjectHelper.requireNonNull(handler.apply(processor), "handler returned a null Publisher");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        WhenReceiver<T, Object> receiver = new WhenReceiver<T, Object>(source);

        RepeatWhenSubscriber<T> subscriber = new RepeatWhenSubscriber<T>(z, processor, receiver);

        receiver.subscriber = subscriber;

        s.onSubscribe(subscriber);

        when.subscribe(receiver);

        receiver.onNext(0);
    }

    static final class WhenReceiver<T, U>
    extends AtomicInteger
    implements FlowableSubscriber<Object>, Subscription {


        private static final long serialVersionUID = 2827772011130406689L;

        final Publisher<T> source;

        final AtomicReference<Subscription> subscription;

        final AtomicLong requested;

        WhenSourceSubscriber<T, U> subscriber;

        WhenReceiver(Publisher<T> source) {
            this.source = source;
            this.subscription = new AtomicReference<Subscription>();
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(subscription, requested, s);
        }

        @Override
        public void onNext(Object t) {
            if (getAndIncrement() == 0) {
                for (;;) {
                    if (SubscriptionHelper.isCancelled(subscription.get())) {
                        return;
                    }

                    source.subscribe(subscriber);

                    if (decrementAndGet() == 0) {
                        break;
                    }
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            subscriber.cancel();
            subscriber.actual.onError(t);
        }

        @Override
        public void onComplete() {
            subscriber.cancel();
            subscriber.actual.onComplete();
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(subscription, requested, n);
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(subscription);
        }
    }

    abstract static class WhenSourceSubscriber<T, U> extends SubscriptionArbiter implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -5604623027276966720L;

        protected final Subscriber<? super T> actual;

        protected final FlowableProcessor<U> processor;

        protected final Subscription receiver;

        private long produced;

        WhenSourceSubscriber(Subscriber<? super T> actual, FlowableProcessor<U> processor,
                Subscription receiver) {
            this.actual = actual;
            this.processor = processor;
            this.receiver = receiver;
        }

        @Override
        public final void onSubscribe(Subscription s) {
            setSubscription(s);
        }

        @Override
        public final void onNext(T t) {
            produced++;
            actual.onNext(t);
        }

        protected final void again(U signal) {
            long p = produced;
            if (p != 0L) {
                produced = 0L;
                produced(p);
            }
            receiver.request(1);
            processor.onNext(signal);
        }

        @Override
        public final void cancel() {
            super.cancel();
            receiver.cancel();
        }
    }

    static final class RepeatWhenSubscriber<T> extends WhenSourceSubscriber<T, Object> {


        private static final long serialVersionUID = -2680129890138081029L;

        RepeatWhenSubscriber(Subscriber<? super T> actual, FlowableProcessor<Object> processor,
                Subscription receiver) {
            super(actual, processor, receiver);
        }

        @Override
        public void onError(Throwable t) {
            receiver.cancel();
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            again(0);
        }
    }
}
