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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import rx.*;
import rx.internal.operators.OnSubscribeFromIterable;
import rx.jmh.LatchedObserver;

/**
 * Benchmark from(Iterable).
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*FromIterablePerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*FromIterablePerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class FromIterablePerf {
    Observable<Integer> from;
    OnSubscribeFromIterable<Integer> direct;
    @Param({"1", "1000", "1000000"})
    public int size;
    
    @Setup
    public void setup() {
        Integer[] array = new Integer[size];
        for (int i = 0; i < size; i++) {
            array[i] = i;
        }
        from = Observable.from(Arrays.asList(array));
        direct = new OnSubscribeFromIterable<Integer>(Arrays.asList(array));
    }
    
    @Benchmark
    public void from(Blackhole bh) {
        from.subscribe(new LatchedObserver<Integer>(bh));
    }
    @Benchmark
    public void fromUnsafe(final Blackhole bh) {
        from.unsafeSubscribe(createSubscriber(bh));
    }
    
    @Benchmark
    public void direct(final Blackhole bh) {
        direct.call(createSubscriber(bh));
    }
    
    Subscriber<Integer> createSubscriber(final Blackhole bh) {
        return new Subscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                bh.consume(t);
            }
            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
            @Override
            public void onCompleted() {
                
            }
        };
    }
}
