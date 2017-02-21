/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;


import java.util.concurrent.atomic.AtomicInteger;

import rx.*;
import rx.internal.producers.ProducerArbiter;
import rx.subscriptions.SerialSubscription;

/**
 * If the Observable completes without emitting any items, subscribe to an alternate Observable. Allows for similar
 * functionality to {@link rx.Observable#defaultIfEmpty(Object)} except instead of one item being emitted when
 * empty, the results of the given Observable will be emitted.
 * @param <T> the value type
 */
public final class OnSubscribeSwitchIfEmpty<T> implements Observable.OnSubscribe<T> {

    final Observable<? extends T> source;

    final Observable<? extends T> alternate;

    public OnSubscribeSwitchIfEmpty(Observable<? extends T> source, Observable<? extends T> alternate) {
        this.source = source;
        this.alternate = alternate;
    }

    @Override
    public void call(Subscriber<? super T> child) {
        final SerialSubscription serial = new SerialSubscription();
        ProducerArbiter arbiter = new ProducerArbiter();
        final ParentSubscriber<T> parent = new ParentSubscriber<T>(child, serial, arbiter, alternate);

        serial.set(parent);
        child.add(serial);
        child.setProducer(arbiter);

        parent.subscribe(source);
    }

    static final class ParentSubscriber<T> extends Subscriber<T> {

        private boolean empty = true;
        private final Subscriber<? super T> child;
        private final SerialSubscription serial;
        private final ProducerArbiter arbiter;
        private final Observable<? extends T> alternate;

        final AtomicInteger wip;
        volatile boolean active;

        ParentSubscriber(Subscriber<? super T> child, final SerialSubscription serial, ProducerArbiter arbiter, Observable<? extends T> alternate) {
            this.child = child;
            this.serial = serial;
            this.arbiter = arbiter;
            this.alternate = alternate;
            this.wip = new AtomicInteger();
        }

        @Override
        public void setProducer(final Producer producer) {
            arbiter.setProducer(producer);
        }

        @Override
        public void onCompleted() {
            if (!empty) {
                child.onCompleted();
            } else if (!child.isUnsubscribed()) {
                active = false;
                subscribe(null);
            }
        }

        void subscribe(Observable<? extends T> source) {
            if (wip.getAndIncrement() == 0) {
                do {
                    if (child.isUnsubscribed()) {
                        break;
                    }

                    if (!active) {
                        if (source == null) {
                            AlternateSubscriber<T> as = new AlternateSubscriber<T>(child, arbiter);
                            serial.set(as);
                            active = true;
                            alternate.unsafeSubscribe(as);
                        } else {
                            active = true;
                            source.unsafeSubscribe(this);
                            source = null;
                        }
                    }

                } while (wip.decrementAndGet() != 0);
            }
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(T t) {
            empty = false;
            child.onNext(t);
            arbiter.produced(1);
        }
    }

    static final class AlternateSubscriber<T> extends Subscriber<T> {

        private final ProducerArbiter arbiter;
        private final Subscriber<? super T> child;

        AlternateSubscriber(Subscriber<? super T> child, ProducerArbiter arbiter) {
            this.child = child;
            this.arbiter = arbiter;
        }

        @Override
        public void setProducer(final Producer producer) {
            arbiter.setProducer(producer);
        }

        @Override
        public void onCompleted() {
            child.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(T t) {
            child.onNext(t);
            arbiter.produced(1);
        }
    }
}
