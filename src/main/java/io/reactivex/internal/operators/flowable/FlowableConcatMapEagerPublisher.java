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

package io.reactivex.internal.operators.flowable;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.internal.operators.flowable.FlowableConcatMapEager.ConcatMapEagerDelayErrorSubscriber;
import io.reactivex.internal.util.ErrorMode;

/**
 * ConcatMapEager which works with an arbitrary Publisher source.
 * @param <T> the input value type
 * @param <R> the output type
 * @since 2.0.7 - experimental
 */
public final class FlowableConcatMapEagerPublisher<T, R> extends Flowable<R> {

    final Publisher<T> source;

    final Function<? super T, ? extends Publisher<? extends R>> mapper;

    final int maxConcurrency;

    final int prefetch;

    final ErrorMode errorMode;

    public FlowableConcatMapEagerPublisher(Publisher<T> source,
            Function<? super T, ? extends Publisher<? extends R>> mapper,
            int maxConcurrency,
            int prefetch,
            ErrorMode errorMode) {
        this.source = source;
        this.mapper = mapper;
        this.maxConcurrency = maxConcurrency;
        this.prefetch = prefetch;
        this.errorMode = errorMode;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new ConcatMapEagerDelayErrorSubscriber<T, R>(
                s, mapper, maxConcurrency, prefetch, errorMode));
    }
}
