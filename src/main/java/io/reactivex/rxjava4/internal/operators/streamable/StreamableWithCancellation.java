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
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;

/// Allows replacing the [StreamerCancellation] the [#stream(StreamerCancellation)] method receives
/// via a callback function.
/// @param <T> the element type of the `Streamable`
/// @param source the upstream source `Streamable`
/// @param onStream the function called with the downstream `StreamerCancellation` and expected
///                 to return a `StreamerCancellation` to pass it to the upstream `source`
public record StreamableWithCancellation<T>(
        Streamable<T> source,
        Function<? super StreamerCancellation, ? extends StreamerCancellation> onStream)
implements Streamable<T>, HasUpstreamStreamableSource<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        try {
            var altCancellation = Objects.requireNonNull(onStream.apply(cancellation), "The onStream returned a null StreamerCancellation");
            return source.stream(altCancellation);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            return StreamableError.createFailed(ex);
        }
    }
}
