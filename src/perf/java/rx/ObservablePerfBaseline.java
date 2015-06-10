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
package rx;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import rx.jmh.InputWithIncrementingInteger;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class ObservablePerfBaseline {

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
    public void observableConsumption(Input input) throws InterruptedException {
        input.firehose.subscribe(input.observer);
    }

    @Benchmark
    public void observableViaRange(Input input) throws InterruptedException {
        input.observable.subscribe(input.observer);
    }
    
    @Benchmark
    public void observableConsumptionUnsafe(Input input) throws InterruptedException {
        input.firehose.unsafeSubscribe(input.newSubscriber());
    }

    @Benchmark
    public void observableViaRangeUnsafe(Input input) throws InterruptedException {
        input.observable.unsafeSubscribe(input.newSubscriber());
    }

    @Benchmark
    public void iterableViaForLoopConsumption(Input input) throws InterruptedException {
        for (int i : input.iterable) {
            input.observer.onNext(i);
        }
    }

    @Benchmark
    public void iterableViaHasNextConsumption(Input input) throws InterruptedException {
        Iterator<Integer> iterator = input.iterable.iterator();
        while (iterator.hasNext()) {
            input.observer.onNext(iterator.next());
        }
    }
}
