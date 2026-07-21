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

public record StreamableRangeLong(long start, long count) implements IsSynchronousStreamable<Long> {

    @Override
    public @NonNull Streamer<@NonNull Long> stream(@NonNull StreamerCancellation cancellation) {
        return new RangeLongStreamer<>(start, start + count);
    }

    static final class RangeLongStreamer<T>
    implements Streamer<Long>, IndexableSource<Long>, EnumerableSource<Long> {

        final long start;

        final long end;

        volatile long current;

        volatile long index;

        RangeLongStreamer(long start, long end) {
            index = start;
            this.start = start;
            this.end = end;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            long i = index;
            if (i >= end) {
                return NEXT_FALSE;
            }
            current = i;
            index = i + 1;
            return NEXT_TRUE;
        }

        @Override
        public @NonNull Long current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            index = end;
            current = end;
            return FINISHED;
        }

        @Override
        public Long elementAt(long index) {
            return start + index;
        }

        @Override
        public long limit() {
            return end - start;
        }

        @Override
        public boolean nextSync() throws Throwable {
            if (++current >= end) {
                return false;
            }
            return true;
        }
    }
}
