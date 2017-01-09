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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Publisher;

import io.reactivex.functions.Function;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class RxVsStreamPerf {
    @Param({ "1", "1000", "1000000" })
    public int times;

    Flowable<Integer> range;

    Observable<Integer> rangeObservable;

    Flowable<Integer> rangeFlatMap;

    Observable<Integer> rangeObservableFlatMap;

    Flowable<Integer> rangeFlatMapJust;

    Observable<Integer> rangeObservableFlatMapJust;

    List<Integer> values;

    @Setup
    public void setup() {
        range = Flowable.range(1, times);

        rangeFlatMapJust = range.flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) {
                return Flowable.just(v);
            }
        });

        rangeFlatMap = range.flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) {
                return Flowable.range(v, 2);
            }
        });

        rangeObservable = Observable.range(1, times);

        rangeObservableFlatMapJust = rangeObservable.flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) {
                return Observable.just(v);
            }
        });

        rangeObservableFlatMap = rangeObservable.flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) {
                return Observable.range(v, 2);
            }
        });

        values = range.toList().blockingGet();
    }

    @Benchmark
    public void range(Blackhole bh) {
        range.subscribe(new PerfSubscriber(bh));
    }

    @Benchmark
    public void rangeObservable(Blackhole bh) {
        rangeObservable.subscribe(new PerfObserver(bh));
    }

    @Benchmark
    public void rangeFlatMap(Blackhole bh) {
        rangeFlatMap.subscribe(new PerfSubscriber(bh));
    }

    @Benchmark
    public void rangeObservableFlatMap(Blackhole bh) {
        rangeObservableFlatMap.subscribe(new PerfObserver(bh));
    }

    @Benchmark
    public void rangeFlatMapJust(Blackhole bh) {
        rangeFlatMapJust.subscribe(new PerfSubscriber(bh));
    }

    @Benchmark
    public void rangeObservableFlatMapJust(Blackhole bh) {
        rangeObservableFlatMapJust.subscribe(new PerfObserver(bh));
    }

}
