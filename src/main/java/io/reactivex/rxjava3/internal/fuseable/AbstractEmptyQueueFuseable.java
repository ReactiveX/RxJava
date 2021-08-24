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

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.operators.QueueDisposable;
import io.reactivex.rxjava3.operators.QueueFuseable;
import io.reactivex.rxjava3.operators.QueueSubscription;

/**
 * Represents an empty, async-only {@link QueueFuseable} instance.
 *
 * @param <T> the output value type
 * @since 3.0.0
 */
public abstract class AbstractEmptyQueueFuseable<T>
implements QueueSubscription<T>, QueueDisposable<T> {

    @Override
    public final int requestFusion(int mode) {
        return mode & ASYNC;
    }

    @Override
    public final boolean offer(@NonNull T value) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public final boolean offer(@NonNull T v1, @NonNull T v2) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public final T poll() throws Throwable {
        return null; // always empty
    }

    @Override
    public final boolean isEmpty() {
        return true; // always empty
    }

    @Override
    public final void clear() {
        // always empty
    }

    @Override
    public final void request(long n) {
        // no items to request
    }

    @Override
    public void cancel() {
        // default No-op
    }

    @Override
    public void dispose() {
        // default No-op
    }

    @Override
    public boolean isDisposed() {
        return false;
    }
}
