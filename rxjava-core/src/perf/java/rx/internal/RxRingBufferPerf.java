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

    @State(Scope.Thread)
    public static class Input extends InputWithIncrementingInteger {

        RxRingBuffer ring = RxRingBuffer.getInstance();

        @Override
        public int getSize() {
            return 1;
        }

    }

    @Benchmark
    public void ringBufferAddRemove(Input input) throws InterruptedException, MissingBackpressureException {
        input.ring.onNext("a");
        input.bh.consume(input.ring.poll());
    }

    @Benchmark
    public void ringBufferAddRemove1000(Input input) throws InterruptedException, MissingBackpressureException {
        for (int i = 0; i < 1000; i++) {
            input.ring.onNext("a");
        }
        for (int i = 0; i < 1000; i++) {
            input.bh.consume(input.ring.poll());
        }
    }

}
