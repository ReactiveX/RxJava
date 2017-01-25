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

import java.util.concurrent.CountDownLatch;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.util.*;

public abstract class BlockingBaseObserver<T> extends CountDownLatch
implements Observer<T>, Disposable {

    T value;
    Throwable error;

    Disposable d;

    volatile boolean cancelled;

    public BlockingBaseObserver() {
        super(1);
    }

    @Override
    public final void onSubscribe(Disposable d) {
        this.d = d;
        if (cancelled) {
            d.dispose();
        }
    }

    @Override
    public final void onComplete() {
        countDown();
    }

    @Override
    public final void dispose() {
        cancelled = true;
        Disposable d = this.d;
        if (d != null) {
            d.dispose();
        }
    }

    @Override
    public final boolean isDisposed() {
        return cancelled;
    }

    /**
     * Block until the first value arrives and return it, otherwise
     * return null for an empty source and rethrow any exception.
     * @return the first value or null if the source is empty
     */
    public final T blockingGet() {
        if (getCount() != 0) {
            try {
                BlockingHelper.verifyNonBlocking();
                await();
            } catch (InterruptedException ex) {
                dispose();
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }

        Throwable e = error;
        if (e != null) {
            throw ExceptionHelper.wrapOrThrow(e);
        }
        return value;
    }
}
