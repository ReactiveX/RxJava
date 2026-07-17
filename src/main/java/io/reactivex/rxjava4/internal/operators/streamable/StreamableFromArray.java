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

import java.util.concurrent.*;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.StreamerCancellation;
import io.reactivex.rxjava4.operators.*;

public record StreamableFromArray<T>(@NonNull T[] items) implements Streamable<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        return new FromArrayStreamer<>(items);
    }

    static final class FromArrayStreamer<T>
    implements Streamer<T>, IndexableSource<T>, EnumerableSource<T> {

        final T[] items;

        int index;

        public FromArrayStreamer(T[] items) {
            this.items = items;
            this.index = -1;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            if (++index >= items.length) {
                return NEXT_FALSE;
            }
            if (current() == null) {
                return CompletableFuture.failedFuture(createNullError(index));
            }
            return NEXT_TRUE;
        }

        static NullPointerException createNullError(int index) {
            return new NullPointerException("Item at index " + index + " is null.");
        }

        @Override
        public boolean nextSync() {
            if (++index < items.length) {
                if (current() == null) {
                    throw createNullError(index);
                }
                return true;
            };
            return false;
        }

        @Override
        public @NonNull T current() {
            return items[index];
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            index = items.length;
            return FINISHED;
        }

        @Override
        public @NonNull T elementAt(long index) throws Throwable {
            var v = items[(int)index];
            if (v ==  null) {
                throw createNullError((int)index);
            }
            return v;
        }

        @Override
        public long limit() {
            return items.length;
        }
    }
}
