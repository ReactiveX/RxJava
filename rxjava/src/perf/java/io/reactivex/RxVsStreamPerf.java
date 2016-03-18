/**
 * Copyright 2016 Netflix, Inc.
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
    
    Observable<Integer> range;
    
    NbpObservable<Integer> rangeNbp;

    Observable<Integer> rangeFlatMap;

    NbpObservable<Integer> rangeNbpFlatMap;

    Observable<Integer> rangeFlatMapJust;

    NbpObservable<Integer> rangeNbpFlatMapJust;

    List<Integer> values;

    @Setup
    public void setup() {
        range = Observable.range(1, times);

        rangeFlatMapJust = range.flatMap(Observable::just);

        rangeFlatMap = range.flatMap(v -> Observable.range(v, 2));
        
        rangeNbp = NbpObservable.range(1, times);

        rangeNbpFlatMapJust = rangeNbp.flatMap(NbpObservable::just);
        
        rangeNbpFlatMap = rangeNbp.flatMap(v -> NbpObservable.range(v, 2));
        
        values = range.toList().toBlocking().first();
    }
    
    @Benchmark
    public void range(Blackhole bh) {
        range.subscribe(new LatchedObserver<>(bh));
    }

    @Benchmark
    public void rangeNbp(Blackhole bh) {
        rangeNbp.subscribe(new LatchedNbpObserver<>(bh));
    }

    @Benchmark
    public void rangeFlatMap(Blackhole bh) {
        rangeFlatMap.subscribe(new LatchedObserver<>(bh));
    }

    @Benchmark
    public void rangeNbpFlatMap(Blackhole bh) {
        rangeNbpFlatMap.subscribe(new LatchedNbpObserver<>(bh));
    }
    
    @Benchmark
    public void rangeFlatMapJust(Blackhole bh) {
        rangeFlatMapJust.subscribe(new LatchedObserver<>(bh));
    }

    @Benchmark
    public void rangeNbpFlatMapJust(Blackhole bh) {
        rangeNbpFlatMapJust.subscribe(new LatchedNbpObserver<>(bh));
    }

}