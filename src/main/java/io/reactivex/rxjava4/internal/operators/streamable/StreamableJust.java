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

public record StreamableJust<T>(@NonNull T item) implements Streamable<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull StreamerCancellation cancellation) {
        return new JustStreamer<>(item);
    }

    static final class JustStreamer<T>
    implements Streamer<T>, IndexableSource<T>, EnumerableSource<T> {

        final T item;

        int stage;

        JustStreamer(T item) {
            this.item = item;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            if (stage == 0) {
                stage = 1;
                return NEXT_TRUE;
            }
            return NEXT_FALSE;
        }

        @Override
        public @NonNull T current() {
            return item;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            return FINISHED;
        }

        @Override
        public @NonNull T elementAt(long index) throws Throwable {
            return item;
        }

        @Override
        public long limit() {
            return 1;
        }

        @Override
        public boolean nextSync() throws Throwable {
            return stage++ == 0;
        }
    }
}
