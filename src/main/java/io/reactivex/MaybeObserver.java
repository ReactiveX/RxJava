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
 * Provides a mechanism for receiving push-based notification of a single value, an error or completion without any value.
 * <p>
 * When a {@code MaybeObserver} is subscribed to a {@link MaybeSource} through the {@link MaybeSource#subscribe(MaybeObserver)} method,
 * the {@code MaybeSource} calls {@link #onSubscribe(Disposable)}  with a {@link Disposable} that allows
 * disposing the sequence at any time. A well-behaved
 * {@code MaybeSource} will call a {@code MaybeObserver}'s {@link #onSuccess(Object)}, {@link #onError(Throwable)}
 * or {@link #onComplete()} method exactly once as they are considered mutually exclusive <strong>terminal signals</strong>.
 * <p>
 * Calling the {@code MaybeObserver}'s method must happen in a serialized fashion, that is, they must not
 * be invoked concurrently by multiple threads in an overlapping fashion and the invocation pattern must
 * adhere to the following protocol:
 * <pre><code>    onSubscribe (onSuccess | onError | onComplete)?</code></pre>
 * <p>
 * Note that unlike with the {@code Observable} protocol, {@link #onComplete()} is not called after the success item has been
 * signalled via {@link #onSuccess(Object)}.
 * <p>
 * Subscribing a {@code MaybeObserver} to multiple {@code MaybeSource}s is not recommended. If such reuse
 * happens, it is the duty of the {@code MaybeObserver} implementation to be ready to receive multiple calls to
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
