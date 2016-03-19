/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.observable;

import io.reactivex.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.EmptyDisposable;

/**
 * Represents a constant scalar value.
 * @param <T> the value type
 */
public final class NbpObservableScalarSource<T> extends Observable<T> {
    private final T value;
    public NbpObservableScalarSource(final T value) {
        super(new NbpOnSubscribe<T>() {
            @Override
            public void accept(Observer<? super T> s) {
                s.onSubscribe(EmptyDisposable.INSTANCE);
                s.onNext(value);
                s.onComplete();
            }
        });
        this.value = value;
    }
    
    public T value() {
        return value;
    }
    
    public <U> NbpOnSubscribe<U> scalarFlatMap(final Function<? super T, ? extends Observable<? extends U>> mapper) {
        return new NbpOnSubscribe<U>() {
            @Override
            public void accept(Observer<? super U> s) {
                Observable<? extends U> other;
                try {
                    other = mapper.apply(value);
                } catch (Throwable e) {
                    EmptyDisposable.error(e, s);
                    return;
                }
                if (other == null) {
                    EmptyDisposable.error(new NullPointerException("The publisher returned by the function is null"), s);
                    return;
                }
                if (other instanceof NbpObservableScalarSource) {
                    @SuppressWarnings("unchecked")
                    NbpObservableScalarSource<U> o = (NbpObservableScalarSource<U>)other;
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    s.onNext(o.value);
                    s.onComplete();
                } else {
                    other.subscribe(s);
                }
            }
        };
    }
}
