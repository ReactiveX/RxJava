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

package io.reactivex;

import io.reactivex.internal.functions.Objects;

/**
 * Simplified backport of Java 8's Optional type.
 *
 * @param <T> the value type
 */
public final class Optional<T> {
    final T value;
    protected Optional(T value) {
        this.value = value;
    }
    
    static final Optional<Object> EMPTY = new Optional<Object>(null);
    
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> empty() {
        return (Optional<T>)EMPTY;
    }
    
    public static <T> Optional<T> of(T value) {
        Objects.requireNonNull(value, "value is null");
        return new Optional<T>(value);
    }
    
    public boolean isPresent() {
        return value != null;
    }
    
    public T get() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Optional<?> other = (Optional<?>) obj;
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }
    
    
}
