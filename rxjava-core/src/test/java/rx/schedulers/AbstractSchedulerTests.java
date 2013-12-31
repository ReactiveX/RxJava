package rx.schedulers;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

public abstract class AbstractSchedulerTests {

    /**
     * The scheduler to test
     * 
     * @return
     */
    protected abstract Scheduler getScheduler();

    @Test
    public void testUnsubscribeRecursiveScheduleWithStateAndFunc2() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger();
        Subscription s = getScheduler().schedule(0L, new Func2<Scheduler, Long, Subscription>() {

            @Override
            public Subscription call(Scheduler innerScheduler, Long i) {
                i++;
                //                System.out.println("i: " + i);
                if (i == 10) {
                    latch.countDown();
                    try {
                        // wait for unsubscribe to finish so we are not racing it
                        unsubscribeLatch.await();
                    } catch (InterruptedException e) {
                        // we expect the countDown if unsubscribe is not working
                        // or to be interrupted if unsubscribe is successful since 
                        // the unsubscribe will interrupt it as it is calling Future.cancel(true)
                        // so we will ignore the stacktrace
                    }
                }

                counter.incrementAndGet();
                return innerScheduler.schedule(i, this);
            }
        });

        latch.await();
        s.unsubscribe();
        unsubscribeLatch.countDown();
        Thread.sleep(200); // let time pass to see if the scheduler is still doing work
        assertEquals(10, counter.get());
    }

    /**
     * Bug report: https://github.com/Netflix/RxJava/issues/431
     */
    @Test
    public void testUnSubscribeForScheduler() throws InterruptedException {

        final AtomicInteger countReceived = new AtomicInteger();
        final AtomicInteger countGenerated = new AtomicInteger();
        final SafeObservableSubscription s = new SafeObservableSubscription();
        final CountDownLatch latch = new CountDownLatch(1);

        s.wrap(Observable.interval(50, TimeUnit.MILLISECONDS)
                .map(new Func1<Long, Long>() {
                    @Override
                    public Long call(Long aLong) {
                        countGenerated.incrementAndGet();
                        return aLong;
                    }
                })
                .subscribeOn(getScheduler())
                .observeOn(getScheduler())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onCompleted() {
                        System.out.println("--- completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("--- onError");
                    }

                    @Override
                    public void onNext(Long args) {
                        if (countReceived.incrementAndGet() == 2) {
                            s.unsubscribe();
                            latch.countDown();
                        }
                        System.out.println("==> Received " + args);
                    }
                }));

        latch.await(1000, TimeUnit.MILLISECONDS);

        System.out.println("----------- it thinks it is finished ------------------ ");
        Thread.sleep(100);

        assertEquals(2, countGenerated.get());
    }

    @Test
    public void scheduleMultipleTasksOnOuterForSequentialExecution() throws InterruptedException {
        final AtomicInteger countExecutions = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        final SafeObservableSubscription outerSubscription = new SafeObservableSubscription();
        final Func2<Scheduler, Long, Subscription> fInner = new Func2<Scheduler, Long, Subscription>() {

            @Override
            public Subscription call(Scheduler innerScheduler, Long i) {
                countExecutions.incrementAndGet();
                i++;
                System.out.println("i: " + i);
                if (i == 1000) {
                    outerSubscription.unsubscribe();
                    latch.countDown();
                }
                if (i < 10000) {
                    return innerScheduler.schedule(i, this);
                } else {
                    latch.countDown();
                    return Subscriptions.empty();
                }
            }
        };

        Func2<Scheduler, Long, Subscription> fOuter = new Func2<Scheduler, Long, Subscription>() {

            @Override
            public Subscription call(Scheduler innerScheduler, Long i) {
                CompositeSubscription s = new CompositeSubscription();
                s.add(innerScheduler.schedule(i, fInner));
                s.add(innerScheduler.schedule(i, fInner));
                return s;
            }
        };

        outerSubscription.wrap(getScheduler().schedule(0L, fOuter));
        latch.await();
        Thread.sleep(200); // let time pass to see if the scheduler is still doing work
        System.out.println("Count: " + countExecutions.get());
        // we unsubscribe on first to 1000 so we hit 1999 instead of 2000
        assertEquals(1999, countExecutions.get());
    }

    @Test
    public void unsubscribeWithFastProducerWithSlowConsumerCausingQueuing() throws InterruptedException {
        final AtomicInteger countEmitted = new AtomicInteger();
        final AtomicInteger countTaken = new AtomicInteger();
        int value = Observable.create(new OnSubscribeFunc<Integer>() {

            @Override
            public Subscription onSubscribe(final Observer<? super Integer> o) {
                final BooleanSubscription s = BooleanSubscription.create();
                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        int i = 1;
                        while (!s.isUnsubscribed() && i <= 100) {
                            System.out.println("onNext from fast producer: " + i);
                            o.onNext(i++);
                        }
                        o.onCompleted();
                    }
                });
                t.setDaemon(true);
                t.start();
                return s;
            }
        }).doOnNext(new Action1<Integer>() {

            @Override
            public void call(Integer i) {
                countEmitted.incrementAndGet();
            }
        }).doOnCompleted(new Action0() {

            @Override
            public void call() {
                System.out.println("-------- Done Emitting from Source ---------");
            }
        }).observeOn(getScheduler()).doOnNext(new Action1<Integer>() {

            @Override
            public void call(Integer i) {
                System.out.println(">> onNext to slowConsumer pre-take: " + i);
                //force it to be slower than the producer
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                countTaken.incrementAndGet();
            }
        }).take(10).toBlockingObservable().last();

        // they will all emit because the consumer is running slow
        assertEquals(100, countEmitted.get());
        // number received after take (but take will filter any extra)
        assertEquals(10, value);
        // so we also want to check the doOnNext after observeOn to see if it got unsubscribed
        Thread.sleep(200); // let time pass to see if the scheduler is still doing work
        // we expect only 10 to make it through the observeOn side
        assertEquals(10, countTaken.get());
    }

    @Test(timeout = 8000)
    public void recursionUsingFunc2() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        getScheduler().schedule(0L, new Func2<Scheduler, Long, Subscription>() {

            @Override
            public Subscription call(Scheduler innerScheduler, Long i) {
                i++;
                if (i % 100000 == 0) {
                    System.out.println(i + "  Total Memory: " + Runtime.getRuntime().totalMemory() + "  Free: " + Runtime.getRuntime().freeMemory());
                }
                if (i < 5000000L) {
                    return innerScheduler.schedule(i, this);
                } else {
                    latch.countDown();
                    return Subscriptions.empty();
                }
            }
        });

        latch.await();
    }

    @Test(timeout = 8000)
    public void recursionUsingAction0() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        getScheduler().schedule(new Action1<Action0>() {

            private long i = 0;

            @Override
            public void call(Action0 self) {
                i++;
                if (i % 100000 == 0) {
                    System.out.println(i + "  Total Memory: " + Runtime.getRuntime().totalMemory() + "  Free: " + Runtime.getRuntime().freeMemory());
                }
                if (i < 5000000L) {
                    self.call();
                } else {
                    latch.countDown();
                }
            }
        });

        latch.await();
    }

}
