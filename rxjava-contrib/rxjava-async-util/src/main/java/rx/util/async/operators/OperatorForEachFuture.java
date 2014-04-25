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

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import rx.Observable;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Convert the observation of a source observable to a big Future call.
 * <p>
 * Remark: the cancellation token version's behavior is in doubt, so left out.
 */
public final class OperatorForEachFuture {
    /** Utility class. */
    private OperatorForEachFuture() { throw new IllegalStateException("No instances!"); }
    
    /**
     * Subscribes to the given source and calls the callback for each emitted item,
     * and surfaces the completion or error through a Future.
     * @param <T> the element type of the Observable
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @return the Future representing the entire for-each operation
     */
    public static <T> FutureTask<Void> forEachFuture(
            Observable<? extends T> source, 
            Action1<? super T> onNext) {
        return forEachFuture(source, onNext, Functionals.emptyThrowable(), Functionals.empty());
    }
    
    /**
     * Subscribes to the given source and calls the callback for each emitted item,
     * and surfaces the completion or error through a Future.
     * @param <T> the element type of the Observable
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @param onError the action to call when an exception is emitted
     * @return the Future representing the entire for-each operation
     */
    public static <T> FutureTask<Void> forEachFuture(
            Observable<? extends T> source, 
            Action1<? super T> onNext,
            Action1<? super Throwable> onError) {
        return forEachFuture(source, onNext, onError, Functionals.empty());
    }
    
    /**
     * Subscribes to the given source and calls the callback for each emitted item,
     * and surfaces the completion or error through a Future.
     * @param <T> the element type of the Observable
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @param onError the action to call when an exception is emitted
     * @param onCompleted the action to call when the source completes
     * @return the Future representing the entire for-each operation
     */
    public static <T> FutureTask<Void> forEachFuture(
            Observable<? extends T> source, 
            Action1<? super T> onNext,
            Action1<? super Throwable> onError,
            Action0 onCompleted) {
        
        LatchedObserver<T> lo = LatchedObserver.create(onNext, onError, onCompleted);

        Subscription s = source.subscribe(lo);
        
        FutureTaskCancel<Void> task = new FutureTaskCancel<Void>(s, new RunAwait<T>(lo));
        
        return task;
    }
    /**
     * A future task that unsubscribes the given subscription when cancelled.
     * @param <T> the return value type
     */
    private static final class FutureTaskCancel<T> extends FutureTask<T> {
        final Subscription cancel;

        public FutureTaskCancel(Subscription cancel, Callable<T> callable) {
            super(callable);
            this.cancel = cancel;
        }

        public FutureTaskCancel(Subscription cancel, Runnable runnable, T result) {
            super(runnable, result);
            this.cancel = cancel;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancel.unsubscribe();
            return super.cancel(mayInterruptIfRunning);
        }
        
    }
    
    /** Await the completion of a latched observer and throw its exception if any. */
    private static final class RunAwait<T> implements Callable<Void> {
        final LatchedObserver<T> observer;

        public RunAwait(LatchedObserver<T> observer) {
            this.observer = observer;
        }
        
        @Override
        public Void call() throws Exception {
            observer.await();
            Throwable t = observer.getThrowable();
            if (t != null) {
                throw Exceptions.propagate(t);
            }
            return null;
        }
    }
}
