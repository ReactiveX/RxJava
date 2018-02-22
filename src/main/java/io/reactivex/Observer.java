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
 * Provides a mechanism for receiving push-based notifications.
 * <p>
 * When an {@code Observer} is subscribed to an {@link ObservableSource} through the {@link ObservableSource#subscribe(Observer)} method,
 * the {@code ObservableSource} calls {@link #onSubscribe(Disposable)}  with a {@link Disposable} that allows
 * disposing the sequence at any time, then the
 * {@code ObservableSource} may call the Observer's {@link #onNext} method any number of times
 * to provide notifications. A well-behaved
 * {@code ObservableSource} will call an {@code Observer}'s {@link #onComplete} method exactly once or the {@code Observer}'s
 * {@link #onError} method exactly once.
 * <p>
 * Calling the {@code Observer}'s method must happen in a serialized fashion, that is, they must not
 * be invoked concurrently by multiple threads in an overlapping fashion and the invocation pattern must
 * adhere to the following protocol:
 * <pre><code>    onSubscribe onNext* (onError | onComplete)?</code></pre>
 * <p>
 * Subscribing an {@code Observer} to multiple {@code ObservableSource}s is not recommended. If such reuse
 * happens, it is the duty of the {@code Observer} implementation to be ready to receive multiple calls to
 * its methods and ensure proper concurrent behavior of its business logic.
 * <p>
 * Calling {@link #onSubscribe(Disposable)}, {@link #onNext(Object)} or {@link #onError(Throwable)} with a
 * {@code null} argument is forbidden.
 * <p>
 * The implementations of the {@code onXXX} methods should avoid throwing runtime exceptions other than the following cases
 * (see <a href="https://github.com/reactive-streams/reactive-streams-jvm#2.13">Rule 2.13</a> of the Reactive Streams specification):
 * <ul>
 * <li>If the argument is {@code null}, the methods can throw a {@code NullPointerException}.
 * Note though that RxJava prevents {@code null}s to enter into the flow and thus there is generally no
 * need to check for nulls in flows assembled from standard sources and intermediate operators.
 * </li>
 * <li>If there is a fatal error (such as {@code VirtualMachineError}).</li>
 * </ul>
 * <p>
 * Violating Rule 2.13 results in undefined flow behavior. Generally, the following can happen:
 * <ul>
 * <li>An upstream operator turns it into an {@link #onError} call.</li>
 * <li>If the flow is synchronous, the {@link ObservableSource#subscribe(Observer)} throws instead of returning normally.</li>
 * <li>If the flow is asynchronous, the exception propagates up to the component ({@link Scheduler} or {@link java.util.concurrent.Executor})
 * providing the asynchronous boundary the code is running and either routes the exception to the global
 * {@link io.reactivex.plugins.RxJavaPlugins#onError(Throwable)} handler or the current thread's
 * {@link java.lang.Thread.UncaughtExceptionHandler#uncaughtException(Thread, Throwable)} handler.</li>
 * </ul>
 * From the {@code Observable}'s perspective, an {@code Observer} is the end consumer thus it is the {@code Observer}'s
 * responsibility to handle the error case and signal it "further down". This means unreliable code in the {@code onXXX}
 * methods should be wrapped into `try-catch`es, specifically in {@link #onError(Throwable)} or {@link #onComplete()}, and handled there
 * (for example, by logging it or presenting the user with an error dialog). However, if the error would be thrown from
 * {@link #onNext(Object)}, <a href="https://github.com/reactive-streams/reactive-streams-jvm#2.13">Rule 2.13</a> mandates
 * the implementation calls {@link Disposable#dispose()} and signals the exception in a way that is adequate to the target context,
 * for example, by calling {@link #onError(Throwable)} on the same {@code Observer} instance.
 * <p>
 * If, for some reason, the {@code Observer} won't follow Rule 2.13, the {@link Observable#safeSubscribe(Observer)} can wrap it
 * with the necessary safeguards and route exceptions thrown from {@code onNext} into {@code onError} and route exceptions thrown
 * from {@code onError} and {@code onComplete} into the global error handler via {@link io.reactivex.plugins.RxJavaPlugins#onError(Throwable)}.
 * @see <a href="http://reactivex.io/documentation/observable.html">ReactiveX documentation: Observable</a>
 * @param <T>
 *          the type of item the Observer expects to observe
 */
public interface Observer<T> {

    /**
     * Provides the Observer with the means of cancelling (disposing) the
     * connection (channel) with the Observable in both
     * synchronous (from within {@link #onNext(Object)}) and asynchronous manner.
     * @param d the Disposable instance whose {@link Disposable#dispose()} can
     * be called anytime to cancel the connection
     * @since 2.0
     */
    void onSubscribe(@NonNull Disposable d);

    /**
     * Provides the Observer with a new item to observe.
     * <p>
     * The {@link Observable} may call this method 0 or more times.
     * <p>
     * The {@code Observable} will not call this method again after it calls either {@link #onComplete} or
     * {@link #onError}.
     *
     * @param t
     *          the item emitted by the Observable
     */
    void onNext(@NonNull T t);

    /**
     * Notifies the Observer that the {@link Observable} has experienced an error condition.
     * <p>
     * If the {@link Observable} calls this method, it will not thereafter call {@link #onNext} or
     * {@link #onComplete}.
     *
     * @param e
     *          the exception encountered by the Observable
     */
    void onError(@NonNull Throwable e);

    /**
     * Notifies the Observer that the {@link Observable} has finished sending push-based notifications.
     * <p>
     * The {@link Observable} will not call this method if it calls {@link #onError}.
     */
    void onComplete();

}
