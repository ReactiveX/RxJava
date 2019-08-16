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

package io.reactivex.rxjava3.internal.observers;

import java.util.concurrent.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.util.*;

/**
 * A combined Observer that awaits the success or error signal via a CountDownLatch.
 * @param <T> the value type
 */
public final class BlockingMultiObserver<T>
extends CountDownLatch
implements SingleObserver<T>, CompletableObserver, MaybeObserver<T> {

    T value;
    Throwable error;

    Disposable upstream;

    volatile boolean cancelled;

    public BlockingMultiObserver() {
        super(1);
    }

    void dispose() {
        cancelled = true;
        Disposable d = this.upstream;
        if (d != null) {
            d.dispose();
        }
    }

    @Override
    public void onSubscribe(Disposable d) {
        this.upstream = d;
        if (cancelled) {
            d.dispose();
        }
    }

    @Override
    public void onSuccess(T value) {
        this.value = value;
        countDown();
    }

    @Override
    public void onError(Throwable e) {
        error = e;
        countDown();
    }

    @Override
    public void onComplete() {
        countDown();
    }

    /**
     * Block until the latch is counted down then rethrow any exception received (wrapped if checked)
     * or return the received value (null if none).
     * @return the value received or null if no value received
     */
    public T blockingGet() {
        if (getCount() != 0) {
            try {
                BlockingHelper.verifyNonBlocking();
                await();
            } catch (InterruptedException ex) {
                dispose();
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }
        Throwable ex = error;
        if (ex != null) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
        return value;
    }

    /**
     * Block until the latch is counted down then rethrow any exception received (wrapped if checked)
     * or return the received value (the defaultValue if none).
     * @param defaultValue the default value to return if no value was received
     * @return the value received or defaultValue if no value received
     */
    public T blockingGet(T defaultValue) {
        if (getCount() != 0) {
            try {
                BlockingHelper.verifyNonBlocking();
                await();
            } catch (InterruptedException ex) {
                dispose();
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }
        Throwable ex = error;
        if (ex != null) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
        T v = value;
        return v != null ? v : defaultValue;
    }

    /**
     * Block until the observer terminates and return true; return false if
     * the wait times out.
     * @param timeout the timeout value
     * @param unit the time unit
     * @return true if the observer terminated in time, false otherwise
     */
    public boolean blockingAwait(long timeout, TimeUnit unit) {
        if (getCount() != 0) {
            try {
                BlockingHelper.verifyNonBlocking();
                if (!await(timeout, unit)) {
                    dispose();
                    return false;
                }
            } catch (InterruptedException ex) {
                dispose();
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }
        Throwable ex = error;
        if (ex != null) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
        return true;
    }
}
