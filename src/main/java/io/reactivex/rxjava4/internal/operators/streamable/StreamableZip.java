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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.DisposableContainer;
import io.reactivex.rxjava4.internal.util.AtomicThrowable;

public record StreamableZip<T>(
        Iterable<? extends Streamable<? extends T>> sources
) implements Streamable<List<T>> {

    @Override
    public @NonNull Streamer<@NonNull List<T>> stream(@NonNull DisposableContainer cancellation) {
        var dc = cancellation.derive();

        var sourcesList = new ArrayList<Streamer<? extends T>>();
        int i = 0;
        for (var inner : sources) {
            if (inner == null) {
                return StreamableError.createFailed(new NullPointerException("The item at index " + i + " is null"));
            }
            sourcesList.add(inner.stream(dc));
            i++;
        }

        if (sourcesList.isEmpty()) {
            return StreamableEmpty.createEmpty();
        }
        return new ZipStreamer<T>(sourcesList, dc);
    }

    static final class ZipStreamer<T>
    implements Streamer<List<T>>, BiConsumer<Object, Throwable> {

        final DisposableContainer innerCancellation;

        final List<? extends Streamer<? extends T>> streamers;

        final AtomicInteger wip;

        final AtomicInteger endWip;

        final CompletableFuture<Void> finishReady;

        final AtomicThrowable errors;

        CompletableFuture<Boolean> nextReady;

        Object[] working;

        List<T> current;

        final List<CompletionStage<Boolean>> nexts;

        volatile boolean done;

        ZipStreamer(List<? extends Streamer<? extends T>> streamers, DisposableContainer innerCancellation) {
            this.streamers = streamers;
            this.innerCancellation = innerCancellation;
            this.wip = new AtomicInteger();
            this.finishReady = new CompletableFuture<>();
            this.errors = new AtomicThrowable();
            this.working =  new Object[streamers.size()];
            this.endWip = new AtomicInteger();
            nexts = new ArrayList<CompletionStage<Boolean>>(streamers.size());
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            nextReady = new CompletableFuture<>();
            wip.set(streamers.size());
            Arrays.fill(working, null);
            nexts.clear();
            for (var innerStreamer : streamers) {
                var n = innerStreamer.next();
                nexts.add(n);
            }
            int i = 0;
            for (var stage : nexts) {
                var j = i++;
                stage.whenComplete((b, e) -> whenComplete(j, b, e));
            }
            return nextReady;
        }

        @Override
        public @NonNull List<T> current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            working = null;
            current = null;
            nexts.clear();
            wip.set(streamers.size());
            errors.set(null);
            done = true;
            for (var innerStreamer : streamers) {
                innerStreamer.finish().whenComplete(this);
            }
            return finishReady;
        }

        void whenComplete(int index, Boolean b, Throwable t) {
            if (t != null) {
                if (wip.getAndSet(0) != 0) {
                    errors.tryAddThrowableOrReport(t);
                    innerCancellation.dispose();
                    endWip.set(streamers.size());
                    int i = 0;
                    for (var next : nexts) {
                        if (index != i++) {
                            next.whenComplete(this);
                        } else {
                            accept(null, null);
                        }
                    }
                }
            } else
            if (b) {
                working[index] = streamers.get(index).current();
                if (wip.decrementAndGet() == 0) {
                    @SuppressWarnings("unchecked")
                    T[] result = (T[])working;
                    current = List.of(result);
                    nextReady.complete(true);
                }
            } else {
                if (wip.getAndSet(0) != 0) {
                    innerCancellation.dispose();
                    endWip.set(streamers.size());
                    int i = 0;
                    for (var next : nexts) {
                        if (index != i++) {
                            next.whenComplete(this);
                        } else {
                            accept(null, null);
                        }
                    }
                }
            }
        }

        @Override
        public void accept(Object t, Throwable u) {
            if (done) {
                if (u != null) {
                    errors.tryAddThrowableOrReport(u);
                }
                if (wip.decrementAndGet() == 0) {
                    var err = errors.terminate();
                    if (err != null) {
                        finishReady.completeExceptionally(err);
                    } else {
                        finishReady.complete(null);
                    }
                }
            } else {
                if (u !=  null && !StreamableHelper.isCancellation(u)) {
                    errors.tryAddThrowableOrReport(u);
                }
                if (endWip.decrementAndGet() == 0) {
                    var err = errors.terminate();
                    if (err != null) {
                        nextReady.completeExceptionally(err);
                    } else {
                        nextReady.complete(false);
                    }
                }
            }
        }
    }
}
