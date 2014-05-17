package rx.archive.operators;

import rx.Observable;
import rx.archive.perf.AbstractPerformanceTester;
import rx.archive.perf.IntegerSumObserver;
import rx.functions.Action0;

public class OperatorTakePerformance extends AbstractPerformanceTester {

    OperatorTakePerformance() {
        super(REPETITIONS);
    }
    
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
     * Run: 10 - 7,936,306 ops/sec
     * Run: 11 - 8,220,209 ops/sec
     * Run: 12 - 7,783,276 ops/sec
     * Run: 13 - 8,435,373 ops/sec
     * Run: 14 - 7,894,454 ops/sec
     * 
     * ... after v0.17 work:
     * 
     * Run: 10 - 9,524,154 ops/sec
     * Run: 11 - 9,527,330 ops/sec
     * Run: 12 - 9,510,006 ops/sec
     * Run: 13 - 9,543,714 ops/sec
     * Run: 14 - 9,467,383 ops/sec
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
