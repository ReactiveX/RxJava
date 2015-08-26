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

package io.reactivex.schedulers;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class Timestamped<T> {
    final T value;
    final long timestamp;
    final TimeUnit unit;
    public Timestamped(T value, long timestamp, TimeUnit unit) {
        this.value = value;
        this.timestamp = timestamp;
        this.unit = unit;
    }
    
    public T value() {
        return value;
    }
    
    public TimeUnit unit() {
        return unit;
    }
    
    public long timestamp() {
        return timestamp;
    }
    
    public long timestamp(TimeUnit unit) {
        return unit.convert(timestamp, this.unit);
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof Timestamped) {
            Timestamped<?> o = (Timestamped<?>) other;
            return Objects.equals(value, o.value)
                    && timestamp == o.timestamp
                    && Objects.equals(unit, o.unit);
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "Timestamped[timestamp=" + timestamp + ", unit=" + unit + ", value=" + value + "]";
    }
}
