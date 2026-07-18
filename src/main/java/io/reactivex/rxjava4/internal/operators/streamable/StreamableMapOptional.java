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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Future.State;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.StreamerCancellation;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;
import io.reactivex.rxjava4.operators.*;

public record StreamableMapOptional<T, R>(
        @NonNull Streamable<T> source,
        @NonNull Function<? super T, ? extends Optional<? extends R>> mapper)
implements Streamable<R>, HasUpstreamStreamableSource<T> {

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull Streamer<@NonNull R> stream(@NonNull StreamerCancellation cancellation) {
        var upstream = source.stream(cancellation);
        if (upstream instanceof DeferredEnumerableSource<?> dsrc) {
            return new MapOptionalStreamerDeferred<T, R>(upstream, (DeferredEnumerableSource<T>)dsrc, mapper);
        } else
        if (upstream instanceof EnumerableSource<?> esrc) {
            return new MapOptionalStreamerEnumerable<T, R>(upstream, (EnumerableSource<T>)esrc, mapper);
        }
        return new MapOptionalStreamerBasic<>(upstream, mapper);
    }

    static abstract class MapOptionalStreamer<T, R> extends AtomicInteger
    implements Streamer<R>, BiConsumer<Boolean, Throwable> {

        @Serial
        private static final long serialVersionUID = 6360266262761297472L;

        final Streamer<T> upstream;

        final Function<? super T, ? extends Optional<? extends R>> mapper;

        R current;

        CompletableFuture<Boolean> nextReady;

        MapOptionalStreamer(Streamer<T> upstream, Function<? super T, ? extends Optional<? extends R>> mapper) {
            this.upstream = upstream;
            this.mapper = mapper;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            nextReady = new CompletableFuture<>();
            drain();
            return nextReady;
        }

        void drain() {
            for (;;) {
                var upstreamNext = upstream.next().toCompletableFuture();
                var state = upstreamNext.state();

                if (state == State.RUNNING) {
                    set(1);
                    upstreamNext.whenComplete(this);
                    if (compareAndSet(1, 0)) {
                        return;
                    }
                    state = upstreamNext.state();
                }
                if (state == State.SUCCESS) {
                    if (upstreamNext.getNow(false)) {
                        boolean pass;
                        T value = upstream.current();
                        Optional<? extends R> result = null;
                        try {
                            result = Objects.requireNonNull(mapper.apply(value), "The mapper returned a null item");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            nextReady.completeExceptionally(ex);
                            return;
                        }
                        pass = result.isEmpty();
                        if (!pass) {
                            current = result.get();
                            nextReady.complete(true);
                            return;
                        }
                    } else {
                        nextReady.complete(false);
                        return;
                    }
                } else {
                    nextReady.completeExceptionally(upstreamNext.exceptionNow());
                    return;
                }
            }
        }

        @Override
        public void accept(Boolean t, Throwable u) {
            if (!compareAndSet(1, 2)) {
                if (u != null) {
                    nextReady.completeExceptionally(u);
                } else
                if (t) {
                    boolean pass;
                    T value = upstream.current();
                    Optional<? extends R> result = null;
                    try {
                        result = Objects.requireNonNull(mapper.apply(value), "The mapper returned a null item");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        nextReady.completeExceptionally(ex);
                        return;
                    }
                    pass = result.isEmpty();
                    if (!pass) {
                        current = result.get();
                        nextReady.complete(true);
                        return;
                    }
                    drain();
                } else {
                    nextReady.complete(false);
                }
            }
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

    static final class MapOptionalStreamerBasic<T, R> extends MapOptionalStreamer<T, R> {

        @Serial
        private static final long serialVersionUID = -1776499708262136887L;

        MapOptionalStreamerBasic(Streamer<T> upstream, Function<? super T, ? extends Optional<? extends R>> mapper) {
            super(upstream, mapper);
        }
    }

    static final class MapOptionalStreamerDeferred<T, R> extends MapOptionalStreamer<T, R>
    implements DeferredEnumerableSource<R> {

        @Serial
        private static final long serialVersionUID = -1776499708262136887L;

        final DeferredEnumerableSource<T> deferred;

        MapOptionalStreamerDeferred(Streamer<T> upstream,
                DeferredEnumerableSource<T> deferred,
                Function<? super T, ? extends Optional<? extends R>> mapper) {
            super(upstream, mapper);
            this.deferred = deferred;
        }

        @Override
        public boolean nextSync() throws Throwable {
            for (;;) {
                if (deferred.nextSync()) {
                    var value = deferred.current();
                    var result = Objects.requireNonNull(mapper.apply(value), "The mapper returned a null item");
                    if (!result.isEmpty()) {
                        current = result.get();
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }

        @Override
        public CompletionStage<Boolean> enumerableReady() {
            return deferred.enumerableReady();
        }
    }

    static final class MapOptionalStreamerEnumerable<T, R> extends MapOptionalStreamer<T, R>
    implements EnumerableSource<R> {

        @Serial
        private static final long serialVersionUID = -1776499708262136887L;

        final EnumerableSource<T> deferred;

        MapOptionalStreamerEnumerable(Streamer<T> upstream,
                EnumerableSource<T> deferred,
                Function<? super T, ? extends Optional<? extends R>> mapper) {
            super(upstream, mapper);
            this.deferred = deferred;
        }

        @Override
        public boolean nextSync() throws Throwable {
            for (;;) {
                if (deferred.nextSync()) {
                    var value = deferred.current();
                    var result = Objects.requireNonNull(mapper.apply(value), "The mapper returned a null item");
                    if (!result.isEmpty()) {
                        current = result.get();
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }
    }
}
