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

package io.reactivex;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.*;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class StrictPerf {
    @Param({ "1", "10", "100", "1000", "10000", "100000", "1000000" })
    public int count;

    @Param({ "1", "10", "100", "1000", "10000" })
    public int cpu;

    Flowable<Integer> source;

    @Setup
    public void setup() {
        Integer[] array = new Integer[count];
        Arrays.fill(array, 777);

        source = Flowable.fromArray(array);
    }

    @Benchmark
    public void internal(Blackhole bh) {
        source.subscribe(new InternalConsumer(bh, cpu));
    }

    @Benchmark
    public void external(Blackhole bh) {
        source.subscribe(new ExternalConsumer(bh, cpu));
    }

    static final class InternalConsumer implements FlowableSubscriber<Object> {
        final Blackhole bh;

        final int cycles;

        InternalConsumer(Blackhole bh, int cycles) {
            this.bh = bh;
            this.cycles = cycles;
        }

        @Override
        public void onNext(Object t) {
            bh.consume(t);
            Blackhole.consumeCPU(cycles);
        }

        @Override
        public void onError(Throwable t) {
            bh.consume(t);
        }

        @Override
        public void onComplete() {
            bh.consume(true);
        }

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }
    }

    static final class ExternalConsumer implements Subscriber<Object> {
        final Blackhole bh;

        final int cycles;

        ExternalConsumer(Blackhole bh, int cycles) {
            this.bh = bh;
            this.cycles = cycles;
        }

        @Override
        public void onNext(Object t) {
            bh.consume(t);
            Blackhole.consumeCPU(cycles);
        }

        @Override
        public void onError(Throwable t) {
            bh.consume(t);
        }

        @Override
        public void onComplete() {
            bh.consume(true);
        }

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }
    }
}
