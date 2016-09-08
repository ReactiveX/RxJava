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

package rx;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import rx.functions.*;
import rx.jmh.LatchedObserver;

/**
 * Benchmark operators that consume their sources completely and signal a single value.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*DeferredScalarPerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*DeferredScalarPerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class DeferredScalarPerf {

    @Param({"1", "10", "100", "1000", "10000", "100000", "1000000"})
    public int count;

    Observable<Integer> last;

    Observable<Integer> reduce;

    Observable<Integer> reduceSeed;

    Observable<int[]> collect;

    @Setup
    public void setup() {
        Integer[] array = new Integer[count];
        Arrays.fill(array, 777);

        Observable<Integer> source = Observable.from(array);

        reduce = source.reduce(new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer a, Integer b) {
                return b;
            }
        });
        reduceSeed = source.reduce(0, new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer a, Integer b) {
                return b;
            }
        });

        last = source.takeLast(1);

        collect = source.collect(new Func0<int[]>() {
            @Override
            public int[] call() {
                return new int[1];
            }
        }, new Action2<int[], Integer>() {
            @Override
            public void call(int[] a, Integer b) {
                a[0] = b.intValue();
            }
        } );
    }

    @Benchmark
    public void reduce(Blackhole bh) {
        reduce.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void reduceSeed(Blackhole bh) {
        reduceSeed.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void last(Blackhole bh) {
        last.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void collect(Blackhole bh) {
        collect.subscribe(new LatchedObserver<int[]>(bh));
    }

}
