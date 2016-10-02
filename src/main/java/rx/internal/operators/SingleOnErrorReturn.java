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
import rx.Single.OnSubscribe;
import rx.exceptions.Exceptions;
import rx.functions.Func1;

/**
 * Signal a value returned by a resumeFunction when the source signals a Throwable.
 *
 * @param <T> the value type
 */
public final class SingleOnErrorReturn<T> implements Single.OnSubscribe<T> {

    final Single.OnSubscribe<T> source;

    final Func1<Throwable, ? extends T> resumeFunction;

    public SingleOnErrorReturn(OnSubscribe<T> source, Func1<Throwable, ? extends T> resumeFunction) {
        this.source = source;
        this.resumeFunction = resumeFunction;
    }

    @Override
    public void call(SingleSubscriber<? super T> t) {
        OnErrorReturnsSingleSubscriber<T> parent = new OnErrorReturnsSingleSubscriber<T>(t, resumeFunction);
        t.add(parent);
        source.call(parent);
    }

    static final class OnErrorReturnsSingleSubscriber<T> extends SingleSubscriber<T> {

        final SingleSubscriber<? super T> actual;

        final Func1<Throwable, ? extends T> resumeFunction;

        public OnErrorReturnsSingleSubscriber(SingleSubscriber<? super T> actual,
                Func1<Throwable, ? extends T> resumeFunction) {
            this.actual = actual;
            this.resumeFunction = resumeFunction;
        }

        @Override
        public void onSuccess(T value) {
            actual.onSuccess(value);
        }

        @Override
        public void onError(Throwable error) {
            T v;

            try {
                v = resumeFunction.call(error);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(ex);
                return;
            }

            actual.onSuccess(v);
        }
    }
}
