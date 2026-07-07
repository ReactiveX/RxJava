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

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.StreamerCancellation;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;
import io.reactivex.rxjava4.internal.util.ExceptionHelper;

public record StreamableMap<T, R>(
        @NonNull Streamable<T> source,
        @NonNull Function<? super T, ? extends R> mapper)
implements Streamable<R>, HasUpstreamStreamableSource<T> {

    @Override
    public @NonNull Streamer<@NonNull R> stream(@NonNull StreamerCancellation cancellation) {
        return new MapStreamer<>(source.stream(cancellation), mapper);
    }

    static final class MapStreamer<T, R> implements Streamer<R>, java.util.function.Function<Boolean, Boolean> {
        final Streamer<T> upstream;
        final Function<? super T, ? extends R> mapper;
        volatile R current;
        MapStreamer(Streamer<T> upstream, Function<? super T, ? extends R> mapper) {
            this.upstream = upstream;
            this.mapper = mapper;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            return upstream.next().thenApply(this);
        }

        @Override
        public Boolean apply(Boolean e) {
            if (e) {
                try {
                    current = Objects.requireNonNull(mapper.apply(upstream.current()), "The mapper returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    throw ExceptionHelper.wrapOrThrow(ex);
                }
                return true;
            }
            return false;
        }

        @Override
        public @NonNull R current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            current = null;
            return upstream.finish();
        }
    }
}
