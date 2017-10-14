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
 * After a MaybeObserver calls a {@link Maybe}'s {@link Maybe#subscribe subscribe} method,
 * first the Maybe calls {@link #onSubscribe(Disposable)} with a {@link Disposable} that allows
 * cancelling the sequence at any time, then the
 * {@code Maybe} calls only one of the MaybeObserver's {@link #onSuccess}, {@link #onError} or
 * {@link #onComplete} methods to provide notifications.
 *
 * @see <a href="http://reactivex.io/documentation/observable.html">ReactiveX documentation: Observable</a>
 * @param <T>
 *          the type of item the MaybeObserver expects to observe
 * @since 2.0
 */
public interface MaybeObserver<T> {

    /**
     * Provides the MaybeObserver with the means of cancelling (disposing) the
     * connection (channel) with the Maybe in both
     * synchronous (from within {@code onSubscribe(Disposable)} itself) and asynchronous manner.
     * @param d the Disposable instance whose {@link Disposable#dispose()} can
     * be called anytime to cancel the connection
     */
    void onSubscribe(@NonNull Disposable d);

    /**
     * Notifies the MaybeObserver with one item and that the {@link Maybe} has finished sending
     * push-based notifications.
     * <p>
     * The {@link Maybe} will not call this method if it calls {@link #onError}.
     *
     * @param t
     *          the item emitted by the Maybe
     */
    void onSuccess(@NonNull T t);

    /**
     * Notifies the MaybeObserver that the {@link Maybe} has experienced an error condition.
     * <p>
     * If the {@link Maybe} calls this method, it will not thereafter call {@link #onSuccess}.
     *
     * @param e
     *          the exception encountered by the Maybe
     */
    void onError(@NonNull Throwable e);

    /**
     * Called once the deferred computation completes normally.
     */
    void onComplete();
}
