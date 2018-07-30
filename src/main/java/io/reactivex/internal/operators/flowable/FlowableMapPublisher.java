/**
 * Copyright (c) 2016-present, RxJava Contributors.
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


package io.reactivex.internal.operators.flowable;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.internal.operators.flowable.FlowableMap.MapSubscriber;

/**
 * Map working with an arbitrary Publisher source.
 * <p>History: 2.0.7 - experimental
 * @param <T> the input value type
 * @param <U> the output value type
 * @since 2.1
 */
public final class FlowableMapPublisher<T, U> extends Flowable<U> {

    final Publisher<T> source;

    final Function<? super T, ? extends U> mapper;
    public FlowableMapPublisher(Publisher<T> source, Function<? super T, ? extends U> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super U> s) {
        source.subscribe(new MapSubscriber<T, U>(s, mapper));
    }
}
