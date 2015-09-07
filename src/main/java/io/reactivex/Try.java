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

package io.reactivex;

import java.util.Objects;

/**
 * Container for either a value of type T or a Throwable.
 *
 * @param <T> the value type
 */
public final class Try<T> {
    /** The value. */
    final T value;
    /** The error or null if this holds a value. */
    final Throwable error;
    
    private Try(T value, Throwable error) {
        this.value = value;
        this.error = error;
    }
    
    /**
     * Constructs a Try instance by wrapping the given value.
     * 
     * @param value the value to wrap 
     * @return the created Try instance
     */
    public static <T> Try<T> ofValue(T value) {
        // TODO ? Objects.requireNonNull(value);
        return new Try<>(value, null);
    }
    
    /**
     * Constructs a Try instance by wrapping the given Throwable.
     * 
     * <p>Null Throwables are replaced by NullPointerException instance in this Try.
     * 
     * @param e the exception to wrap
     * @return the new Try instance holding the exception
     */
    public static <T> Try<T> ofError(Throwable e) {
        return new Try<>(null, e != null ? e : new NullPointerException());
    }
    
    /**
     * Returns the value or null if the value is actually null or if this Try holds an error instead.
     * @return the value contained
     * @see #hasValue()
     */
    public T value() {
        return value;
    }
    
    /**
     * Returns the error or null if this Try holds a value instead.
     * 
     * @return the Throwable contained or null
     * 
     */
    public Throwable error() {
        return error;
    }
    
    /**
     * Returns true if this Try holds an error.
     * @return true if this Try holds an error
     */
    public boolean hasError() {
        return error != null;
    }
    
    /**
     * Returns true if this Try holds a value.
     * @return true if this Try holds a value
     */
    public boolean hasValue() {
        return error == null;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof Try) {
            
            Try<?> o = (Try<?>) other;
            return Objects.equals(value, o.value)
                    && Objects.equals(error, o.error);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(value) + Objects.hashCode(error);
    }
    
    @Override
    public String toString() {
        if (error != null) {
            return "Try[ " + error + " ]";
        }
        return "Try[" + value + "]";
    }
}
