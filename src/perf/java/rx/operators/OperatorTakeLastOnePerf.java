/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.operators;

import org.openjdk.jmh.annotations.*;

import rx.Observable;
import rx.internal.operators.*;
import rx.jmh.InputWithIncrementingInteger;

public class OperatorTakeLastOnePerf {

    private static final OperatorTakeLast<Integer> TAKE_LAST = new OperatorTakeLast<Integer>(1);

    @State(Scope.Thread)
    public static class Input extends InputWithIncrementingInteger {

        @Param({ "5", "100", "1000000" })
        public int size;

        @Override
        public int getSize() {
            return size;
        }

    }

    @Benchmark
    public void takeLastOneUsingTakeLast(Input input) {
       input.observable.lift(TAKE_LAST).subscribe(input.observer);
    }

    @Benchmark
    public void takeLastOneUsingTakeLastOne(Input input) {
       Observable.unsafeCreate(new OnSubscribeTakeLastOne<Integer>(input.observable)).subscribe(input.observer);
    }

}
