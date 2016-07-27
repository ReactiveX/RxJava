/**
 * Copyright 2016 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex;

import io.reactivex.internal.functions.Objects;

/**
 * An observable immutable value
 *
 * @param <T> the value type
 */
public class Immutable<T> implements Value<T> {

    protected final T value;

    /**
     * Init immutable value
     *
     * @param value non null value
     */
    public Immutable(final T value) {
        Objects.requireNonNull(value, "Value cannot be null");
        this.value = value;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public Observable<T> asObservable() {
        return Observable.just(getValue());
    }
}
