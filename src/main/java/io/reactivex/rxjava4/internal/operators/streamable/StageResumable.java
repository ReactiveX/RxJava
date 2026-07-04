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

public final class StageResumable<T> extends AtomicReference<CompletableFuture<T>>
implements BiConsumer<T, Throwable> {

    @Serial
    private static final long serialVersionUID = -7518852864146380895L;

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

    @Override
    public void accept(T t, Throwable u) {
        getAndSet(null);
    }
}
