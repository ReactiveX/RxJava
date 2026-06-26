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

package io.reactivex.rxjava4.schedulers;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava4.annotations.NonNull;

/**
 * Holds onto a value along with time information.
 *
 * @param <T> the value type
 */
public record Timed<T>(T value, long time, TimeUnit unit) {
    /**
     * Constructs a {@code Timed} instance with the given value and time information.
     *
     * @param value the value to hold
     * @param time  the time to hold
     * @param unit  the time unit, not null
     * @throws NullPointerException if {@code value} or {@code unit} is {@code null}
     */
    public Timed(@NonNull T value, long time, @NonNull TimeUnit unit) {
        this.value = Objects.requireNonNull(value, "value is null");
        this.time = time;
        this.unit = Objects.requireNonNull(unit, "unit is null");
    }

    /**
     * Returns the contained time value in the time unit specified.
     *
     * @param unit the time unit
     * @return the converted time
     */
    public long time(@NonNull TimeUnit unit) {
        return unit.convert(time, this.unit);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Timed<?>(Object value1, long time1, TimeUnit unit1)) {
            return Objects.equals(value, value1)
                    && time == time1
                    && Objects.equals(unit, unit1);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = value.hashCode();
        h = h * 31 + (int) ((time >>> 31) ^ time);
        h = h * 31 + unit.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "Timed[time=" + time + ", unit=" + unit + ", value=" + value + "]";
    }
}
