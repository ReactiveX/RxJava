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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import io.reactivex.rxjava4.annotations.*;

/**
 * Represents a reusable ping-pong style notification exchange where
 * one use/thread can signal {@link #ready()} to wake up another use/thread
 * on a {@link #await()} call.
 * @param <T> the element type of the notification pass-around
 * @since 4.0.0
 */
public final class StageResumable<T> extends AtomicReference<CompletableFuture<T>>
implements BiConsumer<T, Throwable> {

    @Serial
    private static final long serialVersionUID = -7518852864146380895L;

    /**
     * When the producer has arranged the item transfer via some field or queue,
     * call this method and call {@link CompletableFuture#complete(Object)}
     * or {@link CompletableFuture#completeExceptionally(Throwable)} to
     * signal resumption for any current or upcoming {@link #await()} caller.
     * @return the {@code CompletableFuture} to complete in some way
     */
    @CheckReturnValue
    @NonNull
    public CompletableFuture<T> ready() {
        CompletableFuture<T> cf;
        for (;;) {
            cf = get();
            if (cf !=  null) {
                break;
            }
            cf = new CompletableFuture<>();
            if (compareAndSet(null, cf)) {
                break;
            }
        }
        return cf;
    }

    /**
     * When the consumer is ready to receive an item, call this method
     * and apply a continuation function, such as {@link CompletableFuture#whenComplete(BiConsumer)}
     * to it to handle the signal and process any external data made ready.
     * @return the {@code CompletableFuture} to observe a completion value or exception
     */
    @CheckReturnValue
    @NonNull
    public CompletableFuture<T> await() {
        CompletableFuture<T> cf;
        for (;;) {
            cf = get();
            if (cf != null) {
                break;
            }
            cf = new CompletableFuture<>();
            if (compareAndSet(null, cf)) {
                break;
            }
        }
        return cf.whenComplete(this);
    }

    /// Used to clear any waiting [CompletableFuture] when the await finishes
    /// no concern to users and should not be called.
    /// @param t the completion value if any, ignored
    /// @param u the exception if any, ignored
    @Override
    public void accept(T t, Throwable u) {
        getAndSet(null);
    }
}
