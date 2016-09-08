/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.*;
import rx.internal.util.UtilityFunctions;

/**
 * Returns an Observable that emits all sequentially distinct items emitted by the source.
 * @param <T> the value type
 * @param <U> the key type
 */
public final class OperatorDistinctUntilChanged<T, U> implements Operator<T, T>, Func2<U, U, Boolean> {
    final Func1<? super T, ? extends U> keySelector;

    final Func2<? super U, ? super U, Boolean> comparator;

    static final class Holder {
        static final OperatorDistinctUntilChanged<?,?> INSTANCE = new OperatorDistinctUntilChanged<Object,Object>(UtilityFunctions.identity());
    }


    /**
     * Returns a singleton instance of OperatorDistinctUntilChanged that was built using
     * the identity function for comparison (<code>new OperatorDistinctUntilChanged(UtilityFunctions.identity())</code>).
     *
     * @param <T> the value type
     * @return Operator that emits sequentially distinct values only using the identity function for comparison
     */
    @SuppressWarnings("unchecked")
    public static <T> OperatorDistinctUntilChanged<T, T> instance() {
        return (OperatorDistinctUntilChanged<T, T>) Holder.INSTANCE;
    }

    public OperatorDistinctUntilChanged(Func1<? super T, ? extends U> keySelector) {
        this.keySelector = keySelector;
        this.comparator = this;

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperatorDistinctUntilChanged(Func2<? super U, ? super U, Boolean> comparator) {
        this.keySelector = (Func1)UtilityFunctions.identity();
        this.comparator = comparator;
    }

    @Override
    public Boolean call(U t1, U t2) {
        return (t1 == t2 || (t1 != null && t1.equals(t2)));
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {
            U previousKey;
            boolean hasPrevious;
            @Override
            public void onNext(T t) {
                U key;
                try {
                    key = keySelector.call(t);
                } catch (Throwable e) {
                    Exceptions.throwOrReport(e, child, t);
                    return;
                }
                U currentKey = previousKey;
                previousKey = key;

                if (hasPrevious) {
                    boolean comparison;

                    try {
                        comparison = comparator.call(currentKey, key);
                    } catch (Throwable e) {
                        Exceptions.throwOrReport(e, child, key);
                        return;
                    }

                    if (!comparison) {
                        child.onNext(t);
                    } else {
                        request(1);
                    }
                } else {
                    hasPrevious = true;
                    child.onNext(t);
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

}
