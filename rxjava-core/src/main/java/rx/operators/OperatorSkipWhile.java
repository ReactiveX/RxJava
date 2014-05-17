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
 * Skips any emitted source items as long as the specified condition holds true. Emits all further source items
 * as soon as the condition becomes false.
 */
public final class OperatorSkipWhile<T> implements Operator<T, T> {
    private final Func2<? super T, Integer, Boolean> predicate;

    public OperatorSkipWhile(Func2<? super T, Integer, Boolean> predicate) {
        this.predicate = predicate;
    }
    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {
            boolean skipping = true;
            int index;
            @Override
            public void onNext(T t) {
                if (!skipping) {
                    child.onNext(t);
                } else {
                    if (!predicate.call(t, index++)) {
                        skipping = false;
                        child.onNext(t);
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onCompleted() {
                child.onCompleted();
            }
        };
    }
    /** Convert to Func2 type predicate. */
    public static <T> Func2<T, Integer, Boolean> toPredicate2(final Func1<? super T, Boolean> predicate) {
        return new Func2<T, Integer, Boolean>() {

            @Override
            public Boolean call(T t1, Integer t2) {
                return predicate.call(t1);
            }
        };
    }
}
