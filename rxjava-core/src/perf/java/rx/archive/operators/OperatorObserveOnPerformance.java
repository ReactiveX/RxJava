package rx.archive.operators;

import rx.Observable;
import rx.archive.perf.AbstractPerformanceTester;
import rx.archive.perf.IntegerSumObserver;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class OperatorObserveOnPerformance extends AbstractPerformanceTester {

    private static long reps = 10000;

    OperatorObserveOnPerformance() {
        super(reps);
    }

    public static void main(String args[]) {

        final OperatorObserveOnPerformance spt = new OperatorObserveOnPerformance();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    spt.timeObserveOn();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Observable.from(1L).observeOn() using ImmediateScheduler
     * 
     * NOTE: Variance is high enough that there is obviously something else that needs to be done to make these benchmarks trustworthy.
     * 
     * --- version 0.17.1
     * 
     * Run: 10 - 69,444,444 ops/sec
     * Run: 11 - 38,167,938 ops/sec
     * Run: 12 - 41,841,004 ops/sec
     * Run: 13 - 113,636,363 ops/sec
     * Run: 14 - 38,759,689 ops/sec
     * 
     * --- version 0.16.1
     * 
     * Run: 10 - 42,735,042 ops/sec
     * Run: 11 - 40,160,642 ops/sec
     * Run: 12 - 63,694,267 ops/sec
     * Run: 13 - 75,757,575 ops/sec
     * Run: 14 - 43,290,043 ops/sec
     * 
     */
    public long timeObserveOn() {
        Observable<Integer> s = Observable.range(1, (int) reps).observeOn(Schedulers.immediate());
        IntegerSumObserver o = new IntegerSumObserver();
        s.subscribe(o);
        return o.sum;
    }

}