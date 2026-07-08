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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.core.Observer;
import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.core.Streamer;
import io.reactivex.rxjava4.disposables.CompositeDisposable;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.disposables.DisposableStreamerCancellation;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;
import io.reactivex.rxjava4.internal.util.ExceptionHelper;

public final class StreamableToObservable<T> extends Observable<T>
implements HasUpstreamStreamableSource<T> {

    final Streamable<T> source;

    public StreamableToObservable(Streamable<T> source) {
        this.source = source;
    }

    @Override
    public @NonNull Streamable<@NonNull T> source() {
        return source;
    }

    @Override
    protected void subscribeActual(@NonNull Observer<? super @NonNull T> observer) {
        var cs = new CompositeDisposable();
        observer.onSubscribe(cs);
        var sto = new StreamToObserver<>(source.stream(cs), observer, cs,
                new AtomicInteger(), new AtomicBoolean(), new AtomicReference<>(), new AtomicBoolean());
        cs.add(sto);
        sto.drain();
    }

    record StreamToObserver<T>(Streamer<T> streamer,
            Observer<? super T> observer,
            DisposableStreamerCancellation cancellation,
            AtomicInteger wip,
            AtomicBoolean done,
            AtomicReference<Throwable> mainError,
            AtomicBoolean disposed)
    implements BiConsumer<Object, Throwable>, Disposable {

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            do {
                if (done.get()) {
                    streamer.finish().whenComplete(this);
                    break;
                } else {
                    streamer.next().whenComplete(this);
                }
            } while (wip.decrementAndGet() != 0);
        }

        @Override
        public void accept(Object t, Throwable u) {
            if (disposed.get()) {
                return;
            }
            if (done.get()) {
                var w = ExceptionHelper.unwrapAndCombine(mainError.get(), u);
                if (w != null) {
                    observer.onError(w);
                } else {
                    observer.onComplete();
                }
            } else {
                if (u != null) {
                    mainError.lazySet(u);
                    done.lazySet(true);
                } else {
                    if ((Boolean)t) {
                        observer.onNext(streamer.current());
                    } else {
                        done.lazySet(true);
                    }
                }
                drain();
            }
        }

        @Override
        public void dispose() {
            disposed.lazySet(true);
            done.lazySet(true);
            drain();
        }

        @Override
        public boolean isDisposed() {
            return disposed.get();
        }
    }
}
