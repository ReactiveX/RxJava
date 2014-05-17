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
package rx.archive.performance;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

public class PerformanceTest {

    /*
     * Example run:
     * 
     * compositionTestTotalTime: 2432
     * nonCompositionalTestWithDirectLoopTotalTime: 2043
     * nonCompositionalTestWithArrayOfFunctionsTotalTime: 1925
     * 
     * compositionTestTotalTime: 2362
     * nonCompositionalTestWithDirectLoopTotalTime: 1910
     * nonCompositionalTestWithArrayOfFunctionsTotalTime: 1823
     * 
     * compositionTestTotalTime: 2456
     * nonCompositionalTestWithDirectLoopTotalTime: 2004
     * nonCompositionalTestWithArrayOfFunctionsTotalTime: 2014
     */

    /*
     * >>> Statically typed <<<
     * 
     * Without chaining:
     * 
     * Sum: 710082754 Time: 130.683ms
     * runNonCompositionalTestWithDirectLoop
     * Sum: 710082754 Time: 21.011ms
     * runNonCompositionalTestWithArrayOfFunctions
     * Sum: 710082754 Time: 20.84ms
     * 
     * 
     * With chaining (composition collapsing):
     * 
     * Sum: 710082754 Time: 28.787ms
     * runNonCompositionalTestWithDirectLoop
     * Sum: 710082754 Time: 19.525ms
     * runNonCompositionalTestWithArrayOfFunctions
     * Sum: 710082754 Time: 19.175ms
     * 
     * 
     * >>> Dynamically typed <<<
     * 
     * When going via generic Functions.execute even with chained sequence:
     * 
     * runCompositionTest
     * Sum: 710082754 Time: 577.3ms <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< very bad when dynamic
     * runNonCompositionalTestWithDirectLoop
     * Sum: 710082754 Time: 31.591ms
     * runNonCompositionalTestWithArrayOfFunctions
     * Sum: 710082754 Time: 38.093ms
     * runCompositionTest
     * 
     * 
     * With Function memoization so we only pay dynamic price during sequence setup, not in onNext:
     * 
     * 
     * Using ArrayList
     * 
     * runCompositionTest
     * Sum: 710082754 Time: 27.078ms
     * runNonCompositionalTestWithDirectLoop
     * Sum: 710082754 Time: 18.911ms
     * runNonCompositionalTestWithArrayOfFunctions
     * Sum: 710082754 Time: 18.852ms
     * 
     * 
     * Using LinkedBlockingQueue
     * 
     * runCompositionTest
     * Sum: 710082754 Time: 46.532ms
     * runNonCompositionalTestWithDirectLoop
     * Sum: 710082754 Time: 18.946ms
     * runNonCompositionalTestWithArrayOfFunctions
     * Sum: 710082754 Time: 18.746ms
     */

    public static void main(String[] args) {
        PerformanceTest test = new PerformanceTest();
        Integer[] values = new Integer[100001];
        for (int i = 0; i < values.length; i++) {
            values[i] = i;
        }

        AtomicLong compositionTestTotalTime = new AtomicLong();
        AtomicLong nonCompositionalTestWithDirectLoopTotalTime = new AtomicLong();
        AtomicLong nonCompositionalTestWithArrayOfFunctionsTotalTime = new AtomicLong();

        for (int i = 0; i < 100; i++) {
            System.out.println("-------------------------------");
            //            test.runCompositionTestWithMultipleOperations(values);
            test.runCompositionTest(compositionTestTotalTime, values);
            test.runNonCompositionalTestWithDirectLoop(nonCompositionalTestWithDirectLoopTotalTime, values);
            test.runNonCompositionalTestWithArrayOfFunctions(nonCompositionalTestWithArrayOfFunctionsTotalTime, values);
        }

        System.out.println("-------------------------------");
        System.out.println("compositionTestTotalTime: " + compositionTestTotalTime.get());
        System.out.println("nonCompositionalTestWithDirectLoopTotalTime: " + nonCompositionalTestWithDirectLoopTotalTime.get());
        System.out.println("nonCompositionalTestWithArrayOfFunctionsTotalTime: " + nonCompositionalTestWithArrayOfFunctionsTotalTime.get());
    }

    public void runCompositionTestWithMultipleOperations(AtomicLong aggregateTime, Integer[] values) {
        System.out.println("runCompositionTestWithMultipleOperations");

        // old code before memoizing
        // Count: 200002 Time: 403.095ms

        // new code with memoizing but no chaining
        // Count: 200002 Time: 103.128ms

        final AtomicInteger onNextSum = new AtomicInteger(0);
        final long start = System.nanoTime();

        MathFunction m = new MathFunction();

        Observable<Integer> a = Observable.from(values)
                .map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m)
                .map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m);

        final Observable<Integer> b = Observable.from(values)
                .map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m)
                .map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m);

        Observable.merge(a, b).filter(new Func1<Integer, Boolean>() {

            @Override
            public Boolean call(Integer t1) {
                return t1 > 10;
            }

        }).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer t1) {
                return t1 + "-value-from-b";
            }
        }).take(1000000).subscribe(new TestStringObserver(onNextSum, start));

    }

    public void runCompositionTest(AtomicLong aggregateTime, Integer[] values) {
        System.out.println("runCompositionTest");

        final AtomicInteger onNextSum = new AtomicInteger(0);
        final long start = System.nanoTime();

        MathFunction m = new MathFunction();
        // 50 levels of composition (same function so that's not the cost)
        Observable.from(values)
                .map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m)
                .map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m)
                .map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m)
                .map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m)
                .map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m).map(m)
                .subscribe(new TestObserver(onNextSum, start, aggregateTime));
    }

    public void runNonCompositionalTestWithDirectLoop(AtomicLong aggregateTime, Integer[] values) {
        System.out.println("runNonCompositionalTestWithDirectLoop");

        final AtomicInteger onNextSum = new AtomicInteger(0);
        final long start = System.nanoTime();
        final MathFunction m = new MathFunction();

        Observable.from(values).map(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                // iterate the 50 times here in a loop rather than via composition
                for (int i = 0; i < 50; i++) {
                    t1 = m.call(t1);
                }
                return t1;
            }

        }).subscribe(new TestObserver(onNextSum, start, aggregateTime));

    }

    public void runNonCompositionalTestWithArrayOfFunctions(AtomicLong aggregateTime, Integer[] values) {
        System.out.println("runNonCompositionalTestWithArrayOfFunctions");

        final AtomicInteger onNextSum = new AtomicInteger(0);
        final long start = System.nanoTime();
        final MathFunction m = new MathFunction();
        final Func1<Integer, Integer>[] functionCalls = new MathFunction[50];
        for (int i = 0; i < 50; i++) {
            functionCalls[i] = m;
        }

        Observable.from(values).map(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                // iterate the 50 times here in a loop rather than via composition
                for (Func1<Integer, Integer> f : functionCalls) {
                    t1 = f.call(t1);
                }
                return t1;
            }

        }).subscribe(new TestObserver(onNextSum, start, aggregateTime));

    }

    private static final class TestObserver extends Subscriber<Integer> {
        private final AtomicInteger onNextSum;
        private final AtomicLong aggregateTime;
        private final long start;

        private TestObserver(AtomicInteger onNextSum, long start, AtomicLong aggregateTime) {
            this.onNextSum = onNextSum;
            this.start = start;
            this.aggregateTime = aggregateTime;
        }

        @Override
        public void onNext(Integer i) {
            onNextSum.addAndGet(i);
        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
        }

        @Override
        public void onCompleted() {
            long end = System.nanoTime();
            double timeInMilliseconds = ((double) (end - start)) / 1000 / 1000;
            aggregateTime.addAndGet(Math.round(timeInMilliseconds));
            System.out.println("Sum: " + onNextSum.get() + " Time: " + timeInMilliseconds + "ms");
        }
    }

    private static final class TestStringObserver extends Subscriber<String> {
        private final AtomicInteger onNextSum;
        private final long start;

        private TestStringObserver(AtomicInteger onNextSum, long start) {
            this.onNextSum = onNextSum;
            this.start = start;
        }

        @Override
        public void onNext(String i) {
            //            System.out.println(i);
            onNextSum.incrementAndGet();
        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
        }

        @Override
        public void onCompleted() {
            long end = System.nanoTime();
            System.out.println("Count: " + onNextSum.get() + " Time: " + ((double) (end - start)) / 1000 / 1000 + "ms");
        }
    }

    private static class MathFunction implements Func1<Integer, Integer> {

        @Override
        public Integer call(Integer t1) {
            return t1 + 1;
        }

    }

}
