package rx.operators;

import rx.Observable;
import rx.functions.Action0;
import rx.perf.AbstractPerformanceTester;
import rx.perf.IntegerSumObserver;
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
                    spt.timeObserveOnUnbounded();
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
    public long timeObserveOnUnbounded() {
        Observable<Integer> s = Observable.range(1, (int) reps).observeOn(Schedulers.immediate());
        IntegerSumObserver o = new IntegerSumObserver();
        s.subscribe(o);
        return o.sum;
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
     */
    public long timeObserveOnBounded() {
        Observable<Integer> s = Observable.range(1, (int) reps).lift(new OperatorObserveOnBounded<Integer>(Schedulers.newThread(), 16));
        IntegerSumObserver o = new IntegerSumObserver();
        s.subscribe(o);
        return o.sum;
    }

}