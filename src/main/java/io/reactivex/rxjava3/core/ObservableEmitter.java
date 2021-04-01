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
 * Abstraction over an RxJava {@link Observer} that allows associating
 * a resource with it.
 * <p>
 * The {@link #onNext(Object)}, {@link #onError(Throwable)}, {@link #tryOnError(Throwable)}
 * and {@link #onComplete()} methods should be called in a sequential manner, just like the
 * {@link Observer}'s methods should be.
 * Use the {@code ObservableEmitter} the {@link #serialize()} method returns instead of the original
 * {@code ObservableEmitter} instance provided by the generator routine if you want to ensure this.
 * The other methods are thread-safe.
 * <p>
 * The emitter allows the registration of a single resource, in the form of a {@link Disposable}
 * or {@link Cancellable} via {@link #setDisposable(Disposable)} or {@link #setCancellable(Cancellable)}
 * respectively. The emitter implementations will dispose/cancel this instance when the
 * downstream cancels the flow or after the event generator logic calls {@link #onError(Throwable)},
 * {@link #onComplete()} or when {@link #tryOnError(Throwable)} succeeds.
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
public interface ObservableEmitter<@NonNull T> extends Emitter<T> {

    /**
     * Sets a {@link Disposable} on this emitter; any previous {@code Disposable}
     * or {@link Cancellable} will be disposed/cancelled.
     * <p>This method is thread-safe.
     * @param d the {@code Disposable}, {@code null} is allowed
     */
    void setDisposable(@Nullable Disposable d);

    /**
     * Sets a {@link Cancellable} on this emitter; any previous {@link Disposable}
     * or {@code Cancellable} will be disposed/cancelled.
     * <p>This method is thread-safe.
     * @param c the {@code Cancellable} resource, {@code null} is allowed
     */
    void setCancellable(@Nullable Cancellable c);

    /**
     * Returns true if the downstream disposed the sequence or the
     * emitter was terminated via {@link #onError(Throwable)}, {@link #onComplete} or a
     * successful {@link #tryOnError(Throwable)}.
     * <p>This method is thread-safe.
     * @return true if the downstream disposed the sequence or the emitter was terminated
     */
    boolean isDisposed();

    /**
     * Ensures that calls to {@code onNext}, {@code onError} and {@code onComplete} are properly serialized.
     * @return the serialized {@link ObservableEmitter}
     */
    @NonNull
    ObservableEmitter<T> serialize();

    /**
     * Attempts to emit the specified {@link Throwable} error if the downstream
     * hasn't cancelled the sequence or is otherwise terminated, returning false
     * if the emission is not allowed to happen due to lifecycle restrictions.
     * <p>
     * Unlike {@link #onError(Throwable)}, the {@link io.reactivex.rxjava3.plugins.RxJavaPlugins#onError(Throwable) RxjavaPlugins.onError}
     * is not called if the error could not be delivered.
     * <p>History: 2.1.1 - experimental
     * @param t the {@code Throwable} error to signal if possible
     * @return true if successful, false if the downstream is not able to accept further
     * events
     * @since 2.2
     */
    boolean tryOnError(@NonNull Throwable t);
}
