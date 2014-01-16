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
import rx.Scheduler;
import rx.util.Timestamped;
import rx.util.functions.Func2;

/**
 * Wraps each item emitted by a source Observable in a {@link Timestamped} object.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/timestamp.png">
 */
public final class OperatorTimestamp<T> implements Func2<Observer<? super Timestamped<T>>, OperatorSubscription, Observer<? super T>> {

    private final Scheduler scheduler;

    public OperatorTimestamp(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * @return a sequence of timestamped values created by adding timestamps to each item in the input sequence.
     */
    @Override
    public Observer<? super T> call(final Observer<? super Timestamped<T>> o, OperatorSubscription os) {
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
                o.onNext(new Timestamped<T>(scheduler.now(), t));
            }

        };
    }

}
