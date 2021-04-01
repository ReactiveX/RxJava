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

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Supplier;
import org.reactivestreams.Subscriber;

import java.util.Objects;

public final class FlowableOnBackpressureReduceWith<T, R> extends AbstractFlowableWithUpstream<T, R> {

    final BiFunction<R, ? super T, R> reducer;
    final Supplier<R> supplier;

    public FlowableOnBackpressureReduceWith(@NonNull Flowable<T> source,
                                            @NonNull Supplier<R> supplier,
                                            @NonNull BiFunction<R, ? super T, R> reducer) {
        super(source);
        this.reducer = reducer;
        this.supplier = supplier;
    }

    @Override
    protected void subscribeActual(@NonNull Subscriber<? super R> s) {
        source.subscribe(new BackpressureReduceWithSubscriber<>(s, supplier, reducer));
    }

    static final class BackpressureReduceWithSubscriber<T, R> extends AbstractBackpressureThrottlingSubscriber<T, R> {

        private static final long serialVersionUID = 8255923705960622424L;

        final BiFunction<R, ? super T, R> reducer;
        final Supplier<R> supplier;

        BackpressureReduceWithSubscriber(@NonNull Subscriber<? super R> downstream,
                                         @NonNull Supplier<R> supplier,
                                         @NonNull BiFunction<R, ? super T, R> reducer) {
            super(downstream);
            this.reducer = reducer;
            this.supplier = supplier;
        }

        @Override
        public void onNext(T t) {
            R v = current.get();
            if (v != null) {
                v = current.getAndSet(null);
            }
            try {
                if (v == null) {
                    current.lazySet(Objects.requireNonNull(
                            reducer.apply(Objects.requireNonNull(supplier.get(), "The supplier returned a null value"), t),
                            "The reducer returned a null value"
                    ));
                } else {
                    current.lazySet(Objects.requireNonNull(reducer.apply(v, t), "The reducer returned a null value"));
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.cancel();
                onError(ex);
                return;
            }
            drain();
        }
    }
}
