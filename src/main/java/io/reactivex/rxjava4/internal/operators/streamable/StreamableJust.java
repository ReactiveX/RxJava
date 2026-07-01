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
import io.reactivex.rxjava4.disposables.*;

public record StreamableJust<T>(@NonNull T item) implements Streamable<T> {

    public StreamableJust {
        Objects.requireNonNull(item, "item is null");
    }

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull DisposableContainer cancellation) {
        return new JustStreamer<>(item, cancellation);
    }

    static final class JustStreamer<T> implements Streamer<T>, Disposable {

        volatile T item;

        volatile DisposableContainer cancellation;

        volatile int stage;

        JustStreamer(T item, DisposableContainer cancellation) {
            this.item = item;
            this.cancellation = cancellation;
            cancellation.add(this);
        }

        @Override
        public @NonNull CompletionStage<Boolean> next(DisposableContainer cancellation) {
            if (stage == 0) {
                stage = 1;
                return NEXT_TRUE;
            }
            item = null;
            cancellation = null;
            stage = 2;
            return NEXT_FALSE;
        }

        @Override
        public @NonNull T current() {
            var item = this.item;
            if (stage == 0) {
                throw new NoSuchElementException("Streamable.just not yet started!");
            }
            if (stage == 2) {
                throw new NoSuchElementException("Streamable.just already completed!");
            }
            return item;
        }

        @Override
        public @NonNull CompletionStage<Void> finish(DisposableContainer canceller) {
            item = null;
            cancellation = null;
            stage = 2;
            return FINISHED;
        }

        @Override
        public void close() {
            Streamer.super.close();
        }

        @Override
        public boolean isDisposed() {
            return stage == 2;
        }

        @Override
        public void dispose() {
            var dc = cancellation;
            if (dc != null) {
                if (dc.delete(this)) {
                    close(); // FIXME not sure about this!
                }
            }
        }
    }
}
