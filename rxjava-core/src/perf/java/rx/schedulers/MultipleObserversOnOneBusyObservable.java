/**
 * Copyright 2014 Netflix, Inc.
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
package rx.schedulers;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MultipleObserversOnOneBusyObservable {

    @GenerateMicroBenchmark
    public void runAll(final Input input) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(input.numObservers);
        Observable<Integer> smallObservable = Observable.from(Arrays.asList(1, 2, 3, 4, 5));
        Observable<Integer> bigObservable = Observable.from(input.numbers);
        for(int o=0; o<input.numObservers; o++) {
            final int foo = o;
            Observable<Integer> theObservable = smallObservable;
            if(((o+1) % 4) != 0) {
                theObservable = bigObservable;
            }
            theObservable
                    .observeOn(input.schedulers[input.schedulerIndex])
                    .subscribe(new Observer<Integer>() {
                        @Override
                        public void onCompleted() {
                            latch.countDown();
                        }

                        @Override
                        public void onError(Throwable e) {
                            throw new RuntimeException(e);
                        }

                        @Override
                        public void onNext(Integer integer) {
                            input.blackHole.consume(integer*integer);
                        }
                    });
        }
        latch.await();
    }

    @State(Scope.Benchmark)
    public static class Input {
        @Param({ "50", "500", "5000" })
        public int NUM_INTEGERS;
        private final int NUM_CORES = Runtime.getRuntime().availableProcessors();
        private int numObservers;
        private Scheduler[] schedulers;
        private List<Integer> numbers;
        private BlackHole blackHole = new BlackHole();

        @Param({ "0", "1", "2", "3" })
        public int schedulerIndex;

        @Setup
        public void setup() {
            schedulers = new Scheduler[]{new EventLoopsScheduler(),
                new BalancingEventLoopScheduler(), new ReroutingEventLoopScheduler(),
                PinningEventLoopScheduler.INSTANCE};
            numObservers = (int)(NUM_CORES * 10);
            numbers = new ArrayList<Integer>(NUM_INTEGERS);
            for(int n=0; n<NUM_INTEGERS; n++)
                numbers.add((int)(Math.random()*1000.0));
        }

    }

}