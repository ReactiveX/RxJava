package rx.operators;

import rx.Observable;
import rx.perf.AbstractPerformanceTester;
import rx.perf.IntegerSumObserver;
import rx.util.functions.Action0;

public class OperatorMergePerformance extends AbstractPerformanceTester {

    public static void main(String args[]) {

        final OperatorMergePerformance spt = new OperatorMergePerformance();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    //                    spt.timeMergeAandBwithSingleItems();
                    spt.timeMergeAandBwith100Items();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Observable.merge(from(1), from(1))
     * 
     * Run: 10 - 2,308,617 ops/sec
     * Run: 11 - 2,309,602 ops/sec
     * Run: 12 - 2,318,590 ops/sec
     * Run: 13 - 2,270,100 ops/sec
     * Run: 14 - 2,312,006 ops/sec
     * 
     */
    public long timeMergeAandBwithSingleItems() {

        Observable<Integer> sA = Observable.from(1);
        Observable<Integer> sB = Observable.from(2);
        Observable<Integer> s = Observable.merge(sA, sB);

        IntegerSumObserver o = new IntegerSumObserver();

        for (long l = 0; l < REPETITIONS; l++) {
            s.subscribe(o);
        }
        return o.sum;
    }

    /**
     * Observable.merge(range(0, 100), range(100, 200))
     * 
     * Run: 10 - 340,049 ops/sec
     * Run: 11 - 339,059 ops/sec
     * Run: 12 - 348,899 ops/sec
     * Run: 13 - 350,953 ops/sec
     * Run: 14 - 352,228 ops/sec
     */
    public long timeMergeAandBwith100Items() {

        Observable<Integer> sA = Observable.range(0, 100);
        Observable<Integer> sB = Observable.range(100, 200);
        Observable<Integer> s = Observable.merge(sA, sB);

        IntegerSumObserver o = new IntegerSumObserver();

        for (long l = 0; l < REPETITIONS; l++) {
            s.subscribe(o);
        }
        return o.sum;
    }
}