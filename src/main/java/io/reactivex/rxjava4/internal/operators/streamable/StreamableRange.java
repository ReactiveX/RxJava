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
import io.reactivex.rxjava4.disposables.StreamerCancellation;
import io.reactivex.rxjava4.operators.*;

public record StreamableRange(int start, int count) implements IsSynchronousStreamable<Integer> {

    @Override
    public @NonNull Streamer<@NonNull Integer> stream(@NonNull StreamerCancellation cancellation) {
        return new RangeStreamer<>(start, start + count);
    }

    static final class RangeStreamer<T>
    implements Streamer<Integer>, IndexableSource<Integer>, EnumerableSource<Integer> {

        final int start;

        final int end;

        int current;

        int index;

        RangeStreamer(int start, int end) {
            this.start = start;
            this.index = start;
            this.end = end;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            return nextSync() ? NEXT_TRUE : NEXT_FALSE;
        }

        @Override
        public @NonNull Integer current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            index = end;
            current = end;
            return FINISHED;
        }

        @Override
        public Integer elementAt(long index) {
            return (int)(start + index);
        }

        @Override
        public long limit() {
            return end - start;
        }

        @Override
        public boolean nextSync() {
            int i = index;
            if (i == end) {
                return false;
            }
            current = i;
            index = i + 1;
            return true;
        }
    }
}
