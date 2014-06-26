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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import rx.exceptions.MissingBackpressureException;
import rx.internal.util.RxRingBuffer;
import rx.jmh.InputWithIncrementingInteger;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class RxRingBufferPerf {

    @Benchmark
    public void spmcRingBufferAddRemove1(SpmcInput input) throws InterruptedException, MissingBackpressureException {
        input.ring.onNext("a");
        input.bh.consume(input.ring.poll());
    }

    @Benchmark
    public void spmcRingBufferAddRemove1000(SpmcInput input) throws InterruptedException, MissingBackpressureException {
        for (int i = 0; i < 1000; i++) {
            input.ring.onNext("a");
        }
        for (int i = 0; i < 1000; i++) {
            input.bh.consume(input.ring.poll());
        }
    }

    @Benchmark
    public void spmcCreateUseAndDestroy1000(Input input) throws InterruptedException, MissingBackpressureException {
        RxRingBuffer buffer = RxRingBuffer.getSpmcInstance();
        for (int i = 0; i < 1000; i++) {
            buffer.onNext("a");
        }
        for (int i = 0; i < 1000; i++) {
            input.bh.consume(buffer.poll());
        }
        buffer.release();
    }

    @Benchmark
    public void spmcCreateUseAndDestroy1(Input input) throws InterruptedException, MissingBackpressureException {
        RxRingBuffer buffer = RxRingBuffer.getSpmcInstance();
        buffer.onNext("a");
        input.bh.consume(buffer.poll());
        buffer.release();
    }

    @Benchmark
    public void spscRingBufferAddRemove1(SpscInput input) throws InterruptedException, MissingBackpressureException {
        input.ring.onNext("a");
        input.bh.consume(input.ring.poll());
    }

    @Benchmark
    public void spscRingBufferAddRemove1000(SpscInput input) throws InterruptedException, MissingBackpressureException {
        for (int i = 0; i < 1000; i++) {
            input.ring.onNext("a");
        }
        for (int i = 0; i < 1000; i++) {
            input.bh.consume(input.ring.poll());
        }
    }

    @Benchmark
    public void spscCreateUseAndDestroy1000(Input input) throws InterruptedException, MissingBackpressureException {
        RxRingBuffer buffer = RxRingBuffer.getSpscInstance();
        for (int i = 0; i < 1000; i++) {
            buffer.onNext("a");
        }
        for (int i = 0; i < 1000; i++) {
            input.bh.consume(buffer.poll());
        }
        buffer.release();
    }

    @Benchmark
    public void spscCreateUseAndDestroy1(Input input) throws InterruptedException, MissingBackpressureException {
        RxRingBuffer buffer = RxRingBuffer.getSpscInstance();
        buffer.onNext("a");
        input.bh.consume(buffer.poll());
        buffer.release();
    }

    @State(Scope.Thread)
    public static class SpmcInput extends InputWithIncrementingInteger {

        RxRingBuffer ring = RxRingBuffer.getSpmcInstance();

        @Override
        public int getSize() {
            return 1;
        }

    }

    @State(Scope.Thread)
    public static class SpscInput extends InputWithIncrementingInteger {

        RxRingBuffer ring = RxRingBuffer.getSpscInstance();

        @Override
        public int getSize() {
            return 1;
        }

    }

    @State(Scope.Thread)
    public static class Input extends InputWithIncrementingInteger {

        @Override
        public int getSize() {
            return 1;
        }

    }

}
