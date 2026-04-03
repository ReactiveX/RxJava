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

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.disposables.*;

/**
 * Consist of a terminal stage and a disposable to be able to cancel a sequence.
 * @param <T> the return and element type of the various stages
 * @param stage the embedded stage to work with
 * @param disposable the way to cancel the stage concurrently
 * @since 4.0.0
 */
public record CompletionStageDisposable<T>(@NonNull CompletionStage<T> stage, @NonNull Disposable disposable) {

    /**
     * Await the completion of the current stage.
     */
    public void await() {
        Streamer.await(stage);
    }

    /**
     * Await the completion of the current stage.
     * @param canceller the canceller link
     */
    public void await(DisposableContainer canceller) {
        Streamer.await(stage, canceller);
    }
}
