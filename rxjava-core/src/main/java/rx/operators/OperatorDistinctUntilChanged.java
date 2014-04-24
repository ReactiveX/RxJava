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
package rx.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Returns an Observable that emits all sequentially distinct items emitted by the source.
 * @param <T> the value type
 * @param <U> the key type
 */
public final class OperatorDistinctUntilChanged<T, U> implements Operator<T, T> {
    final Func1<? super T, ? extends U> keySelector;

    public OperatorDistinctUntilChanged(Func1<? super T, ? extends U> keySelector) {
        this.keySelector = keySelector;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {
            U previousKey;
            boolean hasPrevious;
            @Override
            public void onNext(T t) {
                U currentKey = previousKey;
                U key = keySelector.call(t);
                previousKey = key;
                
                if (hasPrevious) {
                    if (!(currentKey == key || (key != null && key.equals(currentKey)))) {
                        child.onNext(t);
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
