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

package io.reactivex.internal.operators.parallel;

import org.reactivestreams.*;

import io.reactivex.functions.Function;
import io.reactivex.internal.operators.flowable.FlowableFlatMap;
import io.reactivex.parallel.ParallelFlowable;

/**
 * Flattens the generated Publishers on each rail.
 *
 * @param <T> the input value type
 * @param <R> the output value type
 */
public final class ParallelFlatMap<T, R> extends ParallelFlowable<R> {

    final ParallelFlowable<T> source;

    final Function<? super T, ? extends Publisher<? extends R>> mapper;

    final boolean delayError;

    final int maxConcurrency;

    final int prefetch;

    public ParallelFlatMap(
            ParallelFlowable<T> source,
            Function<? super T, ? extends Publisher<? extends R>> mapper,
            boolean delayError,
            int maxConcurrency,
            int prefetch) {
        this.source = source;
        this.mapper = mapper;
        this.delayError = delayError;
        this.maxConcurrency = maxConcurrency;
        this.prefetch = prefetch;
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }

    @Override
    public void subscribe(Subscriber<? super R>[] subscribers) {
        if (!validate(subscribers)) {
            return;
        }

        int n = subscribers.length;

        @SuppressWarnings("unchecked")
        final Subscriber<T>[] parents = new Subscriber[n];

        for (int i = 0; i < n; i++) {
            parents[i] = FlowableFlatMap.subscribe(subscribers[i], mapper, delayError, maxConcurrency, prefetch);
        }

        source.subscribe(parents);
    }
}
