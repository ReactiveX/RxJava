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

import java.util.ArrayDeque;
import java.util.concurrent.*;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import rx.internal.util.SyncArrayQueue;
import rx.internal.util.unsafe.SpscArrayQueue;

/**
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*SyncArrayQueuePerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SyncArrayQueuePerf {
    @State(Scope.Thread)
    public static class Times {
        @Param({ "1", "1000", "1000000" })
        public int times;
    }
    @State(Scope.Thread)
    public static class SAQState {
        public final SyncArrayQueue queue = new SyncArrayQueue();
    }
    @State(Scope.Thread)
    public static class ADState {
        public final ArrayDeque<Object> queue = new ArrayDeque<Object>();
    }
    @State(Scope.Thread)
    public static class ABQState {
        public final ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(1024 * 1024);
    }
    @State(Scope.Thread)
    public static class CAQState {
        public final SpscArrayQueue<Object> queue = new SpscArrayQueue<Object>(1024 * 1024);
    }
    
    @Benchmark
    public void saqAddRemove1(SAQState state, Times times, Blackhole bh) {
        for (int i = 0; i < times.times; i++) {
            state.queue.offer(0);
            bh.consume(state.queue.poll());
        }
    }
    @Benchmark
    public void saqAddRemoveN(SAQState state, Times times, Blackhole bh) {
        for (int i = 0; i < times.times; i++) {
            state.queue.offer(0);
        }
        for (int i = 0; i < times.times; i++) {
            bh.consume(state.queue.poll());
        }
    }
    @Benchmark
    public void adAddRemove1(ADState state, Times times, Blackhole bh) {
        for (int i = 0; i < times.times; i++) {
            state.queue.offer(0);
            bh.consume(state.queue.poll());
        }
    }
    @Benchmark
    public void adAddRemoveN(ADState state, Times times, Blackhole bh) {
        for (int i = 0; i < times.times; i++) {
            state.queue.offer(0);
        }
        for (int i = 0; i < times.times; i++) {
            bh.consume(state.queue.poll());
        }
    }
    @Benchmark
    public void abqAddRemove1(ABQState state, Times times, Blackhole bh) {
        for (int i = 0; i < times.times; i++) {
            state.queue.offer(0);
            bh.consume(state.queue.poll());
        }
    }
    @Benchmark
    public void abqAddRemoveN(ABQState state, Times times, Blackhole bh) {
        for (int i = 0; i < times.times; i++) {
            state.queue.offer(0);
        }
        for (int i = 0; i < times.times; i++) {
            bh.consume(state.queue.poll());
        }
    }
    @Benchmark
    public void abqAddRemove1(CAQState state, Times times, Blackhole bh) {
        for (int i = 0; i < times.times; i++) {
            state.queue.offer(0);
            bh.consume(state.queue.poll());
        }
    }
    @Benchmark
    public void abqAddRemoveN(CAQState state, Times times, Blackhole bh) {
        for (int i = 0; i < times.times; i++) {
            state.queue.offer(0);
        }
        for (int i = 0; i < times.times; i++) {
            bh.consume(state.queue.poll());
        }
    }
}
