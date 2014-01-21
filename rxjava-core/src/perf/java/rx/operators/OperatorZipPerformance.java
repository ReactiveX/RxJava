package rx.operators;

import rx.Observable;
import rx.perf.AbstractPerformanceTester;
import rx.perf.IntegerSumObserver;
import rx.util.functions.Action0;
import rx.util.functions.Func2;

public class OperatorZipPerformance extends AbstractPerformanceTester {

    public static void main(String args[]) {

        final OperatorZipPerformance spt = new OperatorZipPerformance();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    spt.timeZipAandBwithSingleItems();
                    //                    spt.timeZipAandBwith100Items();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Observable.zip(from(1), from(1), {a, b -> a+b})
     * 
     * Run: 10 - 1,419,071 ops/sec
     * Run: 11 - 1,418,867 ops/sec
     * Run: 12 - 1,420,459 ops/sec
     * Run: 13 - 1,409,877 ops/sec
     * Run: 14 - 1,426,019 ops/sec
     */
    public long timeZipAandBwithSingleItems() {

        Observable<Integer> sA = Observable.from(1);
        Observable<Integer> sB = Observable.from(2);
        Observable<Integer> s = Observable.zip(sA, sB, new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 + t2;
            }

        });

        IntegerSumObserver o = new IntegerSumObserver();

        for (long l = 0; l < REPETITIONS; l++) {
            s.subscribe(o);
        }
        return o.sum;
    }

    /**
     * Observable.zip(range(0, 100), range(100, 200), {a, b -> a+b})
     * 
     * ... really slow ...
     * 
     * Run: 0 - 30,698 ops/sec
     * Run: 1 - 31,061 ops/sec
     * 
     */
    public long timeZipAandBwith100Items() {

        Observable<Integer> sA = Observable.range(0, 100);
        Observable<Integer> sB = Observable.range(100, 200);
        Observable<Integer> s = Observable.zip(sA, sB, new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 + t2;
            }

        });

        IntegerSumObserver o = new IntegerSumObserver();

        for (long l = 0; l < REPETITIONS; l++) {
            s.subscribe(o);
        }
        return o.sum;
    }
}