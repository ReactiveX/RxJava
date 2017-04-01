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

package io.reactivex.schedulers;

import java.util.concurrent.TimeUnit;

import io.reactivex.annotations.NonNull;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Holds onto a value along with time information.
 *
 * @param <T> the value type
 */
public final class Timed<T> {
    final T value;
    final long time;
    final TimeUnit unit;

    /**
     * Constructs a Timed instance with the given value and time information.
     * @param value the value to hold
     * @param time the time to hold
     * @param unit the time unit, not null
     * @throws NullPointerException if unit is null
     */
    public Timed(@NonNull T value, long time, @NonNull TimeUnit unit) {
        this.value = value;
        this.time = time;
        this.unit = ObjectHelper.requireNonNull(unit, "unit is null");
    }

    /**
     * Returns the contained value.
     * @return the contained value
     */
    @NonNull
    public T value() {
        return value;
    }

    /**
     * Returns the time unit of the contained time.
     * @return the time unit of the contained time
     */
    @NonNull
    public TimeUnit unit() {
        return unit;
    }

    /**
     * Returns the time value.
     * @return the time value
     */
    public long time() {
        return time;
    }

    /**
     * Returns the contained time value in the time unit specified.
     * @param unit the time unt
     * @return the converted time
     */
    public long time(@NonNull TimeUnit unit) {
        return unit.convert(time, this.unit);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Timed) {
            Timed<?> o = (Timed<?>) other;
            return ObjectHelper.equals(value, o.value)
                    && time == o.time
                    && ObjectHelper.equals(unit, o.unit);
        }
        return false;
    }

    @Override
    public int hashCode() {
         int h = value != null ? value.hashCode() : 0;
         h = h * 31 + (int)((time >>> 31) ^ time);
         h = h * 31 + unit.hashCode();
         return h;
    }

    @Override
    public String toString() {
        return "Timed[time=" + time + ", unit=" + unit + ", value=" + value + "]";
    }
}
