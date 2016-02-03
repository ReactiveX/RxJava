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
import rx.jmh.LatchedObserver;

/**
 * Benchmark typical atomic operations on volatile fields and AtomicXYZ classes.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*ConcatPerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*ConcatPerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class ConcatPerf {

    Observable<Integer> source;
    
    Observable<Integer> baseline;
    
    @Param({"1", "1000", "1000000"})
    int count;
    
    @Setup
    public void setup() {
        Integer[] array = new Integer[count];
        
        for (int i = 0; i < count; i++) {
            array[i] = 777;
        }
        
        baseline = Observable.from(array);
        
        source = Observable.concat(baseline, Observable.<Integer>empty());
    }
    
    @Benchmark
    public void normal(Blackhole bh) {
        source.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void baseline(Blackhole bh) {
        baseline.subscribe(new LatchedObserver<Integer>(bh));
    }
}
