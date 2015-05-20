package rx.operators;
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

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import rx.Observable;
import rx.jmh.LatchedObserver;
import rx.schedulers.Schedulers;

/**
 * Benchmark typical atomic operations on volatile fields and AtomicXYZ classes.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*OperatorPublishPerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*OperatorPublishPerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class OperatorObserveOnBatchPerf {
    @Param({"1", "10", "100", "1000", "10000", "100000", "1000000"})
    int size;
    @Param({"0", "12", "25", "37", "50", "62", "75", "87"})
    int batch;
    Observable<Integer> source;
    @Setup
    public void setup() {
        System.setProperty("rx.observe-on.request-batch-percent", "" + batch);
        source = Observable.range(0, size)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation());
    }
    @Benchmark
    public void run(Blackhole bh) throws InterruptedException {
        LatchedObserver<Integer> lo = new LatchedObserver<Integer>(bh);
        source.subscribe(lo);
        lo.latch.await();
    }
}
