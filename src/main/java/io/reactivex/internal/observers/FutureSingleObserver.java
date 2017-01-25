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

package io.reactivex.internal.observers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.util.BlockingHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * An Observer + Future that expects exactly one upstream value and provides it
 * via the (blocking) Future API.
 *
 * @param <T> the value type
 */
public final class FutureSingleObserver<T> extends CountDownLatch
implements SingleObserver<T>, Future<T>, Disposable {

    T value;
    Throwable error;

    final AtomicReference<Disposable> s;

    public FutureSingleObserver() {
        super(1);
        this.s = new AtomicReference<Disposable>();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        for (;;) {
            Disposable a = s.get();
            if (a == this || a == DisposableHelper.DISPOSED) {
                return false;
            }

            if (s.compareAndSet(a, DisposableHelper.DISPOSED)) {
                if (a != null) {
                    a.dispose();
                }
                countDown();
                return true;
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return DisposableHelper.isDisposed(s.get());
    }

    @Override
    public boolean isDone() {
        return getCount() == 0;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        if (getCount() != 0) {
            BlockingHelper.verifyNonBlocking();
            await();
        }

        if (isCancelled()) {
            throw new CancellationException();
        }
        Throwable ex = error;
        if (ex != null) {
            throw new ExecutionException(ex);
        }
        return value;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (getCount() != 0) {
            BlockingHelper.verifyNonBlocking();
            if (!await(timeout, unit)) {
                throw new TimeoutException();
            }
        }

        if (isCancelled()) {
            throw new CancellationException();
        }

        Throwable ex = error;
        if (ex != null) {
            throw new ExecutionException(ex);
        }
        return value;
    }

    @Override
    public void onSubscribe(Disposable s) {
        DisposableHelper.setOnce(this.s, s);
    }

    @Override
    public void onSuccess(T t) {
        Disposable a = s.get();
        if (a == DisposableHelper.DISPOSED) {
            return;
        }
        value = t;
        s.compareAndSet(a, this);
        countDown();
    }

    @Override
    public void onError(Throwable t) {
        for (;;) {
            Disposable a = s.get();
            if (a == DisposableHelper.DISPOSED) {
                RxJavaPlugins.onError(t);
                return;
            }
            error = t;
            if (s.compareAndSet(a, this)) {
                countDown();
                return;
            }
        }
    }

    @Override
    public void dispose() {
        // ignoring as `this` means a finished Disposable only
    }

    @Override
    public boolean isDisposed() {
        return isDone();
    }
}
