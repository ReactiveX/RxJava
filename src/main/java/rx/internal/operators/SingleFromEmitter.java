/**
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
import rx.Single.OnSubscribe;
import rx.exceptions.Exceptions;
import rx.functions.*;
import rx.internal.subscriptions.*;
import rx.plugins.RxJavaHooks;

/**
 * Calls an action with a SingleEmitter instance for each individual subscribers that
 * generates a terminal signal (eventually).
 *
 * @param <T> the success value type
 */
public final class SingleFromEmitter<T> implements OnSubscribe<T> {

    final Action1<SingleEmitter<T>> producer;

    public SingleFromEmitter(Action1<SingleEmitter<T>> producer) {
        this.producer = producer;
    }

    @Override
    public void call(SingleSubscriber<? super T> t) {
        SingleEmitterImpl<T> parent = new SingleEmitterImpl<T>(t);
        t.add(parent);

        try {
            producer.call(parent);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            parent.onError(ex);
        }
    }

    static final class SingleEmitterImpl<T>
    extends AtomicBoolean
    implements SingleEmitter<T>, Subscription {
        private static final long serialVersionUID = 8082834163465882809L;

        final SingleSubscriber<? super T> actual;

        final SequentialSubscription resource;

        SingleEmitterImpl(SingleSubscriber<? super T> actual) {
            this.actual = actual;
            this.resource = new SequentialSubscription();
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

        @Override
        public void onSuccess(T t) {
            if (compareAndSet(false, true)) {
                try {
                    actual.onSuccess(t);
                } finally {
                    resource.unsubscribe();
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (t == null) {
                t = new NullPointerException();
            }
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
    }
}
