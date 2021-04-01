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

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;

import java.util.Objects;

public final class FlowableDefer<T> extends Flowable<T> {
    final Supplier<? extends Publisher<? extends T>> supplier;
    public FlowableDefer(Supplier<? extends Publisher<? extends T>> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void subscribeActual(Subscriber<? super T> s) {
        Publisher<? extends T> pub;
        try {
            pub = Objects.requireNonNull(supplier.get(), "The publisher supplied is null");
        } catch (Throwable t) {
            Exceptions.throwIfFatal(t);
            EmptySubscription.error(t, s);
            return;
        }

        pub.subscribe(s);
    }
}
