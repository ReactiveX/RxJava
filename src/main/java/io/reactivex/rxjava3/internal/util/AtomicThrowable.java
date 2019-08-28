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

package io.reactivex.rxjava3.internal.util;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Atomic container for Throwables including combining and having a
 * terminal state via ExceptionHelper.
 * <p>
 * Watch out for the leaked AtomicReference methods!
 */
public final class AtomicThrowable extends AtomicReference<Throwable> {

    private static final long serialVersionUID = 3949248817947090603L;

    /**
     * Atomically adds a Throwable to this container (combining with a previous Throwable is necessary).
     * @param t the throwable to add
     * @return true if successful, false if the container has been terminated
     */
    public boolean tryAddThrowable(Throwable t) {
        return ExceptionHelper.addThrowable(this, t);
    }

    /**
     * Atomically adds a Throwable to this container (combining with a previous Throwable is necessary)
     * or reports the error the global error handler and no changes are made.
     * @param t the throwable to add
     * @return true if successful, false if the container has been terminated
     */
    public boolean tryAddThrowableOrReport(Throwable t) {
        if (tryAddThrowable(t)) {
            return true;
        }
        RxJavaPlugins.onError(t);
        return false;
    }

    /**
     * Atomically terminate the container and return the contents of the last
     * non-terminal Throwable of it.
     * @return the last Throwable
     */
    public Throwable terminate() {
        return ExceptionHelper.terminate(this);
    }

    public boolean isTerminated() {
        return get() == ExceptionHelper.TERMINATED;
    }

    /**
     * Tries to terminate this atomic throwable (by swapping in the TERMINATED indicator)
     * and calls {@link RxJavaPlugins#onError(Throwable)} if there was a non-null, non-indicator
     * exception contained within before.
     * @since 3.0.0
     */
    public void tryTerminateAndReport() {
        Throwable ex = terminate();
        if (ex != null && ex != ExceptionHelper.TERMINATED) {
            RxJavaPlugins.onError(ex);
        }
    }

    /**
     * Tries to terminate this atomic throwable (by swapping in the TERMINATED indicator)
     * and notifies the consumer if there was no error (onComplete) or there was a
     * non-null, non-indicator exception contained before (onError).
     * If there was a terminated indicator, the consumer is not signaled.
     * @param consumer the consumer to notify
     */
    public void tryTerminateConsumer(Subscriber<?> consumer) {
        Throwable ex = terminate();
        if (ex == null) {
            consumer.onComplete();
        } else if (ex != ExceptionHelper.TERMINATED) {
            consumer.onError(ex);
        }
    }

    /**
     * Tries to terminate this atomic throwable (by swapping in the TERMINATED indicator)
     * and notifies the consumer if there was no error (onComplete) or there was a
     * non-null, non-indicator exception contained before (onError).
     * If there was a terminated indicator, the consumer is not signaled.
     * @param consumer the consumer to notify
     */
    public void tryTerminateConsumer(Observer<?> consumer) {
        Throwable ex = terminate();
        if (ex == null) {
            consumer.onComplete();
        } else if (ex != ExceptionHelper.TERMINATED) {
            consumer.onError(ex);
        }
    }

    /**
     * Tries to terminate this atomic throwable (by swapping in the TERMINATED indicator)
     * and notifies the consumer if there was no error (onComplete) or there was a
     * non-null, non-indicator exception contained before (onError).
     * If there was a terminated indicator, the consumer is not signaled.
     * @param consumer the consumer to notify
     */
    public void tryTerminateConsumer(MaybeObserver<?> consumer) {
        Throwable ex = terminate();
        if (ex == null) {
            consumer.onComplete();
        } else if (ex != ExceptionHelper.TERMINATED) {
            consumer.onError(ex);
        }
    }

    /**
     * Tries to terminate this atomic throwable (by swapping in the TERMINATED indicator)
     * and notifies the consumer if there was no error (onComplete) or there was a
     * non-null, non-indicator exception contained before (onError).
     * If there was a terminated indicator, the consumer is not signaled.
     * @param consumer the consumer to notify
     */
    public void tryTerminateConsumer(SingleObserver<?> consumer) {
        Throwable ex = terminate();
        if (ex != null && ex != ExceptionHelper.TERMINATED) {
            consumer.onError(ex);
        }
    }

    /**
     * Tries to terminate this atomic throwable (by swapping in the TERMINATED indicator)
     * and notifies the consumer if there was no error (onComplete) or there was a
     * non-null, non-indicator exception contained before (onError).
     * If there was a terminated indicator, the consumer is not signaled.
     * @param consumer the consumer to notify
     */
    public void tryTerminateConsumer(CompletableObserver consumer) {
        Throwable ex = terminate();
        if (ex == null) {
            consumer.onComplete();
        } else if (ex != ExceptionHelper.TERMINATED) {
            consumer.onError(ex);
        }
    }

    /**
     * Tries to terminate this atomic throwable (by swapping in the TERMINATED indicator)
     * and notifies the consumer if there was no error (onComplete) or there was a
     * non-null, non-indicator exception contained before (onError).
     * If there was a terminated indicator, the consumer is not signaled.
     * @param consumer the consumer to notify
     */
    public void tryTerminateConsumer(Emitter<?> consumer) {
        Throwable ex = terminate();
        if (ex == null) {
            consumer.onComplete();
        } else if (ex != ExceptionHelper.TERMINATED) {
            consumer.onError(ex);
        }
    }
}
