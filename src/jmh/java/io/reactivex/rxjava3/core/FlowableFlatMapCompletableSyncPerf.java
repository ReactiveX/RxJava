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

package io.reactivex.rxjava3.core;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import io.reactivex.rxjava3.internal.functions.Functions;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class FlowableFlatMapCompletableSyncPerf {

    @Param({"1", "10", "100", "1000", "10000", "100000", "1000000"})
    int items;

    @Param({"1", "8", "32", "128", "256"})
    int maxConcurrency;

    Completable flatMapCompletable;

    Flowable<Object> flatMap;

    @Setup
    public void setup() {
        Integer[] array = new Integer[items];
        Arrays.fill(array, 777);

        flatMapCompletable = Flowable.fromArray(array)
                .flatMapCompletable(Functions.justFunction(Completable.complete()), false, maxConcurrency);

        flatMap = Flowable.fromArray(array)
                .flatMap(Functions.justFunction(Completable.complete().toFlowable()), false, maxConcurrency);
    }

    @Benchmark
    public Object flatMap(Blackhole bh) {
        return flatMap.subscribeWith(new PerfConsumer(bh));
    }

    @Benchmark
    public Object flatMapCompletable(Blackhole bh) {
        return flatMapCompletable.subscribeWith(new PerfConsumer(bh));
    }
}
