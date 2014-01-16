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

import rx.Observable.OperatorSubscription;
import rx.Observer;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Applies a function of your choosing to every item emitted by an Observable, and returns this
 * transformation as a new Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/map.png">
 */
public final class OperatorMap<T, R> implements Func2<Observer<? super R>, OperatorSubscription, Observer<? super T>> {

    private final Func1<? super T, ? extends R> transformer;

    public OperatorMap(Func1<? super T, ? extends R> transformer) {
        this.transformer = transformer;
    }

    @Override
    public Observer<? super T> call(final Observer<? super R> o, OperatorSubscription os) {
        return new Observer<T>() {

            @Override
            public void onCompleted() {
                o.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onNext(T t) {
                try {
                    o.onNext(transformer.call(t));
                } catch (Throwable e) {
                    onError(e);
                }
            }

        };
    }

}
