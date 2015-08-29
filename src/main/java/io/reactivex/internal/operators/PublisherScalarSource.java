/*
 * Copyright 2011-2015 David Karnok
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.internal.operators;

import java.util.function.Function;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.*;

/**
 * 
 */
public final class PublisherScalarSource<T> implements Publisher<T> {
    private final T value;
    public PublisherScalarSource(T value) {
        this.value = value;
    }
    @Override
    public void subscribe(Subscriber<? super T> s) {
        s.onSubscribe(new ScalarSubscription<>(s, value));
    }
    
    public T value() {
        return value;
    }
    
    public <U> Publisher<U> flatMap(Function<? super T, ? extends Publisher<? extends U>> mapper) {
        return s -> {
            Publisher<? extends U> other;
            try {
                other = mapper.apply(value);
            } catch (Throwable e) {
                EmptySubscription.error(e, s);
                return;
            }
            other.subscribe(s);
        };
    }
}
