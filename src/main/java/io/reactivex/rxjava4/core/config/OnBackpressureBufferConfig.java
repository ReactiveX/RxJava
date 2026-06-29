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
import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.internal.functions.*;

/**
 * Configuration record for onBackpressureBuffer() operators.
 * @param <T> the element type of the sequences being compared
 * @param capacity
 *                number of slots available in the buffer.
 * @param delayError
 *                if {@code true}, an exception from the current {@code Flowable} is delayed until all buffered elements have been
 *                consumed by the downstream; if {@code false}, an exception is immediately signaled to the downstream, skipping
 *                any buffered element
 * @param unbounded
 *                if {@code true}, the capacity value is interpreted as the internal "island" size of the unbounded buffer
 * @param onDropped
 *                the {@link Consumer} to be called with the item that could not be buffered due to capacity constraints.
 * @since 4.0.0
 */

public record OnBackpressureBufferConfig<T>(
        int capacity,
        boolean delayError,
        boolean unbounded,
        @NonNull Consumer<? super T> onDropped) {

    /**
     * The default settings with no error delay, unbounded, no onOverflow or onDropped activity.
     */
    public static final OnBackpressureBufferConfig<Object> DEFAULT = new OnBackpressureBufferConfig<>(false, true);

    /**
     * Creates a config with the given capacity, no error delay, bounded, no callbacks.
     * @param capacity
     *                number of slots available in the buffer.
     */
    public OnBackpressureBufferConfig(int capacity) {
        this(capacity, false, false, Functions.emptyConsumer());
    }

    /**
     * Creates a config with the given error delay mode, capacity of {@link Flowable#bufferSize()},
     * bounded, and no callback.
     * @param delayError
     *                if {@code true}, an exception from the current {@code Flowable} is delayed until all buffered elements have been
     *                consumed by the downstream; if {@code false}, an exception is immediately signaled to the downstream, skipping
     *                any buffered element
     */
    public OnBackpressureBufferConfig(boolean delayError) {
        this(Flowable.bufferSize(), delayError, false, Functions.emptyConsumer());
    }

    /**
     * Creates a config with the given capacity, given error delay mode,
     * bounded, and no callback.
     * @param capacity
     *                number of slots available in the buffer.
     * @param delayError
     *                if {@code true}, an exception from the current {@code Flowable} is delayed until all buffered elements have been
     *                consumed by the downstream; if {@code false}, an exception is immediately signaled to the downstream, skipping
     *                any buffered element
     */
    public OnBackpressureBufferConfig(int capacity, boolean delayError) {
        this(capacity, delayError, false, Functions.emptyConsumer());
    }

    /**
     * Creates a config with the given error delay mode, given boundedneess,
     * capacity of {@link Flowable#bufferSize()} and no callback.
     * @param delayError
     *                if {@code true}, an exception from the current {@code Flowable} is delayed until all buffered elements have been
     *                consumed by the downstream; if {@code false}, an exception is immediately signaled to the downstream, skipping
     *                any buffered element
     * @param unbounded
     *                if {@code true}, the capacity value is interpreted as the internal "island" size of the unbounded buffer
     */
    public OnBackpressureBufferConfig(boolean delayError, boolean unbounded) {
        this(Flowable.bufferSize(), delayError, unbounded, Functions.emptyConsumer());
    }

    /**
     * Creates a config with the given capacity, error delay mode and unboundedness,
     * and no callback.
     * @param capacity
     *                number of slots available in the buffer.
     * @param delayError
     *                if {@code true}, an exception from the current {@code Flowable} is delayed until all buffered elements have been
     *                consumed by the downstream; if {@code false}, an exception is immediately signaled to the downstream, skipping
     *                any buffered element
     * @param unbounded
     *                if {@code true}, the capacity value is interpreted as the internal "island" size of the unbounded buffer
     */
    public OnBackpressureBufferConfig(int capacity, boolean delayError, boolean unbounded) {
        this(capacity, delayError, unbounded, Functions.emptyConsumer());
    }

    /**
     * Creates a config with the given onDropped callback, capacity of {@link Flowable#bufferSize()},
     * no error delay, bounded, and no callback.
     * @param onDropped
     *                the {@link Consumer} to be called with the item that could not be buffered due to capacity constraints.
     */
    public OnBackpressureBufferConfig(@NonNull Consumer<? super T> onDropped) {
        this(Flowable.bufferSize(), false, false, onDropped);
    }

    /**
     * Creates a config with the given capacity, given onDropped callback
     * no error delay, bounded, and no callback.
     * @param capacity
     *                number of slots available in the buffer.
     * @param onDropped
     *                the {@link Consumer} to be called with the item that could not be buffered due to capacity constraints.
     */
    public OnBackpressureBufferConfig(int capacity, @NonNull Consumer<? super T> onDropped) {
        this(capacity, false, false, onDropped);
    }

    /**
     * Creates a config with all the provided values.
     * @param capacity
     *                number of slots available in the buffer.
     * @param delayError
     *                if {@code true}, an exception from the current {@code Flowable} is delayed until all buffered elements have been
     *                consumed by the downstream; if {@code false}, an exception is immediately signaled to the downstream, skipping
     *                any buffered element
     * @param unbounded
     *                if {@code true}, the capacity value is interpreted as the internal "island" size of the unbounded buffer
     * @param onDropped
     *                the {@link Consumer} to be called with the item that could not be buffered due to capacity constraints.
     */
    public OnBackpressureBufferConfig {
        ObjectHelper.verifyPositive(capacity, "capacity");
        Objects.requireNonNull(onDropped, "onDropped is null");
    }
}
