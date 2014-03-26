package rx.archive;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.archive.perf.AbstractPerformanceTester;
import rx.archive.perf.LongSumObserver;
import rx.functions.Action0;

public class ObservableCreatePerformance extends AbstractPerformanceTester {

    ObservableCreatePerformance() {
        super(REPETITIONS);
    }

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
     * Run: 10 - 15,846,002 ops/sec
     * Run: 11 - 15,671,181 ops/sec
     * Run: 12 - 15,401,580 ops/sec
     * Run: 13 - 15,841,283 ops/sec
     * Run: 14 - 15,317,970 ops/sec
     */
    public long timeCreateAndSubscribe() {

        Observable<Long> s = Observable.create(new OnSubscribe<Long>() {

            @Override
            public void call(Subscriber<? super Long> o) {
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