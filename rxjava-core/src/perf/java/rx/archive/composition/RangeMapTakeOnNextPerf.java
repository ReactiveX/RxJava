package rx.archive.composition;

import rx.Observable;
import rx.archive.perf.AbstractPerformanceTester;
import rx.archive.perf.IntegerSumObserver;
import rx.functions.Action0;
import rx.functions.Func1;

public class RangeMapTakeOnNextPerf extends AbstractPerformanceTester {

    final static int NUM = 10;
    final static long REPS = REPETITIONS / NUM;

    RangeMapTakeOnNextPerf() {
        super(REPS);
    }

    public static void main(String args[]) {

        final RangeMapTakeOnNextPerf spt = new RangeMapTakeOnNextPerf();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    spt.test();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 
     * With 'lift' calling the `f` function directly:
     * 
     * Run: 10 - 11,152,996 ops/sec
     * Run: 11 - 9,791,825 ops/sec
     * Run: 12 - 10,080,035 ops/sec
     * Run: 13 - 10,189,525 ops/sec
     * Run: 14 - 10,145,486 ops/sec
     * 
     * With `lift` calling `subscribe`:
     * 
     * Run: 10 - 5,592,153 ops/sec
     * Run: 11 - 5,881,799 ops/sec
     * Run: 12 - 5,853,430 ops/sec
     * Run: 13 - 5,902,769 ops/sec
     * Run: 14 - 5,907,721 ops/sec
     */
    public long test() {

        Observable<Integer> s = Observable.range(0, 100).map(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer l) {
                return l + 1;
            }

        }).take(NUM);
        IntegerSumObserver o = new IntegerSumObserver();

        for (long l = 0; l < REPS; l++) {
            s.subscribe(o);
        }
        return o.sum;
    }

}