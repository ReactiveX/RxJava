/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import rx.IObservable;
import rx.Observable.OnSubscribeFunc;
import rx.util.functions.Func1;

/**
 * Converts the elements of an observable sequence to the specified type.
 */
public class OperationCast {

    public static <T, R> OnSubscribeFunc<R> cast(
            IObservable<? extends T> source, final Class<R> klass) {
        return OperationMap.map(source, new Func1<T, R>() {
            @Override
            public R call(T t) {
                return klass.cast(t);
            }
        });
    }
}
