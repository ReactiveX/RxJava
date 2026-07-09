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
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;

public record StreamableTake<T>(Streamable<T> source, long count)
implements Streamable<T>, HasUpstreamStreamableSource<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        var dsc = cancellation.derive();
        return new TakeStreamer<>(source.stream(dsc), count, dsc);
    }

    static final class TakeStreamer<T> implements Streamer<T> {
        final Streamer<T> upstream;

        final Disposable upstreamDisposable;

        long remaining;

        TakeStreamer(Streamer<T> upstream, long count, Disposable upstreamDisposable) {
            this.upstream = upstream;
            this.upstreamDisposable = upstreamDisposable;
            this.remaining = count;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            if (remaining-- <= 0L) {
                upstreamDisposable.dispose();
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
}
