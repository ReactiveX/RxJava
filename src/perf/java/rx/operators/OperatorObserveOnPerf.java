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

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import rx.jmh.InputWithIncrementingInteger;
import rx.jmh.LatchedObserver;
import rx.schedulers.Schedulers;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class OperatorObserveOnPerf {

    @State(Scope.Thread)
    public static class Input extends InputWithIncrementingInteger {

        @Param({ "1", "1000", "1000000" })
        public int size;

        @Override
        public int getSize() {
            return size;
        }

    }
    
    @Benchmark
    public void observeOnComputation(Input input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        input.observable.observeOn(Schedulers.computation()).subscribe(o);
        o.latch.await();
    }

    @Benchmark
    public void observeOnNewThread(Input input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        input.observable.observeOn(Schedulers.newThread()).subscribe(o);
        o.latch.await();
    }

    @Benchmark
    public void observeOnImmediate(Input input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        input.observable.observeOn(Schedulers.immediate()).subscribe(o);
        o.latch.await();
    }
    
    @Benchmark
    public void observeOnComputationSubscribedOnComputation(Input input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        input.observable.subscribeOn(Schedulers.computation()).observeOn(Schedulers.computation()).subscribe(o);
        o.latch.await();
    }

    @Benchmark
    public void observeOnNewThreadSubscribedOnComputation(Input input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        input.observable.subscribeOn(Schedulers.computation()).observeOn(Schedulers.newThread()).subscribe(o);
        o.latch.await();
    }

    @Benchmark
    public void observeOnImmediateSubscribedOnComputation(Input input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        input.observable.subscribeOn(Schedulers.computation()).observeOn(Schedulers.immediate()).subscribe(o);
        o.latch.await();
    }

}
