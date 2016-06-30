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
import rx.functions.Func2;
import rx.jmh.LatchedObserver;
import rx.schedulers.Schedulers;

/**
 * Benchmark the Zip operator.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*ZipPerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*ZipPerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class ZipPerf {
    
    @Param({"1", "1000", "1000000"})
    public int firstLen;
    @Param({"1", "1000", "1000000"})
    public int secondLen;
    
    Observable<Integer> baseline;
    
    Observable<Integer> bothSync;
    Observable<Integer> firstSync;
    Observable<Integer> secondSync;
    Observable<Integer> bothAsync;
    
    boolean small;
    
    @Setup
    public void setup() {
        Integer[] array1 = new Integer[firstLen];
        Arrays.fill(array1, 777);
        Integer[] array2 = new Integer[secondLen];
        Arrays.fill(array2, 777);
        
        baseline = Observable.from(firstLen < secondLen? array2 : array1);
    
        Observable<Integer> o1 = Observable.from(array1);
        
        Observable<Integer> o2 = Observable.from(array2);
        
        Func2<Integer, Integer, Integer> plus = new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer a, Integer b) {
                return a + b;
            }
        };
        
        bothSync = Observable.zip(o1, o2, plus);

        firstSync = Observable.zip(o1, o2.subscribeOn(Schedulers.computation()), plus);

        secondSync = Observable.zip(o1.subscribeOn(Schedulers.computation()), o2, plus);

        bothAsync = Observable.zip(o1.subscribeOn(Schedulers.computation()), o2.subscribeOn(Schedulers.computation()), plus);
    
        small = Math.min(firstLen, secondLen) < 100;
    }
    
    @Benchmark
    public void baseline(Blackhole bh) {
        baseline.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void syncSync(Blackhole bh) {
        bothSync.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void syncAsync(Blackhole bh) throws Exception {
        LatchedObserver<Integer> o = new LatchedObserver<Integer>(bh);
        firstSync.subscribe(o);
        
        if (small) {
            while (o.latch.getCount() != 0);
        } else {
            o.latch.await();
        }
    }

    @Benchmark
    public void asyncSync(Blackhole bh) throws Exception {
        LatchedObserver<Integer> o = new LatchedObserver<Integer>(bh);
        secondSync.subscribe(o);
        
        if (small) {
            while (o.latch.getCount() != 0);
        } else {
            o.latch.await();
        }
    }

    @Benchmark
    public void asyncAsync(Blackhole bh) throws Exception {
        LatchedObserver<Integer> o = new LatchedObserver<Integer>(bh);
        bothAsync.subscribe(o);
        
        if (small) {
            while (o.latch.getCount() != 0);
        } else {
            o.latch.await();
        }
    }

}
