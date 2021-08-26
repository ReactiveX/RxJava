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

package io.reactivex.rxjava3.internal.fuseable;

import io.reactivex.rxjava3.operators.QueueFuseable;

/**
 * Represents an empty, async-only {@link QueueFuseable} instance that tracks and exposes a
 * canceled/disposed state.
 *
 * @param <T> the output value type
 * @since 3.0.0
 */
public final class CancellableQueueFuseable<T>
extends AbstractEmptyQueueFuseable<T> {

    volatile boolean disposed;

    @Override
    public void cancel() {
        disposed = true;
    }

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
