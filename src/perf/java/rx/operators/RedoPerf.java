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

import java.util.Arrays;
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
import rx.internal.util.UtilityFunctions;
import rx.jmh.LatchedObserver;

/**
 * Benchmark typical atomic operations on volatile fields and AtomicXYZ classes.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*RedoPerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*RedoPerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class RedoPerf {
    @Param({"1,1", "1,1000", "1,1000000", "1000,1", "1000,1000", "1000000,1"})
    public String params;
    
    public int len;
    public int repeat;
    
    Observable<Integer> sourceRepeating;
    
    Observable<Integer> sourceRetrying;
    
    Observable<Integer> redoRepeating;
    
    Observable<Integer> redoRetrying;

    Observable<Integer> baseline;
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Setup
    public void setup() {
        String[] ps = params.split(",");
        len = Integer.parseInt(ps[0]);
        repeat = Integer.parseInt(ps[1]);        
    
        Integer[] values = new Integer[len];
        Arrays.fill(values, 777);
        
        Observable<Integer> source = Observable.from(values);
        
        Observable<Integer> error = source.concatWith(Observable.<Integer>error(new RuntimeException()));
        
        Integer[] values2 = new Integer[len * repeat];
        Arrays.fill(values2, 777);
        
        baseline = Observable.from(values2); 
        
        sourceRepeating = source.repeat(repeat);
        
        sourceRetrying = error.retry(repeat);
        
        redoRepeating = source.repeatWhen((Func1)UtilityFunctions.identity()).take(len * repeat);

        redoRetrying = error.retryWhen((Func1)UtilityFunctions.identity()).take(len * repeat);
    }
    
    @Benchmark
    public void baseline(Blackhole bh) {
        baseline.subscribe(new LatchedObserver<Integer>(bh));
    }
    
    @Benchmark
    public void repeatCounted(Blackhole bh) {
        sourceRepeating.subscribe(new LatchedObserver<Integer>(bh));
    }
    
    @Benchmark
    public void retryCounted(Blackhole bh) {
        sourceRetrying.subscribe(new LatchedObserver<Integer>(bh));
    }
    
    @Benchmark
    public void repeatWhen(Blackhole bh) {
        redoRepeating.subscribe(new LatchedObserver<Integer>(bh));
    }
    
    @Benchmark
    public void retryWhen(Blackhole bh) {
        redoRetrying.subscribe(new LatchedObserver<Integer>(bh));
    }
}
