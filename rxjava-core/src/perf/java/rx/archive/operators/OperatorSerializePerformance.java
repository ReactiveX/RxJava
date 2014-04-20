package rx.archive.operators;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.logic.BlackHole;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.archive.perf.AbstractPerformanceTester;
import rx.archive.perf.IntegerSumObserver;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OperatorSerializePerformance extends AbstractPerformanceTester {
    static int reps = Integer.MAX_VALUE / 16384; // timeTwoStreams

    //    static int reps = Integer.MAX_VALUE / 1024; // timeSingleStream
    //    static int reps = 1000; // interval streams

    OperatorSerializePerformance() {
        super(reps);
    }

    public static void main(String args[]) {

        final OperatorSerializePerformance spt = new OperatorSerializePerformance();
        final Input input = new Input();
        input.setup();
        try {
            spt.runTest(new Action0() {

                @Override
                public void call() {
                    //                    spt.noSerializationSingleThreaded(input);
                    spt.serializedSingleStream(input);
                    //                    spt.synchronizedSingleStream(input);
                    //                    spt.timeTwoStreams();
                    //                    spt.timeSingleStream();
                    //                    spt.timeTwoStreamsIntervals();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Run: 10 - 12,186,982 ops/sec
     * Run: 11 - 10,236,722 ops/sec
     * Run: 12 - 11,377,690 ops/sec
     * Run: 13 - 10,876,358 ops/sec
     * Run: 14 - 11,383,619 ops/sec
     * 
     * @param input
     */
    public void noSerializationSingleThreaded(Input input) {
        for (int i = 0; i < reps; i++) {
            input.observable.subscribe(input.subscriber);
        }
    }

    /**
     * 0.17.3:
     * 
     * Run: 10 - 9,746,505 ops/sec
     * Run: 11 - 9,956,019 ops/sec
     * Run: 12 - 10,053,770 ops/sec
     * Run: 13 - 10,076,958 ops/sec
     * Run: 14 - 9,983,319 ops/sec
     * 
     * 0.17.2:
     * 
     * Run: 10 - 9,851,999 ops/sec
     * Run: 11 - 9,726,975 ops/sec
     * Run: 12 - 9,719,762 ops/sec
     * Run: 13 - 9,668,141 ops/sec
     * Run: 14 - 9,799,700 ops/sec
     * 
     * @param input
     */
    public void serializedSingleStream(Input input) {
        for (int i = 0; i < reps; i++) {
            input.observable.serialize().subscribe(input.subscriber);
        }
    }

    /**
     * 1 streams emitting in a tight loop. Testing for single-threaded overhead.
     * 
     * -> blocking synchronization (SynchronizedObserver)
     * 
     * Run: 10 - 58,186,310 ops/sec
     * Run: 11 - 60,592,037 ops/sec
     * Run: 12 - 58,099,263 ops/sec
     * Run: 13 - 59,034,765 ops/sec
     * Run: 14 - 58,231,548 ops/sec
     * 
     * -> state machine technique (SerializedObserverViaStateMachine)
     * 
     * Run: 10 - 34,668,810 ops/sec
     * Run: 11 - 32,874,312 ops/sec
     * Run: 12 - 33,389,339 ops/sec
     * Run: 13 - 35,269,946 ops/sec
     * Run: 14 - 34,165,013 ops/sec
     * 
     * -> using queue and counter technique (SerializedObserverViaQueueAndCounter)
     * 
     * Run: 10 - 19,548,387 ops/sec
     * Run: 11 - 19,471,069 ops/sec
     * Run: 12 - 19,480,112 ops/sec
     * Run: 13 - 18,720,550 ops/sec
     * Run: 14 - 19,070,383 ops/sec
     * 
     * -> using queue and lock technique (SerializedObserverViaQueueAndLock)
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
     * 2 streams emitting in tight loops so very high contention.
     * 
     * -> blocking synchronization (SynchronizedObserver)
     * 
     * Run: 10 - 8,361,252 ops/sec
     * Run: 11 - 7,184,728 ops/sec
     * Run: 12 - 8,249,685 ops/sec
     * Run: 13 - 6,831,595 ops/sec
     * Run: 14 - 8,003,358 ops/sec
     * 
     * (faster because it allows each thread to be "single threaded" while blocking the other)
     * 
     * -> state machine technique (SerializedObserverViaStateMachine)
     * 
     * Run: 10 - 4,060,062 ops/sec
     * Run: 11 - 3,561,131 ops/sec
     * Run: 12 - 3,721,387 ops/sec
     * Run: 13 - 3,693,909 ops/sec
     * Run: 14 - 3,516,324 ops/sec
     * 
     * -> using queue and counter technique (SerializedObserverViaQueueAndCounter)
     * 
     * Run: 10 - 4,300,229 ops/sec
     * Run: 11 - 4,395,995 ops/sec
     * Run: 12 - 4,551,550 ops/sec
     * Run: 13 - 4,443,235 ops/sec
     * Run: 14 - 4,158,475 ops/sec
     * 
     * -> using queue and lock technique (SerializedObserverViaQueueAndLock)
     * 
     * Run: 10 - 6,369,781 ops/sec
     * Run: 11 - 6,933,872 ops/sec
     * Run: 12 - 5,652,535 ops/sec
     * Run: 13 - 5,503,716 ops/sec
     * Run: 14 - 6,219,264 ops/sec
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
     * 2 streams emitting once a millisecond. Slow emission so little to no contention.
     * 
     * -> blocking synchronization (SynchronizedObserver)
     * 
     * Run: 10 - 1,996 ops/sec
     * Run: 11 - 1,996 ops/sec
     * Run: 12 - 1,995 ops/sec
     * Run: 13 - 1,997 ops/sec
     * Run: 14 - 1,996 ops/sec
     * 
     * -> state machine technique (SerializedObserverViaStateMachine)
     * 
     * Run: 10 - 1,996 ops/sec
     * Run: 11 - 1,996 ops/sec
     * Run: 12 - 1,996 ops/sec
     * Run: 13 - 1,996 ops/sec
     * Run: 14 - 1,996 ops/sec
     * 
     * -> using queue and counter technique (SerializedObserverViaQueueAndCounter)
     * 
     * Run: 10 - 1,996 ops/sec
     * Run: 11 - 1,996 ops/sec
     * Run: 12 - 1,996 ops/sec
     * Run: 13 - 1,996 ops/sec
     * Run: 14 - 1,995 ops/sec
     * 
     * -> using queue and lock technique (SerializedObserverViaQueueAndLock)
     * 
     * Run: 10 - 1,996 ops/sec
     * Run: 11 - 1,996 ops/sec
     * Run: 12 - 1,997 ops/sec
     * Run: 13 - 1,996 ops/sec
     * Run: 14 - 1,995 ops/sec
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

    public static class Input {

        public int size = 1048576;

        public Observable<Integer> observable;
        public TestSubscriber<Integer> subscriber;

        private CountDownLatch latch;

        public void setup() {
            observable = Observable.create(new OnSubscribe<Integer>() {
                @Override
                public void call(Subscriber<? super Integer> o) {
                    for (int value = 0; value < size; value++) {
                        if (o.isUnsubscribed())
                            return;
                        o.onNext(value);
                    }
                    o.onCompleted();
                }
            });

            final BlackHole bh = new BlackHole();
            latch = new CountDownLatch(1);

            subscriber = new TestSubscriber<Integer>(new Observer<Integer>() {
                @Override
                public void onCompleted() {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable e) {
                    throw new RuntimeException(e);
                }

                @Override
                public void onNext(Integer value) {
                    bh.consume(value);
                }
            });

        }

        public void awaitCompletion() throws InterruptedException {
            latch.await();
        }
    }
}
