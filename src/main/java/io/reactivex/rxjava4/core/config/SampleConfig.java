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

package io.reactivex.rxjava4.core.config;

import java.util.Objects;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.functions.Consumer;
import io.reactivex.rxjava4.internal.functions.Functions;

/**
 * Configuration record for the timed sample() operator.
 * @param <T> the element type of the sequence being sampled
 * @param emitLast
 *            if {@code true} and the upstream completes while there is still an unsampled item available,
 *            that item is emitted to downstream before completion
 *            if {@code false}, an unsampled last item is ignored.
 * @param onDropped
 *            called with the current entry when it has been replaced by a new one
 * @since 4.0.0
 */
public record SampleConfig<T>(boolean emitLast, @NonNull Consumer<? super T> onDropped) {

    /**
     * Default configuration with no emit last and an empty onDropped consumer.
     */
    public static final SampleConfig<Object> DEFAULT = new SampleConfig<>(false, Functions.emptyConsumer());
    /**
     * Default configuration with emit last setting and an empty onDropped consumer.
     */
    public static final SampleConfig<Object> EMIT_LAST = new SampleConfig<>(true, Functions.emptyConsumer());

    /**
     * Creates a config with the given emit last option ad an empty onDropped callback.
     * @param emitLast
     *            if {@code true} and the upstream completes while there is still an unsampled item available,
     *            that item is emitted to downstream before completion
     *            if {@code false}, an unsampled last item is ignored.
     */
    public SampleConfig(boolean emitLast) {
        this(emitLast, Functions.emptyConsumer());
    }

    /**
     * Creates a config with the given onDropped callback and an no-emit last.
     * @param onDropped
     *            called with the current entry when it has been replaced by a new one
     */
    public SampleConfig(@NonNull Consumer<? super T> onDropped) {
        this(false, onDropped);
    }

    /**
     * Creates a config with the given parameters.
     * @param emitLast
     *            if {@code true} and the upstream completes while there is still an unsampled item available,
     *            that item is emitted to downstream before completion
     *            if {@code false}, an unsampled last item is ignored.
     * @param onDropped
     *            called with the current entry when it has been replaced by a new one
     */
    public SampleConfig {
        Objects.requireNonNull(onDropped, "onDropped is null");
    }
}
