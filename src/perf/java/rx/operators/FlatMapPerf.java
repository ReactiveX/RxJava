/*
 * Copyright 2011-2015 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.operators;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import rx.Observable;
import rx.functions.Func1;

/**
 * Benchmark flatMap's optimizations.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*FlatMapPerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*FlatMapPerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class FlatMapPerf {
    @Param({ "1", "1000", "1000000" })
    public int times;

    Observable<Integer> rxSource;
    Observable<Integer> rxSource2;
    
    @Setup
    public void setup() {
        Observable<Integer> rxRange = Observable.range(0, times);
        rxSource = rxRange.flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t) {
                return Observable.just(t);
            }
        });
        rxSource2 = rxRange.flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.range(v, 2);
            }
        });
    }
    
    @Benchmark
    public Object rxFlatMap() {
        return rxSource.subscribe();
    }
    @Benchmark
    public Object rxFlatMap2() {
        return rxSource2.subscribe();
    }
}
