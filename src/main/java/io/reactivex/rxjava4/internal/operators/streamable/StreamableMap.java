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
import java.util.concurrent.*;
import java.util.concurrent.Future.State;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.StreamerCancellation;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;
import io.reactivex.rxjava4.internal.util.ExceptionHelper;
import io.reactivex.rxjava4.operators.*;

public record StreamableMap<T, R>(
        @NonNull Streamable<T> source,
        @NonNull Function<? super T, ? extends R> mapper)
implements Streamable<R>, HasUpstreamStreamableSource<T> {

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull Streamer<@NonNull R> stream(@NonNull StreamerCancellation cancellation) {
        var upstream = source.stream(cancellation);
        if (upstream instanceof IndexableSource<?> isrc) {
            return new MapStreamerIndexed<>(upstream, (IndexableSource<T>)isrc, mapper);
        } else
        if (upstream instanceof DeferredEnumerableSource<?> esrc) {
            return new MapStreamerDeferredEnumerated<>(upstream, (DeferredEnumerableSource<T>)esrc, mapper);
        } else
        if (upstream instanceof EnumerableSource<?> esrc) {
            return new MapStreamerEnumerated<>(upstream, (EnumerableSource<T>)esrc, mapper);
        }
        return new MapStreamerBasic<>(upstream, mapper);
    }

    static abstract class MapStreamerBase<T, R> implements Streamer<R>, java.util.function.Function<Boolean, Boolean> {

        final Streamer<T> upstream;

        final Function<? super T, ? extends R> mapper;

        R current;

        MapStreamerBase(Streamer<T> upstream, Function<? super T, ? extends R> mapper) {
            this.upstream = upstream;
            this.mapper = mapper;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            var stage = upstream.next();
            if (stage instanceof CompletableFuture<Boolean> cf) {
                if (cf.state() == State.SUCCESS) {
                    if (cf.getNow(false)) {
                        try {
                            current = Objects.requireNonNull(mapper.apply(upstream.current()), "The mapper returned a null value");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            return CompletableFuture.failedFuture(ex);
                        }
                        return NEXT_TRUE;
                    }
                    return NEXT_FALSE;
                }
            }

            return stage.thenApply(this);
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

    static final class MapStreamerBasic<T, R> extends MapStreamerBase<T, R> {
        MapStreamerBasic(Streamer<T> upstream, Function<? super T, ? extends R> mapper) {
            super(upstream, mapper);
        }
    }

    static final class MapStreamerIndexed<T, R> extends MapStreamerBase<T, R>
    implements IndexableSource<R>, EnumerableSource<R> {

        final IndexableSource<T> indexed;

        long index = -1L;

        MapStreamerIndexed(Streamer<T> upstream, IndexableSource<T> indexed, Function<? super T, ? extends R> mapper) {
            super(upstream, mapper);
            this.indexed = indexed;
        }

        @Override
        public @NonNull R elementAt(long index) throws Throwable {
            return Objects.requireNonNull(mapper.apply(indexed.elementAt(index)), "The mapper returned a null item");
        }

        @Override
        public long limit() {
            return indexed.limit();
        }

        @Override
        public boolean nextSync() throws Throwable {
            if (++index >= indexed.limit()) {
                return false;
            }
            current = elementAt(index);
            return true;
        }
    }

    static final class MapStreamerEnumerated<T, R> extends MapStreamerBase<T, R>
    implements EnumerableSource<R> {

        final EnumerableSource<T> enumerable;

        MapStreamerEnumerated(Streamer<T> upstream, EnumerableSource<T> enumerable, Function<? super T, ? extends R> mapper) {
            super(upstream, mapper);
            this.enumerable = enumerable;
        }

        @Override
        public boolean nextSync() throws Throwable {
            if (enumerable.nextSync()) {
                current = Objects.requireNonNull(mapper.apply(enumerable.current()), "The mapper returned a null item");
                return true;
            }
            return false;
        }

    }

    static final class MapStreamerDeferredEnumerated<T, R> extends MapStreamerBase<T, R>
    implements DeferredEnumerableSource<R> {

        final DeferredEnumerableSource<T> enumerable;

        MapStreamerDeferredEnumerated(Streamer<T> upstream, DeferredEnumerableSource<T> enumerable, Function<? super T, ? extends R> mapper) {
            super(upstream, mapper);
            this.enumerable = enumerable;
        }

        @Override
        public boolean nextSync() throws Throwable {
            if (enumerable.nextSync()) {
                current = Objects.requireNonNull(mapper.apply(enumerable.current()), "The mapper returned a null item");
                return true;
            }
            return false;
        }

        @Override
        public CompletionStage<Boolean> enumerableReady() {
            return enumerable.enumerableReady();
        }

    }
}
