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

package io.reactivex.rxjava4.internal.operators.streamable;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collector;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.StreamerCancellation;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;

public record StreamableCollector<T, A, R>(
        Streamable<T> source,
        Collector<T, A, R> collector
) implements Streamable<R>, HasUpstreamStreamableSource<T> {

    @Override
    public @NonNull Streamer<@NonNull R> stream(@NonNull StreamerCancellation cancellation) {
        return new CollectorStreamable<>(
                source.stream(cancellation),
                collector.supplier().get(),
                collector.accumulator(),
                collector.finisher());
    }

    static final class CollectorStreamable<T, A, R>
    implements Streamer<R>, BiConsumer<Object, Throwable> {

        final AtomicInteger wip;

        final Streamer<T> upstream;

        final A storage;

        final BiConsumer<A, T> accumulator;

        final Function<A, R> finisher;

        final CompletableFuture<Boolean> nextReady;

        final CompletableFuture<Void> finishReady;

        R current;

        int stage;

        volatile boolean done;

        CollectorStreamable(Streamer<T> upstream, A storage, BiConsumer<A, T> accumulator, Function<A, R> finisher) {
            this.upstream = upstream;
            this.wip = new AtomicInteger();
            this.storage = storage;
            this.accumulator = accumulator;
            this.finisher = finisher;
            this.nextReady = new CompletableFuture<>();
            this.finishReady = new CompletableFuture<>();
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            if (stage++ == 0) {
                drain();
                return nextReady;
            }
            return NEXT_FALSE;
        }

        @Override
        public @NonNull R current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            done = true;
            drain();
            return finishReady;
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            do {
                if (done) {
                    upstream.finish().whenComplete(this);
                    break;
                } else {
                    upstream.next().whenComplete(this);
                }
            } while (wip.decrementAndGet() != 0);
        }

        @Override
        public void accept(Object t, Throwable u) {
            if (done) {
                if (u != null) {
                    finishReady.completeExceptionally(u);
                } else {
                    finishReady.complete(null);
                }
            } else {
                if (u != null) {
                    nextReady.completeExceptionally(u);
                } else
                if ((Boolean)t) {
                    accumulator.accept(storage, upstream.current());
                    drain();
                } else {
                    current = finisher.apply(storage);
                    nextReady.complete(true);
                }
            }
        }
    }
}
