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

import rx.IObservable;
import rx.IObservable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func0;

/**
 * Do not create the Observable until an Observer subscribes; create a fresh Observable on each
 * subscription.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/defer.png">
 * <p>
 * Pass defer an Observable factory function (a function that generates Observables), and defer will
 * return an Observable that will call this function to generate its Observable sequence afresh
 * each time a new Observer subscribes.
 */
public final class OperationDefer {

    public static <T> IObservable<T> defer(final Func0<? extends IObservable<? extends T>> observableFactory) {
        return new IObservable<T>() {
            @Override
            public Subscription subscribe(Observer<? super T> observer) {
                IObservable<? extends T> obs = observableFactory.call();
                return obs.subscribe(observer);
            }
        };

    }
}
