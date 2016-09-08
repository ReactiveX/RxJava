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

import rx.*;
import rx.exceptions.*;
import rx.functions.Func1;
import rx.plugins.RxJavaHooks;

/**
 * Applies a function of your choosing to every item emitted by an {@code Single}, and emits the results of
 * this transformation as a new {@code Single}.
 *
 * @param <T> the input value type
 * @param <R> the return value type
 */
public final class SingleOnSubscribeMap<T, R> implements Single.OnSubscribe<R> {

    final Single<T> source;

    final Func1<? super T, ? extends R> transformer;

    public SingleOnSubscribeMap(Single<T> source, Func1<? super T, ? extends R> transformer) {
        this.source = source;
        this.transformer = transformer;
    }

    @Override
    public void call(final SingleSubscriber<? super R> o) {
        MapSubscriber<T, R> parent = new MapSubscriber<T, R>(o, transformer);
        o.add(parent);
        source.subscribe(parent);
    }

    static final class MapSubscriber<T, R> extends SingleSubscriber<T> {

        final SingleSubscriber<? super R> actual;

        final Func1<? super T, ? extends R> mapper;

        boolean done;

        public MapSubscriber(SingleSubscriber<? super R> actual, Func1<? super T, ? extends R> mapper) {
            this.actual = actual;
            this.mapper = mapper;
        }

        @Override
        public void onSuccess(T t) {
            R result;

            try {
                result = mapper.call(t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                unsubscribe();
                onError(OnErrorThrowable.addValueAsLastCause(ex, t));
                return;
            }

            actual.onSuccess(result);
        }

        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaHooks.onError(e);
                return;
            }
            done = true;

            actual.onError(e);
        }
    }

}

