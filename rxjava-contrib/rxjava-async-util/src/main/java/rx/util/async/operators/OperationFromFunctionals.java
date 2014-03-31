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
package rx.util.async.operators;

import java.util.concurrent.Callable;

import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Actions;
import rx.functions.Func0;
import rx.subscriptions.Subscriptions;

/**
 * Operators that invoke a function or action if
 * an observer subscribes.
 * Asynchrony can be achieved by using subscribeOn or observeOn.
 */
public final class OperationFromFunctionals {
    /** Utility class. */
    private OperationFromFunctionals() { throw new IllegalStateException("No instances!"); }
    
    /** Subscriber function that invokes an action and returns the given result. */
    public static <R> OnSubscribeFunc<R> fromAction(Action0 action, R result) {
        return new InvokeAsync<R>(Actions.toFunc(action, result));
    }

    /**
     * Subscriber function that invokes a function and returns its value.
     *
     * @deprecated  Unnecessary now that Func0 extends Callable. Just call
     *              {@link #fromCallable(Callable)} instead.
     */
    @Deprecated
    public static <R> OnSubscribeFunc<R> fromFunc0(Func0<? extends R> function) {
        return fromCallable(function);
    }
    
    /** 
     * Subscriber function that invokes the callable and returns its value or
     * propagates its checked exception.
     */
    public static <R> OnSubscribeFunc<R> fromCallable(Callable<? extends R> callable) {
        return new InvokeAsync<R>(callable);
    }
    /** Subscriber function that invokes a runnable and returns the given result. */
    public static <R> OnSubscribeFunc<R> fromRunnable(final Runnable run, final R result) {
        return new InvokeAsync<R>(new Func0<R>() {
            @Override
            public R call() {
                run.run();
                return result;
            }
        });
    }
    
    /**
     * Invokes a java.util.concurrent.Callable when an observer subscribes.
     * @param <R> the return type
     */
    static final class InvokeAsync<R> implements OnSubscribeFunc<R> {
        final Callable<? extends R> callable;
        public InvokeAsync(Callable<? extends R> callable) {
            if (callable == null) {
                throw new NullPointerException("function");
            }
            this.callable = callable;
        }
        @Override
        public Subscription onSubscribe(Observer<? super R> t1) {
            Subscription s = Subscriptions.empty();
            try {
                t1.onNext(callable.call());
            } catch (Throwable t) {
                t1.onError(t);
                return s;
            }
            t1.onCompleted();
            return s;
        }
    }
}
