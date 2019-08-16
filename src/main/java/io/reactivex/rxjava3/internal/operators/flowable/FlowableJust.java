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

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.internal.fuseable.ScalarSupplier;
import io.reactivex.rxjava3.internal.subscriptions.ScalarSubscription;

/**
 * Represents a constant scalar value.
 * @param <T> the value type
 */
public final class FlowableJust<T> extends Flowable<T> implements ScalarSupplier<T> {
    private final T value;
    public FlowableJust(final T value) {
        this.value = value;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        s.onSubscribe(new ScalarSubscription<T>(s, value));
    }

    @Override
    public T get() {
        return value;
    }
}
