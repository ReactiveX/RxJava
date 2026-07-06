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

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Subscriber;

import io.reactivex.rxjava4.annotations.*;

/**
 * An interface to submit items and terminal events to a consumer that indacates when the processing of
 * said item or terminal event has completed, similar to how {@link Subscriber} can receive events.
 * <p>
 * The general contract is to call {@link #next(Object)} zero or more times, then
 * call {@link #finish(Throwable)} at most once, all in a non-overlapping fashion and only if the
 * returned {@link CompletionStage} has completed in some fashion.
 * @param <T> the item type to be offered
 * @since 4.0.0
 */
public interface StreamerInput<@NonNull T> {

    /**
     * Offer the next item.
     * @param item the item being offered
     * @return a {@link CompletionStage} that completes with {@code true} if the value was successfully consumed,
     *         {@code false} if the value was rejected or exceptionally on error
     */
    CompletionStage<Boolean> next(T item);

    /**
     * Offer the final, terminal event.
     * @param throwable the optional throwable to signal error, null to signal normal completion
     * @return a {@link CompletionStage} that completes with {@code null} if the call succeeded
     *         or exceptionally on error
     */
    CompletionStage<Void> finish(@Nullable Throwable throwable);
}
