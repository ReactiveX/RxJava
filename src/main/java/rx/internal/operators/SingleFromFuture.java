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

import java.util.concurrent.*;

import rx.*;
import rx.exceptions.Exceptions;
import rx.subscriptions.Subscriptions;

/**
 * Wait and emit the value of the Future.
 *
 * @param <T> the value type
 */
public final class SingleFromFuture<T> implements Single.OnSubscribe<T> {

    final Future<? extends T> future;

    final long timeout;

    final TimeUnit unit;

    public SingleFromFuture(Future<? extends T> future, long timeout, TimeUnit unit) {
        this.future = future;
        this.timeout = timeout;
        this.unit = unit;
    }

    @Override
    public void call(SingleSubscriber<? super T> t) {
        Future<? extends T> f = future;

        t.add(Subscriptions.from(f));

        T v;

        try {
            if (timeout == 0L) {
                v = f.get();
            } else {
                v = f.get(timeout, unit);
            }
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            t.onError(ex);
            return;
        }

        t.onSuccess(v);
    }
}
