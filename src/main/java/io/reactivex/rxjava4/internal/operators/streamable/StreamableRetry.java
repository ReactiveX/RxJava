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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.BiFunction;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;

public record StreamableRetry<T>(
        Streamable<T> source,
        BiFunction<? super Long, ? super Throwable, ? extends CompletionStage<Boolean>> whenFunction
)
implements Streamable<T>, HasUpstreamStreamableSource<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        var streamer = new RetryStreamer<>(source, cancellation, whenFunction);
        streamer.retrySource();
        return streamer;
    }

    static final class RetryStreamer<T>
    implements Streamer<T>, BiConsumer<Object, Throwable> {

        final Streamable<T> source;

        final StreamerCancellation downstreamCancellation;

        final BiFunction<? super Long, ? super Throwable, ? extends CompletionStage<Boolean>> whenFunction;

        final AtomicInteger wipSource;

        Streamer<T> currentStreamer;

        CompletableFuture<Boolean> nextWaiter;

        volatile int stage;

        long failureCount;

        Disposable whenFunctionCancel;

        Throwable currentThrowable;

        RetryStreamer(Streamable<T> source, StreamerCancellation downstreamCancellation,
                BiFunction<? super Long, ? super Throwable, ? extends CompletionStage<Boolean>> whenFunction) {
            this.source = source;
            this.downstreamCancellation = downstreamCancellation;
            this.whenFunction = whenFunction;
            this.wipSource = new AtomicInteger();
            this.stage = -1;
        }

        void retrySource() {
            if (wipSource.getAndIncrement() != 0) {
                return;
            }
            do {
                // FIXME some operators don't clean up their StreamerCancellations so we hand out clean ones for now
                var innerCanceller = downstreamCancellation.derive();
                currentStreamer = source.stream(innerCanceller);
                if (stage == 0) {
                    stage = 1;
                    currentStreamer.next().whenComplete(this);
                }
            } while (wipSource.decrementAndGet() != 0);
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            nextWaiter = new CompletableFuture<>();
            stage = 1;
            currentStreamer.next().whenComplete(this);
            return nextWaiter;
        }

        @Override
        public void accept(Object t, Throwable u) {
            if (stage == 1) {
                if (u != null) {
                    currentThrowable = u;
                    var streamer = currentStreamer;
                    currentStreamer = null;
                    stage = 2;
                    streamer.finish().whenComplete(this);
                } else {
                    nextWaiter.complete((Boolean)t);
                }
            } else
            if (stage == 2) {
                if (u != null) {
                    u.addSuppressed(currentThrowable);
                } else {
                    u = currentThrowable;
                }
                currentThrowable = null;
                try {
                    var cs = whenFunction.apply(failureCount++, u);
                    whenFunctionCancel = Disposable.fromAction(() -> cs.toCompletableFuture().cancel(true));
                    downstreamCancellation.add(whenFunctionCancel);
                    stage = 3;
                    cs.whenComplete(this);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    ex.addSuppressed(u);
                    nextWaiter.completeExceptionally(ex);
                }
            } else { // stage 3
                downstreamCancellation.delete(whenFunctionCancel);
                whenFunctionCancel = null;
                var cf = nextWaiter;
                if (u != null) {
                    cf.completeExceptionally(u);
                } else
                if ((Boolean)t){
                    stage = 0;
                    retrySource();
                } else {
                    cf.complete(false);
                }
            }
        }

        @Override
        public @NonNull T current() {
            return currentStreamer.current();
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            if (currentStreamer != null) {
                return currentStreamer.finish();
            }
            return FINISHED;
        }
    }
}
