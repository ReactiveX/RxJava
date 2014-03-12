package rx.operators;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.perf.AbstractPerformanceTester;
import rx.perf.IntegerSumObserver;
import rx.schedulers.Schedulers;

public class OperatorSerializePerformance extends AbstractPerformanceTester {
    //    static int reps = Integer.MAX_VALUE / 16384; // timeTwoStreams

    static int reps = Integer.MAX_VALUE / 1024; // timeSingleStream

    //    static int reps = 1000; // interval streams

    OperatorSerializePerformance() {
        super(reps);
    }

    public static void main(String args[]) {

        final OperatorSerializePerformance spt = new OperatorSerializePerformance();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    //                    spt.timeTwoStreams();
                    spt.timeSingleStream();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 
     * -> state machine technique
     * 
     * Run: 10 - 34,668,810 ops/sec
     * Run: 11 - 32,874,312 ops/sec
     * Run: 12 - 33,389,339 ops/sec
     * Run: 13 - 35,269,946 ops/sec
     * Run: 14 - 34,165,013 ops/sec
     * 
     * -> using queue and counter technique
     * 
     * Run: 10 - 19,548,387 ops/sec
     * Run: 11 - 19,471,069 ops/sec
     * Run: 12 - 19,480,112 ops/sec
     * Run: 13 - 18,720,550 ops/sec
     * Run: 14 - 19,070,383 ops/sec
     * 
     * -> using queue and lock technique
     * 
     * Run: 10 - 51,295,152 ops/sec
     * Run: 11 - 50,317,937 ops/sec
     * Run: 12 - 51,126,331 ops/sec
     * Run: 13 - 52,418,291 ops/sec
     * Run: 14 - 51,694,710 ops/sec
     */
    public long timeSingleStream() {

        final Observable<Integer> s1 = Observable.range(0, reps).subscribeOn(Schedulers.newThread());

        Observable<Integer> s = Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> s) {
                final CountDownLatch latch = new CountDownLatch(1);
                // first
                s1.doOnTerminate(new Action0() {

                    @Override
                    public void call() {
                        latch.countDown();
                    }

                }).subscribe(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        s.onNext(t1);
                    }

                });

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                s.onCompleted();
            }

        }).serialize();

        IntegerSumObserver o = new IntegerSumObserver();
        s.subscribe(o);
        //        System.out.println("sum : " + o.sum);

        return o.sum;
    }

    /**
     * -> state machine technique
     * 
     * Run: 10 - 3,432,256 ops/sec
     * Run: 11 - 3,570,444 ops/sec
     * Run: 12 - 3,791,137 ops/sec
     * Run: 13 - 3,664,579 ops/sec
     * Run: 14 - 5,211,156 ops/sec
     * 
     * -> using "observeOn" technique
     * 
     * Run: 10 - 3,995,336 ops/sec
     * Run: 11 - 4,033,077 ops/sec
     * Run: 12 - 4,510,978 ops/sec
     * Run: 13 - 3,218,915 ops/sec
     * Run: 14 - 3,938,549 ops/sec
     * 
     * -> using queue and lock technique
     * 
     * Run: 10 - 5,348,090 ops/sec
     * Run: 11 - 6,458,608 ops/sec
     * Run: 12 - 5,430,743 ops/sec
     * Run: 13 - 5,159,666 ops/sec
     * Run: 14 - 6,129,682 ops/sec
     */
    public long timeTwoStreams() {

        final Observable<Integer> s1 = Observable.range(0, reps).subscribeOn(Schedulers.newThread());
        final Observable<Integer> s2 = Observable.range(0, reps).subscribeOn(Schedulers.newThread());

        Observable<Integer> s = Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> s) {
                final CountDownLatch latch = new CountDownLatch(2);
                // first
                s1.doOnTerminate(new Action0() {

                    @Override
                    public void call() {
                        latch.countDown();
                    }

                }).subscribe(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        s.onNext(t1);
                    }

                });

                // second
                s2.doOnTerminate(new Action0() {

                    @Override
                    public void call() {
                        latch.countDown();
                    }

                }).subscribe(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        s.onNext(t1);
                    }

                });

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                s.onCompleted();
            }

        }).serialize();

        IntegerSumObserver o = new IntegerSumObserver();
        s.subscribe(o);
        //        System.out.println("sum : " + o.sum);

        return o.sum;
    }

    /**
     * Run: 10 - 1,996 ops/sec
     * Run: 11 - 1,996 ops/sec
     * Run: 12 - 1,996 ops/sec
     * Run: 13 - 1,996 ops/sec
     * Run: 14 - 1,996 ops/sec
     */
    public long timeTwoStreamsIntervals() {

        final Observable<Integer> s1 = Observable.interval(1, TimeUnit.MILLISECONDS).take(reps / 2).flatMap(new Func1<Long, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Long l) {
                return Observable.range(l.intValue(), 100);
            }

        }).subscribeOn(Schedulers.newThread());
        final Observable<Integer> s2 = Observable.range(1, reps / 2).subscribeOn(Schedulers.newThread());

        Observable<Integer> s = Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> s) {
                final CountDownLatch latch = new CountDownLatch(2);
                // first
                s1.doOnTerminate(new Action0() {

                    @Override
                    public void call() {
                        latch.countDown();
                    }

                }).subscribe(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        s.onNext(t1);
                    }

                });

                // second
                s2.doOnTerminate(new Action0() {

                    @Override
                    public void call() {
                        latch.countDown();
                    }

                }).subscribe(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        s.onNext(t1);
                    }

                });

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                s.onCompleted();
            }

        }).serialize();

        IntegerSumObserver o = new IntegerSumObserver();
        s.subscribe(o);
        //        System.out.println("sum : " + o.sum);

        return o.sum;
    }

}
