/**
 * Copyright 2014 Netflix, Inc.
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
package rx.util.async.operators;

import java.util.concurrent.Future;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Func0;

/**
 * Defer the execution of a factory method which produces an observable sequence.
 */
public final class OperatorDeferFuture {
    /** Utility class. */
    private OperatorDeferFuture() { throw new IllegalStateException("No instances!"); }
    
    /**
     * Returns an observable sequence that starts the specified asynchronous 
     * factory function whenever a new observer subscribes.
     * @param <T> the result type
     * @param observableFactoryAsync the asynchronous function to start for each observer
     * @return the observable sequence containing values produced by the asynchronous observer
     * produced by the factory
     */
    public static <T> Observable<T> deferFuture(Func0<? extends Future<? extends Observable<? extends T>>> observableFactoryAsync) {
        return Observable.defer(new DeferFutureFunc0<T>(observableFactoryAsync));
    }
    /** The function called by the defer operator. */
    private static final class DeferFutureFunc0<T> implements Func0<Observable<T>> {
        final Func0<? extends Future<? extends Observable<? extends T>>> observableFactoryAsync;

        public DeferFutureFunc0(Func0<? extends Future<? extends Observable<? extends T>>> observableFactoryAsync) {
            this.observableFactoryAsync = observableFactoryAsync;
        }
        
        @Override
        public Observable<T> call() {
            return Observable.merge(OperatorStartFuture.startFuture(observableFactoryAsync));
        }
        
    }
    
    /**
     * Returns an observable sequence that starts the specified asynchronous 
     * factory function whenever a new observer subscribes.
     * @param <T> the result type
     * @param observableFactoryAsync the asynchronous function to start for each observer
     * @param scheduler the scheduler where the completion of the Future is awaited
     * @return the observable sequence containing values produced by the asynchronous observer
     * produced by the factory
     */
    public static <T> Observable<T> deferFuture(
            Func0<? extends Future<? extends Observable<? extends T>>> observableFactoryAsync,
            Scheduler scheduler) {
        return Observable.defer(new DeferFutureFunc0Scheduled<T>(observableFactoryAsync, scheduler));
    }
    /** The function called by the defer operator. */
    private static final class DeferFutureFunc0Scheduled<T> implements Func0<Observable<T>> {
        final Func0<? extends Future<? extends Observable<? extends T>>> observableFactoryAsync;
        final Scheduler scheduler;

        public DeferFutureFunc0Scheduled(Func0<? extends Future<? extends Observable<? extends T>>> observableFactoryAsync,
                Scheduler scheduler) {
            this.observableFactoryAsync = observableFactoryAsync;
            this.scheduler = scheduler;
        }
        
        @Override
        public Observable<T> call() {
            return Observable.merge(OperatorStartFuture.startFuture(observableFactoryAsync, scheduler));
        }
        
    }
}
