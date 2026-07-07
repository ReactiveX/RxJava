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
import io.reactivex.rxjava4.core.Scheduler.Worker;
import io.reactivex.rxjava4.disposables.*;

public record StreamableTimeout<T>(
        Streamable<T> source,
        long timeout, TimeUnit unit, Scheduler scheduler,
        Streamable<T> fallback)
implements Streamable<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        var worker = scheduler.createWorker();
        cancellation.add(worker);
        var dc = cancellation.derive();
        var streamer = new TimeoutStreamer<>(
                source.stream(dc), dc, cancellation,
                timeout, unit, worker, fallback);
        return streamer;
    }

    static final class TimeoutStreamer<T> implements Streamer<T> {

        final long timeout;

        final TimeUnit unit;

        final Worker worker;

        final Streamable<T> fallback;

        final Disposable mainDisposable;

        final StreamerCancellation downstreamDisposable;

        Streamer<T> mainStreamer;

        Streamer<T> fallbackStreamer;

        TimeoutStreamer(
                Streamer<T> mainStreamer, Disposable mainDisposable, StreamerCancellation downstreamDisposable,
                long timeout, TimeUnit unit, Worker worker, Streamable<T> fallback) {
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
            this.fallback = fallback;
            this.mainStreamer = mainStreamer;
            this.mainDisposable = mainDisposable;
            this.downstreamDisposable = downstreamDisposable;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            if (fallbackStreamer != null) {
                return fallbackStreamer.next();
            }

            var mainNextResume = new CompletableFuture<Boolean>();
            var mainNext = mainStreamer.next();
            var mainNextTimeout = new CompletableFuture<Boolean>();
            Disposable d = worker.schedule(() -> mainNextTimeout.complete(false), timeout, unit);
            StreamableHelper.whenEither(mainNext, mainNextTimeout, winner -> {
                if (winner == 1) {
                    d.dispose();
                    StreamableHelper.forward(mainNext, mainNextResume);
                } else {
                    mainDisposable.dispose();
                    StreamableHelper.andThenSupply(
                            StreamableHelper.suppressValueAndCancel(mainNext, false),
                            () -> mainStreamer.finish()
                    ).whenComplete((_, e) -> {
                        if (e != null) {
                            mainNextResume.completeExceptionally(e);
                        } else {
                            fallbackStreamer = fallback.stream(downstreamDisposable);
                            StreamableHelper.forward(fallbackStreamer.next(), mainNextResume);
                        }
                    })
                    ;
                }
            });
            return mainNextResume;
        }

        @Override
        public @NonNull T current() {
            if (fallbackStreamer != null) {
                return fallbackStreamer.current();
            }
            return mainStreamer.current();
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            worker.dispose();
            if (fallbackStreamer != null) {
                return fallbackStreamer.finish();
            }
            return mainStreamer.finish();
        }
    }
}
