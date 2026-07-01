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

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.DisposableContainer;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.Predicate;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;

public record StreamableFilter<T>(
        @NonNull Streamable<T> source,
        @NonNull Predicate<? super T> predicate)
implements Streamable<T>, HasUpstreamStreamableSource<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull DisposableContainer cancellation) {
        return new FilterStreamer<>(source.stream(cancellation), predicate);
    }

    static final class FilterStreamer<T> implements Streamer<T> {
        final Streamer<T> upstream;
        final Predicate<? super T> predicate;
        volatile T current;

        final AtomicInteger wip = new AtomicInteger();

        FilterStreamer(Streamer<T> upstream, Predicate<? super T> predicate) {
            this.upstream = upstream;
            this.predicate = predicate;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next(@NonNull DisposableContainer cancellation) {
            var cf = new CompletableFuture<Boolean>();
            drain(cf, cancellation);
            return cf;
        }

        @Override
        public @NonNull T current() {
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
                                var w = upstream.current();
                                if (predicate.test(w)) {
                                    current = w;
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
