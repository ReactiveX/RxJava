/**
 * Copyright 2015 Netflix, Inc.
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

import java.util.NoSuchElementException;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.exceptions.Exceptions;
import rx.functions.Func2;

public final class OnSubscribeReduce<T> implements OnSubscribe<T> {

    final Observable<T> source;
    
    final Func2<T, T, T> reducer;

    public OnSubscribeReduce(Observable<T> source, Func2<T, T, T> reducer) {
        this.source = source;
        this.reducer = reducer;
    }
    
    @Override
    public void call(Subscriber<? super T> t) {
        new ReduceSubscriber<T>(t, reducer).subscribeTo(source);
    }
    
    static final class ReduceSubscriber<T> extends DeferredScalarSubscriber<T, T> {

        final Func2<T, T, T> reducer;

        public ReduceSubscriber(Subscriber<? super T> actual, Func2<T, T, T> reducer) {
            super(actual);
            this.reducer = reducer;
        }

        @Override
        public void onNext(T t) {
            if (!hasValue) {
                hasValue = true;
                value = t;
            } else {
                try {
                    value = reducer.call(value, t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    unsubscribe();
                    actual.onError(ex);
                }
            }
        }
        
        @Override
        public void onCompleted() {
            if (hasValue) {
                complete(value);
            } else {
                actual.onError(new NoSuchElementException());
            }
        }
        
    }
}
