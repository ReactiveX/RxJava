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
import java.util.concurrent.atomic.*;
import java.util.function.BiConsumer;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;
import io.reactivex.rxjava4.internal.queue.MpscLinkedQueue;
import io.reactivex.rxjava4.internal.util.AtomicThrowable;

public record StreamableFlatMap<T, R>(
        Streamable<T> source,
        Function<? super T, ? extends Streamable<? extends R>> mapper,
        int maxConcurrency
)
implements Streamable<R>, HasUpstreamStreamableSource<T> {

    @Override
    public @NonNull Streamer<@NonNull R> stream(@NonNull StreamerCancellation cancellation) {
        var result = new FlatMapMainStreamer<>(source.stream(cancellation), mapper, maxConcurrency, cancellation);
        result.loopMain();
        return result;
    }

    static final class FlatMapMainStreamer<T, R> implements Streamer<R>, BiConsumer<Boolean, Throwable> {

        final Streamer<T> upstream;

        final Function<? super T, ? extends Streamable<? extends R>> mapper;

        final int maxConcurrency;

        final AtomicInteger wip = new AtomicInteger();

        final StreamerCancellation mainCanceller;

        final MpscLinkedQueue<InnerStreamer<T, R>> queue;

        final AtomicInteger queueWip;

        final AtomicThrowable errors;

        final AtomicReference<CompletableFuture<Boolean>> ready;

        volatile R current;

        final AtomicInteger active;

        volatile boolean mainDone;

        FlatMapMainStreamer(Streamer<T> upstream,
                Function<? super T, ? extends Streamable<? extends R>> mapper,
                int maxConcurrency,
                StreamerCancellation container) {
            this.upstream = upstream;
            this.mapper = mapper;
            this.maxConcurrency = maxConcurrency;
            this.mainCanceller = container;
            this.queue = new MpscLinkedQueue<>();
            this.queueWip = new AtomicInteger();
            this.errors = new AtomicThrowable();
            this.ready = new AtomicReference<>();
            this.active = new AtomicInteger();
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            var cf = new CompletableFuture<Boolean>();
            ready.lazySet(cf);
            drain();
            return cf;
        }

        @Override
        public @NonNull R current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            current = null;
            return upstream.finish();
        }

        void drain() {
            if (queueWip.getAndIncrement() != 0) {
                return;
            }

            do {
                var cf = ready.get();
                if (cf != null) {
                    var md = mainDone;
                    var n = active.get();
                    var inner = queue.poll();
                    if (md && n == 0 && inner == null) {
                        ready.lazySet(null);
                        current = null;
                        var err = errors.get();
                        if (err != null) {
                            cf.completeExceptionally(err);
                        } else {
                            cf.complete(false);
                        }
                    } else
                    if (inner != null) {
                        ready.lazySet(null);
                        current = inner.inner.current();
                        inner.drain();
                        cf.complete(true);
                    }
                }
            } while (queueWip.decrementAndGet() != 0);
        }

        void loopMain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            do {
                if (!mainDone) {
                    upstream.next().whenComplete(this);
                } else {
                    drain();
                }
            } while (wip.decrementAndGet() != 0);
        }

        @Override
        public void accept(Boolean v, Throwable e) {
            if (e != null) {
                errors.tryAddThrowableOrReport(e);
                mainDone = true;
                drain();
            } else
            if (v) {
                var n = active.incrementAndGet();

                try {
                    var innerCanceller = mainCanceller.derive();
                    var inner = Objects.requireNonNull(mapper.apply(upstream.current()), "The mapper returned a null value.")
                            .stream(innerCanceller);

                    new InnerStreamer<T, R>(inner, this, innerCanceller).drain();

                    if (n < maxConcurrency) {
                        loopMain();
                    }
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    errors.tryAddThrowableOrReport(ex);
                    active.decrementAndGet();
                    mainDone = true;
                    drain();
                }
            } else {
                mainDone = true;
                drain();
            }
        }
    }

    static final class InnerStreamer<T, R> extends AtomicInteger implements BiConsumer<Object, Throwable> {
        @Serial
        private static final long serialVersionUID = 1840568797902248120L;

        final Streamer<? extends R> inner;

        final FlatMapMainStreamer<T, R> parent;

        final DisposableContainer canceller;

        volatile boolean done;

        boolean finishing;

        InnerStreamer(Streamer<? extends R> inner, FlatMapMainStreamer<T, R> parent, DisposableContainer canceller) {
            this.inner = inner;
            this.parent = parent;
            this.canceller = canceller;
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            do {
                if (!done) {
                    inner.next().whenComplete(this);
                } else {
                    finishing = true;
                    inner.finish().whenComplete(this);
                    break;
                }
            } while (decrementAndGet() != 0);
        }

        @Override
        public void accept(Object t, Throwable u) {
            if (finishing) {
                parent.active.decrementAndGet();
                if (u != null) {
                    parent.errors.tryAddThrowableOrReport(u);
                }
                parent.loopMain();
            } else {
                if (u != null) {
                    parent.errors.tryAddThrowableOrReport(u);
                    done = true;
                    drain();
                } else {
                    if ((Boolean)t) {
                        parent.queue.offer(this);
                        parent.drain();
                    } else {
                        done = true;
                        drain();
                    }
                }
            }
        }
    }
}
