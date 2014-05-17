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

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;

import rx.Observable.Operator;
import rx.functions.Func1;
import rx.jmh.InputWithIncrementingInteger;

public class OperatorMapPerf {

    @GenerateMicroBenchmark
    public void mapIdentityFunction(InputWithIncrementingInteger input) throws InterruptedException {
        input.observable.lift(MAP_OPERATOR).subscribe(input.observer);
        input.awaitCompletion();
    }

    private static final Func1<Integer, Integer> IDENTITY_FUNCTION = new Func1<Integer, Integer>() {
        @Override
        public Integer call(Integer value) {
            return value;
        }
    };

    private static final Operator<Integer, Integer> MAP_OPERATOR = new OperatorMap<Integer, Integer>(IDENTITY_FUNCTION);

}
