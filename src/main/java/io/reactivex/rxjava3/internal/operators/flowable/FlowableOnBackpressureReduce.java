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
import org.reactivestreams.Subscriber;

import java.util.Objects;

public final class FlowableOnBackpressureReduce<T> extends AbstractFlowableWithUpstream<T, T> {

    final BiFunction<T, T, T> reducer;

    public FlowableOnBackpressureReduce(@NonNull Flowable<T> source, @NonNull  BiFunction<T, T, T> reducer) {
        super(source);
        this.reducer = reducer;
    }

    @Override
    protected void subscribeActual(@NonNull Subscriber<? super T> s) {
        source.subscribe(new BackpressureReduceSubscriber<>(s, reducer));
    }

    static final class BackpressureReduceSubscriber<T> extends AbstractBackpressureThrottlingSubscriber<T, T> {

        private static final long serialVersionUID = 821363947659780367L;

        final BiFunction<T, T, T> reducer;

        BackpressureReduceSubscriber(@NonNull Subscriber<? super T> downstream, @NonNull BiFunction<T, T, T> reducer) {
            super(downstream);
            this.reducer = reducer;
        }

        @Override
        public void onNext(T t) {
            T v = current.get();
            if (v != null) {
                v = current.getAndSet(null);
            }
            if (v == null) {
                current.lazySet(t);
            } else {
                try {
                    current.lazySet(Objects.requireNonNull(reducer.apply(v, t), "The reducer returned a null value"));
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    upstream.cancel();
                    onError(ex);
                    return;
                }
            }
            drain();
        }
    }
}
