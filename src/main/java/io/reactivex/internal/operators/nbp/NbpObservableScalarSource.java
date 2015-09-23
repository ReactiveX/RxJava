/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import java.util.function.Function;

import io.reactivex.NbpObservable;
import io.reactivex.internal.disposables.EmptyDisposable;

/**
 * Represents a constant scalar value.
 */
public final class NbpObservableScalarSource<T> extends NbpObservable<T> {
    private final T value;
    public NbpObservableScalarSource(T value) {
        super(new NbpOnSubscribe<T>() {
            @Override
            public void accept(NbpSubscriber<? super T> s) {
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
    
    public <U> NbpOnSubscribe<U> scalarFlatMap(Function<? super T, ? extends NbpObservable<? extends U>> mapper) {
        return s -> {
            NbpObservable<? extends U> other;
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
        };
    }
}
