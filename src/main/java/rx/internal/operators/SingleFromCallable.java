/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.concurrent.Callable;

import rx.*;
import rx.exceptions.Exceptions;

/**
 * Execute a callable and emit its resulting value.
 *
 * @param <T> the value type
 */
public final class SingleFromCallable<T> implements Single.OnSubscribe<T> {

    final Callable<? extends T> callable;

    public SingleFromCallable(Callable<? extends T> callable) {
        this.callable = callable;
    }

    @Override
    public void call(SingleSubscriber<? super T> t) {
        T v;
        try {
            v = callable.call();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            t.onError(ex);
            return;
        }

        t.onSuccess(v);
    }
}
