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

import rx.Operator;
import rx.subscriptions.CompositeSubscription;
import rx.util.functions.Func1;

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
public final class OperatorTake<T> implements OperatorFunc<T, T> {

    final int limit;

    public OperatorTake(int limit) {
        this.limit = limit;
    }

    @Override
    public Operator<? super T> call(final Operator<? super T> o) {
        CompositeSubscription parent = new CompositeSubscription();
        if (limit == 0) {
            o.onCompleted();
            parent.unsubscribe();
        }
        /*
         * We decouple the parent and child subscription so there can be multiple take() in a chain
         * such as for the groupBy operator use case where you may take(1) on groups and take(20) on the children.
         * 
         * Thus, we only unsubscribe UPWARDS to the parent and an onComplete DOWNSTREAM.
         */
        return new Operator<T>(parent) {

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
                if (!isUnsubscribed()) {
                    o.onNext(i);
                    if (++count >= limit) {
                        completed = true;
                        o.onCompleted();
                        unsubscribe();
                    }
                }
            }

        };
    }

}
