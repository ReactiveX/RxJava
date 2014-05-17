package rx.archive.subscriptions;

import rx.archive.perf.AbstractPerformanceTester;
import rx.functions.Action0;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.CompositeSubscription;

public class CompositeSubscriptionAddRemovePerf extends AbstractPerformanceTester {

    CompositeSubscriptionAddRemovePerf() {
        super(REPETITIONS);
    }

    public static void main(String[] args) {
        final CompositeSubscriptionAddRemovePerf spt = new CompositeSubscriptionAddRemovePerf();
        try {
            spt.runTest(new Action0() {
                @Override
                public void call() {
                    spt.timeAddAndRemove();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test simple add + remove on a composite.
     * 
     * With old Composite add/remove:
     * 
     * Run: 10 - 14,985,141 ops/sec
     * Run: 11 - 15,257,104 ops/sec
     * Run: 12 - 14,797,996 ops/sec
     * Run: 13 - 14,438,643 ops/sec
     * Run: 14 - 14,985,864 ops/sec
     * 
     * With optimized Composite add/remove:
     * 
     * Run: 10 - 19,802,782 ops/sec
     * Run: 11 - 18,896,021 ops/sec
     * Run: 12 - 18,829,296 ops/sec
     * Run: 13 - 19,729,876 ops/sec
     * Run: 14 - 19,830,678 ops/sec
     * 
     * about 32% increase
     */
    void timeAddAndRemove() {
        CompositeSubscription csub = new CompositeSubscription();
        BooleanSubscription bs = new BooleanSubscription();
        for (int i = 0; i < REPETITIONS; i++) {
            csub.add(bs);
            csub.remove(bs);
        }
    }
}
