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

package io.reactivex.rxjava3.xmapz;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.Function;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class FlowableFlatMapSinglePerf {
    @Param({ "1", "10", "100", "1000", "10000", "100000", "1000000" })
    public int count;

    Flowable<Integer> flowableConvert;

    Flowable<Integer> flowableDedicated;

    Flowable<Integer> flowablePlain;

    @Setup
    public void setup() {
        Integer[] sourceArray = new Integer[count];
        Arrays.fill(sourceArray, 777);

        Flowable<Integer> source = Flowable.fromArray(sourceArray);

        flowablePlain = source.flatMap(new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v)
                    throws Exception {
                return Flowable.just(v);
            }
        });

        flowableConvert = source.flatMap(new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v)
                    throws Exception {
                return Single.just(v).toFlowable();
            }
        });

        flowableDedicated = source.flatMapSingle(new Function<Integer, Single<? extends Integer>>() {
            @Override
            public Single<? extends Integer> apply(Integer v)
                    throws Exception {
                return Single.just(v);
            }
        });
    }

    @Benchmark
    public Object flowablePlain(Blackhole bh) {
        return flowablePlain.subscribeWith(new PerfConsumer(bh));
    }

    @Benchmark
    public Object flowableConvert(Blackhole bh) {
        return flowableConvert.subscribeWith(new PerfConsumer(bh));
    }

    @Benchmark
    public Object flowableDedicated(Blackhole bh) {
        return flowableDedicated.subscribeWith(new PerfConsumer(bh));
    }
}
