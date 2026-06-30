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

import java.util.concurrent.CompletionStage;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.DisposableContainer;

public record StreamableRange(int start, int count) implements Streamable<Integer> {

    @Override
    public @NonNull Streamer<@NonNull Integer> stream(@NonNull DisposableContainer cancellation) {
        return new RangeStreamer<>(start, start + count);
    }

    static final class RangeStreamer<T> implements Streamer<Integer> {

        final int end;

        volatile int current;

        volatile int index;

        RangeStreamer(int start, int end) {
            index = start;
            this.end = end;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next(@NonNull DisposableContainer cancellation) {
            int i = index;
            if (i >= end) {
                return NEXT_FALSE;
            }
            current = i;
            index = i + 1;
            return NEXT_TRUE;
        }

        @Override
        public @NonNull Integer current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish(@NonNull DisposableContainer cancellation) {
            index = end;
            current = end;
            return FINISHED;
        }
    }
}
