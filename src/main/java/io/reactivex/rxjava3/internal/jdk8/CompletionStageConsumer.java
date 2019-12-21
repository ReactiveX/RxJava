/**
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
package io.reactivex.rxjava3.internal.jdk8;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Class that extends CompletableFuture and converts multiple types of reactive consumers
 * and their signals into completion signals.
 * @param <T> the element type
 * @since 3.0.0
 */
public final class CompletionStageConsumer<T> extends CompletableFuture<T>
implements MaybeObserver<T>, SingleObserver<T>, CompletableObserver {

    final AtomicReference<Disposable> upstream;

    final boolean hasDefault;

    final T defaultItem;

    public CompletionStageConsumer(boolean hasDefault, T defaultItem) {
        this.hasDefault = hasDefault;
        this.defaultItem = defaultItem;
        this.upstream = new AtomicReference<>();
    }

    @Override
    public void onSubscribe(@NonNull Disposable d) {
        DisposableHelper.setOnce(upstream, d);
    }

    @Override
    public void onSuccess(@NonNull T t) {
        clear();
        complete(t);
    }

    @Override
    public void onError(Throwable t) {
        clear();
        if (!completeExceptionally(t)) {
            RxJavaPlugins.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (hasDefault) {
            complete(defaultItem);
        } else {
            completeExceptionally(new NoSuchElementException("The source was empty"));
        }
    }

    void cancelUpstream() {
        DisposableHelper.dispose(upstream);
    }

    void clear() {
        upstream.lazySet(DisposableHelper.DISPOSED);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        cancelUpstream();
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean complete(T value) {
        cancelUpstream();
        return super.complete(value);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        cancelUpstream();
        return super.completeExceptionally(ex);
    }
}
