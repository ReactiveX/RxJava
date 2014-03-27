package rx.archive.operators;

import rx.Observable;
import rx.archive.perf.AbstractPerformanceTester;
import rx.archive.perf.IntegerSumObserver;
import rx.functions.Action0;
import rx.functions.Func1;

public class OperatorParallelPerformance extends AbstractPerformanceTester {

    private final static int REPS = 10000000;

    OperatorParallelPerformance() {
        super(REPS);
    }

    public static void main(String args[]) {

        final OperatorParallelPerformance spt = new OperatorParallelPerformance();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    spt.parallelSum();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 
     * Run: 10 - 11,220,888 ops/sec
     * Run: 11 - 12,372,424 ops/sec
     * Run: 12 - 11,028,921 ops/sec
     * Run: 13 - 11,813,711 ops/sec
     * Run: 14 - 12,098,364 ops/sec
     * 
     */
    public long parallelSum() {

        Observable<Integer> s = Observable.range(1, REPS).parallel(new Func1<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Observable<Integer> l) {
                return l.map(new Func1<Integer, Integer>() {

                    @Override
                    public Integer call(Integer i) {
                        return i + 1;
                    }

                });
            }

        });
        IntegerSumObserver o = new IntegerSumObserver();

        s.subscribe(o);
        return o.sum;
    }

}