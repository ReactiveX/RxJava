/**
 * Copyright 2016 Netflix, Inc.
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

import rx.*;
import rx.Observable.OnSubscribe;
import rx.exceptions.Exceptions;
import rx.functions.Func2;

public final class OnSubscribeReduceSeed<T, R> implements OnSubscribe<R> {

    final Observable<T> source;

    final R initialValue;

    final Func2<R, ? super T, R> reducer;

    public OnSubscribeReduceSeed(Observable<T> source, R initialValue, Func2<R, ? super T, R> reducer) {
        this.source = source;
        this.initialValue = initialValue;
        this.reducer = reducer;
    }

    @Override
    public void call(Subscriber<? super R> t) {
        new ReduceSeedSubscriber<T, R>(t, initialValue, reducer).subscribeTo(source);
    }

    static final class ReduceSeedSubscriber<T, R> extends DeferredScalarSubscriber<T, R> {

        final Func2<R, ? super T, R> reducer;

        public ReduceSeedSubscriber(Subscriber<? super R> actual, R initialValue, Func2<R, ? super T, R> reducer) {
            super(actual);
            this.value = initialValue;
            this.hasValue = true;
            this.reducer = reducer;
        }

        @Override
        public void onNext(T t) {
            try {
                value = reducer.call(value, t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                unsubscribe();
                actual.onError(ex);
            }
        }

    }
}
