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
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;
import io.reactivex.rxjava4.operators.*;

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
                collector.finisher(),
                cancellation);
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

        final StreamerCancellation cancellation;

        R current;

        boolean once;

        volatile boolean done;

        CollectorStreamable(Streamer<T> upstream, A storage,
                BiConsumer<A, T> accumulator,
                Function<A, R> finisher,
                StreamerCancellation cancellation) {
            this.upstream = upstream;
            this.wip = new AtomicInteger();
            this.storage = storage;
            this.accumulator = accumulator;
            this.finisher = finisher;
            this.nextReady = new CompletableFuture<>();
            this.finishReady = new CompletableFuture<>();
            this.cancellation = cancellation;
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NonNull CompletionStage<Boolean> next() {
            if (!once) {
                once = true;

                if (upstream instanceof IndexableSource<?> isrc) {
                    long max = isrc.limit();
                    for (long index = 0; index < max; index++) {
                        if (cancellation.isDisposed()) {
                            return CompletableFuture.failedFuture(new CancellationException());
                        }

                        T value;

                        try {
                            value = (T)isrc.elementAt(index);
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            return CompletableFuture.failedFuture(ex);
                        }
                        accumulator.accept(storage, value);
                    }
                    current = finisher.apply(storage);
                    return NEXT_TRUE;
                } else
                if (upstream instanceof DeferredEnumerableSource<?> dsrc) {
                    StreamableHelper.whenComplete(dsrc.enumerableReady(), this::deferredEnumerate);
                    return nextReady;
                } else
                if (upstream instanceof EnumerableSource<?> esrc) {
                    try {
                        while (esrc.nextSync()) {
                            if (cancellation.isDisposed()) {
                                return CompletableFuture.failedFuture(new CancellationException());
                            }
                            accumulator.accept(storage, (T)esrc.current());
                        }
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        return CompletableFuture.failedFuture(ex);
                    }
                    current = finisher.apply(storage);
                    return NEXT_TRUE;
                }
                drain();
                return nextReady;
            }
            return NEXT_FALSE;
        }

        void deferredEnumerate(boolean hasInitialValue, Throwable error) {
            if (error != null) {
                nextReady.completeExceptionally(error);
                return;
            }
            if (hasInitialValue) {
                @SuppressWarnings("unchecked")
                var upstreamCast = (DeferredEnumerableSource<T>)upstream;

                try {
                    while (upstreamCast.nextSync()) {
                        if (cancellation.isDisposed()) {
                            nextReady.completeExceptionally(new CancellationException());
                            return;
                        }
                        accumulator.accept(storage, upstreamCast.current());
                    }
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    nextReady.completeExceptionally(ex);
                    return;
                }
            }
            current = finisher.apply(storage);
            nextReady.complete(true);
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

            int wipMax = 1;
            int wipIndex = 0;
            do {
                if (done) {
                    StreamableHelper.whenComplete(upstream.finish(), this);
                    break;
                } else {
                    StreamableHelper.whenComplete(upstream.next(), this);
                }
                if (++wipIndex == wipMax) {
                    var newWip = wip.get();
                    if (newWip != wipMax) {
                        wipMax = newWip;
                    } else {
                        wipMax = wip.addAndGet(-wipMax);
                        if (wipMax == 0) {
                            break;
                        }
                        wipIndex = 0;
                    }
                }
            } while (true);
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
