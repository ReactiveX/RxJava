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
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.Predicate;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;
import io.reactivex.rxjava4.operators.*;

public record StreamableFilter<T>(
        @NonNull Streamable<T> source,
        @NonNull Predicate<? super T> predicate)
implements Streamable<T>, HasUpstreamStreamableSource<T> {

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        var upstream = source.stream(cancellation);
        // No IndexableSource because we don't know how many items would pass the predicate
        // thus limit() would be non-calculatable
        if (upstream instanceof DeferredEnumerableSource<?> dsrc) {
            return new FilterStreamerDeferredEnumerable<>(upstream, (DeferredEnumerableSource<T>)dsrc, predicate, cancellation);
        } else
        if (upstream instanceof EnumerableSource<?> esrc) {
            return new FilterStreamerEnumerable<>(upstream, (EnumerableSource<T>)esrc, predicate, cancellation);
        }
        return new FilterStreamerBasic<>(upstream, predicate, cancellation);
    }

    static abstract class FilterStreamerBase<T> extends AtomicInteger
    implements Streamer<T>, BiConsumer<Boolean, Throwable> {

        @Serial
        private static final long serialVersionUID = -4830414233351804049L;

        final Streamer<T> upstream;

        final Predicate<? super T> predicate;

        StreamerCancellation cancellation;

        T current;

        CompletableFuture<Boolean> nextReady;

        FilterStreamerBase(Streamer<T> upstream, Predicate<? super T> predicate, StreamerCancellation cancellation) {
            this.upstream = upstream;
            this.cancellation = cancellation;
            this.predicate = predicate;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            nextReady = new CompletableFuture<Boolean>();
            drain();
            return nextReady;
        }

        @Override
        public @NonNull T current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            current = null;
            cancellation = null;
            return upstream.finish();
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
                    boolean has = upstreamNext.getNow(false);
                    if (!has) {
                        nextReady.complete(false);
                        return;
                    }
                    T value = upstream.current();
                    boolean pass;
                    try {
                        pass = predicate.test(value);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        nextReady.completeExceptionally(ex);
                        return;
                    }
                    if (pass) {
                        current = value;
                        nextReady.complete(true);
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
                } else {
                    if (t) {
                        T value = upstream.current();
                        boolean pass;
                        try {
                            pass = predicate.test(value);
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            nextReady.completeExceptionally(ex);
                            return;
                        }
                        if (pass) {
                            current = value;
                            nextReady.complete(true);
                        } else {
                            drain();
                        }
                    } else {
                        nextReady.complete(false);
                    }
                }
            }
        }
    }

    static final class FilterStreamerBasic<T> extends FilterStreamerBase<T> {

        @Serial
        private static final long serialVersionUID = -7116891990338100320L;

        FilterStreamerBasic(Streamer<T> upstream,
                Predicate<? super T> predicate, StreamerCancellation cancellation) {
            super(upstream, predicate, cancellation);
        }
    }

    static final class FilterStreamerEnumerable<T> extends FilterStreamerBase<T>
    implements EnumerableSource<T> {

        @Serial
        private static final long serialVersionUID = -7116891990338100320L;

        final EnumerableSource<T> enumerable;

        FilterStreamerEnumerable(Streamer<T> upstream,
                EnumerableSource<T> enumerable,
                Predicate<? super T> predicate, StreamerCancellation cancellation) {
            super(upstream, predicate, cancellation);
            this.enumerable = enumerable;
        }

        @Override
        public boolean nextSync() throws Throwable {
            while (!cancellation.isDisposed()) {
                if (enumerable.nextSync()) {
                    var value = enumerable.current();
                    if (predicate.test(value)) {
                        current = value;
                        return true;
                    }
                } else {
                    return false;
                }
            }
            throw new CancellationException(); // FIXME maybe???
        }
    }

    static final class FilterStreamerDeferredEnumerable<T> extends FilterStreamerBase<T>
    implements DeferredEnumerableSource<T> {

        @Serial
        private static final long serialVersionUID = -7116891990338100320L;

        final DeferredEnumerableSource<T> enumerable;

        FilterStreamerDeferredEnumerable(Streamer<T> upstream,
                DeferredEnumerableSource<T> enumerable,
                Predicate<? super T> predicate, StreamerCancellation cancellation) {
            super(upstream, predicate, cancellation);
            this.enumerable = enumerable;
        }

        @Override
        public boolean nextSync() throws Throwable {
            while (!cancellation.isDisposed()) {
                if (enumerable.nextSync()) {
                    var value = enumerable.current();
                    if (predicate.test(value)) {
                        current = value;
                        return true;
                    }
                } else {
                    return false;
                }
            }
            throw new CancellationException(); // FIXME maybe???
        }

        @Override
        public CompletionStage<Boolean> enumerableReady() {
            return enumerable.enumerableReady();
        }
    }
}
