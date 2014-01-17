package rx;

import rx.perf.AbstractPerformanceTester;
import rx.perf.LongSumObserver;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

public class ObservableCreatePerformance extends AbstractPerformanceTester {

    public static void main(String args[]) {

        final ObservableCreatePerformance spt = new ObservableCreatePerformance();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    spt.timeCreateAndSubscribe();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Observable.create(f).subscribe()
     * 
     * With old OnSubscribeFunc implementation:
     * 
     * --- With Subscriptions.empty():
     * 
     * Run: 10 - 19,339,367 ops/sec
     * Run: 11 - 19,530,258 ops/sec
     * Run: 12 - 19,616,843 ops/sec
     * Run: 13 - 19,705,598 ops/sec
     * Run: 14 - 19,314,042 ops/sec
     * 
     * --- With CompositeSubscription():
     * 
     * Run: 10 - 11,407,321 ops/sec
     * Run: 11 - 11,481,425 ops/sec
     * Run: 12 - 11,215,188 ops/sec
     * Run: 13 - 11,235,096 ops/sec
     * Run: 14 - 11,269,688 ops/sec
     * 
     * With new Action2 implementation (bind operator changes):
     * 
     * --- always with CompositeSubscription():
     * 
     * Run: 10 - 13,716,405 ops/sec
     * Run: 11 - 13,920,015 ops/sec
     * Run: 12 - 14,122,977 ops/sec
     * Run: 13 - 14,048,534 ops/sec
     * Run: 14 - 13,824,451 ops/sec
     */
    public long timeCreateAndSubscribe() {

        Observable<Long> s = Observable.create(new Action1<Operator<? super Long>>() {

            @Override
            public void call(Operator<? super Long> o) {
                o.onNext(1L);
                o.onCompleted();
            }

        });
        LongSumObserver o = new LongSumObserver();

        for (long l = 0; l < REPETITIONS; l++) {
            s.subscribe(o);
        }
        return o.sum;
    }

}