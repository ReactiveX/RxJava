/**
 * Copyright 2014 Netflix, Inc.
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

package rx.internal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmark typical atomic operations on volatile fields and AtomicXYZ classes.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*AtomicPerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*AtomicPerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class AtomicPerf {
    @State(Scope.Thread)
    public static class Times {
        @Param({ "1", "1000", "1000000" })
        public int times;
    }
    @State(Scope.Thread)
    public static class VolatileIntState {
        public volatile int value;
    }
    @State(Scope.Thread)
    public static class VolatileLongState {
        public volatile long value;
    }
    @State(Scope.Thread)
    public static class VolatileIntFieldState {
        public volatile int value;
        public static final AtomicIntegerFieldUpdater<VolatileIntFieldState> UPDATER
        = AtomicIntegerFieldUpdater.newUpdater(VolatileIntFieldState.class, "value");
    }
    @State(Scope.Thread)
    public static class VolatileLongFieldState {
        public volatile long value;
        public static final AtomicLongFieldUpdater<VolatileLongFieldState> UPDATER
        = AtomicLongFieldUpdater.newUpdater(VolatileLongFieldState.class, "value");
    }
    @State(Scope.Thread)
    public static class AtomicIntState {
        public final AtomicInteger value = new AtomicInteger();
    }
    @State(Scope.Thread)
    public static class AtomicLongState {
        public final AtomicLong value = new AtomicLong();
    }
    
    // -----------------------------------------------------------------------------------
    
    @Benchmark
    public void volatileIntRead(VolatileIntState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(state.value);
        }
    }
    @Benchmark
    public void volatileLongRead(VolatileLongState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(state.value);
        }
    }
    @Benchmark
    public void volatileIntWrite(VolatileIntState state, Times repeat) {
        for (int i = 0; i < repeat.times; i++) {
            state.value = 1;
        }
    }
    @Benchmark
    public void volatileLongWrite(VolatileLongState state, Times repeat) {
        for (int i = 0; i < repeat.times; i++) {
            state.value = 1L;
        }
    }
    // -------------------------------------------------------------
    @Benchmark
    public void atomicIntIncrementAndGet(AtomicIntState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(state.value.incrementAndGet());
        }
    }
    @Benchmark
    public void atomicIntGetAndIncrement(AtomicIntState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(state.value.getAndIncrement());
        }
    }
    @Benchmark
    public void atomicLongIncrementAndGet(AtomicLongState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(state.value.incrementAndGet());
        }
    }
    @Benchmark
    public void atomicLongGetAndIncrement(AtomicLongState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(state.value.getAndIncrement());
        }
    }
    
    @Benchmark
    public void atomicIntLazySet(AtomicIntState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            state.value.lazySet(1);
        }
    }
    @Benchmark
    public void atomicLongLazySet(AtomicLongState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            state.value.lazySet(1L);
        }
    }

    @Benchmark
    public void atomicIntCASSuccess(AtomicIntState state, Times repeat, Blackhole bh) {
        state.value.set(0);
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(state.value.compareAndSet(i, i + 1));
        }
    }
    @Benchmark
    public void atomicLongCASSuccess(AtomicLongState state, Times repeat, Blackhole bh) {
        state.value.set(0L);
        for (long i = 0; i < repeat.times; i++) {
            bh.consume(state.value.compareAndSet(i, i + 1));
        }
    }
    @Benchmark
    public void atomicIntCASCheckSuccess(AtomicIntState state, Times repeat, Blackhole bh) {
        state.value.set(0);
        for (int i = 0; i < repeat.times; i++) {
            if (state.value.get() == i) {
                bh.consume(state.value.compareAndSet(i, i + 1));
            }
        }
    }
    @Benchmark
    public void atomicLongCASCheckSuccess(AtomicLongState state, Times repeat, Blackhole bh) {
        state.value.set(0L);
        for (long i = 0; i < repeat.times; i++) {
            if (state.value.get() == i) {
                bh.consume(state.value.compareAndSet(i, i + 1));
            }
        }
    }
    @Benchmark
    public void atomicIntCASFailure(AtomicIntState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(state.value.compareAndSet(1, 2));
        }
    }
    @Benchmark
    public void atomicLongCASFailure(AtomicLongState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(state.value.compareAndSet(1L, 2L));
        }
    }
    @Benchmark
    public void atomicIntCASCheckFailure(AtomicIntState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            if (state.value.get() == 1) {
                bh.consume(state.value.compareAndSet(1, 2));
            }
        }
    }
    @Benchmark
    public void atomicLongCASCheckFailure(AtomicLongState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            if (state.value.get() == 1L) {
                bh.consume(state.value.compareAndSet(1L, 2L));
            }
        }
    }
    // ----------------------------------
    @Benchmark
    public void atomicIntFieldIncrementAndGet(VolatileIntFieldState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(VolatileIntFieldState.UPDATER.incrementAndGet(state));
        }
    }
    @Benchmark
    public void atomicIntFieldGetAndIncrement(VolatileIntFieldState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(VolatileIntFieldState.UPDATER.getAndIncrement(state));
        }
    }
    @Benchmark
    public void atomicLongFieldIncrementAndGet(VolatileLongFieldState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(VolatileLongFieldState.UPDATER.incrementAndGet(state));
        }
    }
    @Benchmark
    public void atomicLongFieldGetAndIncrement(VolatileLongFieldState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(VolatileLongFieldState.UPDATER.getAndIncrement(state));
        }
    }
    
    @Benchmark
    public void atomicIntFieldLazySet(VolatileIntFieldState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            VolatileIntFieldState.UPDATER.lazySet(state, 1);
        }
    }
    @Benchmark
    public void atomicLongFieldLazySet(VolatileLongFieldState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            VolatileLongFieldState.UPDATER.lazySet(state, 1L);
        }
    }

    @Benchmark
    public void atomicIntFieldCASSuccess(VolatileIntFieldState state, Times repeat, Blackhole bh) {
        state.value = 0;
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(VolatileIntFieldState.UPDATER.compareAndSet(state, i, i + 1));
        }
    }
    @Benchmark
    public void atomicLongFieldCASSuccess(VolatileLongFieldState state, Times repeat, Blackhole bh) {
        state.value = 0L;
        for (long i = 0; i < repeat.times; i++) {
            bh.consume(VolatileLongFieldState.UPDATER.compareAndSet(state, i, i + 1));
        }
    }
    @Benchmark
    public void atomicIntFieldCASFailure(VolatileIntFieldState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(VolatileIntFieldState.UPDATER.compareAndSet(state, 1, 2));
        }
    }
    @Benchmark
    public void atomicLongFieldCASFailure(VolatileLongFieldState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(VolatileLongFieldState.UPDATER.compareAndSet(state, 1L, 2L));
        }
    }
    @Benchmark
    public void atomicIntGetAndSet(AtomicIntState state, Times repeat, Blackhole bh) {
        for (int i = 0; i < repeat.times; i++) {
            bh.consume(state.value.getAndSet(i));
        }
    }
    @Benchmark
    public void atomicLongGetAndSet(AtomicLongState state, Times repeat, Blackhole bh) {
        for (long i = 0; i < repeat.times; i++) {
            bh.consume(state.value.getAndSet(i));
        }
    }
}
