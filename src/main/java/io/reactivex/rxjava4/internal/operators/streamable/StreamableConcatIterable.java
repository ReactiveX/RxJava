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
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;

public record StreamableConcatIterable<T>(
        Iterable<? extends Streamable<? extends T>> sources,
        ErrorMode errorMode
) implements Streamable<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        return new ConcatIteratorStreamer<>(sources.iterator(), cancellation);
    }

    static final class ConcatIteratorStreamer<T> extends AtomicInteger implements Streamer<T> {

        @Serial
        private static final long serialVersionUID = -9136569444189652718L;

        final Iterator<? extends Streamable<? extends T>> iterator;

        final StreamerCancellation cancellation;

        DisposableStreamerCancellation currentCancellation;

        Streamer<? extends T> upstream;

        CompletableFuture<Boolean> nextReady;

        ConcatIteratorStreamer(Iterator<? extends Streamable<? extends T>> iterator,
                StreamerCancellation cancellation) {
            this.iterator = iterator;
            this.cancellation = cancellation;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            nextReady = new CompletableFuture<Boolean>();
            drain();
            return nextReady;
        }

        @Override
        public @NonNull T current() {
            return upstream.current();
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            var localUpstream = upstream;
            var localCurrentCancellation = currentCancellation;
            upstream = null;
            nextReady = null;
            currentCancellation = null;
            if (localUpstream != null) {
                cancellation.delete(localCurrentCancellation);
                return localUpstream.finish();
            }
            return FINISHED;
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            do {
                if (upstream == null) {
                    if (iterator.hasNext()) {
                        currentCancellation = cancellation.derive();
                        var nextStreamable = iterator.next();
                        if (nextStreamable == null) {
                            nextReady.completeExceptionally(new NullPointerException("The iterator returned a null Streamable"));
                        } else {
                            upstream = nextStreamable.stream(currentCancellation);
                            drain();
                        }
                    } else {
                        nextReady.complete(false);
                    }
                } else {
                    upstream.next().whenComplete((v, e) -> {
                       if (e != null) {
                           nextReady.completeExceptionally(e);
                       } else
                       if (v)  {
                           nextReady.complete(true);
                       } else {
                           cancellation.delete(currentCancellation);
                           upstream.finish().whenComplete((_, u) -> {
                               if (u != null) {
                                   nextReady.completeExceptionally(u);
                               } else {
                                   upstream = null;
                                   drain();
                               }
                           });
                       }
                    });
                }
            } while (decrementAndGet() != 0);
        }
    }
}
