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

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import rx.Observable;
import rx.functions.Func1;
import rx.jmh.LatchedObserver;

/**
 * Benchmark ConcatMapIterable.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*ConcatMapInterablePerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*ConcatMapInterablePerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class ConcatMapInterablePerf {

    @Param({"1", "10", "100", "1000", "10000", "100000", "1000000"})
    public int count;
    
    Observable<Integer> justPlain;
    
    Observable<Integer> justIterable;

    Observable<Integer> rangePlain;
    
    Observable<Integer> rangeIterable;

    Observable<Integer> xrangePlain;
    
    Observable<Integer> xrangeIterable;

    Observable<Integer> chainPlain;
    
    Observable<Integer> chainIterable;

    @Setup
    public void setup() {
        Integer[] values = new Integer[count];
        for (int i = 0; i < count; i++) {
            values[i] = i;
        }
        
        int c = 1000000 / count;
        Integer[] xvalues = new Integer[c];
        for (int i = 0; i < c; i++) {
            xvalues[i] = i;
        }
        
        Observable<Integer> source = Observable.from(values);

        justPlain = source.concatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.just(v);
            }
        });
        justIterable = source.concatMapIterable(new Func1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> call(Integer v) {
                return Collections.singleton(v);
            }
        });

        final Observable<Integer> range = Observable.range(1, 2);
        final List<Integer> xrange = Arrays.asList(1, 2);
        
        rangePlain = source.concatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return range;
            }
        });
        rangeIterable = source.concatMapIterable(new Func1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> call(Integer v) {
                return xrange;
            }
        });
        
        final Observable<Integer> xsource = Observable.from(xvalues);
        final List<Integer> xvaluesList = Arrays.asList(xvalues);
        
        xrangePlain = source.concatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return xsource;
            }
        });
        xrangeIterable = source.concatMapIterable(new Func1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> call(Integer v) {
                return xvaluesList;
            }
        });

        chainPlain = xrangePlain.concatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.just(v);
            }
        });
        chainIterable = xrangeIterable.concatMapIterable(new Func1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> call(Integer v) {
                return Collections.singleton(v);
            }
        });
    }
    
    @Benchmark
    public void justPlain(Blackhole bh) {
        justPlain.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void justIterable(Blackhole bh) {
        justIterable.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void rangePlain(Blackhole bh) {
        rangePlain.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void rangeIterable(Blackhole bh) {
        rangeIterable.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void xrangePlain(Blackhole bh) {
        xrangePlain.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void xrangeIterable(Blackhole bh) {
        xrangeIterable.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void chainPlain(Blackhole bh) {
        chainPlain.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void chainIterable(Blackhole bh) {
        chainIterable.subscribe(new LatchedObserver<Integer>(bh));
    }

}
