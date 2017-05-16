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

package io.reactivex;

import io.reactivex.annotations.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;

/**
 * Abstraction over an RxJava {@link MaybeObserver} that allows associating
 * a resource with it.
 * <p>
 * All methods are safe to call from multiple threads.
 * <p>
 * Calling onSuccess, onError or onComplete multiple times has no effect.
 *
 * @param <T> the value type to emit
 */
public interface MaybeEmitter<T> {

    /**
     * Signal a success value.
     * @param t the value, not null
     */
    void onSuccess(@NonNull T t);

    /**
     * Signal an exception.
     * @param t the exception, not null
     */
    void onError(@NonNull Throwable t);

    /**
     * Signal the completion.
     */
    void onComplete();

    /**
     * Sets a Disposable on this emitter; any previous Disposable
     * or Cancellation will be unsubscribed/cancelled.
     * @param s the disposable, null is allowed
     */
    void setDisposable(@Nullable Disposable s);

    /**
     * Sets a Cancellable on this emitter; any previous Disposable
     * or Cancellation will be unsubscribed/cancelled.
     * @param c the cancellable resource, null is allowed
     */
    void setCancellable(@Nullable Cancellable c);

    /**
     * Returns true if the downstream cancelled the sequence.
     * @return true if the downstream cancelled the sequence
     */
    boolean isDisposed();

    /**
     * Attempts to emit the specified {@code Throwable} error if the downstream
     * hasn't cancelled the sequence or is otherwise terminated, returning false
     * if the emission is not allowed to happen due to lifecycle restrictions.
     * <p>
     * Unlike {@link #onError(Throwable)}, the {@code RxJavaPlugins.onError} is not called
     * if the error could not be delivered.
     * @param t the throwable error to signal if possible
     * @return true if successful, false if the downstream is not able to accept further
     * events
     * @since 2.1.1 - experimental
     */
    @Experimental
    boolean tryOnError(@NonNull Throwable t);
}
