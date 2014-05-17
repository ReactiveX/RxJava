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

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func0;

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
public final class OperatorDefer<T> implements OnSubscribe<T> {
    final Func0<? extends Observable<? extends T>> observableFactory;

    public OperatorDefer(Func0<? extends Observable<? extends T>> observableFactory) {
        this.observableFactory = observableFactory;
    }

    @Override
    public void call(Subscriber<? super T> s) {
        Observable<? extends T> o;
        try {
            o = observableFactory.call();
        } catch (Throwable t) {
            s.onError(t);
            return;
        }
        o.unsafeSubscribe(s);
    }
    
}
