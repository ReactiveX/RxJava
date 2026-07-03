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

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.config.StreamableInterceptConfig;
import io.reactivex.rxjava4.disposables.DisposableContainer;

/**
 * Tracks the calls to the various lifecycle events and allows verifying the call patterns.
 * @param <T> the element type of the sequence
 * @since 4.0.0
 */
public final class StreamableLifecycleVerifier<T> {

    final StreamableInterceptConfig<T> config;

    final AtomicInteger onStreamCount;

    final AtomicInteger onNextCount;

    final AtomicInteger onCurrentCount;

    final AtomicInteger onFinishCount;

    public StreamableLifecycleVerifier() {
        onStreamCount = new AtomicInteger();
        onNextCount = new AtomicInteger();
        onCurrentCount = new AtomicInteger();
        onFinishCount = new AtomicInteger();
        config = new StreamableInterceptConfig<>(
                (_, v) -> { onStreamCount.getAndIncrement(); return v; },
                (_, v) -> { onNextCount.getAndIncrement(); return v; },
                (v)    -> { onCurrentCount.getAndIncrement(); return v; },
                (_, v) -> { onFinishCount.getAndIncrement(); return v; }
        );
    }

    public StreamableInterceptConfig<T> config() {
        return config;
    }

    /**
     * Verify the intercept registered only one {@link Streamable#stream(DisposableContainer)}
     * call and only one {@link Streamer#finish()} call.
     */
    public void verify() {
        assertAll(
                () -> assertEquals(1, onStreamCount.get(), "onStreamCount"),
                () -> assertEquals(1, onFinishCount.get(), "onFinishCount")
        );
    }

    /**
     * Verify the intercept registered only one {@link Streamable#stream(DisposableContainer)}
     * call and only one {@link Streamer#finish()} call and one more
     * {@link Streamer#next()} calls than {@link Streamer#current()} calls
     */
    public void verifyStrict() {
        assertAll(
                () -> assertEquals(1, onStreamCount.get(), "onStreamCount"),
                () -> assertEquals(onNextCount.get() - 1, onCurrentCount.get(), "onNextCount > onCurrentCount"),
                () -> assertEquals(1, onFinishCount.get(), "onFinishCount")
        );
    }
}
