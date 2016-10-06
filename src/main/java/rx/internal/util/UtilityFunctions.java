/**
 * Copyright 2014 Netflix, Inc.
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
package rx.internal.util;

import rx.functions.Func1;

/**
 * Utility functions for internal use that we don't want part of the public API.
 */
public final class UtilityFunctions {

    /** Utility class. */
    private UtilityFunctions() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Returns a function that always returns {@code true}.
     *
     * @param <T> the value type
     * @return a {@link Func1} that accepts an Object and returns the Boolean {@code true}
     */
    public static <T> Func1<? super T, Boolean> alwaysTrue() {
        return AlwaysTrue.INSTANCE;
    }

    /**
     * Returns a function that always returns {@code false}.
     *
     * @param <T> the value type
     * @return a {@link Func1} that accepts an Object and returns the Boolean {@code false}
     */
    public static <T> Func1<? super T, Boolean> alwaysFalse() {
        return AlwaysFalse.INSTANCE;
    }

    /**
     * Returns a function that always returns the Object it is passed.
     *
     * @param <T> the input and output value type
     * @return a {@link Func1} that accepts an Object and returns the same Object
     */
    @SuppressWarnings("unchecked")
    public static <T> Func1<T, T> identity() {
        return (Func1<T, T>) Identity.INSTANCE;
    }

    enum AlwaysTrue implements Func1<Object, Boolean> {
        INSTANCE;

        @Override
        public Boolean call(Object o) {
            return true;
        }
    }

    enum AlwaysFalse implements Func1<Object, Boolean> {
        INSTANCE;

        @Override
        public Boolean call(Object o) {
            return false;
        }
    }

    enum Identity implements Func1<Object, Object> {
        INSTANCE;

        @Override
        public Object call(Object o) {
            return o;
        }
    }
}
