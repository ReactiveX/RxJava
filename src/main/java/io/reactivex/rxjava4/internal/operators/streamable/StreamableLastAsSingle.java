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
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.internal.disposables.DisposableOnly;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;
import io.reactivex.rxjava4.internal.util.ExceptionHelper;

public final class StreamableLastAsSingle<T>
extends Single<T> implements HasUpstreamStreamableSource<T> {

    final Streamable<T> source;

    final T defaultItem;

    public StreamableLastAsSingle(@Nullable Streamable<T> source, @Nullable T defaultItem) {
        this.source = source;
        this.defaultItem = defaultItem;
    }

    @Override
    public @NonNull Streamable<@NonNull T> source() {
        return source;
    }

    @Override
    protected void subscribeActual(@NonNull SingleObserver<? super @NonNull T> observer) {
        var cancellation = new CompositeDisposable();
        var drainer = new LastStreamer<>(observer, defaultItem, cancellation);
        observer.onSubscribe(drainer);
        drainer.upstream = source.stream(cancellation);
        drainer.drain();
    }

    static final class LastStreamer<T>
    extends AtomicInteger
    implements BiConsumer<Object, Throwable>, DisposableOnly {

        @Serial
        private static final long serialVersionUID = 2423155860070143815L;

        final SingleObserver<? super T> downstream;

        final DisposableStreamerCancellation cancellation;

        final T defaultItem;

        Streamer<T> upstream;

        T current;

        Throwable nextFailure;

        volatile boolean done;

        LastStreamer(
                SingleObserver<? super T> downstream,
                T defaultItem,
                DisposableStreamerCancellation cancellation) {
            this.downstream = downstream;
            this.defaultItem = defaultItem;
            this.cancellation = cancellation;
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            do {
                if (done) {
                    upstream.finish().whenComplete(this);
                    break;
                } else {
                    upstream.next().whenComplete(this);
                }
            } while (decrementAndGet() != 0);
        }

        @Override
        public void accept(Object t, Throwable u) {
            if (done) {
                if (u != null || nextFailure != null) {
                    downstream.onError(ExceptionHelper.unwrapAndCombine(nextFailure, u));
                } else {
                    if (current == null) {
                        if (defaultItem != null) {
                            downstream.onSuccess(defaultItem);
                        } else {
                            downstream.onError(new NoSuchElementException());
                        }
                    } else {
                        downstream.onSuccess(current);
                    }
                }
            } else {
                if (u != null) {
                    nextFailure = u;
                    done = true;
                    drain();
                } else {
                    if ((Boolean)t) {
                        current = upstream.current();
                    } else {
                        done = true;
                    }
                    drain();
                }
            }
        }

        @Override
        public void dispose() {
            cancellation.dispose();
        }
    }
}
