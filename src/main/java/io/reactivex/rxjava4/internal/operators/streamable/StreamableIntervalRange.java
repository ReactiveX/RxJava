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
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.internal.disposables.DisposableHelper;
import io.reactivex.rxjava4.schedulers.Schedulers;

public record StreamableIntervalRange(
        long start,
        long count,
        long initialDelay,
        long period,
        TimeUnit unit,
        Scheduler scheduler,
        ExecutorService executor
) implements Streamable<Long> {

    public StreamableIntervalRange {
        if (scheduler == null && executor == null) {
            throw new IllegalArgumentException("scheduler and executor cannot be both null");
        }
    }

    @Override
    public @NonNull Streamer<Long> stream(@NonNull StreamerCancellation cancellation) {
        var streamer = new IntervalStreamer(start, start + count);
        if (scheduler != null) {
            var d = scheduler.schedulePeriodicallyDirect(streamer, initialDelay, period, unit);
            DisposableHelper.setOnce(streamer, d);
        } else
        if (executor instanceof ScheduledExecutorService se) {
            var f = se.scheduleAtFixedRate(streamer, initialDelay, period, unit);
            DisposableHelper.setOnce(streamer, Disposable.fromFuture(f, true));
        } else {
            var s = Schedulers.from(executor, true);
            var d = s.schedulePeriodicallyDirect(streamer, initialDelay, period, unit);
            DisposableHelper.setOnce(streamer, d);
        }
        cancellation.add(streamer);
        return streamer;
    }

    static final class IntervalStreamer extends AtomicReference<Disposable>
    implements Streamer<Long>, Runnable, java.util.function.Function<Boolean, Boolean>, Disposable {

        @Serial
        private static final long serialVersionUID = 197364198498939579L;

        final long end;

        long counterTask;

        long counterLocal;

        volatile Long available;

        volatile Long current;

        final AtomicReference<CompletableFuture<Boolean>> waiter;

        IntervalStreamer(long start, long end) {
            this.counterTask = start;
            this.counterLocal = start;
            this.end = end;
            this.waiter = new AtomicReference<>();
        }

        @Override
        public void run() {
            var c = counterTask++;
            available = c;
            if (c + 1 >= end) {
                DisposableHelper.dispose(this);
            }
            for (;;) {
                var cf = waiter.get();
                if (cf != null) {
                    cf.complete(true);
                    break;
                }
                cf = CompletableFuture.completedFuture(true);
                if (waiter.compareAndSet(null, cf)) {
                    break;
                }
            }
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            waiter.getAndSet(null);
            for (;;) {
                if (counterLocal >= end) {
                    return NEXT_FALSE;
                }
                var c = current;
                var a = available;
                if (c == null && a != null) {
                    current = counterLocal++;
                    return NEXT_TRUE;
                }
                if (c != null && c < a) {
                    current = counterLocal++;
                    return NEXT_TRUE;
                }
                if (isDisposed()) {
                    return CompletableFuture.failedFuture(new CancellationException());
                }
                var cf = waiter.get();
                if (cf != null) {
                    waiter.getAndSet(null);
                    return cf.thenApply(this);
                }
                cf = new CompletableFuture<Boolean>();
                if (waiter.compareAndSet(null, cf)) {
                    return cf.thenApply(this);
                }
            }
        }

        @Override
        public Boolean apply(Boolean t) {
            current = counterLocal++;
            return true;
        }

        @Override
        public @NonNull Long current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            DisposableHelper.dispose(this);
            return FINISHED;
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
            var cf = waiter.getAndSet(null);
            if (cf != null) {
                cf.completeExceptionally(new CancellationException());
            }
        }

        @Override
        public boolean isDisposed() {
            return get() == DisposableHelper.DISPOSED;
        }
    }
}
