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

import rx.Observable.Operator;
import rx.functions.Func1;
import rx.internal.operators.OperatorMap;
import rx.jmh.InputWithIncrementingInteger;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class OperatorMapPerf {

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
    public void mapPassThruViaLift(Input input) throws InterruptedException {
        input.observable.lift(MAP_OPERATOR).subscribe(input.observer);
    }

    @Benchmark
    public void mapPassThru(Input input) throws InterruptedException {
        input.observable.map(IDENTITY_FUNCTION).subscribe(input.observer);
    }

    private static final Func1<Integer, Integer> IDENTITY_FUNCTION = new Func1<Integer, Integer>() {
        @Override
        public Integer call(Integer value) {
            return value;
        }
    };

    private static final Operator<Integer, Integer> MAP_OPERATOR = new OperatorMap<Integer, Integer>(IDENTITY_FUNCTION);

}
