/**
 * Copyright 2015 Netflix, Inc.
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

import java.util.concurrent.*;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class EachTypeFlatMapPerf {
    @Param({ "1", "1000", "1000000" })
    public int times;
    
    Observable<Integer> bpRange;
    NbpObservable<Integer> nbpRange;
    Single<Integer> singleJust;

    Observable<Integer> bpRangeMapJust;
    NbpObservable<Integer> nbpRangeMapJust;
    Single<Integer> singleJustMapJust;

    Observable<Integer> bpRangeMapRange;
    NbpObservable<Integer> nbpRangeMapRange;

    @Setup
    public void setup() {
        bpRange = Observable.range(1, times);
        nbpRange = NbpObservable.range(1, times);

        bpRangeMapJust = bpRange.flatMap(Observable::just);
        nbpRangeMapJust = nbpRange.flatMap(NbpObservable::just);
        
        bpRangeMapRange = bpRange.flatMap(v -> Observable.range(v, 2));
        nbpRangeMapRange = nbpRange.flatMap(v -> NbpObservable.range(v, 2));

        singleJust = Single.just(1);
        singleJustMapJust = singleJust.flatMap(Single::just);
    }
    
    @Benchmark
    public void bpRange(Blackhole bh) {
        bpRange.subscribe(new LatchedObserver<>(bh));
    }
    @Benchmark
    public void bpRangeMapJust(Blackhole bh) {
        bpRangeMapJust.subscribe(new LatchedObserver<>(bh));
    }
    @Benchmark
    public void bpRangeMapRange(Blackhole bh) {
        bpRangeMapRange.subscribe(new LatchedObserver<>(bh));
    }

    @Benchmark
    public void nbpRange(Blackhole bh) {
        nbpRange.subscribe(new LatchedNbpObserver<>(bh));
    }
    @Benchmark
    public void nbpRangeMapJust(Blackhole bh) {
        nbpRangeMapJust.subscribe(new LatchedNbpObserver<>(bh));
    }
    @Benchmark
    public void nbpRangeMapRange(Blackhole bh) {
        nbpRangeMapRange.subscribe(new LatchedNbpObserver<>(bh));
    }

    @Benchmark
    public void singleJust(Blackhole bh) {
        singleJust.subscribe(new LatchedSingleObserver<>(bh));
    }
    @Benchmark
    public void singleJustMapJust(Blackhole bh) {
        singleJustMapJust.subscribe(new LatchedSingleObserver<>(bh));
    }
}