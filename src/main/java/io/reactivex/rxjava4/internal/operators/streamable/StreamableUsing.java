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
import io.reactivex.rxjava4.disposables.DisposableContainer;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.*;

public record StreamableUsing<T, R>(
        Supplier<? extends R> resourceSupplier,
        Function<? super R, ? extends Streamable<? extends T>> resourceMapper,
        Consumer<? super R> resourceCleaner
)
implements Streamable<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull DisposableContainer cancellation) {
        R resource;
        Streamable<? extends T> source;
        try {
            resource = resourceSupplier.get();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            return StreamableError.createFailed(ex);
        }

        try {
            source = Objects.requireNonNull(resourceMapper.apply(resource), "The resourceMapper returned a null Streamable");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            return new UsingStreamer<>(StreamableError.createFailed(ex), () -> resourceCleaner.accept(resource));
        }

        return new UsingStreamer<>(source.stream(cancellation), () -> resourceCleaner.accept(resource));
    }

    static final class UsingStreamer<T, R> implements Streamer<T> {

        final Streamer<? extends T> upstream;

        Action cleanup;

        UsingStreamer(Streamer<? extends T> upstream, Action cleanup) {
            this.upstream = upstream;
            this.cleanup = cleanup;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            return upstream.next();
        }

        @Override
        public @NonNull T current() {
            return upstream.current();
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            var cf = new CompletableFuture<Void>();
            upstream.finish().whenComplete((_, e) -> {
                try {
                    var c = cleanup;
                    cleanup = null;
                    c.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(e);
                    cf.completeExceptionally(ex);
                    return;
                }
                cf.complete(null);
            });
            return cf;
        }
    }
}
