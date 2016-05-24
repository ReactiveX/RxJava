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
package rx.internal.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.*;

/**O
 * Returns an Observable that emits items emitted by the source Observable as long as a specified
 * condition is true.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/takeWhile.png" alt="">
 * @param <T> the value type
 */
public final class OperatorTakeWhile<T> implements Operator<T, T> {

    final Func2<? super T, ? super Integer, Boolean> predicate;

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
        Subscriber<T> s = new Subscriber<T>(subscriber, false) {

            private int counter = 0;

            private boolean done = false;

            @Override
            public void onNext(T t) {
                boolean isSelected;
                try {
                    isSelected = predicate.call(t, counter++);
                } catch (Throwable e) {
                    done = true;
                    Exceptions.throwOrReport(e, subscriber, t);
                    unsubscribe();
                    return;
                }
                if (isSelected) {
                    subscriber.onNext(t);
                } else {
                    done = true;
                    subscriber.onCompleted();
                    unsubscribe();
                }
            }

            @Override
            public void onCompleted() {
                if (!done) {
                    subscriber.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!done) {
                    subscriber.onError(e);
                }
            }

        };
        subscriber.add(s);
        return s;
    }

}
