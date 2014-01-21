package rx.operators;

import rx.Observable;
import rx.perf.AbstractPerformanceTester;
import rx.perf.IntegerSumObserver;
import rx.util.functions.Action0;

public class OperatorTakePerformance extends AbstractPerformanceTester {

    public static void main(String args[]) {

        final OperatorTakePerformance spt = new OperatorTakePerformance();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    spt.timeTake5();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Observable.range(0, 10).take(5);
     * 
     * Run: 10 - 3,951,557 ops/sec
     * Run: 11 - 3,981,329 ops/sec
     * Run: 12 - 3,988,949 ops/sec
     * Run: 13 - 3,925,971 ops/sec
     * Run: 14 - 4,033,468 ops/sec
     */
    public long timeTake5() {

        Observable<Integer> s = Observable.range(0, 10).take(5);

        IntegerSumObserver o = new IntegerSumObserver();

        for (long l = 0; l < REPETITIONS; l++) {
            s.subscribe(o);
        }
        return o.sum;
    }

}
