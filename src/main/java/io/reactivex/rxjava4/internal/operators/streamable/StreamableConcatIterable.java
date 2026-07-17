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
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.Exceptions;

public record StreamableConcatIterable<T>(
        Iterable<? extends Streamable<? extends T>> sources,
        ErrorMode errorMode
) implements Streamable<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        return new ConcatIteratorStreamer<>(sources.iterator(), cancellation);
    }

    static final class ConcatIteratorStreamer<T> extends AtomicInteger
    implements Streamer<T>, BiConsumer<Boolean, Throwable> {

        @Serial
        private static final long serialVersionUID = -9136569444189652718L;

        final Iterator<? extends Streamable<? extends T>> iterator;

        final StreamerCancellation cancellation;

        DisposableStreamerCancellation currentCancellation;

        volatile Streamer<? extends T> upstream;

        CompletableFuture<Boolean> nextReady;

        ConcatIteratorStreamer(Iterator<? extends Streamable<? extends T>> iterator,
                StreamerCancellation cancellation) {
            this.iterator = iterator;
            this.cancellation = cancellation;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            for (;;) {
                if (upstream == null) {
                    if (iterator.hasNext()) {
                        try {
                            var source = Objects.requireNonNull(iterator.next(), "The iterable returned a null Streamable");
                            currentCancellation = cancellation.derive();
                            upstream = source.stream(currentCancellation);
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            return CompletableFuture.failedFuture(ex);
                        }
                    } else {
                        return NEXT_FALSE;
                    }
                }
                var stage = upstream.next();
                if (stage instanceof CompletableFuture<Boolean> cf) {
                    if (cf.state() == State.SUCCESS) {
                        if (cf.getNow(false)) {
                            return NEXT_TRUE;
                        } else {
                            var finishStage = upstream.finish();
                            if (finishStage instanceof CompletableFuture<Void> cff && cff.isDone()) {
                                upstream = null;
                                cancellation.delete(currentCancellation);
                                currentCancellation = null;
                                if (cff.isCompletedExceptionally()) {
                                    return CompletableFuture.failedFuture(cff.exceptionNow());
                                }
                                // will get the next source synchronously
                            } else {
                                nextReady = new CompletableFuture<Boolean>();
                                finishStage.whenComplete(this::whenFinishComplete);
                                return nextReady;
                            }
                        }
                    } else {
                        nextReady = new CompletableFuture<Boolean>();
                        stage.whenComplete(this);
                        return nextReady;
                    }
                } else {
                    nextReady = new CompletableFuture<Boolean>();
                    stage.whenComplete(this);
                    return nextReady;
                }
            }
        }

        @Override
        public @NonNull T current() {
            return upstream.current();
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            var localUpstream = upstream;
            var localCurrentCancellation = currentCancellation;
            upstream = null;
            nextReady = null;
            currentCancellation = null;
            if (localUpstream != null) {
                cancellation.delete(localCurrentCancellation);
                return localUpstream.finish();
            }
            return FINISHED;
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            do {
                if (upstream == null) {
                    if (iterator.hasNext()) {
                        currentCancellation = cancellation.derive();
                        var nextStreamable = iterator.next();
                        if (nextStreamable == null) {
                            nextReady.completeExceptionally(new NullPointerException("The iterator returned a null Streamable"));
                        } else {
                            upstream = nextStreamable.stream(currentCancellation);
                        }
                    } else {
                        nextReady.complete(false);
                    }
                }
                if (upstream != null) {
                    StreamableHelper.whenComplete(upstream.next(), this);
                }
            } while (decrementAndGet() != 0);
        }

        @Override
        public void accept(Boolean v, Throwable e) {
            if (e != null) {
                nextReady.completeExceptionally(e);
            } else
            if (v)  {
                nextReady.complete(true);
            } else {
                cancellation.delete(currentCancellation);
                StreamableHelper.whenComplete(upstream.finish(), this::whenFinishComplete);
            }
        }

        void whenFinishComplete(Void t, Throwable u) {
            if (u != null) {
                nextReady.completeExceptionally(u);
            } else {
                upstream = null;
                drain();
            }
        }
    }
}
