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

import rx.*;
import rx.exceptions.Exceptions;
import rx.functions.Action0;

/**
 * Call an Action0 when the subscription happens to the source.
 *
 * @param <T> the value type
 */
public final class SingleDoOnSubscribe<T> implements Single.OnSubscribe<T> {

    final Single.OnSubscribe<T> source;

    final Action0 onSubscribe;

    public SingleDoOnSubscribe(Single.OnSubscribe<T> source, Action0 onSubscribe) {
        this.source = source;
        this.onSubscribe = onSubscribe;
    }

    @Override
    public void call(SingleSubscriber<? super T> t) {

        try {
            onSubscribe.call();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            t.onError(ex);
            return;
        }

        source.call(t);
    }
}
