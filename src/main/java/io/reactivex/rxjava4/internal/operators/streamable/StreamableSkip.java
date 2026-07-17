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

import java.io.Serial;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.StreamerCancellation;

public record StreamableSkip<T>(Streamable<T> source, long count) implements Streamable<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        return new SkipStreamer<>(source.stream(cancellation), count);
    }

    static final class SkipStreamer<T> extends AtomicInteger implements Streamer<T>, BiConsumer<Boolean, Throwable> {

        @Serial
        private static final long serialVersionUID = 1988154737845167665L;

        final Streamer<T> upstream;

        long remaining;

        CompletableFuture<Boolean> waiter;

        SkipStreamer(Streamer<T> upstream, long count) {
            this.upstream = upstream;
            this.remaining = count;
            this.waiter = new CompletableFuture<>();
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            if (remaining <= 0) {
                return upstream.next();
            }
            drain();
            return waiter;
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            int wipMax = 1;
            int wipIndex = 0;
            do {
                StreamableHelper.whenComplete(upstream.next(), this);
                if (++wipIndex == wipMax) {
                    wipMax = get();
                    if (wipIndex == wipMax) {
                        wipMax = addAndGet(-wipMax);
                        if (wipMax != 0) {
                            wipIndex = 0;
                        }
                    }
                }
            } while (wipMax != 0);
        }

        @Override
        public void accept(Boolean t, Throwable u) {
            if (u != null) {
                waiter.completeExceptionally(u);
            } else
            if (t) {
                if (remaining-- > 0) {
                    drain();
                } else {
                    waiter.complete(true);
                }
            } else {
                waiter.complete(false);
            }
        }

        @Override
        public @NonNull T current() {
            return upstream.current();
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            return upstream.finish();
        }
    }
}
