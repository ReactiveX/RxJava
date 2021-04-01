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

package io.reactivex.rxjava3.observers;

/**
 * An interface that indicates that the implementing type is composed of individual components and exposes information
 * about their behavior.
 *
 * <p><em>NOTE:</em> This is considered a read-only public API and is not intended to be implemented externally.
 * <p>History: 2.1.4 - experimental
 * @since 2.2
 */
public interface LambdaConsumerIntrospection {

    /**
     * Returns {@code true} or {@code false} if a custom {@code onError} consumer has been provided.
     * @return {@code true} if a custom {@code onError} consumer implementation was supplied. Returns {@code false} if the
     * implementation is missing an error consumer and thus using a throwing default implementation.
     */
    boolean hasCustomOnError();

}
