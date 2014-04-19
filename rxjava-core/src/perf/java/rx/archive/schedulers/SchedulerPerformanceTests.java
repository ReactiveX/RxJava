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
package rx.archive.schedulers;

import java.util.Arrays;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class SchedulerPerformanceTests {

    private static final int REPETITIONS = 5 * 1000 * 1000;
    private static final int NUM_PRODUCERS = 1;

    public static void main(String args[]) {

        final SchedulerPerformanceTests spt = new SchedulerPerformanceTests();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    spt.singleResponse(Schedulers.computation());
                    //                    spt.singleResponse(Schedulers.currentThread());
                    //                    spt.singleResponse(Schedulers.computation());

                    //                    spt.arrayResponse(Schedulers.immediate());
                    //                                        spt.arrayResponse(Schedulers.currentThread());
                    //                    spt.arrayResponse(Schedulers.computation());
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
     * Run: 10 - 14,973,870 ops/sec
     * Run: 11 - 15,345,142 ops/sec
     * Run: 12 - 14,962,533 ops/sec
     * Run: 13 - 14,793,030 ops/sec
     * Run: 14 - 15,177,685 ops/sec
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
     * Run: 10 - 12,616,099 ops/sec
     * Run: 11 - 12,661,625 ops/sec
     * Run: 12 - 12,775,536 ops/sec
     * Run: 13 - 12,711,358 ops/sec
     * Run: 14 - 12,815,452 ops/sec
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
     * Run: 10 - 9,805,017 ops/sec
     * Run: 11 - 9,880,427 ops/sec
     * Run: 12 - 9,615,809 ops/sec
     * Run: 13 - 10,920,297 ops/sec
     * Run: 14 - 10,822,721 ops/sec
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

    private static class LongObserver extends Subscriber<Long> {

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
