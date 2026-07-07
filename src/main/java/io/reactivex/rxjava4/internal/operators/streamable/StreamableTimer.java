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

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.internal.disposables.DisposableHelper;

public record StreamableTimer(long delay, @NonNull TimeUnit unit,
        @Nullable Scheduler scheduler,
        @Nullable ExecutorService executor) implements Streamable<Long> {

    public StreamableTimer {
        if (scheduler == null && executor == null) {
            throw new IllegalArgumentException("scheduler and executor cannot be both null");
        }
    }

    @Override
    public @NonNull Streamer<Long> stream(@NonNull StreamerCancellation cancellation) {
        var streamer = new TimerStreamer();
        cancellation.add(streamer);
        if (scheduler != null) {
            var d = scheduler.scheduleDirect(streamer, delay, unit);
            DisposableHelper.setOnce(streamer, d);
        } else
        if (executor instanceof ScheduledExecutorService se) {
            var f = se.schedule(streamer, delay, unit);
            DisposableHelper.setOnce(streamer, Disposable.fromFuture(f, true));
        } else {
            var f = executor.submit(() -> {
                try {
                    unit.sleep(delay);
                } catch (InterruptedException ex) {
                    streamer.interrupedSleep(ex);
                    return null;
                }
                streamer.run();
                return null;
            });
            DisposableHelper.setOnce(streamer, Disposable.fromFuture(f, true));
        }
        return streamer;
    }

    static final class TimerStreamer extends AtomicReference<Disposable> implements Streamer<Long>,  Runnable, Disposable {

        @Serial
        private static final long serialVersionUID = 1738554471573342053L;

        int state;

        final CompletableFuture<Boolean> waiter = new CompletableFuture<>();

        @Override
        public void run() {
            waiter.complete(true);
        }

        void interrupedSleep(InterruptedException ex) {
            if (!isDisposed()) {
                waiter.completeExceptionally(ex);
            }
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            if (state == 0) {
                state = 1;
                return waiter;
            }
            return NEXT_FALSE;
        }

        @Override
        public @NonNull Long current() {
            return 0L;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            waiter.complete(false);
            lazySet(DisposableHelper.DISPOSED);
            return FINISHED;
        }

        @Override
        public void dispose() {
            if (DisposableHelper.dispose(this)) {
                waiter.completeExceptionally(new CancellationException());
            }
        }

        @Override
        public boolean isDisposed() {
            return get() == DisposableHelper.DISPOSED;
        }
    }
}
