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

import java.util.Objects;
import java.util.concurrent.*;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.Function;

public record StreamableOnErrorResumeNext<T>(
        Streamable<T> source,
        Function<? super Throwable, ? extends Streamable<? extends T>> fallbackMapper
) implements Streamable<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        return new OnErrorResumeNextStreamer<>(source.stream(cancellation), fallbackMapper, cancellation);
    }

    static final class OnErrorResumeNextStreamer<T> implements Streamer<T> {

        final StreamerCancellation downstreamDisposable;

        final Function<? super Throwable, ? extends Streamable<? extends T>> fallbackMapper;

        Streamer<T> mainStreamer;

        Streamer<? extends T> fallbackStreamer;

        public OnErrorResumeNextStreamer(Streamer<T> mainStreamer,
                Function<? super Throwable, ? extends Streamable<? extends T>> fallbackMapper,
                        StreamerCancellation downstreamDisposable) {
            this.mainStreamer = mainStreamer;
            this.fallbackMapper = fallbackMapper;
            this.downstreamDisposable = downstreamDisposable;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            if (fallbackStreamer != null) {
                return fallbackStreamer.next();
            }
            var cf = new CompletableFuture<Boolean>();
            mainStreamer.next().whenComplete((v, e) -> {
                if (e != null) {
                    var ms = mainStreamer;
                    mainStreamer = null;
                    ms.finish().whenComplete((_, e1) -> {
                        if (e1 != null) {
                            e.addSuppressed(e1);
                        }
                        try {
                            var fallback = Objects.requireNonNull(fallbackMapper.apply(e), "The fallbackMapper returned a null Streamable");
                            fallbackStreamer = fallback.stream(downstreamDisposable);
                            StreamableHelper.forward(fallbackStreamer.next(), cf);
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            ex.addSuppressed(e);
                            cf.completeExceptionally(ex);
                        }
                    });
                } else {
                    cf.complete(v);
                }
            });
            return cf;
        }

        @Override
        public @NonNull T current() {
            if (fallbackStreamer != null) {
                return fallbackStreamer.current();
            }
            return mainStreamer.current();
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            if (fallbackStreamer != null) {
                return fallbackStreamer.finish();
            }
            if (mainStreamer != null) {
                return mainStreamer.finish();
            }
            return FINISHED;
        }
    }
}
