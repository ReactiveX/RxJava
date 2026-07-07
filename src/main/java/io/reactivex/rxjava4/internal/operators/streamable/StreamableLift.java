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

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.StreamerCancellation;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;

public record StreamableLift<T, R>(
        Streamable<T> source,
        @NonNull StreamableOperator<? super T, ? extends R> lifter
) implements Streamable<R>, HasUpstreamStreamableSource<T> {

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull Streamer<@NonNull R> stream(@NonNull StreamerCancellation cancellation) {
        var upstream = source.stream(cancellation);
        try {
            return (Streamer<R>)Objects.requireNonNull(lifter.apply(cancellation, upstream),
                    "lifter returned a null Streaner");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            return StreamableError.createFailed(ex);
            // FIXME what should happen to upstream in this case really? have the error delivery call its finish() method?
        }
    }
}
