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

import java.util.concurrent.CompletionStage;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.StreamerCancellation;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;
import io.reactivex.rxjava4.operators.*;

public record StreamableTake<T>(Streamable<T> source, long count)
implements Streamable<T>, HasUpstreamStreamableSource<T> {

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        var upstream = source.stream(cancellation);
        if (upstream instanceof IndexableSource<?> isrc) {
            return new TakeStreamerIndexable<>(upstream, (IndexableSource<T>)isrc, count);
        } else
        if (upstream instanceof DeferredEnumerableSource<?> dsrc) {
            return new TakeStreamerDeferredEnumerable<>(upstream, (DeferredEnumerableSource<T>)dsrc, count);
        } else
        if (upstream instanceof EnumerableSource<?> esrc) {
            return new TakeStreamerEnumerable<>(upstream, (EnumerableSource<T>)esrc, count);
        }
        return new TakeStreamerBasic<>(upstream, count);
    }

    static abstract class TakeStreamerBase<T> implements Streamer<T> {
        final Streamer<T> upstream;

        long remaining;

        TakeStreamerBase(Streamer<T> upstream, long count) {
            this.upstream = upstream;
            this.remaining = count;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            if (remaining-- <= 0L) {
                return NEXT_FALSE;
            }
            return upstream.next();
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

    static final class TakeStreamerBasic<T> extends TakeStreamerBase<T> {

        TakeStreamerBasic(Streamer<T> upstream, long count) {
            super(upstream, count);
        }

    }

    static final class TakeStreamerIndexable<T> extends TakeStreamerBase<T>
    implements IndexableSource<T> {

        final IndexableSource<T> indexable;

        final long count;

        TakeStreamerIndexable(Streamer<T> upstream, IndexableSource<T> indexable, long count) {
            super(upstream, count);
            this.indexable = indexable;
            this.count = count;
        }

        @Override
        public @NonNull T elementAt(long index) throws Throwable {
            return indexable.elementAt(index);
        }

        @Override
        public long limit() {
            return Math.min(count, indexable.limit());
        }
    }

    static final class TakeStreamerEnumerable<T> extends TakeStreamerBase<T>
    implements EnumerableSource<T> {

        final EnumerableSource<T> enumerable;

        final long count;

        TakeStreamerEnumerable(Streamer<T> upstream, EnumerableSource<T> enumerable, long count) {
            super(upstream, count);
            this.enumerable = enumerable;
            this.count = count;
        }

        @Override
        public boolean nextSync() throws Throwable {
            while (remaining-- > 0) {
                return enumerable.nextSync();
            }
            return false;
        }

    }

    static final class TakeStreamerDeferredEnumerable<T> extends TakeStreamerBase<T>
    implements DeferredEnumerableSource<T> {

        final DeferredEnumerableSource<T> enumerable;

        final long count;

        TakeStreamerDeferredEnumerable(Streamer<T> upstream, DeferredEnumerableSource<T> enumerable, long count) {
            super(upstream, count);
            this.enumerable = enumerable;
            this.count = count;
        }

        @Override
        public boolean nextSync() throws Throwable {
            while (remaining-- > 0) {
                return enumerable.nextSync();
            }
            return false;
        }

        @Override
        public CompletionStage<Boolean> enumerableReady() {
            return enumerable.enumerableReady();
        }

    }
}
