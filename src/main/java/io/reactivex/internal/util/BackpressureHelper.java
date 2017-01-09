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
package io.reactivex.internal.util;

import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.plugins.RxJavaPlugins;

/**
 * Utility class to help with backpressure-related operations such as request aggregation.
 */
public final class BackpressureHelper {
    /** Utility class. */
    private BackpressureHelper() {
        throw new IllegalStateException("No instances!");
    }

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
     * Atomically adds the positive value n to the requested value in the AtomicLong and
     * caps the result at Long.MAX_VALUE and returns the previous value and
     * considers Long.MIN_VALUE as a cancel indication (no addition then).
     * @param requested the AtomicLong holding the current requested value
     * @param n the value to add, must be positive (not verified)
     * @return the original value before the add
     */
    public static long addCancel(AtomicLong requested, long n) {
        for (;;) {
            long r = requested.get();
            if (r == Long.MIN_VALUE) {
                return Long.MIN_VALUE;
            }
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
     * Atomically subtract the given number (positive, not validated) from the target field unless it contains Long.MAX_VALUE.
     * @param requested the target field holding the current requested amount
     * @param n the produced element count, positive (not validated)
     * @return the new amount
     */
    public static long produced(AtomicLong requested, long n) {
        for (;;) {
            long current = requested.get();
            if (current == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long update = current - n;
            if (update < 0L) {
                RxJavaPlugins.onError(new IllegalStateException("More produced than requested: " + update));
                update = 0L;
            }
            if (requested.compareAndSet(current, update)) {
                return update;
            }
        }
    }

    /**
     * Atomically subtract the given number (positive, not validated) from the target field if
     * it doesn't contain Long.MIN_VALUE (indicating some cancelled state) or Long.MAX_VALUE (unbounded mode).
     * @param requested the target field holding the current requested amount
     * @param n the produced element count, positive (not validated)
     * @return the new amount
     */
    public static long producedCancel(AtomicLong requested, long n) {
        for (;;) {
            long current = requested.get();
            if (current == Long.MIN_VALUE) {
                return Long.MIN_VALUE;
            }
            if (current == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long update = current - n;
            if (update < 0L) {
                RxJavaPlugins.onError(new IllegalStateException("More produced than requested: " + update));
                update = 0L;
            }
            if (requested.compareAndSet(current, update)) {
                return update;
            }
        }
    }
}
