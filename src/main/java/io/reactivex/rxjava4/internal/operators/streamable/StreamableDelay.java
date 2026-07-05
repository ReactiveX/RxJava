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
import java.util.function.BiConsumer;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.Scheduler.Worker;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;

public record StreamableDelay<T>(
        Streamable<T> source,
        long delay, TimeUnit unit, Scheduler scheduler
) implements Streamable<T>, HasUpstreamStreamableSource<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull DisposableContainer cancellation) {
        var worker = scheduler.createWorker();
        cancellation.add(worker);
        return new DelayStreamer<>(source.stream(cancellation), delay, unit, worker, cancellation);
    }

    static final class DelayStreamer<T> implements Streamer<T>, BiConsumer<Boolean, Throwable>, Runnable {

        final Worker worker;

        final Streamer<T> upstream;

        final long delay;

        final TimeUnit unit;

        final DisposableContainer cancellation;

        CompletableFuture<Boolean> nextReady;

        Disposable onDisposed;

        DelayStreamer(Streamer<T> upstream, long delay, TimeUnit unit, Worker worker, DisposableContainer cancellation) {
            this.upstream = upstream;
            this.delay = delay;
            this.unit = unit;
            this.worker = worker;
            this.cancellation = cancellation;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            nextReady = new CompletableFuture<Boolean>();
            onDisposed = Disposable.fromFuture(nextReady, true);
            cancellation.add(onDisposed);
            upstream.next().whenComplete(this);
            return nextReady;
        }

        @Override
        public void accept(Boolean t, Throwable u) {
            if (u != null) {
                cancellation.delete(onDisposed);
                nextReady.completeExceptionally(u);
            } else {
                if (t) {
                    worker.schedule(this, delay, unit);
                } else {
                    cancellation.delete(onDisposed);
                    nextReady.complete(false);
                }
            }
        }

        @Override
        public void run() {
            cancellation.delete(onDisposed);
            nextReady.complete(true);
        }

        @Override
        public @NonNull T current() {
            return upstream.current();
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            cancellation.delete(worker);
            nextReady = null;
            onDisposed = null;
            return FINISHED;
        }
    }
}
