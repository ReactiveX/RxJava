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

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BiConsumer;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;

public record StreamableGroupBy<K, T>(Streamable<T> source, Function<? super T, ? extends K> keySelector)
implements Streamable<GroupedStreamable<K, T>>, HasUpstreamStreamableSource<T> {

    @Override
    public @NonNull Streamer<GroupedStreamable<K, T>> stream(@NonNull DisposableContainer cancellation) {
        var streamer = new GroupByStreamer<K, T>(source.stream(cancellation), keySelector);
        streamer.drain();
        return streamer;
    }

    static final class GroupByStreamer<K, T>
    implements Streamer<GroupedStreamable<K, T>>,
    BiConsumer<Object, Throwable>, java.util.function.Function<Boolean, Boolean> {

        final Streamer<T> upstream;

        final Function<? super T, ? extends K> keySelector;

        final AtomicInteger wip;

        final Map<K, BasicGroupedStreamable<K, T>> groups;

        final CompletableFuture<Void> onFinish;

        volatile boolean done;

        volatile GroupedStreamable<K, T> currentGroup;

        volatile GroupedStreamable<K, T> currentNext;

        final StageResumable<Boolean> mainNext;

        GroupByStreamer(Streamer<T> upstream, Function<? super T, ? extends K> keySelector) {
            this.upstream = upstream;
            this.keySelector = keySelector;
            this.wip = new AtomicInteger();
            this.groups = new ConcurrentHashMap<>();
            this.mainNext = new StageResumable<>();
            this.onFinish = new CompletableFuture<>();
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            var cs = mainNext.await().thenApply(this);
            return cs;
        }

        @Override
        public Boolean apply(Boolean v) {
            if (v) {
                currentNext = currentGroup;
            } else {
                currentNext = null;
            }
            currentGroup = null;
            return v;
        }

        @Override
        public @NonNull GroupedStreamable<K, T> current() {
            return currentNext;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            done = true;
            drain();
            return onFinish;
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            do {
                if (done) {
                    upstream.finish().whenComplete(this);
                    break;
                } else {
                    upstream.next().whenComplete(this);
                }
            } while (wip.decrementAndGet() != 0);
        }

        @Override
        public void accept(Object t, Throwable u) {
            if (done) {
                upstream.finish().whenComplete((v, e) -> {
                    currentNext = null;
                    currentGroup = null;
                    if (e != null) {
                        onFinish.completeExceptionally(e);
                    } else {
                        onFinish.complete(v);
                    }
                });
            } else {
                if (u != null) {
                    done = true;
                    for (var g : groups.values()) {
                        g.finish(u); // TODO whenComplete
                    }
                    groups.clear();
                    mainNext.ready().completeExceptionally(u);
                } else
                if ((Boolean)t) {
                    try {
                        var c = upstream.current();
                        var key = keySelector.apply(c);

                        var g = groups.get(key);
                        if (g == null) {
                            g = new AsyncGroup<>(key, this);
                            groups.put(key, g);
                            currentGroup = g;
                            mainNext.ready().complete(true);
                        }
                        g.next(c).whenComplete((_, _) -> drain());
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        done = true;
                        for (var g : groups.values()) {
                            g.finish(ex);
                        }
                        groups.clear();
                        mainNext.ready().completeExceptionally(ex);
                    }
                } else {
                    done = true;
                    for (var g : groups.values()) {
                        g.finish(null);
                    }
                    groups.clear();
                    mainNext.ready().complete(false);
                }
            }
        }

        @SuppressWarnings("unchecked")
        void delete(K key) {
            groups.replace(key, (BasicGroupedStreamable<K, T>)TombstoneGroup.INSTANCE);
        }

        boolean isDeleted(K key) {
            var e = groups.get(key);
            return e == null || e == TombstoneGroup.INSTANCE;
        }
    }

    static abstract class BasicGroupedStreamable<K, T> extends GroupedStreamable<K, T>
    implements StreamerInput<T> {
        BasicGroupedStreamable(K key) {
            super(key);
        }

        public abstract CompletionStage<Boolean> next(@NonNull T value);

        public abstract CompletionStage<Void> finish(@Nullable Throwable throwable);
    }

    static final class AsyncGroup<K, T> extends BasicGroupedStreamable<K, T>
    implements Streamer<T>, Disposable, java.util.function.Function<Boolean, Boolean> {

        final AtomicBoolean once;

        final GroupByStreamer<K, T> parent;

        final StageResumable<Boolean> sendCanProgress;

        final StageResumable<Boolean> nextCanProgress;

        volatile T item;

        volatile T current;

        DisposableContainer cancellation;

        AsyncGroup(K key, GroupByStreamer<K, T> parent) {
            super(key);
            this.once = new AtomicBoolean();
            this.parent = parent;
            this.sendCanProgress = new StageResumable<>();
            this.nextCanProgress = new StageResumable<>();
        }

        @Override
        public @NonNull Streamer<@NonNull T> stream(@NonNull DisposableContainer cancellation) {
            if (once.compareAndSet(false, true)) {
                this.cancellation = cancellation;
                cancellation.add(this);
                return this;
            }
            return StreamableError.createFailed(new IllegalStateException("Only one streamer is allowed!"));
        }

        @Override
        public CompletionStage<Boolean> next(T value) {
            return sendCanProgress.await().thenApply(_ -> {
                item = value;
                nextCanProgress.ready().complete(true);
                return true;
            });
        }

        @Override
        public CompletionStage<Void> finish(@Nullable Throwable throwable) {
            return sendCanProgress.await().thenAccept(_ -> {
                if (throwable == null) {
                    nextCanProgress.ready().complete(false);
                } else {
                    nextCanProgress.ready().completeExceptionally(throwable);
                }
            });
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            sendCanProgress.ready().complete(true);
            return nextCanProgress.await().thenApply(this);
        }

        @Override
        public @NonNull Boolean apply(@NonNull Boolean b) {
            current = item;
            item = null;
            return b;
        }

        @Override
        public @NonNull T current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            this.cancellation.remove(this);
            return FINISHED;
        }

        @Override
        public void dispose() {
            parent.delete(getKey());
        }

        @Override
        public boolean isDisposed() {
            // TODO Auto-generated method stub
            return parent.isDeleted(getKey());
        }
    }

    static final class TombstoneGroup extends BasicGroupedStreamable<Object, Object> {

        static final Object TOMBSTONE_KEY = new Object();

        static final TombstoneGroup INSTANCE = new TombstoneGroup();

        TombstoneGroup() {
            super(TOMBSTONE_KEY);
        }

        @Override
        public @NonNull Streamer<@NonNull Object> stream(@NonNull DisposableContainer cancellation) {
            return StreamableError.createFailed(new CancellationException("TOMBSTONE"));
        }

        // This is what happens with generics without co- and contravariance support
        @Override
        public CompletionStage<Boolean> next(Object value) {
            return Streamer.NEXT_TRUE;
        }

        @Override
        public CompletionStage<Void> finish(@Nullable Throwable throwable) {
            return Streamer.FINISHED;
        }
    }
}
