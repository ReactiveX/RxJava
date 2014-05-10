/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.util.async.operators;

import java.util.concurrent.Future;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Func0;

/**
 * Start an asynchronous Future immediately and observe its result through
 * an observable.
 */
public final class OperatorStartFuture {
    /** Utility class. */
    private OperatorStartFuture() { throw new IllegalStateException("No instances!"); }
    /**
     * Invokes the asynchronous function, surfacing the result through an observable sequence.
     * <p>
     * <em>Important note</em> subscribing to the resulting observable blocks until
     * the future completes.
     * @param <T> the result type
     * @param functionAsync the asynchronous function to run
     * @return the observable
     */
    public static <T> Observable<T> startFuture(Func0<? extends Future<? extends T>> functionAsync) {
        Future<? extends T> task;
        try {
            task = functionAsync.call();
        } catch (Throwable t) {
            return Observable.error(t);
        }
        return Observable.from(task);
    }
    /**
     * Invokes the asynchronous function, surfacing the result through an observable sequence
     * running on the given scheduler.
     * @param <T> the result type
     * @param functionAsync the asynchronous function to run
     * @param scheduler the scheduler where the completion of the Future is awaited
     * @return the observable
     */
    public static <T> Observable<T> startFuture(Func0<? extends Future<? extends T>> functionAsync,
            Scheduler scheduler) {
        Future<? extends T> task;
        try {
            task = functionAsync.call();
        } catch (Throwable t) {
            return Observable.error(t);
        }
        return Observable.from(task, scheduler);
    }
}
