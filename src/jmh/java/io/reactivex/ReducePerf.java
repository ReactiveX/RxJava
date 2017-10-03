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

import io.reactivex.functions.BiFunction;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = { "-XX:MaxInlineLevel=20" })
@State(Scope.Thread)
public class ReducePerf implements BiFunction<Integer, Integer, Integer> {
    @Param({ "1", "1000", "1000000" })
    public int times;

    Single<Integer> obsSingle;

    Single<Integer> flowSingle;

    Maybe<Integer> obsMaybe;

    Maybe<Integer> flowMaybe;

    @Override
    public Integer apply(Integer t1, Integer t2) throws Exception {
        return t1 + t2;
    }

    @Setup
    public void setup() {
        Integer[] array = new Integer[times];
        Arrays.fill(array, 777);

        obsSingle = Observable.fromArray(array).reduce(0, this);

        obsMaybe = Observable.fromArray(array).reduce(this);

        flowSingle = Flowable.fromArray(array).reduce(0, this);

        flowMaybe = Flowable.fromArray(array).reduce(this);
    }

    @Benchmark
    public void obsSingle(Blackhole bh) {
        obsSingle.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void flowSingle(Blackhole bh) {
        flowSingle.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void obsMaybe(Blackhole bh) {
        obsMaybe.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void flowMaybe(Blackhole bh) {
        flowMaybe.subscribe(new PerfConsumer(bh));
    }
}
