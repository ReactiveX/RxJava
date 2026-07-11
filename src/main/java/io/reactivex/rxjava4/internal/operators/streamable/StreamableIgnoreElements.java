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
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.StreamerCancellation;
import io.reactivex.rxjava4.internal.util.ExceptionHelper;

public record StreamableIgnoreElements<T>(Streamable<T> source)
implements Streamable<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        return new IgnoreElementsStreamer<>(source.stream(cancellation));
    }

    static final class IgnoreElementsStreamer<T> extends AtomicInteger
    implements Streamer<T>, BiConsumer<Object, Throwable> {

        @Serial
        private static final long serialVersionUID = 2265801211815192189L;

        final Streamer<T> upstream;

        final CompletableFuture<Boolean> waiter;

        int stage;

        Throwable mainError;

        volatile boolean done;

        IgnoreElementsStreamer(Streamer<T> upstream)  {
            this.upstream = upstream;
            this.waiter = new CompletableFuture<>();
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            if (stage == 0) {
                stage = 1;
                drain();
                return waiter;
            }
            return NEXT_FALSE;
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            do {
                if (done) {
                    upstream.finish().whenComplete(this);
                    break;
                } else {
                    upstream.next().whenComplete(this);
                }
            } while (decrementAndGet() != 0);
        }


        @Override
        public void accept(Object t, Throwable u) {
            if (done) {
                if (mainError != null || u != null) {
                    waiter.completeExceptionally(ExceptionHelper.unwrapAndCombine(mainError, u));
                } else {
                    waiter.complete(false);
                }
            } else {
                if (u != null) {
                    mainError = u;
                    done = true;
                } else
                if (!(Boolean)t) {
                    done = true;
                }
                drain();
            }
        }

        @Override
        public @NonNull T current() {
            throw new NoSuchElementException();
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            return FINISHED;
        }

    }
}
