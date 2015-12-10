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

import rx.Observable.Operator;
import rx.Subscriber;

public class OperatorIgnoreElements<R, T> implements Operator<R, T> {

    private static class Holder {
        static final OperatorIgnoreElements<?, ?> INSTANCE = new OperatorIgnoreElements<Object, Object>();
    }
    
    @SuppressWarnings("unchecked")
    public static <T, R> OperatorIgnoreElements<T, R> instance() {
        return (OperatorIgnoreElements<T, R>) Holder.INSTANCE;
    }

    private OperatorIgnoreElements() {

    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super R> child) {
        Subscriber<T> parent = new Subscriber<T>() {

            @Override
            public void onCompleted() {
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(T t) {
                // ignore element
            }

        };
        child.add(parent);
        return parent;
    }

}
