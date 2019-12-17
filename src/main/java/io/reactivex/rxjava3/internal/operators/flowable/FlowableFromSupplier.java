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
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.internal.subscriptions.DeferredScalarSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

import java.util.Objects;

/**
 * Call a Supplier for each incoming Subscriber and signal the returned value or the thrown exception.
 * @param <T> the value type and element type returned by the supplier and the flow
 * @since 3.0.0
 */
public final class FlowableFromSupplier<T> extends Flowable<T> implements Supplier<T> {

    final Supplier<? extends T> supplier;

    public FlowableFromSupplier(Supplier<? extends T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void subscribeActual(Subscriber<? super T> s) {
        DeferredScalarSubscription<T> deferred = new DeferredScalarSubscription<T>(s);
        s.onSubscribe(deferred);

        T t;
        try {
            t = Objects.requireNonNull(supplier.get(), "The supplier returned a null value");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            if (deferred.isCancelled()) {
                RxJavaPlugins.onError(ex);
            } else {
                s.onError(ex);
            }
            return;
        }

        deferred.complete(t);
    }

    @Override
    public T get() throws Throwable {
        return Objects.requireNonNull(supplier.get(), "The supplier returned a null value");
    }
}
