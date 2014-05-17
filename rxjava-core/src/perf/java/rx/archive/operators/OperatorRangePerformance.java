package rx.archive.operators;

import rx.Observable;
import rx.archive.perf.AbstractPerformanceTester;
import rx.archive.perf.IntegerSumObserver;
import rx.functions.Action0;

public class OperatorRangePerformance extends AbstractPerformanceTester {

    static int reps = Integer.MAX_VALUE / 8;

    OperatorRangePerformance() {
        super(reps);
    }

    public static void main(String args[]) {

        final OperatorRangePerformance spt = new OperatorRangePerformance();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    spt.timeRange();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 
     * -- 0.17
     * 
     * Run: 10 - 271,147,198 ops/sec
     * Run: 11 - 274,821,481 ops/sec
     * Run: 12 - 271,632,295 ops/sec
     * Run: 13 - 277,876,014 ops/sec
     * Run: 14 - 274,821,763 ops/sec
     * 
     * -- 0.16.1
     * 
     * Run: 10 - 222,104,280 ops/sec
     * Run: 11 - 224,311,761 ops/sec
     * Run: 12 - 222,999,339 ops/sec
     * Run: 13 - 222,344,174 ops/sec
     * Run: 14 - 225,247,983 ops/sec
     * 
     * @return
     */
    public long timeRange() {
        IntegerSumObserver o = new IntegerSumObserver();
        Observable.range(1, reps).subscribe(o);
        return o.sum;
    }

}
