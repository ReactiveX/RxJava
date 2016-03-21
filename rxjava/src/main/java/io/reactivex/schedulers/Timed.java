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

package io.reactivex.schedulers;

import java.util.concurrent.TimeUnit;

import io.reactivex.internal.functions.Objects;

/**
 * Holds onto a value along with time information.
 *
 * @param <T> the value type
 */
public final class Timed<T> {
    final T value;
    final long time;
    final TimeUnit unit;
    public Timed(T value, long time, TimeUnit unit) {
        this.value = value;
        this.time = time;
        this.unit = Objects.requireNonNull(unit, "unit is null");
    }
    
    public T value() {
        return value;
    }
    
    public TimeUnit unit() {
        return unit;
    }
    
    public long time() {
        return time;
    }
    
    public long time(TimeUnit unit) {
        return unit.convert(time, this.unit);
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof Timed) {
            Timed<?> o = (Timed<?>) other;
            return Objects.equals(value, o.value)
                    && time == o.time
                    && Objects.equals(unit, o.unit);
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "Timed[time=" + time + ", unit=" + unit + ", value=" + value + "]";
    }
}
