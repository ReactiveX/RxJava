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
package io.reactivex.internal.util;

import java.util.concurrent.atomic.*;

/**
 * Utility class to help with backpressure-related operations such as request aggregation.
 */
public enum BackpressureHelper {
    ;
    /**
     * Adds two long values and caps the sum at Long.MAX_VALUE.
     * @param a the first value
     * @param b the second value
     * @return the sum capped at Long.MAX_VALUE
     */
    public static long addCap(long a, long b) {
        long u = a + b;
        if (u < 0L) {
            return Long.MAX_VALUE;
        }
        return u;
    }
    
    /**
     * Multiplies two long values and caps the product at Long.MAX_VALUE.
     * @param a the first value
     * @param b the second value
     * @return the product capped at Long.MAX_VALUE
     */
    public static long multiplyCap(long a, long b) {
        long u = a * b;
        if (((a | b) >>> 31) != 0) {
            if (u / a != b) {
                return Long.MAX_VALUE;
            }
        }
        return u;
    }
    
    /**
     * Atomically adds the positive value n to the requested value in the AtomicLong and
     * caps the result at Long.MAX_VALUE and returns the previous value.
     * @param requested the AtomicLong holding the current requested value
     * @param n the value to add, must be positive (not verified)
     * @return the original value before the add
     */
    public static long add(AtomicLong requested, long n) {
        for (;;) {
            long r = requested.get();
            if (r == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long u = addCap(r, n);
            if (requested.compareAndSet(r, u)) {
                return r;
            }
        }
    }
    
    /**
     * Atomically adds the positive value n to the value in the instance through the field updater and
     * caps the result at Long.MAX_VALUE and returns the previous value.
     * @param updater the field updater for the requested value
     * @param instance the instance holding the requested value
     * @param n the value to add, must be positive (not verified)
     * @return the original value before the add
     */
    public static <T> long add(AtomicLongFieldUpdater<T> updater, T instance, long n) {
        for (;;) {
            long r = updater.get(instance);
            if (r == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long u = addCap(r, n);
            if (updater.compareAndSet(instance, r, u)) {
                return r;
            }
        }
    }
}
