/**
 * Copyright 2013 Netflix, Inc.
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
package rx.schedulers;

import java.util.Arrays;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.util.functions.Action0;

public class SchedulerPerformanceTests {

    private static final int REPETITIONS = 5 * 1000 * 1000;
    private static final int NUM_PRODUCERS = 1;

    public static void main(String args[]) {

        final SchedulerPerformanceTests spt = new SchedulerPerformanceTests();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    //                    spt.singleResponse(Schedulers.immediate());
                    //                    spt.singleResponse(Schedulers.currentThread());
                    //                    spt.singleResponse(Schedulers.threadPoolForComputation());

                    spt.arrayResponse(Schedulers.immediate());
                    //                    spt.arrayResponse(Schedulers.currentThread());
                    //                    spt.arrayResponse(Schedulers.threadPoolForComputation());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void runTest(Action0 action) throws InterruptedException {
        for (int runNum = 0; runNum < 15; runNum++) {
            System.gc();
            Thread.sleep(1000L);

            final long start = System.nanoTime();

            action.call();

            long duration = System.nanoTime() - start;
            long opsPerSec = (REPETITIONS * NUM_PRODUCERS * 1000L * 1000L * 1000L) / duration;
            System.out.printf("Run: %d - %,d ops/sec \n",
                    Integer.valueOf(runNum),
                    Long.valueOf(opsPerSec));
        }
    }

    /**
     * Baseline ops/second without a subject.
     * 
     * Perf along this order of magnitude:
     * 
     * Run: 10 - 316,235,532 ops/sec
     * Run: 11 - 301,886,792 ops/sec
     * Run: 12 - 310,472,228 ops/sec
     * Run: 13 - 313,469,797 ops/sec
     * Run: 14 - 305,380,809 ops/sec
     */
    public long baseline() {
        LongObserver o = new LongObserver();
        for (long l = 0; l < REPETITIONS; l++) {
            o.onNext(l);
        }
        o.onCompleted();
        return o.sum;
    }

    /**
     * Observable.from(Arrays.asList(1L), scheduler);
     * 
     * --- Schedulers.immediate() ---
     * 
     * Run: 10 - 4,113,672 ops/sec
     * Run: 11 - 4,068,351 ops/sec
     * Run: 12 - 4,070,318 ops/sec
     * Run: 13 - 4,161,793 ops/sec
     * Run: 14 - 4,156,725 ops/sec
     * 
     * --- Schedulers.currentThread() ---
     * 
     * Run: 10 - 1,692,286 ops/sec
     * Run: 11 - 1,765,054 ops/sec
     * Run: 12 - 1,763,100 ops/sec
     * Run: 13 - 1,770,907 ops/sec
     * Run: 14 - 1,732,291 ops/sec
     * 
     * --- Schedulers.computation() ---
     * 
     * Run: 0 - 224,004 ops/sec
     * Run: 1 - 227,101 ops/sec
     * 
     */
    public long singleResponse(Scheduler scheduler) {
        Observable<Long> s = Observable.from(Arrays.asList(1L), scheduler);
        LongObserver o = new LongObserver();

        for (long l = 0; l < REPETITIONS; l++) {
            s.subscribe(o);
        }
        return o.sum;
    }

    /**
     * Observable.from(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L), scheduler);
     * 
     * --- Schedulers.immediate() ---
     * 
     * Run: 0 - 1,849,947 ops/sec
     * Run: 1 - 2,076,067 ops/sec
     * Run: 2 - 2,114,688 ops/sec
     * Run: 3 - 2,114,301 ops/sec
     * Run: 4 - 2,102,543 ops/sec
     * 
     * --- Schedulers.currentThread() ---
     * 
     * Run: 0 - 548,862 ops/sec
     * Run: 1 - 559,955 ops/sec
     * Run: 2 - 581,412 ops/sec
     * Run: 3 - 562,187 ops/sec
     * Run: 4 - 565,723 ops/sec
     * 
     */
    public long arrayResponse(Scheduler scheduler) {
        Observable<Long> s = Observable.from(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L), scheduler);
        LongObserver o = new LongObserver();

        for (long l = 0; l < REPETITIONS; l++) {
            s.subscribe(o);
        }
        return o.sum;
    }

    private static class LongObserver implements Observer<Long> {

        long sum = 0;

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            throw new RuntimeException(e);
        }

        @Override
        public void onNext(Long l) {
            sum += l;
        }
    }

}
