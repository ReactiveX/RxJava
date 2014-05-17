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
package rx.archive.subjects;

import rx.Subscriber;
import rx.functions.Action0;
import rx.subjects.ReplaySubject;

public class SubjectPerformanceTests {

    private static final int REPETITIONS = 10 * 1000 * 1000;
    private static final int NUM_PRODUCERS = 1;

    public static void main(String args[]) {

        final SubjectPerformanceTests spt = new SubjectPerformanceTests();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    //                    spt.baseline();
                    spt.unboundedReplaySubject();
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
     * ReplaySubject
     * 
     * This is testing pass-thru, not replay speed (though it will be storing all of the history).
     * 
     * ArrayList with raw values & synchronized access
     * 
     * Run: 10 - 11,993,341 ops/sec
     * Run: 11 - 11,719,523 ops/sec
     * Run: 12 - 11,965,214 ops/sec
     * Run: 13 - 11,814,730 ops/sec
     * Run: 14 - 11,947,459 ops/sec
     * 
     * Custom linked list + Notification<T> (failed experiment)
     * 
     * Run: 10 - 2,102,785 ops/sec
     * Run: 11 - 2,109,057 ops/sec
     * Run: 12 - 2,094,372 ops/sec
     * Run: 13 - 2,096,183 ops/sec
     * Run: 14 - 2,107,019 ops/sec
     * 
     * Custom linked list + raw values (elegant code ... but another failed experiment)
     * 
     * Run: 10 - 5,131,634 ops/sec
     * Run: 11 - 5,133,114 ops/sec
     * Run: 12 - 5,080,652 ops/sec
     * Run: 13 - 5,072,743 ops/sec
     * Run: 14 - 5,198,813 ops/sec
     * 
     * ArrayList with raw values & non-blocking (no synchronization)
     * 
     * Run: 10 - 16,069,678 ops/sec
     * Run: 11 - 15,954,688 ops/sec
     * Run: 12 - 16,158,874 ops/sec
     * Run: 13 - 16,209,504 ops/sec
     * Run: 14 - 16,151,174 ops/sec
     * 
     * Map to Arrays and other enhancements from https://github.com/Netflix/RxJava/pull/652
     * 
     * Run: 10 - 54,231,405 ops/sec
     * Run: 11 - 56,239,490 ops/sec
     * Run: 12 - 55,424,384 ops/sec
     * Run: 13 - 56,370,421 ops/sec
     * Run: 14 - 56,617,767 ops/sec
     */
    public long unboundedReplaySubject() {
        ReplaySubject<Long> s = ReplaySubject.create();
        LongObserver o = new LongObserver();
        s.subscribe(o);
        for (long l = 0; l < REPETITIONS; l++) {
            s.onNext(l);
        }
        s.onCompleted();
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
