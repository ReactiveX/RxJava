package rx.operators;

import rx.Observable;
import rx.perf.AbstractPerformanceTester;
import rx.perf.IntegerSumObserver;
import rx.schedulers.Schedulers;
import rx.util.functions.Action0;

public class OperatorObserveOnPerformance extends AbstractPerformanceTester {

    private static long reps = 1000000;

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
     */
    public long timeObserveOn() {

        Observable<Integer> s = Observable.range(1, (int) reps).observeOn(Schedulers.newThread());
        IntegerSumObserver o = new IntegerSumObserver();
        s.subscribe(o);
        return o.sum;
    }

}