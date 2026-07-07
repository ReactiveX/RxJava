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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.internal.disposables.DisposableHelper;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamMaybeSource;

public record StreamableFromMaybe<T>(MaybeSource<T> source)
implements Streamable<T>, HasUpstreamMaybeSource<T> {

    @Override
    public @NonNull Streamer<T> stream(@NonNull StreamerCancellation cancellation) {
        var streamer = new MaybeStreamer<T>(cancellation);
        cancellation.add(streamer);
        source.subscribe(streamer);
        return streamer;
    }

    static final class MaybeStreamer<T>
    extends AtomicReference<Disposable>
    implements Streamer<T>, MaybeObserver<T>, Disposable {

        @Serial
        private static final long serialVersionUID = -4580514428263096178L;

        final StreamerCancellation cancellation;

        final CompletableFuture<Boolean> waiter = new CompletableFuture<>();

        int stage;

        volatile T current;

        MaybeStreamer(StreamerCancellation cancellation) {
            this.cancellation = cancellation;
        }

        @Override
        public void onSubscribe(@NonNull Disposable d) {
            DisposableHelper.setOnce(this, d);
        }

        @Override
        public void onSuccess(@NonNull T t) {
            current = t;
            waiter.complete(true);
        }

        @Override
        public void onComplete() {
            waiter.complete(false);
        }

        @Override
        public void onError(@NonNull Throwable e) {
            waiter.completeExceptionally(e);
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            if (stage++ == 0) {
                return waiter;
            }
            return NEXT_FALSE;
        }

        @Override
        public @NonNull T current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            current = null;
            cancellation.delete(this);
            return FINISHED;
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
            waiter.completeExceptionally(new CancellationException());
        }

        @Override
        public boolean isDisposed() {
            return get() == DisposableHelper.DISPOSED;
        }
    }
}
