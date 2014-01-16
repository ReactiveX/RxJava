package rx.operators;

import java.util.Arrays;

import rx.Observable;
import rx.perf.AbstractPerformanceTester;
import rx.perf.LongSumObserver;
import rx.util.functions.Action0;

public class OperatorFromIterablePerformance extends AbstractPerformanceTester {

    public static void main(String args[]) {

        final OperatorFromIterablePerformance spt = new OperatorFromIterablePerformance();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    spt.timeTenLongs();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Observable.from(Iterable)
     * 
     * Run: 10 - 10,629,658 ops/sec
     * Run: 11 - 9,573,775 ops/sec
     * Run: 12 - 10,589,787 ops/sec
     * Run: 13 - 10,514,141 ops/sec
     * Run: 14 - 9,765,586 ops/sec
     */
    public long timeTenLongs() {

        Observable<Long> s = Observable.from(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));
        LongSumObserver o = new LongSumObserver();

        for (long l = 0; l < REPETITIONS; l++) {
            s.subscribe(o);
        }
        return o.sum;
    }

}
