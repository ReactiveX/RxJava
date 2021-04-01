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

package io.reactivex.rxjava3.internal.observers;

import io.reactivex.rxjava3.internal.fuseable.QueueDisposable;

/**
 * An abstract QueueDisposable implementation that defaults all
 * unnecessary Queue methods to throw UnsupportedOperationException.
 * @param <T> the output value type
 */
public abstract class BasicQueueDisposable<T> implements QueueDisposable<T> {

    @Override
    public final boolean offer(T e) {
        throw new UnsupportedOperationException("Should not be called");
    }

    @Override
    public final boolean offer(T v1, T v2) {
        throw new UnsupportedOperationException("Should not be called");
    }
}
