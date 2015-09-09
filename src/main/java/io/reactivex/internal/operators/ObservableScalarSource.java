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

package io.reactivex.internal.operators;

import java.util.function.Function;

import org.reactivestreams.*;

import io.reactivex.Observable;
import io.reactivex.internal.subscriptions.*;

/**
 * Represents a constant scalar value.
 */
public final class ObservableScalarSource<T> extends Observable<T> {
    private final T value;
    public ObservableScalarSource(T value) {
        super(new Publisher<T>() {
            @Override
            public void subscribe(Subscriber<? super T> s) {
                s.onSubscribe(new ScalarSubscription<>(s, value));
            }
        });
        this.value = value;
    }
    
    public T value() {
        return value;
    }
    
    public <U> Publisher<U> scalarFlatMap(Function<? super T, ? extends Publisher<? extends U>> mapper) {
        return s -> {
            Publisher<? extends U> other;
            try {
                other = mapper.apply(value);
            } catch (Throwable e) {
                EmptySubscription.error(e, s);
                return;
            }
            if (other == null) {
                EmptySubscription.error(new NullPointerException("The publisher returned by the function is null"), s);
                return;
            }
            other.subscribe(s);
        };
    }
}
