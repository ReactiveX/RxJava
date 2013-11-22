/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.util;

import java.util.concurrent.TimeUnit;

/**
 * Represents a time value and time unit.
 * <p>
 * Rx.NET note: System.TimeSpan has a fixed unit of measure of 100 nanoseconds
 * per value; the Java way is to specify the TimeUnit along with the time value.
 * <p>
 * Usage:
 * <pre>
 * TimeSpan oneSecond = TimeSpan.of(1, TimeUnit.SECONDS);
 * </pre>
 */
public final class TimeSpan implements Comparable<TimeSpan> {
    private final long value;
    private final TimeUnit unit;
    /** Lazy hash. */
    private int hash;
    private TimeSpan(long value, TimeUnit unit) {
        this.value = value;
        this.unit = unit;
    }
    /**
     * Create a TimeSpan instance with the given time value and
     * time unit.
     * @param value the time value, must be nonnegative
     * @param unit the time unit
     * @return the TimeSpan instance with the given time value and 
     *         time unit
     * @throws IllegalArgumentException if the value &lt; 0
     * @throws NullPointerException if unit is null
     */
    public static TimeSpan of(long value, TimeUnit unit) {
        if (value < 0) {
            throw new IllegalArgumentException("value negative");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return new TimeSpan(value, unit);
    }
    /**
     * Returns the time value.
     * @return the time value
     */
    public long value() {
        return value;
    }
    /**
     * Returns the time unit.
     * @return the time unit
     */
    public TimeUnit unit() {
        return unit;
    }
    /**
     * Create a new TimeSpan with the same time unit as this
     * instance and the given newValue as the time value.
     * @param newValue the new time value
     * @return the new TimeSpan instance with the new time value
     * @throws IllegalArgumentException if the value &lt; 0
     */
    public TimeSpan withValue(long newValue) {
        if (newValue < 0) {
            throw new IllegalArgumentException("newValue negative");
        }
        return new TimeSpan(newValue, unit);
    }
    /**
     * Create a new TimeSpan with the same time value as this
     * instance and the given newUnit as the time unit.
     * @param newUnit the new time unit
     * @return the new TimeSpan instance with the new time unit
     * @throws NullPointerException if unit is null
     */
    public TimeSpan withUnit(TimeUnit newUnit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return new TimeSpan(value, unit);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() == getClass()) {
            TimeSpan other = (TimeSpan)obj;
            return value == other.value && unit == other.unit;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = (int)(value ^ (value >>> 32));
            h = h * 31 + unit.hashCode();
            hash = h;
        }
        return h;
    }

    @Override
    public int compareTo(TimeSpan o) {
        long t1 = unit.toNanos(value);
        long t2 = o.unit.toNanos(value);
        
        return t1 < t2 ? -1 : (t1 > t2 ? 1 : 0);
    }
    
}
