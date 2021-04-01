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

package io.reactivex.rxjava3.core;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.*;

/**
 * Abstraction over an RxJava {@link SingleObserver} that allows associating
 * a resource with it.
 * <p>
 * All methods are safe to call from multiple threads, but note that there is no guarantee
 * whose terminal event will win and get delivered to the downstream.
 * <p>
 * Calling {@link #onSuccess(Object)} multiple times has no effect.
 * Calling {@link #onError(Throwable)} multiple times or after {@code onSuccess} will route the
 * exception into the global error handler via {@link io.reactivex.rxjava3.plugins.RxJavaPlugins#onError(Throwable)}.
 * <p>
 * The emitter allows the registration of a single resource, in the form of a {@link Disposable}
 * or {@link Cancellable} via {@link #setDisposable(Disposable)} or {@link #setCancellable(Cancellable)}
 * respectively. The emitter implementations will dispose/cancel this instance when the
 * downstream cancels the flow or after the event generator logic calls {@link #onSuccess(Object)},
 * {@link #onError(Throwable)}, or when {@link #tryOnError(Throwable)} succeeds.
 * <p>
 * Only one {@code Disposable} or {@code Cancellable} object can be associated with the emitter at
 * a time. Calling either {@code set} method will dispose/cancel any previous object. If there
 * is a need for handling multiple resources, one can create a {@link io.reactivex.rxjava3.disposables.CompositeDisposable}
 * and associate that with the emitter instead.
 * <p>
 * The {@link Cancellable} is logically equivalent to {@code Disposable} but allows using cleanup logic that can
 * throw a checked exception (such as many {@code close()} methods on Java IO components). Since
 * the release of resources happens after the terminal events have been delivered or the sequence gets
 * cancelled, exceptions throw within {@code Cancellable} are routed to the global error handler via
 * {@link io.reactivex.rxjava3.plugins.RxJavaPlugins#onError(Throwable)}.
 *
 * @param <T> the value type to emit
 */
public interface SingleEmitter<@NonNull T> {

    /**
     * Signal a success value.
     * @param t the value, not null
     */
    void onSuccess(@NonNull T t);

    /**
     * Signal an exception.
     * @param t the exception, not {@code null}
     */
    void onError(@NonNull Throwable t);

    /**
     * Sets a {@link Disposable} on this emitter; any previous {@code Disposable}
     * or {@link Cancellable} will be disposed/cancelled.
     * <p>This method is thread-safe.
     * @param d the {@code Disposable}, {@code null} is allowed
     */
    void setDisposable(@Nullable Disposable d);

    /**
     * Sets a Cancellable on this emitter; any previous {@link Disposable}
     * or {@link Cancellable} will be disposed/cancelled.
     * <p>This method is thread-safe.
     * @param c the {@code Cancellable} resource, {@code null} is allowed
     */
    void setCancellable(@Nullable Cancellable c);

    /**
     * Returns true if the downstream disposed the sequence or the
     * emitter was terminated via {@link #onSuccess(Object)}, {@link #onError(Throwable)},
     * or a successful {@link #tryOnError(Throwable)}.
     * <p>This method is thread-safe.
     * @return true if the downstream disposed the sequence or the emitter was terminated
     */
    boolean isDisposed();

    /**
     * Attempts to emit the specified {@link Throwable} error if the downstream
     * hasn't cancelled the sequence or is otherwise terminated, returning false
     * if the emission is not allowed to happen due to lifecycle restrictions.
     * <p>
     * Unlike {@link #onError(Throwable)}, the {@link io.reactivex.rxjava3.plugins.RxJavaPlugins#onError(Throwable) RxjavaPlugins.onError}
     * is not called if the error could not be delivered.
     * <p>History: 2.1.1 - experimental
     * @param t the throwable error to signal if possible
     * @return true if successful, false if the downstream is not able to accept further
     * events
     * @since 2.2
     */
    boolean tryOnError(@NonNull Throwable t);
}
