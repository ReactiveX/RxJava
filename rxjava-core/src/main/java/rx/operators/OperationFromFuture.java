/**
 * Copyright 2013 Netflix, Inc.
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
package rx.operators;

import java.util.concurrent.Future;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Scheduler;
import rx.subscriptions.BooleanSubscription;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

/**
 * Converts to asynchronous function into an observable sequence where 
 * each subscription to the resulting sequence causes the function to be started.
 */
public final class OperationFromFuture {
    /** Utility class. */
    private OperationFromFuture() { throw new IllegalStateException("No instances!"); }
    /**
     * Converts to asynchronous function into an observable sequence where 
     * each subscription to the resulting sequence causes the function to be started.
     */
    public static <T> OnSubscribeFunc<T> fromFuture(Func0<? extends Future<? extends T>> functionAsync) {
        return OperationDefer.defer(new FromFuture<T>(functionAsync));
    }
    /**
     * Converts to asynchronous function into an observable sequence where 
     * each subscription to the resulting sequence causes the function to be started
     * and run on the given scheduler.
     */
    public static <T> OnSubscribeFunc<T> fromFuture(Func0<? extends Future<? extends T>> functionAsync, Scheduler scheduler) {
        return OperationDefer.defer(new FromFutureScheduled<T>(functionAsync, scheduler));
    }
    /**
     * Converts to asynchronous function into an observable sequence
     * where each subscription to the resulting sequence causes the function to be started and
     * the passed-in BooleanSubscription is tied to the Observable sequence's subscription.
     */
    public static <T> OnSubscribeFunc<T> fromFuture(Func1<? super BooleanSubscription, ? extends Future<? extends T>> functionAsync) {
        return OperationDefer.defer(new FromFutureToken<T>(functionAsync));
    }
    
    /**
     * Converts to asynchronous function into an observable sequence
     * where each subscription to the resulting sequence causes the function to be started and
     * run on the given scheduler, and
     * the passed-in BooleanSubscription is tied to the Observable sequence's subscription.
     */
    public static <T> OnSubscribeFunc<T> fromFuture(
            Func1<? super BooleanSubscription, ? extends Future<? extends T>> functionAsync,
            Scheduler scheduler) {
        return OperationDefer.defer(new FromFutureTokenScheduled<T>(functionAsync, scheduler));
    }
    
    /** From a future started by OperationStartFuture. */
    private static final class FromFuture<T> implements Func0<Observable<T>> {
        final Func0<? extends Future<? extends T>> functionAsync;

        public FromFuture(Func0<? extends Future<? extends T>> functionAsync) {
            this.functionAsync = functionAsync;
        }

        @Override
        public Observable<T> call() {
            return OperationStartFuture.startFuture(functionAsync);
        }
    }
    /** From a future started by OperationStartFuture. */
    private static final class FromFutureScheduled<T> implements Func0<Observable<T>> {
        final Func0<? extends Future<? extends T>> functionAsync;
        final Scheduler scheduler;

        public FromFutureScheduled(Func0<? extends Future<? extends T>> functionAsync, Scheduler scheduler) {
            this.functionAsync = functionAsync;
            this.scheduler = scheduler;
        }

        @Override
        public Observable<T> call() {
            return OperationStartFuture.startFuture(functionAsync, scheduler);
        }
    }
    
    /** From a future started by OperationStartFuture. */
    private static final class FromFutureToken<T> implements Func0<Observable<T>> {
        final Func1<? super BooleanSubscription, ? extends Future<? extends T>> functionAsync;

        public FromFutureToken(Func1<? super BooleanSubscription, ? extends Future<? extends T>> functionAsync) {
            this.functionAsync = functionAsync;
        }

        @Override
        public Observable<T> call() {
            return OperationStartFuture.startFuture(functionAsync);
        }
    }
    /** From a future started by OperationStartFuture. */
    private static final class FromFutureTokenScheduled<T> implements Func0<Observable<T>> {
        final Func1<? super BooleanSubscription, ? extends Future<? extends T>> functionAsync;
        final Scheduler scheduler;

        public FromFutureTokenScheduled(Func1<? super BooleanSubscription, ? extends Future<? extends T>> functionAsync, Scheduler scheduler) {
            this.functionAsync = functionAsync;
            this.scheduler = scheduler;
        }

        @Override
        public Observable<T> call() {
            return OperationStartFuture.startFuture(functionAsync, scheduler);
        }
    }
}
