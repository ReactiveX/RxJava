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

import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * Returns an Observable that emits items emitted by the source Observable as long as a specified
 * condition is true.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeWhile.png">
 */
public final class OperatorTakeWhile<T> implements Operator<T, T> {

    private final Func2<? super T, ? super Integer, Boolean> predicate;

    public OperatorTakeWhile(final Func1<? super T, Boolean> underlying) {
        this(new Func2<T, Integer, Boolean>() {
            @Override
            public Boolean call(T input, Integer index) {
                return underlying.call(input);
            }
        });
    }

    public OperatorTakeWhile(Func2<? super T, ? super Integer, Boolean> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        return new Subscriber<T>(subscriber) {

            private int counter = 0;

            @Override
            public void onNext(T args) {
                boolean isSelected;
                try {
                    isSelected = predicate.call(args, counter++);
                } catch (Throwable e) {
                    subscriber.onError(e);
                    unsubscribe();
                    return;
                }
                if (isSelected) {
                    subscriber.onNext(args);
                } else {
                    subscriber.onCompleted();
                    unsubscribe();
                }
            }

            @Override
            public void onCompleted() {
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

        };
    }

}
