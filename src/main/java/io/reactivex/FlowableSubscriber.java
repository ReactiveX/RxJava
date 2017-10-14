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

package io.reactivex;

import io.reactivex.annotations.*;
import org.reactivestreams.*;

/**
 * Represents a Reactive-Streams inspired Subscriber that is RxJava 2 only
 * and weakens rules §1.3 and §3.9 of the specification for gaining performance.
 *
 * <p>History: 2.0.7 - experimental
 * @param <T> the value type
 * @since 2.1 - beta
 */
@Beta
public interface FlowableSubscriber<T> extends Subscriber<T> {

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
