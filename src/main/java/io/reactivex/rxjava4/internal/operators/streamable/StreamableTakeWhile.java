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

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.StreamerCancellation;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.Predicate;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;

public record StreamableTakeWhile<T>(Streamable<T> source, Predicate<? super T> predicate)
implements Streamable<T>, HasUpstreamStreamableSource<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        return new TakeWhileStreamer<>(source.stream(cancellation), predicate);
    }

    static final class TakeWhileStreamer<T>
    implements Streamer<T>, java.util.function.Function<Boolean, CompletionStage<Boolean>> {

        final Streamer<T> upstream;

        final Predicate<? super T> predicate;

        TakeWhileStreamer(Streamer<T> upstream, Predicate<? super T> predicate) {
            this.upstream = upstream;
            this.predicate = predicate;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            return upstream.next().thenCompose(this);
        }

        @Override
        public @NonNull T current() {
            return upstream.current();
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            return upstream.finish();
        }

        @Override
        public @NonNull CompletionStage<Boolean> apply(@NonNull Boolean t) {
            if (t) {
                try {
                    if (predicate.test(upstream.current())) {
                        return NEXT_TRUE;
                    }
                    return NEXT_FALSE;
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    return CompletableFuture.failedFuture(ex);
                }
            }
            return NEXT_FALSE;
        }
    }
}
