/**
 * Copyright 2016 Netflix, Inc.
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
 * Benchmark flatMap running over a mixture of normal and empty Observables.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*FlatMapAsFilterPerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*FlatMapAsFilterPerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class FlatMapAsFilterPerf {

    @Param({"1", "1000", "1000000"})
    public int count;
    
    @Param({"0", "1", "3", "7"})
    public int mask;
    
    public Observable<Integer> justEmptyFlatMap;
    
    public Observable<Integer> rangeEmptyFlatMap;

    public Observable<Integer> justEmptyConcatMap;
    
    public Observable<Integer> rangeEmptyConcatMap;

    @Setup
    public void setup() {
        if (count == 1 && mask != 0) {
            throw new RuntimeException("Force skip");
        }
        Integer[] values = new Integer[count];
        for (int i = 0; i < count; i++) {
            values[i] = i;
        }
        final Observable<Integer> just = Observable.just(1);
        
        final Observable<Integer> range = Observable.range(1, 2);
        
        final Observable<Integer> empty = Observable.empty();
        
        final int m = mask;
        
        justEmptyFlatMap = Observable.from(values).flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return (v & m) == 0 ? empty : just;
            }
        });
        
        rangeEmptyFlatMap = Observable.from(values).flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return (v & m) == 0 ? empty : range;
            }
        });

        justEmptyConcatMap = Observable.from(values).concatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return (v & m) == 0 ? empty : just;
            }
        });
        
        rangeEmptyConcatMap = Observable.from(values).concatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return (v & m) == 0 ? empty : range;
            }
        });
    }

    @Benchmark
    public void justEmptyFlatMap(Blackhole bh) {
        justEmptyFlatMap.subscribe(new LatchedObserver<Integer>(bh));
    }
    
    @Benchmark
    public void rangeEmptyFlatMap(Blackhole bh) {
        rangeEmptyFlatMap.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void justEmptyConcatMap(Blackhole bh) {
        justEmptyConcatMap.subscribe(new LatchedObserver<Integer>(bh));
    }
    
    @Benchmark
    public void rangeEmptyConcatMap(Blackhole bh) {
        rangeEmptyConcatMap.subscribe(new LatchedObserver<Integer>(bh));
    }
}