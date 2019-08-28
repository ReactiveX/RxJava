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

import java.util.Collection;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;

public final class FlowableToList<T, U extends Collection<? super T>> extends AbstractFlowableWithUpstream<T, U> {
    final Supplier<U> collectionSupplier;

    public FlowableToList(Flowable<T> source, Supplier<U> collectionSupplier) {
        super(source);
        this.collectionSupplier = collectionSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super U> s) {
        U coll;
        try {
            coll = ExceptionHelper.nullCheck(collectionSupplier.get(), "The collectionSupplier returned a null Collection.");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptySubscription.error(e, s);
            return;
        }
        source.subscribe(new ToListSubscriber<T, U>(s, coll));
    }

    static final class ToListSubscriber<T, U extends Collection<? super T>>
    extends DeferredScalarSubscription<U>
    implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -8134157938864266736L;
        Subscription upstream;

        ToListSubscriber(Subscriber<? super U> actual, U collection) {
            super(actual);
            this.value = collection;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            U v = value;
            if (v != null) {
                v.add(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            value = null;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            complete(value);
        }

        @Override
        public void cancel() {
            super.cancel();
            upstream.cancel();
        }
    }
}
