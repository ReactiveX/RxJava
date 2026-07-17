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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.internal.disposables.*;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamSingleSource;
import io.reactivex.rxjava4.operators.DeferredEnumerableSource;

public record StreamableSingleFlattenAs<T, U>(
        SingleSource<T> source,
        Function<? super T, @NonNull ? extends Iterable<? extends U>> mapper
) implements Streamable<U>, HasUpstreamSingleSource<T> {

    @Override
    public @NonNull Streamer<@NonNull U> stream(@NonNull StreamerCancellation cancellation) {
        var observer = new FlattenAsSingleObserver<>(mapper, cancellation);
        cancellation.add(observer);
        source.subscribe(observer);
        return observer;
    }

    static final class FlattenAsSingleObserver<T, U>
    extends AtomicInteger
    implements SingleObserver<T>, Streamer<U>, DisposableOnly,
            DeferredEnumerableSource<U> {

        @Serial
        private static final long serialVersionUID = 796267562672678347L;

        final StreamerCancellation cancellation;

        final Function<? super T, @NonNull ? extends Iterable<? extends U>> mapper;

        final SequentialDisposable upstream;

        final CompletableFuture<Boolean> iteratorReady;

        U current;

        volatile Iterator<? extends U> currentIterator;

        boolean deferredOnce;

        FlattenAsSingleObserver(
                Function<? super T, @NonNull ? extends Iterable<? extends U>> mapper,
                StreamerCancellation cancellation) {
            this.cancellation = cancellation;
            this.mapper = mapper;
            this.iteratorReady = new CompletableFuture<>();
            this.upstream = new SequentialDisposable();
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            var it = currentIterator;
            if (it == null) {
                return iteratorReady;
            }
            if (!deferredOnce) {
                deferredOnce = true;
                return NEXT_TRUE;
            }
            if (it.hasNext()) {
                current = it.next();
                return NEXT_TRUE;
            }
            return NEXT_FALSE;
        }

        @Override
        public @NonNull U current() {
            return current;
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            currentIterator = null;
            DisposableHelper.dispose(upstream);
            return FINISHED;
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(upstream);
            iteratorReady.completeExceptionally(new CancellationException());
        }

        @Override
        public void onSubscribe(@NonNull Disposable d) {
            DisposableHelper.setOnce(upstream, d);
        }

        @Override
        public void onSuccess(@NonNull T t) {
            try {
                var iterator = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Iterable").iterator();
                if (iterator.hasNext()) {
                    current = iterator.next();
                    this.currentIterator = iterator;
                    iteratorReady.complete(true);
                } else {
                    iteratorReady.complete(false);
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.lazySet(DisposableHelper.DISPOSED);
                iteratorReady.completeExceptionally(ex);
                return;
            }
        }

        @Override
        public void onError(@NonNull Throwable e) {
            upstream.lazySet(DisposableHelper.DISPOSED);
            iteratorReady.completeExceptionally(e);
        }

        @Override
        public CompletionStage<Boolean> enumerableReady() {
            return iteratorReady;
        }

        @Override
        public boolean nextSync() throws Throwable {
            // because onSuccess will pull out the first item
            if (!deferredOnce) {
                deferredOnce = true;
                return true;
            }
            if (currentIterator.hasNext()) {
                current = currentIterator.next();
                return true;
            }
            return false;
        }
    }
}
