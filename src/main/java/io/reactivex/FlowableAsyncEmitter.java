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

import io.reactivex.annotations.Experimental;
import io.reactivex.functions.Cancellable;

/**
 * Provides an API on top of the {@link Emitter} signals that allow
 * setting and replacing a {@link Cancellable} resource to be cancelled
 * when the associated flow is cancelled.
 *
 * @param <T> the value type of the items emitted via {@link #onNext(Object)}.
 * @since 2.1.11 - experimental
 */
@Experimental
public interface FlowableAsyncEmitter<T> extends Emitter<T> {

    /**
     * Sets the current {@link Cancellable} resource to the provided one
     * and cancels the previous one if present.
     * <p>
     * If the underlying flow has been cancelled, the Cancellable
     * provided will be cancelled immediately before returning false.
     * @param c the new {@code Cancellable} to set
     * @return if true, the operation was successful, if false,
     *         the associated flow has been cancelled
     */
    boolean setCancellable(Cancellable c);

    /**
     * Sets the current {@link Cancellable} resource to the provided one.
     * <p>
     * If the underlying flow has been cancelled, the {@code Cancellable}
     * provided will be cancelled immediately before returning false.
     * <p>
     * Unlike {@link #setCancellable(Cancellable)}, the previous
     * {@code Cancellable} is not cancelled when returning false.
     * @param c the new {@code Cancellable} to set
     * @return if true, the operation was successful, if false,
     *         the associated flow has been cancelled
     */
    boolean replaceCancellable(Cancellable c);

    /**
     * Returns true if the associated flow has been cancelled.
     * @return true if the associated flow has been cancelled
     */
    boolean isCancelled();

    /**
     * The async logic may call this method to indicate the async
     * API invocation didn't produce any items but it hasn't ended
     * either, therefore, the generator can perform another
     * API invocation immediately.
     */
    void onNothing();
}
