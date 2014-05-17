package rx.archive.operators;

import rx.Observable;
import rx.archive.perf.AbstractPerformanceTester;
import rx.archive.perf.IntegerSumObserver;
import rx.archive.perf.LongSumObserver;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class OperatorMergePerformance extends AbstractPerformanceTester {

    OperatorMergePerformance() {
        super(REPETITIONS);
    }
    
    public static void main(String args[]) {

        final OperatorMergePerformance spt = new OperatorMergePerformance();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    spt.timeRepetitionsEmissionSynchronous();
                    //                    spt.timeMergeAandBwithSingleItems();
                    //                    spt.timeMergeAandBwith100Items();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Run: 10 - 32,609,617 ops/sec
     * Run: 11 - 33,511,839 ops/sec
     * Run: 12 - 34,768,096 ops/sec
     * Run: 13 - 32,376,499 ops/sec
     * Run: 14 - 33,166,835 ops/sec
     * 
     * ... after v0.17 work:
     * 
     * Run: 10 - 45,945,747 ops/sec
     * Run: 11 - 46,342,209 ops/sec
     * Run: 12 - 44,493,090 ops/sec
     * Run: 13 - 44,999,640 ops/sec
     * Run: 14 - 47,389,771 ops/sec
     */
    public long timeRepetitionsEmissionSynchronous() {

        Observable<Long> sA = Observable.from(ITERABLE_OF_REPETITIONS);
        Observable<Long> sB = Observable.from(ITERABLE_OF_REPETITIONS);
        Observable<Long> s = Observable.merge(sA, sB);

        LongSumObserver o = new LongSumObserver();
        s.subscribe(o);
        return o.sum;
    }

    /**
     * Run: 10 - 7,911,392,405 ops/sec
     * Run: 11 - 8,620,689,655 ops/sec
     * Run: 12 - 8,333,333,333 ops/sec
     * Run: 13 - 6,775,067,750 ops/sec
     * Run: 14 - 9,074,410,163 ops/sec
     */
    public long timeRepetitionsEmissionConcurrent() {

        Observable<Long> sA = Observable.from(ITERABLE_OF_REPETITIONS).subscribeOn(Schedulers.newThread());
        Observable<Long> sB = Observable.from(ITERABLE_OF_REPETITIONS).subscribeOn(Schedulers.newThread());
        Observable<Long> s = Observable.merge(sA, sB);

        LongSumObserver o = new LongSumObserver();
        s.subscribe(o);
        return o.sum;
    }

    /**
     * Observable.merge(from(1), from(1))
     * 
     * -- Old pre-bind
     * 
     * Run: 10 - 2,308,617 ops/sec
     * Run: 11 - 2,309,602 ops/sec
     * Run: 12 - 2,318,590 ops/sec
     * Run: 13 - 2,270,100 ops/sec
     * Run: 14 - 2,312,006 ops/sec
     * 
     * -- new post-bind create
     * 
     * Run: 10 - 1,983,888 ops/sec
     * Run: 11 - 1,963,829 ops/sec
     * Run: 12 - 1,952,321 ops/sec
     * Run: 13 - 1,936,031 ops/sec
     * Run: 14 - 1,862,887 ops/sec
     * 
     * -- new merge operator
     * 
     * Run: 10 - 2,630,464 ops/sec
     * Run: 11 - 2,627,986 ops/sec
     * Run: 12 - 2,628,281 ops/sec
     * Run: 13 - 2,617,781 ops/sec
     * Run: 14 - 2,625,995 ops/sec
     * 
     */
    public long timeMergeAandBwithSingleItems() {

        Observable<Integer> sA = Observable.from(1);
        Observable<Integer> sB = Observable.from(2);
        Observable<Integer> s = Observable.merge(sA, sB);

        IntegerSumObserver o = new IntegerSumObserver();

        for (long l = 0; l < REPETITIONS; l++) {
            s.subscribe(o);
        }
        return o.sum;
    }

    /**
     * Observable.merge(range(0, 100), range(100, 200))
     * 
     * -- Old pre-bind
     * 
     * Run: 10 - 340,049 ops/sec
     * Run: 11 - 339,059 ops/sec
     * Run: 12 - 348,899 ops/sec
     * Run: 13 - 350,953 ops/sec
     * Run: 14 - 352,228 ops/sec
     * 
     * -- new post-bind create
     * 
     * Run: 0 - 236,536 ops/sec
     * Run: 1 - 254,272 ops/sec
     * 
     * -- new merge operator
     * 
     * Run: 0 - 266,204 ops/sec
     * Run: 1 - 290,318 ops/sec
     * Run: 2 - 285,908 ops/sec
     * Run: 3 - 289,695 ops/sec
     * Run: 4 - 281,689 ops/sec
     * Run: 5 - 290,375 ops/sec
     * Run: 6 - 287,271 ops/sec
     */
    public long timeMergeAandBwith100Items() {

        Observable<Integer> sA = Observable.range(0, 100);
        Observable<Integer> sB = Observable.range(100, 200);
        Observable<Integer> s = Observable.merge(sA, sB);

        IntegerSumObserver o = new IntegerSumObserver();

        for (long l = 0; l < REPETITIONS; l++) {
            s.subscribe(o);
        }
        return o.sum;
    }
}