package rx.operators;

import rx.Observable;
import rx.perf.AbstractPerformanceTester;
import rx.perf.LongSumObserver;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

public class OperatorMapPerformance extends AbstractPerformanceTester {

    public static void main(String args[]) {

        final OperatorMapPerformance spt = new OperatorMapPerformance();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    spt.timeMapPlusOne();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Observable.from(1L).map((l) -> { l+1})
     * 
     * Run: 10 - 11,375,632 ops/sec
     * Run: 11 - 11,390,325 ops/sec
     * Run: 12 - 11,655,527 ops/sec
     * Run: 13 - 11,528,440 ops/sec
     * Run: 14 - 11,321,181 ops/sec
     */
    public long timeMapPlusOne() {

        Observable<Long> s = Observable.from(1L).map(new Func1<Long, Long>() {

            @Override
            public Long call(Long l) {
                return l + 1;
            }

        });
        LongSumObserver o = new LongSumObserver();

        for (long l = 0; l < REPETITIONS; l++) {
            s.subscribe(o);
        }
        return o.sum;
    }

}