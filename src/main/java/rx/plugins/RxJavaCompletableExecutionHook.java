/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.plugins;

import rx.*;
import rx.annotations.Experimental;
import rx.functions.Func1;

/**
 * Abstract ExecutionHook with invocations at different lifecycle points of {@link Completable} execution with a
 * default no-op implementation.
 * <p>
 * See {@link RxJavaPlugins} or the RxJava GitHub Wiki for information on configuring plugins:
 * <a href="https://github.com/ReactiveX/RxJava/wiki/Plugins">https://github.com/ReactiveX/RxJava/wiki/Plugins</a>.
 * <p>
 * <b>Note on thread-safety and performance:</b>
 * <p>
 * A single implementation of this class will be used globally so methods on this class will be invoked
 * concurrently from multiple threads so all functionality must be thread-safe.
 * <p>
 * Methods are also invoked synchronously and will add to execution time of the completable so all behavior
 * should be fast. If anything time-consuming is to be done it should be spawned asynchronously onto separate
 * worker threads.
 *
 * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
 */
@Experimental
public abstract class RxJavaCompletableExecutionHook {
    /**
     * Invoked during the construction by {@link Completable#create(Completable.CompletableOnSubscribe)}
     * <p>
     * This can be used to decorate or replace the <code>onSubscribe</code> function or just perform extra
     * logging, metrics and other such things and pass through the function.
     *
     * @param f
     *            original {@link rx.Completable.CompletableOnSubscribe}<{@code T}> to be executed
     * @return {@link rx.Completable.CompletableOnSubscribe} function that can be modified, decorated, replaced or just
     *         returned as a pass through
     */
    public Completable.CompletableOnSubscribe onCreate(Completable.CompletableOnSubscribe f) {
        return f;
    }

    /**
     * Invoked before {@link Completable#subscribe(Subscriber)} is about to be executed.
     * <p>
     * This can be used to decorate or replace the <code>onSubscribe</code> function or just perform extra
     * logging, metrics and other such things and pass through the function.
     *
     * @param completableInstance the target completable instance
     * @param onSubscribe
     *            original {@link rx.Completable.CompletableOnSubscribe}<{@code T}> to be executed
     * @return {@link rx.Completable.CompletableOnSubscribe}<{@code T}> function that can be modified, decorated, replaced or just
     *         returned as a pass through
     */
    public Completable.CompletableOnSubscribe onSubscribeStart(Completable completableInstance, final Completable.CompletableOnSubscribe onSubscribe) {
        // pass through by default
        return onSubscribe;
    }

    /**
     * Invoked after failed execution of {@link Completable#subscribe(Subscriber)} with thrown Throwable.
     * <p>
     * This is <em>not</em> errors emitted via {@link Subscriber#onError(Throwable)} but exceptions thrown when
     * attempting to subscribe to a {@link Func1}<{@link Subscriber}{@code <T>}, {@link Subscription}>.
     *
     * @param e
     *            Throwable thrown by {@link Completable#subscribe(Subscriber)}
     * @return Throwable that can be decorated, replaced or just returned as a pass through
     */
    public Throwable onSubscribeError(Throwable e) {
        // pass through by default
        return e;
    }

    /**
     * Invoked just as the operator functions is called to bind two operations together into a new
     * {@link Completable} and the return value is used as the lifted function
     * <p>
     * This can be used to decorate or replace the {@link rx.Completable.CompletableOperator} instance or just perform extra
     * logging, metrics and other such things and pass through the onSubscribe.
     *
     * @param lift
     *            original {@link rx.Completable.CompletableOperator}{@code <R, T>}
     * @return {@link rx.Completable.CompletableOperator}{@code <R, T>} function that can be modified, decorated, replaced or just
     *         returned as a pass through
     */
    public Completable.CompletableOperator onLift(final Completable.CompletableOperator lift) {
        return lift;
    }
}
