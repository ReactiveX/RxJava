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

import rx.functions.Func1;

/**
 * Converts the element of a Single to the specified type.
 * @param <T> the input value type
 * @param <R> the output value type
 */
public class SingleOperatorCast<T, R> implements Func1<T, R> {

    final Class<R> castClass;

    public SingleOperatorCast(Class<R> castClass) {
        this.castClass = castClass;
    }

    @Override
    public R call(T t) {
        return castClass.cast(t);
    }
}
