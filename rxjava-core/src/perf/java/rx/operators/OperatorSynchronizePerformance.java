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

public class OperatorSynchronizePerformance extends AbstractPerformanceTester {
    static int reps = Integer.MAX_VALUE / 1024;

    //    static int reps = 1000; // timeTwoStreamsIntervals

    OperatorSynchronizePerformance() {
        super(reps);
    }

    public static void main(String args[]) {

        final OperatorSynchronizePerformance spt = new OperatorSynchronizePerformance();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    spt.timeTwoStreams();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Run: 10 - 59,593,390 ops/sec
     * Run: 11 - 55,784,194 ops/sec
     * Run: 12 - 58,778,300 ops/sec
     * Run: 13 - 60,679,696 ops/sec
     * Run: 14 - 59,370,693 ops/sec
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

        }).synchronize();

        IntegerSumObserver o = new IntegerSumObserver();
        s.subscribe(o);
        //        System.out.println("sum : " + o.sum);

        return o.sum;
    }

    /**
     * Run: 10 - 9,139,226 ops/sec
     * Run: 11 - 8,456,526 ops/sec
     * Run: 12 - 8,072,174 ops/sec
     * Run: 13 - 8,667,381 ops/sec
     * Run: 14 - 8,853,370 ops/sec
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

        }).synchronize();

        IntegerSumObserver o = new IntegerSumObserver();
        s.subscribe(o);
        //        System.out.println("sum : " + o.sum);

        return o.sum;
    }

    /**
     * Run: 10 - 1,996 ops/sec
     * Run: 11 - 1,997 ops/sec
     * Run: 12 - 1,996 ops/sec
     * Run: 13 - 1,996 ops/sec
     * Run: 14 - 1,995 ops/sec
     * 
     * @return
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

        }).synchronize();

        IntegerSumObserver o = new IntegerSumObserver();
        s.subscribe(o);
        //                System.out.println("sum : " + o.sum);

        return o.sum;
    }
}
