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

import java.util.*;
import java.util.concurrent.*;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.StreamerCancellation;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.internal.operators.streamable.StreamableEmpty.EmptyStreamer;

public record StreamableFromIterable<T>(@NonNull Iterable<? extends T> items) implements Streamable<T> {

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        Iterator<? extends T> iterator;
        try {
            iterator = Objects.requireNonNull(items.iterator(), "iterator is null");
            if (!iterator.hasNext()) {
                return (Streamer<T>)EmptyStreamer.INSTANCE;
            }
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            return StreamableError.createFailed(ex);
        }
        return new IteratorStreamer<>(iterator);
    }

    static final class IteratorStreamer<T> implements Streamer<T> {

        Iterator<? extends T> iterator;

        long index;

        volatile T current;

        IteratorStreamer(Iterator<? extends T> iterator) {
            this.iterator = iterator;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            if (index == 0L || iterator.hasNext()) {
                var v = iterator.next();
                current = v;
                if (v == null) {
                    return CompletableFuture.failedStage(new NullPointerException("Item at index " + index + " is null."));
                }
                index++;
                return NEXT_TRUE;
            }
            current = null;
            return NEXT_FALSE;
        }

        @Override
        public @NonNull T current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            iterator = null;
            current = null;
            return FINISHED;
        }
    }
}
