package rx.concurrency;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.operators.SafeObservableSubscription;
import rx.util.functions.Func1;

public class SchedulerUnsubscribeTest {

    /**
     * Bug report: https://github.com/Netflix/RxJava/issues/431
     */
    @Test
    public void testUnsubscribeOfNewThread() throws InterruptedException {
        testUnSubscribeForScheduler(Schedulers.newThread());
    }

    @Test
    public void testUnsubscribeOfThreadPoolForIO() throws InterruptedException {
        testUnSubscribeForScheduler(Schedulers.threadPoolForIO());
    }

    @Test
    public void testUnsubscribeOfThreadPoolForComputation() throws InterruptedException {
        testUnSubscribeForScheduler(Schedulers.threadPoolForComputation());
    }

    @Test
    public void testUnsubscribeOfImmediateThread() throws InterruptedException {
        testUnSubscribeForScheduler(Schedulers.immediate());
    }

    @Test
    public void testUnsubscribeOfCurrentThread() throws InterruptedException {
        testUnSubscribeForScheduler(Schedulers.currentThread());
    }

    public void testUnSubscribeForScheduler(Scheduler scheduler) throws InterruptedException {

        final AtomicInteger countReceived = new AtomicInteger();
        final AtomicInteger countGenerated = new AtomicInteger();
        final SafeObservableSubscription s = new SafeObservableSubscription();
        final CountDownLatch latch = new CountDownLatch(1);

        s.wrap(Observable.interval(50, TimeUnit.MILLISECONDS)
                .map(new Func1<Long, Long>() {
                    @Override
                    public Long call(Long aLong) {
                        System.out.println("generated " + aLong);
                        countGenerated.incrementAndGet();
                        return aLong;
                    }
                })
                .subscribeOn(scheduler)
                .observeOn(scheduler)
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
}
