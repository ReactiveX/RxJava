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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.DisposableContainer;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;

public record StreamableMapOptional<T, R>(
        @NonNull Streamable<T> source,
        @NonNull Function<? super T, ? extends Optional<? extends R>> mapper)
implements Streamable<R>, HasUpstreamStreamableSource<T> {

    @Override
    public @NonNull Streamer<@NonNull R> stream(@NonNull DisposableContainer cancellation) {
        return new MapStreamer<>(source.stream(cancellation), mapper);
    }

    static final class MapStreamer<T, R> implements Streamer<R> {
        final Streamer<T> upstream;
        final Function<? super T, ? extends Optional<? extends R>> mapper;
        volatile R current;

        final AtomicInteger wip = new AtomicInteger();

        MapStreamer(Streamer<T> upstream, Function<? super T, ? extends Optional<? extends R>> mapper) {
            this.upstream = upstream;
            this.mapper = mapper;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next(@NonNull DisposableContainer cancellation) {
            var cf = new CompletableFuture<Boolean>();
            drain(cf, cancellation);
            return cf;
        }

        @Override
        public @NonNull R current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish(@NonNull DisposableContainer cancellation) {
            current = null;
            return upstream.finish(cancellation);
        }

        void drain(CompletableFuture<Boolean> cf, DisposableContainer cancellation) {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            do {
                upstream.next(cancellation)
                .whenComplete((v, e) -> {
                    if (e != null) {
                        cf.completeExceptionally(e);
                    } else  {
                        if (v) {
                            try {
                                var w = Objects.requireNonNull(mapper.apply(upstream.current()), "The mapper returned a null value");
                                if (w.isPresent()) {
                                    current = w.get();
                                    cf.complete(true);
                                } else {
                                    drain(cf, cancellation);
                                }
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                cf.completeExceptionally(ex);
                            }
                        } else {
                            cf.complete(false);
                        }
                    }
                });
            } while (wip.decrementAndGet() != 0);
        }
    }
}
