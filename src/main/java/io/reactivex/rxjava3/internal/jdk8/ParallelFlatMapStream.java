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

package io.reactivex.rxjava3.internal.jdk8;

import java.util.stream.Stream;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.parallel.ParallelFlowable;

/**
 * Flattens the generated {@link Stream}s on each rail.
 *
 * @param <T> the input value type
 * @param <R> the output value type
 * @since 3.0.0
 */
public final class ParallelFlatMapStream<T, R> extends ParallelFlowable<R> {

    final ParallelFlowable<T> source;

    final Function<? super T, ? extends Stream<? extends R>> mapper;

    final int prefetch;

    public ParallelFlatMapStream(
            ParallelFlowable<T> source,
            Function<? super T, ? extends Stream<? extends R>> mapper,
            int prefetch) {
        this.source = source;
        this.mapper = mapper;
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
            parents[i] = FlowableFlatMapStream.subscribe(subscribers[i], mapper, prefetch);
        }

        source.subscribe(parents);
    }
}
