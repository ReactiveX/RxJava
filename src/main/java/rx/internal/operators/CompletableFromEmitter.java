/*
 * Copyright 2016 Netflix, Inc.
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

import java.util.concurrent.atomic.AtomicBoolean;

import rx.*;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.internal.operators.OnSubscribeFromEmitter.CancellableSubscription;
import rx.internal.subscriptions.SequentialSubscription;
import rx.plugins.RxJavaHooks;

/**
 * Allows push-based emission of terminal events to a CompletableSubscriber.
 */
public final class CompletableFromEmitter implements Completable.OnSubscribe {

    final Action1<CompletableEmitter> producer;

    public CompletableFromEmitter(Action1<CompletableEmitter> producer) {
        this.producer = producer;
    }

    @Override
    public void call(CompletableSubscriber t) {
        FromEmitter emitter = new FromEmitter(t);
        t.onSubscribe(emitter);

        try {
            producer.call(emitter);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            emitter.onError(ex);
        }

    }

    static final class FromEmitter
    extends AtomicBoolean
    implements CompletableEmitter, Subscription {

        /** */
        private static final long serialVersionUID = 5539301318568668881L;

        final CompletableSubscriber actual;

        final SequentialSubscription resource;

        public FromEmitter(CompletableSubscriber actual) {
            this.actual = actual;
            resource = new SequentialSubscription();
        }

        @Override
        public void onCompleted() {
            if (compareAndSet(false, true)) {
                try {
                    actual.onCompleted();
                } finally {
                    resource.unsubscribe();
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (compareAndSet(false, true)) {
                try {
                    actual.onError(t);
                } finally {
                    resource.unsubscribe();
                }
            } else {
                RxJavaHooks.onError(t);
            }
        }

        @Override
        public void setSubscription(Subscription s) {
            resource.update(s);
        }

        @Override
        public void setCancellation(Cancellable c) {
            setSubscription(new CancellableSubscription(c));
        }

        @Override
        public void unsubscribe() {
            if (compareAndSet(false, true)) {
                resource.unsubscribe();
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return get();
        }

    }
}
