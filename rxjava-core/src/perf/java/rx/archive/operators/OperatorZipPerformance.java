package rx.archive.operators;

import rx.Observable;
import rx.archive.perf.AbstractPerformanceTester;
import rx.archive.perf.IntegerSumObserver;
import rx.functions.Action0;
import rx.functions.Func2;

public class OperatorZipPerformance extends AbstractPerformanceTester {

    OperatorZipPerformance() {
        super(REPETITIONS);
    }
    
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
     * 
     * ... after v0.17 work (still old implementation of zip):
     * 
     * Run: 10 - 1,202,371 ops/sec
     * Run: 11 - 1,204,806 ops/sec
     * Run: 12 - 1,196,630 ops/sec
     * Run: 13 - 1,206,332 ops/sec
     * Run: 14 - 1,206,169 ops/sec
     * 
     * ... after v0.17 work (new implementation):
     * 
     * Run: 10 - 1,668,248 ops/sec
     * Run: 11 - 1,673,052 ops/sec
     * Run: 12 - 1,672,479 ops/sec
     * Run: 13 - 1,675,018 ops/sec
     * Run: 14 - 1,668,830 ops/sec
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

        int sum = 0;
        for (long l = 0; l < REPETITIONS; l++) {
            IntegerSumObserver so = new IntegerSumObserver();
            s.subscribe(so);
            sum += so.sum;
        }
        return sum;
    }

    /**
     * Observable.zip(range(0, 100), range(100, 200), {a, b -> a+b})
     * 
     * ... really slow ...
     * 
     * Run: 0 - 30,698 ops/sec
     * Run: 1 - 31,061 ops/sec
     * 
     * ... after v0.17 work (still old implementation of zip):
     * 
     * Run: 0 - 40,048 ops/sec
     * Run: 1 - 40,165 ops/sec
     * 
     * ... after v0.17 work (new implementation):
     * 
     * Run: 0 - 63,079 ops/sec
     * Run: 1 - 63,505 ops/sec
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

        int sum = 0;
        for (long l = 0; l < REPETITIONS; l++) {
            IntegerSumObserver so = new IntegerSumObserver();
            s.subscribe(so);
            sum += so.sum;
        }
        return sum;
    }
}