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
 * Given a condition, subscribe to one of the observables when an Observer
 * subscribes.
 * 
 * @param <R>
 *            the result value type
 */
public final class OperatorIfThen<R> implements OnSubscribe<R> {
    final Func0<Boolean> condition;
    final Observable<? extends R> then;
    final Observable<? extends R> orElse;

    public OperatorIfThen(Func0<Boolean> condition, Observable<? extends R> then, Observable<? extends R> orElse) {
        this.condition = condition;
        this.then = then;
        this.orElse = orElse;
    }

    @Override
    public void call(Subscriber<? super R> t1) {
        Observable<? extends R> target;
        try {
            if (condition.call()) {
                target = then;
            } else {
                target = orElse;
            }
        } catch (Throwable t) {
            t1.onError(t);
            return;
        }
        target.subscribe(t1);
    }
}