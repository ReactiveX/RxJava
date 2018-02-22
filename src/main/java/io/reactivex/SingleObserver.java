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
 * Provides a mechanism for receiving push-based notification of a single value or an error.
 * <p>
 * When a {@code SingleObserver} is subscribed to a {@link SingleSource} through the {@link SingleSource#subscribe(SingleObserver)} method,
 * the {@code SingleSource} calls {@link #onSubscribe(Disposable)}  with a {@link Disposable} that allows
 * disposing the sequence at any time. A well-behaved
 * {@code SingleSource} will call a {@code SingleObserver}'s {@link #onSuccess(Object)} method exactly once or the {@code SingleObserver}'s
 * {@link #onError} method exactly once as they are considered mutually exclusive <strong>terminal signals</strong>.
 * <p>
 * Calling the {@code SingleObserver}'s method must happen in a serialized fashion, that is, they must not
 * be invoked concurrently by multiple threads in an overlapping fashion and the invocation pattern must
 * adhere to the following protocol:
 * <pre><code>    onSubscribe (onSuccess | onError)?</code></pre>
 * <p>
 * Subscribing a {@code SingleObserver} to multiple {@code SingleSource}s is not recommended. If such reuse
 * happens, it is the duty of the {@code SingleObserver} implementation to be ready to receive multiple calls to
 * its methods and ensure proper concurrent behavior of its business logic.
 * <p>
 * Calling {@link #onSubscribe(Disposable)}, {@link #onSuccess(Object)} or {@link #onError(Throwable)} with a
 * {@code null} argument is forbidden.
 * <p>
 * The implementations of the {@code onXXX} methods should avoid throwing runtime exceptions other than the following cases:
 * <ul>
 * <li>If the argument is {@code null}, the methods can throw a {@code NullPointerException}.
 * Note though that RxJava prevents {@code null}s to enter into the flow and thus there is generally no
 * need to check for nulls in flows assembled from standard sources and intermediate operators.
 * </li>
 * <li>If there is a fatal error (such as {@code VirtualMachineError}).</li>
 * </ul>
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
