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

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Publisher;

import io.reactivex.functions.Function;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class EachTypeFlatMapPerf {
    @Param({ "1", "1000", "1000000" })
    public int times;

    Flowable<Integer> bpRange;
    Observable<Integer> nbpRange;
    Single<Integer> singleJust;

    Flowable<Integer> bpRangeMapJust;
    Observable<Integer> nbpRangeMapJust;
    Single<Integer> singleJustMapJust;

    Flowable<Integer> bpRangeMapRange;
    Observable<Integer> nbpRangeMapRange;

    @Setup
    public void setup() {
        bpRange = Flowable.range(1, times);
        nbpRange = Observable.range(1, times);

        bpRangeMapJust = bpRange.flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) {
                return Flowable.just(v);
            }
        });
        nbpRangeMapJust = nbpRange.flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) {
                return Observable.just(v);
            }
        });

        bpRangeMapRange = bpRange.flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) {
                return Flowable.range(v, 2);
            }
        });
        nbpRangeMapRange = nbpRange.flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) {
                return Observable.range(v, 2);
            }
        });

        singleJust = Single.just(1);
        singleJustMapJust = singleJust.flatMap(new Function<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> apply(Integer v) {
                return Single.just(v);
            }
        });
    }

    @Benchmark
    public void bpRange(Blackhole bh) {
        bpRange.subscribe(new PerfSubscriber(bh));
    }
    @Benchmark
    public void bpRangeMapJust(Blackhole bh) {
        bpRangeMapJust.subscribe(new PerfSubscriber(bh));
    }
    @Benchmark
    public void bpRangeMapRange(Blackhole bh) {
        bpRangeMapRange.subscribe(new PerfSubscriber(bh));
    }

    @Benchmark
    public void nbpRange(Blackhole bh) {
        nbpRange.subscribe(new PerfObserver(bh));
    }
    @Benchmark
    public void nbpRangeMapJust(Blackhole bh) {
        nbpRangeMapJust.subscribe(new PerfObserver(bh));
    }
    @Benchmark
    public void nbpRangeMapRange(Blackhole bh) {
        nbpRangeMapRange.subscribe(new PerfObserver(bh));
    }

    @Benchmark
    public void singleJust(Blackhole bh) {
        singleJust.subscribe(new LatchedSingleObserver<Integer>(bh));
    }
    @Benchmark
    public void singleJustMapJust(Blackhole bh) {
        singleJustMapJust.subscribe(new LatchedSingleObserver<Integer>(bh));
    }
}
