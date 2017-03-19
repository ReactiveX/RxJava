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

/**
 * Provides a mechanism for receiving push-based notifications.
 * <p>
 * After a SingleObserver calls a {@link Single}'s {@link Single#subscribe subscribe} method,
 * first the Single calls {@link #onSubscribe(Disposable)} with a {@link Disposable} that allows
 * cancelling the sequence at any time, then the
 * {@code Single} calls only one of the SingleObserver {@link #onSuccess} and {@link #onError} methods to provide
 * notifications.
 *
 * @see <a href="http://reactivex.io/documentation/observable.html">ReactiveX documentation: Observable</a>
 * @param <T>
 *          the type of item the SingleObserver expects to observe
 * @since 2.0
 */
public interface SingleObserver<T> {

    /**
     * Provides the SingleObserver with the means of cancelling (disposing) the
     * connection (channel) with the Single in both
     * synchronous (from within {@code onSubscribe(Disposable)} itself) and asynchronous manner.
     * @param d the Disposable instance whose {@link Disposable#dispose()} can
     * be called anytime to cancel the connection
     * @since 2.0
     */
    void onSubscribe(@NonNull Disposable d);

    /**
     * Notifies the SingleObserver with a single item and that the {@link Single} has finished sending
     * push-based notifications.
     * <p>
     * The {@link Single} will not call this method if it calls {@link #onError}.
     *
     * @param t
     *          the item emitted by the Single
     */
    void onSuccess(@NonNull T t);

    /**
     * Notifies the SingleObserver that the {@link Single} has experienced an error condition.
     * <p>
     * If the {@link Single} calls this method, it will not thereafter call {@link #onSuccess}.
     *
     * @param e
     *          the exception encountered by the Single
     */
    void onError(@NonNull Throwable e);
}
