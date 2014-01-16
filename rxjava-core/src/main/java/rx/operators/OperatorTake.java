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
import rx.util.functions.Func2;

/**
 * Returns an Observable that emits the first <code>num</code> items emitted by the source
 * Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/take.png">
 * <p>
 * You can choose to pay attention only to the first <code>num</code> items emitted by an
 * Observable by using the take operation. This operation returns an Observable that will invoke a
 * subscribing Observer's <code>onNext</code> function a maximum of <code>num</code> times before
 * invoking <code>onCompleted</code>.
 */
public final class OperatorTake<T> implements Func2<Observer<? super T>, OperatorSubscription, Observer<? super T>> {

    final int limit;

    public OperatorTake(int limit) {
        this.limit = limit;
    }

    @Override
    public Observer<T> call(final Observer<? super T> o, final OperatorSubscription s) {
        if (limit == 0) {
            o.onCompleted();
            s.unsubscribe();
        }
        return new Observer<T>() {

            int count = 0;
            boolean completed = false;

            @Override
            public void onCompleted() {
                if (!completed) {
                    o.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!completed) {
                    o.onError(e);
                }
            }

            @Override
            public void onNext(T i) {
                if (!s.isUnsubscribed()) {
                    o.onNext(i);
                    if (++count >= limit) {
                        completed = true;
                        o.onCompleted();
                        s.unsubscribe();
                    }
                }
            }

        };
    }

}
