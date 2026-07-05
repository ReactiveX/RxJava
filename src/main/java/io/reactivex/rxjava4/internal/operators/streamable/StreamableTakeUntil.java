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

import static io.reactivex.rxjava4.internal.operators.streamable.StreamableHelper.*;

import java.util.concurrent.*;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.DisposableContainer;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;

public record StreamableTakeUntil<T, U>(
        Streamable<T> source,
        Streamable<U> other
) implements Streamable<T>, HasUpstreamStreamableSource<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull DisposableContainer cancellation) {
        var otherCancellation = cancellation.derive();
        var otherStreamer = other.stream(otherCancellation);
        var mainCancellation = cancellation.derive();
        var mainStreamer = source.stream(mainCancellation);

        var streamer = new TakeUntilMainStreamer<>(
                mainStreamer, otherStreamer, mainCancellation, otherCancellation,
                suppressValueAndCancel(otherStreamer.next(), false)
            );

        return streamer;
    }

    static final class TakeUntilMainStreamer<T, U> implements Streamer<T> {

        final Streamer<? extends T> upstream;

        final Streamer<? extends U> otherStreamer;

        final DisposableContainer mainCancellation;

        final DisposableContainer otherCancellation;

        final CompletionStage<Boolean> otherNext;

        CompletionStage<Boolean> mainNext;

        public TakeUntilMainStreamer(
                Streamer<? extends T> upstream,
                Streamer<? extends U> otherStreamer,
                DisposableContainer mainCancellation,
                DisposableContainer otherCancellation,
                CompletionStage<Boolean> otherNext
        ) {
            this.upstream = upstream;
            this.otherStreamer = otherStreamer;
            this.mainCancellation = mainCancellation;
            this.otherCancellation = otherCancellation;
            this.otherNext = otherNext;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            mainNext = suppressCancel(upstream.next(), mainCancellation, false);
            return StreamableHelper.race(mainNext, otherNext, w -> {
                if (w == 2) {
                    mainCancellation.dispose();
                }
            });
        }

        @Override
        public @NonNull T current() {
            return upstream.current();
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            otherCancellation.dispose();
            return andThenSupply(
                    whenBoth(mainNext, otherNext),
                    () -> whenBoth(upstream.finish(), otherStreamer.finish())
                   );
        }
    }
}
