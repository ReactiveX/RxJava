/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.groovy

import org.junit.Test

import rx.Observable
import rx.Scheduler
import rx.schedulers.Schedulers
import rx.functions.Func1

class TestParallel {

    @Test
    public void testParallelOperator() {
        Observable.range(0, 100)
                .parallel({
                    it.map({ return it; })
                })
                .toBlockingObservable()
                .forEach({ println("T: " + it + " Thread: " + Thread.currentThread());  });
    }
}
