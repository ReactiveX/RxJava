/*
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

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.internal.fuseable.ScalarSupplier;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;

/**
 * A source Flowable that signals an onSubscribe() + onComplete() only.
 */
public final class FlowableEmpty extends Flowable<Object> implements ScalarSupplier<Object> {

    public static final Flowable<Object> INSTANCE = new FlowableEmpty();

    private FlowableEmpty() {
    }

    @Override
    public void subscribeActual(Subscriber<? super Object> s) {
        EmptySubscription.complete(s);
    }

    @Override
    public Object get() {
        return null; // null scalar is interpreted as being empty
    }
}
