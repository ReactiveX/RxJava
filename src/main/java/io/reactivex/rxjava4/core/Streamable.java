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

package io.reactivex.rxjava4.core;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.disposables.*;

/**
 * The {@code IAsyncEnumerable} of the Java world.
 * Runs best with Virtual Threads.
 * TODO proper docs
 * @param <T> the element type of the stream.
 * @since 4.0.0
 */
public abstract class Streamable<@NonNull T> {

    /**
     * Realizes the stream and returns an interface that let's one consume it.
     * @param cancellation where to register and listen for cancellation calls.
     * @return the Streamer instance to consume.
     */
    @NonNull
    public abstract Streamer<T> stream(@NonNull DisposableContainer cancellation);

    /**
     * Realizes the stream and returns an interface that let's one consume it.
     * @return the Streamer instance to consume.
     */
    @NonNull
    public final Streamer<T> stream() {
        return stream(new CompositeDisposable()); // FIXME, use a practically no-op disposable container instead
    }
}
