/*
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

package io.reactivex.rxjava3.core;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * Represents a Reactive-Streams inspired {@link Subscriber} that is RxJava 3 only
 * and weakens the Reactive Streams rules <a href='https://github.com/reactive-streams/reactive-streams-jvm#1.3'>§1.3</a>
 * and <a href='https://github.com/reactive-streams/reactive-streams-jvm#3.9'>§3.9</a> of the specification
 * for gaining performance.
 *
 * <p>History: 2.0.7 - experimental; 2.1 - beta
 * @param <T> the value type
 * @since 2.2
 */
public interface FlowableSubscriber<@NonNull T> extends Subscriber<T> {

    /**
     * Implementors of this method should make sure everything that needs
     * to be visible in {@link #onNext(Object)} is established before
     * calling {@link Subscription#request(long)}. In practice this means
     * no initialization should happen after the {@code request()} call and
     * additional behavior is thread safe in respect to {@code onNext}.
     *
     * {@inheritDoc}
     */
    @Override
    void onSubscribe(@NonNull Subscription s);
}
