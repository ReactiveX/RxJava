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

package io.reactivex.rxjava4.processors;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.internal.operators.streamable.*;
import io.reactivex.rxjava4.internal.util.ExceptionHelper;

/**
 * Signals the various {@link #next(Object)} and {@link #finish(Throwable)} events to one or more
 * downstream {@link Streamer}s.
 * <p>
 * This is equivalent with to a {@link PublishProcessor} or {@link MulticastProcessor}, adapted to
 * the {@link Streamable} world.
 * @param <T> the element type of the input and output values
 * @since 4.0.0
 */
public final class DispatchStreamProcessor<T> implements StreamProcessor<T, T> {

    static final DispatchStreamer<?>[] TERMINATED = new DispatchStreamer<?>[0];

    static final DispatchStreamer<?>[] EMPTY = new DispatchStreamer<?>[0];

    final AtomicReference<DispatchStreamer<?>[]> streamers = new AtomicReference<>(EMPTY);

    volatile Throwable terminalEvent;

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        var result = new DispatchStreamer<T>(this);
        cancellation.add(result);
        if (add(result)) {
            return result;
        }
        var t = terminalEvent;
        if (t != ExceptionHelper.TERMINATED) {
            return StreamableError.createFailed(t);
        }
        return StreamableEmpty.createEmpty();
    }

    @Override
    public CompletionStage<Boolean> next(@NonNull T item) {
        @SuppressWarnings("unchecked")
        var currentStreamers = (DispatchStreamer<T>[])streamers.get();
        if (currentStreamers.length == 0) {
            return Streamer.NEXT_FALSE;
        }
        List<CompletionStage<Boolean>> waiters = new ArrayList<>();
        for (var str : currentStreamers) {
            waiters.add(str.send(item));
        }
        return StreamableHelper.awaitAllBoolean(waiters);
    }

    @Override
    public CompletionStage<Void> finish(@Nullable Throwable throwable) {
        terminalEvent = throwable == null ? ExceptionHelper.TERMINATED : throwable;
        @SuppressWarnings("unchecked")
        var currentStreamers = (DispatchStreamer<T>[])streamers.getAndSet(TERMINATED);
        if (currentStreamers.length == 0) {
            return Streamer.FINISHED;
        }
        List<CompletionStage<Void>> waiters = new ArrayList<>();
        for (var str : currentStreamers) {
            waiters.add(str.error(throwable));
        }
        return StreamableHelper.awaitAllVoid(waiters);
    }

    boolean add(DispatchStreamer<T> streamer) {
        for (;;) {
            var currentStreamers = streamers.get();
            if (currentStreamers == TERMINATED) {
                return false;
            }
            var length = currentStreamers.length;
            var nextStreamers = Arrays.copyOf(currentStreamers, length + 1);
            nextStreamers[length] = streamer;
            if (streamers.compareAndSet(currentStreamers, nextStreamers)) {
                return true;
            }
        }
    }

    boolean remove(DispatchStreamer<T> streamer) {
        for (;;) {
            var currentStreamers = streamers.get();
            var length = currentStreamers.length;
            if (length == 0) {
                return false;
            }
            int j = -1;
            for (var i = 0; i < length; i++) {
                if (currentStreamers[i] == streamer) {
                    j = i;
                    break;
                }
            }
            if (j < 0) {
                return false;
            }
            DispatchStreamer<?>[] nextStreamers;
            if (length == 1) {
                nextStreamers = EMPTY;
            } else {
                nextStreamers = new DispatchStreamer<?>[length - 1];
                System.arraycopy(currentStreamers, 0, nextStreamers, 0, j);
                System.arraycopy(currentStreamers, j + 1, nextStreamers, j, length - j - 1);
            }
            if (streamers.compareAndSet(currentStreamers, nextStreamers)) {
                return true;
            }
        }
    }

    @Override
    public boolean hasStreamers() {
        return streamers.get().length != 0;
    }

    @Override
    public int streamerCount() {
        return streamers.get().length;
    }

    @Override
    public boolean hasComplete() {
        return terminalEvent == ExceptionHelper.TERMINATED;
    }

    @Override
    public boolean hasThrowable() {
        var te = terminalEvent;
        return te != null && te != ExceptionHelper.TERMINATED;
    }

    @Override
    public @Nullable Throwable getThrowable() {
        var te = terminalEvent;
        if (te != null && te != ExceptionHelper.TERMINATED) {
            return te;
        }
        return null;
    }

    static final class DispatchStreamer<T>
    implements Streamer<T>, Function<Boolean, Boolean>, Disposable {

        final DispatchStreamProcessor<T> parent;

        final StageResumable<Boolean> consumerReady;

        final StageResumable<Boolean> producerReady;

        T current;

        T incoming;

        volatile boolean disposed;

        DispatchStreamer(DispatchStreamProcessor<T> parent) {
            this.parent = parent;
            consumerReady = new StageResumable<>();
            producerReady = new StageResumable<>();
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            // I O.println("DispatchStreamer.next()");
            consumerReady.ready().complete(true);
            return producerReady.await().thenApply(this);
        }

        @Override
        public Boolean apply(Boolean t) {
            // I O.println("DispatchStreamer.apply(" + incoming + ")");
            current = incoming;
            incoming = null;
            return t;
        }

        @Override
        public @NonNull T current() {
            // I O.println("DispatchStreamer.current(" + current + ")");
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            // I O.println("DispatchStreamer.finish()");
            // new Exception().printStackTrace(System.out);
            current = null;
            parent.remove(this);
            consumerReady.ready().complete(false);
            return FINISHED;
        }

        CompletionStage<Boolean> send(T item) {
            // I O.println("DispatchStreamer.send(" + item + ") // <-----------------------");
            return consumerReady.await().thenApply(v -> {
                // I O.println("DSP: consumerReady.await().thenAccept(" + v + ", " + item + ")");
                incoming = item;
                producerReady.ready().complete(true);
                return v;
            });
        }

        CompletionStage<Void> error(Throwable t) {
            // I O.println("DispatchStreamer.error(" + t + ") // <-------------------------");
            return consumerReady.await().thenAccept(_ -> {
                if (t != null) {
                    producerReady.ready().completeExceptionally(t);
                } else {
                    producerReady.ready().complete(false);
                }
            });
        }

        @Override
        public void dispose() {
            // I O.println("DispatchStreamer.dispose()");
            disposed = true;
            if (parent.remove(this)) {
                var ce = new CancellationException();
                producerReady.ready().completeExceptionally(ce);
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
