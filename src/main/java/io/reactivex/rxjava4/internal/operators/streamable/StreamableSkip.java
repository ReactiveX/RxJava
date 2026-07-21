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
import java.util.concurrent.Future.State;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.StreamerCancellation;
import io.reactivex.rxjava4.operators.*;

public record StreamableSkip<T>(Streamable<T> source, long count) implements Streamable<T> {

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        var upstream = source.stream(cancellation);
        if (upstream instanceof IndexableSource<?> isrc) {
            return new SkipStreamerIndexed<>(upstream, (IndexableSource<T>)isrc, count);
        } else
        if (upstream instanceof DeferredEnumerableSource<?> dsrc) {
            return new SkipStreamerDeferredEnumerable<>(upstream, (DeferredEnumerableSource<T>)dsrc, count);
        } else
        if (upstream instanceof EnumerableSource<?> esrc) {
            return new SkipStreamerEnumerable<>(upstream, (EnumerableSource<T>)esrc, count);
        }
        return new SkipStreamerBasic<>(upstream, count);
    }

    static abstract class SkipStreamer<T> extends AtomicInteger implements Streamer<T>, BiConsumer<Boolean, Throwable> {

        @Serial
        private static final long serialVersionUID = 1988154737845167665L;

        final Streamer<T> upstream;

        long remaining;

        CompletableFuture<Boolean> waiter;

        SkipStreamer(Streamer<T> upstream, long count) {
            this.upstream = upstream;
            this.remaining = count;
            this.waiter = new CompletableFuture<>();
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            if (remaining <= 0) {
                return upstream.next();
            }
            drain();
            return waiter;
        }

        void drain() {
            for (;;) {
                var nextStage = upstream.next().toCompletableFuture();
                var state = nextStage.state();
                if (state == State.RUNNING) {
                    set(1);
                    nextStage.whenComplete(this);
                    if (compareAndSet(1, 0)) {
                        return;
                    }
                    state = nextStage.state();
                }

                if (state == State.SUCCESS) {
                    if (nextStage.getNow(false)) {
                        if (remaining-- <= 0) {
                            waiter.complete(true);
                            return;
                        }
                        // still skipping, try the next upstream value
                    } else {
                        waiter.complete(false);
                        return;
                    }
                } else {
                    waiter.completeExceptionally(nextStage.exceptionNow());
                    return;
                }
            }
        }

        @Override
        public void accept(Boolean t, Throwable u) {
            if (!compareAndSet(1, 2)) {
                if (u != null) {
                    waiter.completeExceptionally(u);
                } else {
                    if (t) {
                        if (remaining-- <= 0) {
                            waiter.complete(true);
                        } else {
                            drain();
                        }
                    } else {
                        waiter.complete(false);
                    }
                }
            }
        }

        @Override
        public @NonNull T current() {
            return upstream.current();
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            return upstream.finish();
        }
    }

    static final class SkipStreamerBasic<T> extends SkipStreamer<T> {

        @Serial
        private static final long serialVersionUID = 7002195716600087893L;

        SkipStreamerBasic(Streamer<T> upstream, long count) {
            super(upstream, count);
        }
    }

    static final class SkipStreamerIndexed<T> extends SkipStreamer<T>
    implements IndexableSource<T> {

        @Serial
        private static final long serialVersionUID = 773832461044750722L;

        final IndexableSource<T> indexable;

        final long count;

        SkipStreamerIndexed(Streamer<T> upstream, IndexableSource<T> indexable, long count) {
            super(upstream, count);
            this.indexable = indexable;
            this.count = count;
        }

        @Override
        public @NonNull T elementAt(long index) throws Throwable {
            return indexable.elementAt(index + count);
        }

        @Override
        public long limit() {
            return Math.max(0, indexable.limit() - count);
        }
    }

    static final class SkipStreamerEnumerable<T> extends SkipStreamer<T>
    implements EnumerableSource<T> {

        @Serial
        private static final long serialVersionUID = 773832461044750722L;

        final EnumerableSource<T> enumerable;

        SkipStreamerEnumerable(Streamer<T> upstream, EnumerableSource<T> enumerable, long count) {
            super(upstream, count);
            this.enumerable = enumerable;
        }

        @Override
        public boolean nextSync() throws Throwable {
            while (remaining-- > 0) {
                if (!enumerable.nextSync()) {
                    return false;
                }
            }
            return enumerable.nextSync();
        }

    }

    static final class SkipStreamerDeferredEnumerable<T> extends SkipStreamer<T>
    implements DeferredEnumerableSource<T> {

        @Serial
        private static final long serialVersionUID = 773832461044750722L;

        final DeferredEnumerableSource<T> enumerable;

        SkipStreamerDeferredEnumerable(Streamer<T> upstream, DeferredEnumerableSource<T> enumerable, long count) {
            super(upstream, count);
            this.enumerable = enumerable;
        }

        @Override
        public boolean nextSync() throws Throwable {
            while (remaining-- > 0) {
                if (!enumerable.nextSync()) {
                    return false;
                }
            }
            return enumerable.nextSync();
        }

        @Override
        public CompletionStage<Boolean> enumerableReady() {
            return enumerable.enumerableReady();
        }

    }
}
