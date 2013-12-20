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
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.CompositeSubscription;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

/**
 * Start an asynchronous Future immediately and observe its result through
 * an observable.
 */
public final class OperationStartFuture {
    /** Utility class. */
    private OperationStartFuture() { throw new IllegalStateException("No instances!"); }
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
    /**
     * Invokes the asynchronous function, surfacing the result through an observable
     * sequence and shares a BooleanSubscription between all subscribers.
     * <em>Important note</em> subscribing to the resulting observable blocks until
     * the future completes.
     * @param <T> the result type
     * @param functionAsync the asynchronous function to run
     * @return  the observable
     */
    public static <T> Observable<T> startFuture(Func1<? super BooleanSubscription, ? extends Future<? extends T>> functionAsync) {
        final BooleanSubscription token = new BooleanSubscription();
        Future<? extends T> task;
        try {
            task = functionAsync.call(token);
        } catch (Throwable t) {
            return Observable.error(t);
        }
        final Observable<T> result = Observable.from(task);
        return Observable.create(new StartAsyncSubscribe<T>(token, result));
    }
    
    /**
     * Invokes the asynchronous function, surfacing the result through an observable
     * sequence and shares a BooleanSubscription between all subscribers.
     * @param <T> the result type
     * @param functionAsync the asynchronous function to run
     * @param scheduler the scheduler where the completion of the Future is awaited
     * @return  the observable
     */
    public static <T> Observable<T> startFuture(
            Func1<? super BooleanSubscription, ? extends Future<? extends T>> functionAsync,
            Scheduler scheduler) {
        final BooleanSubscription token = new BooleanSubscription();
        Future<? extends T> task;
        try {
            task = functionAsync.call(token);
        } catch (Throwable t) {
            return Observable.error(t);
        }
        final Observable<T> result = Observable.from(task, scheduler);
        return Observable.create(new StartAsyncSubscribe<T>(token, result));
    }
    /** 
     * The subsription function that combines the token and the subscription to the
     * source.
     * @param <T> the value type
     */
    private static final class StartAsyncSubscribe<T> implements OnSubscribeFunc<T> {
        final Subscription token;
        final Observable<? extends T> source;

        public StartAsyncSubscribe(Subscription token, Observable<? extends T> source) {
            this.token = token;
            this.source = source;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {
            Subscription s = source.subscribe(t1);
            return new CompositeSubscription(token, s);
        }
    }   
}
