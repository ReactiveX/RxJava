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

package rx.operators;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import rx.Observable;
import rx.functions.Func1;
import rx.jmh.LatchedObserver;

/**
 * Benchmark typical atomic operations on volatile fields and AtomicXYZ classes.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*FlatMapRangePerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*FlatMapRangePerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class FlatMapRangePerf {
    @Param({ "1", "10", "1000", "1000000" })
    public int times;
    
    Observable<Integer> rangeFlatMapJust;
    Observable<Integer> rangeFlatMapRange;
    
    @Setup
    public void setup() {
        Observable<Integer> range = Observable.range(1, times);
        
        rangeFlatMapJust = range.flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.just(v);
            }
        });
        rangeFlatMapRange = range.flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.range(v, 2);
            }
        });
    }
    
    @Benchmark
    public void rangeFlatMapJust(Blackhole bh) {
        rangeFlatMapJust.subscribe(new LatchedObserver<Object>(bh));
    }

    @Benchmark
    public void rangeFlatMapRange(Blackhole bh) {
        rangeFlatMapRange.subscribe(new LatchedObserver<Object>(bh));
    }

}
