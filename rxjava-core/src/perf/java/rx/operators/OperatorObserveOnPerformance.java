package rx.operators;

import rx.Observable;
import rx.perf.AbstractPerformanceTester;
import rx.perf.IntegerSumObserver;
import rx.schedulers.Schedulers;
import rx.util.functions.Action0;

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
     * Observable.from(1L).observeOn()
     * 
     * --- version 0.17.1 => with queue size == 1
     * 
     * Run: 10 - 115,033 ops/sec
     * Run: 11 - 118,155 ops/sec
     * Run: 12 - 120,526 ops/sec
     * Run: 13 - 115,035 ops/sec
     * Run: 14 - 116,102 ops/sec
     * 
     * --- version 0.17.1 => with queue size == 16
     * 
     * Run: 10 - 850,412 ops/sec
     * Run: 11 - 711,642 ops/sec
     * Run: 12 - 788,332 ops/sec
     * Run: 13 - 1,064,056 ops/sec
     * Run: 14 - 656,857 ops/sec
     * 
     * --- version 0.17.1 => with queue size == 1000000
     * 
     * Run: 10 - 5,162,622 ops/sec
     * Run: 11 - 5,271,481 ops/sec
     * Run: 12 - 4,442,470 ops/sec
     * Run: 13 - 5,149,330 ops/sec
     * Run: 14 - 5,146,680 ops/sec
     * 
     * --- version 0.16.1
     * 
     * Run: 10 - 27,098,802 ops/sec
     * Run: 11 - 24,204,284 ops/sec
     * Run: 12 - 27,208,663 ops/sec
     * Run: 13 - 26,879,552 ops/sec
     * Run: 14 - 26,658,846 ops/sec
     * 
     * 
     */
    public long timeObserveOn() {

        Observable<Integer> s = Observable.range(1, (int) reps).observeOn(Schedulers.newThread(), 16);
        IntegerSumObserver o = new IntegerSumObserver();
        s.subscribe(o);
        return o.sum;
    }

}