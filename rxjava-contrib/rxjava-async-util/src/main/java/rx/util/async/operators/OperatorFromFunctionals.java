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

import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Actions;
import rx.functions.Func0;

/**
 * Operators that invoke a function or action if
 * an observer subscribes.
 * Asynchrony can be achieved by using subscribeOn or observeOn.
 */
public final class OperatorFromFunctionals {
    /** Utility class. */
    private OperatorFromFunctionals() { throw new IllegalStateException("No instances!"); }
    
    /** Subscriber function that invokes an action and returns the given result. */
    public static <R> OnSubscribe<R> fromAction(Action0 action, R result) {
        return new InvokeAsync<R>(Actions.toFunc(action, result));
    }

    /** 
     * Subscriber function that invokes the callable and returns its value or
     * propagates its checked exception.
     */
    public static <R> OnSubscribe<R> fromCallable(Callable<? extends R> callable) {
        return new InvokeAsync<R>(callable);
    }
    /** Subscriber function that invokes a runnable and returns the given result. */
    public static <R> OnSubscribe<R> fromRunnable(final Runnable run, final R result) {
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
    static final class InvokeAsync<R> implements OnSubscribe<R> {
        final Callable<? extends R> callable;
        public InvokeAsync(Callable<? extends R> callable) {
            if (callable == null) {
                throw new NullPointerException("function");
            }
            this.callable = callable;
        }
        @Override
        public void call(Subscriber<? super R> t1) {
            try {
                t1.onNext(callable.call());
            } catch (Throwable t) {
                t1.onError(t);
                return;
            }
            t1.onCompleted();
        }
    }
}
