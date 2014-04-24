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

/**
 * Returns the elements of the specified sequence or the specified default value
 * in a singleton sequence if the sequence is empty.
 * @param <T> the value type
 */
public class OperatorDefaultIfEmpty<T> implements Operator<T, T> {
    final T defaultValue;

    public OperatorDefaultIfEmpty(T defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {
            boolean hasValue;
            @Override
            public void onNext(T t) {
                hasValue = true;
                child.onNext(t);
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onCompleted() {
                if (!hasValue) {
                    try {
                        child.onNext(defaultValue);
                    } catch (Throwable e) {
                        child.onError(e);
                        return;
                    }
                }
                child.onCompleted();
            }
            
        };
    }
    
}
