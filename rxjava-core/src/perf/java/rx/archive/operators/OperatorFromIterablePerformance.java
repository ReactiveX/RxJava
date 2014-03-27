package rx.archive.operators;

import java.util.Arrays;
import java.util.Iterator;

import rx.Observable;
import rx.archive.perf.AbstractPerformanceTester;
import rx.archive.perf.IntegerSumObserver;
import rx.archive.perf.LongSumObserver;
import rx.functions.Action0;

public class OperatorFromIterablePerformance extends AbstractPerformanceTester {

    OperatorFromIterablePerformance() {
        super(REPETITIONS);
    }
    
    public static void main(String args[]) {

        final OperatorFromIterablePerformance spt = new OperatorFromIterablePerformance();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    //                    spt.timeRepetitionsEmission();
                    spt.timeTenLongs();
                    //                                        spt.time1000Longs();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Single Observable from an Iterable with REPETITIONS items.
     * 
     * Run: 10 - 210,730,391 ops/sec
     * Run: 11 - 137,608,366 ops/sec
     * Run: 12 - 204,114,957 ops/sec
     * Run: 13 - 217,561,569 ops/sec
     * Run: 14 - 185,061,810 ops/sec
     * 
     * For comparison, if we don't check for isUnsubscribed then we get this:
     * 
     * Run: 10 - 243,546,030 ops/sec
     * Run: 11 - 149,102,403 ops/sec
     * Run: 12 - 250,325,423 ops/sec
     * Run: 13 - 249,289,524 ops/sec
     * Run: 14 - 266,965,668 ops/sec
     * 
     * @return
     */
    public long timeRepetitionsEmission() {
        LongSumObserver o = new LongSumObserver();
        Observable.from(ITERABLE_OF_REPETITIONS).subscribe(o);
        return o.sum;
    }

    /**
     * Observable.from(Iterable)
     * 
     * Run: 0 - 259,801 ops/sec
     * Run: 1 - 263,876 ops/sec
     * Run: 2 - 262,117 ops/sec
     * Run: 3 - 260,515 ops/sec
     * Run: 4 - 261,124 ops/sec
     */
    public long time1000Longs() {

        Iterable<Integer> i = new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int count = 0;

                    @Override
                    public boolean hasNext() {
                        return count <= 1000;
                    }

                    @Override
                    public Integer next() {
                        return count++;
                    }

                    @Override
                    public void remove() {
                        // do nothing
                    }

                };
            };
        };

        Observable<Integer> s = Observable.from(i);
        IntegerSumObserver o = new IntegerSumObserver();

        for (long l = 0; l < REPETITIONS; l++) {
            s.subscribe(o);
        }
        return o.sum;
    }

    /**
     * Observable.from(Iterable)
     * 
     * --- Old synchronous implementation with Subscriptions.empty() and no unsubscribe support
     * 
     * Run: 10 - 10,629,658 ops/sec
     * Run: 11 - 9,573,775 ops/sec
     * Run: 12 - 10,589,787 ops/sec
     * Run: 13 - 10,514,141 ops/sec
     * Run: 14 - 9,765,586 ops/sec
     * 
     * --- New synchronous implementation with OperatorSubscription and unsubscribe support
     * 
     * Run: 10 - 8,096,667 ops/sec
     * Run: 11 - 8,382,131 ops/sec
     * Run: 12 - 8,256,288 ops/sec
     * Run: 13 - 8,139,703 ops/sec
     * Run: 14 - 8,011,023 ops/sec
     * 
     * ... after v0.17 work:
     * 
     * Run: 10 - 31,296,553 ops/sec
     * Run: 11 - 30,080,435 ops/sec
     * Run: 12 - 31,886,941 ops/sec
     * Run: 13 - 32,281,807 ops/sec
     * Run: 14 - 33,519,028 ops/sec
     * 
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
