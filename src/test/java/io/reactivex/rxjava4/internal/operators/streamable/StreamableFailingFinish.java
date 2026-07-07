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
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.TestException;

public enum StreamableFailingFinish implements Streamable<Integer> {

    NEVER(0),
    MAIN_FAILS(1),
    MAIN_COMPLETES(2)
    ;

    private final class StreamableFailingFinishStreamer implements Streamer<Integer> {
        private final @NonNull DisposableContainer dc;

        private StreamableFailingFinishStreamer(@NonNull DisposableContainer dc) {
            this.dc = dc;
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            var cf = new CompletableFuture<Boolean>();
            var d = Disposable.fromFuture(cf, true);
            dc.add(d);
            if (mode == 1) {
                cf.completeExceptionally(new TestException("StreamableFailingFinish(true)"));
            } else
            if (mode == 2) {
                cf.complete(false);
            }
            return cf;
        }

        @Override
        public @NonNull Integer current() {
            return null;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            return CompletableFuture.failedFuture(new TestException());
        }
    }

    final int mode;

    StreamableFailingFinish(int mode) {
        this.mode = mode;
    }

    @Override
    public @NonNull Streamer<@NonNull Integer> stream(@NonNull DisposableContainer dc) {
        return new StreamableFailingFinishStreamer(dc);
    }
}