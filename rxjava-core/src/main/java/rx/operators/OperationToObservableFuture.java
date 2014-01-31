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
package rx.operators;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

/**
 * Converts a Future into an Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/from.Future.png">
 * <p>
 * You can convert any object that supports the Future interface into an Observable that emits the
 * return value of the get() method of that object, by using the from operation.
 * <p>
 * This is blocking so the Subscription returned when calling
 * <code>Observable.subscribe(Observer)</code> does nothing.
 */
public class OperationToObservableFuture {
    /* package accessible for unit tests */static class ToObservableFuture<T> implements OnSubscribeFunc<T> {
        private final Future<? extends T> that;
        private final Long time;
        private final TimeUnit unit;

        public ToObservableFuture(Future<? extends T> that) {
            this.that = that;
            this.time = null;
            this.unit = null;
        }

        public ToObservableFuture(Future<? extends T> that, long time, TimeUnit unit) {
            this.that = that;
            this.time = time;
            this.unit = unit;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            try {
                T value = (time == null) ? (T) that.get() : (T) that.get(time, unit);

                if (!that.isCancelled()) {
                    observer.onNext(value);
                }
                observer.onCompleted();
            } catch (Throwable e) {
                observer.onError(e);
            }

            // the get() has already completed so there is no point in
            // giving the user a way to cancel.
            return Subscriptions.empty();
        }
    }

    public static <T> OnSubscribeFunc<T> toObservableFuture(final Future<? extends T> that) {
        return new ToObservableFuture<T>(that);
    }

    public static <T> OnSubscribeFunc<T> toObservableFuture(final Future<? extends T> that, long time, TimeUnit unit) {
        return new ToObservableFuture<T>(that, time, unit);
    }
}
