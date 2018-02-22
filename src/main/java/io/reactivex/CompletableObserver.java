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

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

/**
 * Provides a mechanism for receiving push-based notification of a valueless completion or an error.
 * <p>
 * When a {@code CompletableObserver} is subscribed to a {@link CompletableSource} through the {@link CompletableSource#subscribe(CompletableObserver)} method,
 * the {@code CompletableSource} calls {@link #onSubscribe(Disposable)}  with a {@link Disposable} that allows
 * disposing the sequence at any time. A well-behaved
 * {@code CompletableSource} will call a {@code CompletableObserver}'s {@link #onError(Throwable)}
 * or {@link #onComplete()} method exactly once as they are considered mutually exclusive <strong>terminal signals</strong>.
 * <p>
 * Calling the {@code CompletableObserver}'s method must happen in a serialized fashion, that is, they must not
 * be invoked concurrently by multiple threads in an overlapping fashion and the invocation pattern must
 * adhere to the following protocol:
 * <pre><code>    onSubscribe (onError | onComplete)?</code></pre>
 * <p>
 * Subscribing a {@code CompletableObserver} to multiple {@code CompletableSource}s is not recommended. If such reuse
 * happens, it is the duty of the {@code CompletableObserver} implementation to be ready to receive multiple calls to
 * its methods and ensure proper concurrent behavior of its business logic.
 * <p>
 * Calling {@link #onSubscribe(Disposable)} or {@link #onError(Throwable)} with a
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
 * @since 2.0
 */
public interface CompletableObserver {
    /**
     * Called once by the Completable to set a Disposable on this instance which
     * then can be used to cancel the subscription at any time.
     * @param d the Disposable instance to call dispose on for cancellation, not null
     */
    void onSubscribe(@NonNull Disposable d);

    /**
     * Called once the deferred computation completes normally.
     */
    void onComplete();

    /**
     * Called once if the deferred computation 'throws' an exception.
     * @param e the exception, not null.
     */
    void onError(@NonNull Throwable e);
}
