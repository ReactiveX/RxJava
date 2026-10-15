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
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.Future.State;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.StreamerCancellation;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;
import io.reactivex.rxjava4.operators.*;

public record StreamableCollect<T, U>(
        Streamable<T> source,
        Supplier<? extends U> initialItemSupplier,
        BiConsumer<? super U, ? super T> collector
) implements Streamable<U>, HasUpstreamStreamableSource<T> {

    @Override
    public @NonNull Streamer<@NonNull U> stream(@NonNull StreamerCancellation cancellation) {
        U storage;
        try {
            storage = Objects.requireNonNull(initialItemSupplier.get());
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            return StreamableError.createFailed(ex);
        }
        return new CollectStreamable<>(
                source.stream(cancellation),
                storage,
                collector,
                cancellation);
    }

    static final class CollectStreamable<T, U>
    extends AtomicInteger
    implements Streamer<U>, java.util.function.BiConsumer<Object, Throwable> {

        @Serial
        private static final long serialVersionUID = -2893736592415047972L;

        final Streamer<T> upstream;

        final U storage;

        final BiConsumer<? super U, ? super T> collector;

        final CompletableFuture<Boolean> nextReady;

        final CompletableFuture<Void> finishReady;

        final StreamerCancellation cancellation;

        U current;

        boolean once;

        boolean deferredOnce;

        volatile boolean done;

        CollectStreamable(Streamer<T> upstream, U storage,
                BiConsumer<? super U, ? super T> collector,
                StreamerCancellation cancellation) {
            this.upstream = upstream;
            this.storage = storage;
            this.collector = collector;
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

                        try {
                            var value = (T)isrc.elementAt(index);
                            collector.accept(storage, value);
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            return CompletableFuture.failedFuture(ex);
                        }
                    }
                    current = storage;
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
                            collector.accept(storage, (T)esrc.current());
                        }
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        return CompletableFuture.failedFuture(ex);
                    }
                    current = storage;
                    return NEXT_TRUE;
                }
                drain();
                return nextReady;
            }
            return NEXT_FALSE;
        }

        void deferredEnumerate(Boolean hasInitialValue, Throwable error) {
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
                        collector.accept(storage, upstreamCast.current());
                    }
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    nextReady.completeExceptionally(ex);
                    return;
                }
            }
            current = storage;
            nextReady.complete(true);
        }

        @Override
        public @NonNull U current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            done = true;
            drain();
            return finishReady;
        }

        void drain() {
            for (;;) {
                if (done) {
                    StreamableHelper.whenComplete(upstream.finish(), this);
                    return;
                } else {
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
                        if (upstreamNext.getNow((false))) {
                            try {
                                collector.accept(storage, upstream.current());
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                nextReady.completeExceptionally(ex);
                                return;
                            }
                        } else {
                            current = storage;
                            nextReady.complete(true);
                            return;
                        }
                    } else {
                        nextReady.completeExceptionally(upstreamNext.exceptionNow());
                        return;
                    }
                }
            }
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
                if (!compareAndSet(1, 2)) {
                    if (u != null) {
                        nextReady.completeExceptionally(u);
                    } else
                    if ((Boolean)t) {
                        try {
                            collector.accept(storage, upstream.current());
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            nextReady.completeExceptionally(ex);
                            return;
                        }
                        drain();
                    } else {
                        current = storage;
                        nextReady.complete(true);
                    }
                }
            }
        }
    }
}
