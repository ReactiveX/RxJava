package rx.archive.perf;

import java.util.Iterator;

import rx.functions.Action0;

public abstract class AbstractPerformanceTester {

    public static final long REPETITIONS = 5 * 1000 * 1000;
    public static final int NUM_PRODUCERS = 1;

    private final long repetitions;

    protected AbstractPerformanceTester(long repetitions) {
        this.repetitions = repetitions;
    }

    public final void runTest(Action0 action) throws InterruptedException {
        for (int runNum = 0; runNum < 15; runNum++) {
            System.gc();
            Thread.sleep(1000L);

            final long start = System.nanoTime();

            action.call();

            long duration = System.nanoTime() - start;
            long opsPerSec = (repetitions * NUM_PRODUCERS * 1000L * 1000L * 1000L) / duration;
            System.out.printf("Run: %d - %,d ops/sec \n",
                    Integer.valueOf(runNum),
                    Long.valueOf(opsPerSec));
        }
    }

    /**
     * Baseline ops/second without a subject.
     * 
     * Perf along this order of magnitude:
     * 
     * Run: 10 - 316,235,532 ops/sec
     * Run: 11 - 301,886,792 ops/sec
     * Run: 12 - 310,472,228 ops/sec
     * Run: 13 - 313,469,797 ops/sec
     * Run: 14 - 305,380,809 ops/sec
     */
    public long baseline() {
        LongSumObserver o = new LongSumObserver();
        for (long l = 0; l < repetitions; l++) {
            o.onNext(l);
        }
        o.onCompleted();
        return o.sum;
    }

    public Iterable<Long> ITERABLE_OF_REPETITIONS = new Iterable<Long>() {

        @Override
        public Iterator<Long> iterator() {
            return new Iterator<Long>() {
                long count = 0;

                @Override
                public boolean hasNext() {
                    return count <= repetitions;
                }

                @Override
                public Long next() {
                    return count++;
                }

                @Override
                public void remove() {
                    // do nothing
                }

            };
        };
    };

}
