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

package io.reactivex.rxjava3.internal.observers;

import java.util.concurrent.CountDownLatch;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.util.BlockingHelper;

/**
 * Blocks until the upstream terminates and dispatches the outcome to
 * the actual observer.
 *
 * @param <T> the element type of the source
 * @since 3.0.0
 */
public final class BlockingDisposableMultiObserver<T>
extends CountDownLatch
implements MaybeObserver<T>, SingleObserver<T>, CompletableObserver, Disposable {

    T value;
    Throwable error;

    final SequentialDisposable upstream;

    public BlockingDisposableMultiObserver() {
        super(1);
        upstream = new SequentialDisposable();
    }

    @Override
    public void dispose() {
        upstream.dispose();
        countDown();
    }

    @Override
    public boolean isDisposed() {
        return upstream.isDisposed();
    }

    @Override
    public void onSubscribe(@NonNull Disposable d) {
        DisposableHelper.setOnce(upstream, d);
    }

    @Override
    public void onSuccess(@NonNull T t) {
        this.value = t;
        upstream.lazySet(Disposable.disposed());
        countDown();
    }

    @Override
    public void onError(@NonNull Throwable e) {
        this.error = e;
        upstream.lazySet(Disposable.disposed());
        countDown();
    }

    @Override
    public void onComplete() {
        upstream.lazySet(Disposable.disposed());
        countDown();
    }

    public void blockingConsume(CompletableObserver observer) {
        if (getCount() != 0) {
            try {
                BlockingHelper.verifyNonBlocking();
                await();
            } catch (InterruptedException ex) {
                dispose();
                observer.onError(ex);
                return;
            }
        }
        if (isDisposed()) {
            return;
        }

        Throwable ex = error;
        if (ex != null) {
            observer.onError(ex);
        } else {
            observer.onComplete();
        }
    }

    public void blockingConsume(SingleObserver<? super T> observer) {
        if (getCount() != 0) {
            try {
                BlockingHelper.verifyNonBlocking();
                await();
            } catch (InterruptedException ex) {
                dispose();
                observer.onError(ex);
                return;
            }
        }
        if (isDisposed()) {
            return;
        }

        Throwable ex = error;
        if (ex != null) {
            observer.onError(ex);
        } else {
            observer.onSuccess(value);
        }
    }

    public void blockingConsume(MaybeObserver<? super T> observer) {
        if (getCount() != 0) {
            try {
                BlockingHelper.verifyNonBlocking();
                await();
            } catch (InterruptedException ex) {
                dispose();
                observer.onError(ex);
                return;
            }
        }
        if (isDisposed()) {
            return;
        }

        Throwable ex = error;
        if (ex != null) {
            observer.onError(ex);
        } else {
            T v = value;
            if (v == null) {
                observer.onComplete();
            } else {
                observer.onSuccess(v);
            }
        }
    }
}
