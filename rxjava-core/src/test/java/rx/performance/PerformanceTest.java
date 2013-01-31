package rx.performance;

import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observer;
import rx.util.functions.Func1;

public class PerformanceTest {

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

        for (int i = 0; i < 100; i++) {
            System.out.println("-------------------------------");
//            test.runCompositionTestWithMultipleOperations(values);
            test.runCompositionTest(values);
            test.runNonCompositionalTestWithDirectLoop(values);
            test.runNonCompositionalTestWithArrayOfFunctions(values);
        }
    }

    public void runCompositionTestWithMultipleOperations(Integer[] values) {
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

    public void runCompositionTest(Integer[] values) {
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
                .subscribe(new TestObserver(onNextSum, start));
    }

    public void runNonCompositionalTestWithDirectLoop(Integer[] values) {
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

        }).subscribe(new TestObserver(onNextSum, start));

    }

    public void runNonCompositionalTestWithArrayOfFunctions(Integer[] values) {
        System.out.println("runNonCompositionalTestWithArrayOfFunctions");

        final AtomicInteger onNextSum = new AtomicInteger(0);
        final long start = System.nanoTime();
        final MathFunction m = new MathFunction();
        final Func1[] functionCalls = new Func1<?, ?>[50];
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

        }).subscribe(new TestObserver(onNextSum, start));

    }

    private static final class TestObserver implements Observer<Integer> {
        private final AtomicInteger onNextSum;
        private final long start;

        private TestObserver(AtomicInteger onNextSum, long start) {
            this.onNextSum = onNextSum;
            this.start = start;
        }

        @Override
        public void onNext(Integer i) {
            onNextSum.addAndGet(i);
        }

        @Override
        public void onError(Exception e) {
            e.printStackTrace();
        }

        @Override
        public void onCompleted() {
            long end = System.nanoTime();
            System.out.println("Sum: " + onNextSum.get() + " Time: " + ((double) (end - start)) / 1000 / 1000 + "ms");
        }
    }

    private static final class TestStringObserver implements Observer<String> {
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
        public void onError(Exception e) {
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
