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

import java.util.NoSuchElementException;
import java.util.concurrent.CompletionStage;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.StreamerCancellation;
import io.reactivex.rxjava4.operators.*;

public enum StreamableEmpty implements Streamable<Object> {

    INSTANCE;

    @Override
    public @NonNull Streamer<Object> stream(@NonNull StreamerCancellation cancellation) {
        return EmptyStreamer.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Streamer<T> createEmpty() {
        return (Streamer<T>)EmptyStreamer.INSTANCE;
    }

    enum EmptyStreamer implements Streamer<Object>, IndexableSource<Object>, EnumerableSource<Object> {

        INSTANCE;

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            return NEXT_FALSE;
        }

        @Override
        public @NonNull Object current() {
            throw new NoSuchElementException("This Streamable/Streamer never has elements");
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            return FINISHED;
        }

        @Override
        public boolean nextSync() throws Throwable {
            return false;
        }

        @Override
        public @NonNull Object elementAt(long index) throws Throwable {
            throw new NoSuchElementException("This Streamable/Streamer never has elements");
        }

        @Override
        public long limit() {
            return 0;
        }
    }
}
