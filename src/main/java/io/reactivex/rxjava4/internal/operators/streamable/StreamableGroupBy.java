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
import io.reactivex.rxjava4.disposables.DisposableContainer;
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
    BiConsumer<Object, Throwable>, java.util.function.Function<K, BasicGroupedStreamable<K, T>> {

        final Streamer<T> upstream;

        final Function<? super T, ? extends K> keySelector;

        final AtomicInteger wip;

        final Map<K, BasicGroupedStreamable<K, T>> groups;

        volatile boolean done;

        volatile GroupedStreamable<K, T> current;

        GroupByStreamer(Streamer<T> upstream, Function<? super T, ? extends K> keySelector) {
            this.upstream = upstream;
            this.keySelector = keySelector;
            this.wip = new AtomicInteger();
            this.groups = new ConcurrentHashMap<>();
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public @NonNull GroupedStreamable<K, T> current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            current = null;
            return upstream.finish();
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
        public BasicGroupedStreamable<K, T> apply(K t) {
            var g = new AsyncGroup<>(t, this);
            current = g;
            // TODO signal group is ready
            return g;
        }

        @Override
        public void accept(Object t, Throwable u) {
            if (done) {
                // TODO
            } else {
                if (u != null) {
                    for (var g : groups.values()) {
                        g.terminate(u); // TODO whenComplete
                    }
                    groups.clear();
                    done = true;
                } else
                if ((Boolean)t) {
                    try {
                        var c = upstream.current();
                        var key = keySelector.apply(c);

                        var g = groups.computeIfAbsent(key, this);
                        g.send(c).whenComplete((_, _) -> drain());
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        for (var g : groups.values()) {
                            g.terminate(ex);
                        }
                        groups.clear();
                        done = true;
                    }
                } else {
                    for (var g : groups.values()) {
                        g.terminate(null);
                    }
                    groups.clear();
                    done = true;
                }
                drain();
            }
        }
    }

    static abstract class BasicGroupedStreamable<K, T> extends GroupedStreamable<K, T> {
        BasicGroupedStreamable(K key) {
            super(key);
        }

        abstract CompletionStage<Void> send(@NonNull T value);

        abstract CompletionStage<Void> terminate(@Nullable Throwable throwable);
    }

    static final class AsyncGroup<K, T> extends BasicGroupedStreamable<K, T> {

        final AtomicBoolean once;

        final GroupByStreamer<K, T> parent;

        AsyncGroup(K key, GroupByStreamer<K, T> parent) {
            super(key);
            this.once = new AtomicBoolean();
            this.parent = parent;
        }

        @Override
        public @NonNull Streamer<@NonNull T> stream(@NonNull DisposableContainer cancellation) {
            // TODO Auto-generated method stub
            if (once.compareAndSet(false, true)) {
                return null;
            }
            return StreamableError.createFailed(new IllegalStateException("Only one streamer is allowed!"));
        }

        @Override
        CompletionStage<Void> send(T value) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        CompletionStage<Void> terminate(@Nullable Throwable throwable) {
            // TODO Auto-generated method stub
            return null;
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

        @Override
        CompletionStage<Void> send(Object value) {
            return Streamer.FINISHED;
        }

        @Override
        CompletionStage<Void> terminate(@Nullable Throwable throwable) {
            return Streamer.FINISHED;
        }
    }
}
