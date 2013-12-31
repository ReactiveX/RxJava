package rx.schedulers;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Scheduler;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func2;

/**
 * Base tests for schedulers that involve threads (concurrency).
 * 
 * These can only run on Schedulers that launch threads since they expect async/concurrent behavior.
 * 
 * The Current/Immediate schedulers will not work with these tests.
 */
public abstract class AbstractSchedulerConcurrencyTests extends AbstractSchedulerTests {

    @Test
    public void testUnsubscribeRecursiveScheduleWithStateAndFunc2() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger();
        Subscription s = getScheduler().schedule(1L, new Func2<Scheduler, Long, Subscription>() {

            @Override
            public Subscription call(Scheduler innerScheduler, Long i) {
                System.out.println("Run: " + i);
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
                return innerScheduler.schedule(i + 1, this);
            }
        });

        latch.await();
        s.unsubscribe();
        unsubscribeLatch.countDown();
        Thread.sleep(200); // let time pass to see if the scheduler is still doing work
        assertEquals(10, counter.get());
    }

    @Test
    public void testUnsubscribeRecursiveScheduleWithStateAndFunc2AndDelay() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger();
        Subscription s = getScheduler().schedule(1L, new Func2<Scheduler, Long, Subscription>() {

            @Override
            public Subscription call(Scheduler innerScheduler, Long i) {
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
                return innerScheduler.schedule(i + 1, this, 10, TimeUnit.MILLISECONDS);
            }
        }, 10, TimeUnit.MILLISECONDS);

        latch.await();
        s.unsubscribe();
        unsubscribeLatch.countDown();
        Thread.sleep(200); // let time pass to see if the scheduler is still doing work
        assertEquals(10, counter.get());
    }

    @Test(timeout = 8000)
    public void recursionUsingFunc2() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        getScheduler().schedule(1L, new Func2<Scheduler, Long, Subscription>() {

            @Override
            public Subscription call(Scheduler innerScheduler, Long i) {
                if (i % 100000 == 0) {
                    System.out.println(i + "  Total Memory: " + Runtime.getRuntime().totalMemory() + "  Free: " + Runtime.getRuntime().freeMemory());
                }
                if (i < 1000000L) {
                    return innerScheduler.schedule(i + 1, this);
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
                if (i < 1000000L) {
                    self.call();
                } else {
                    latch.countDown();
                }
            }
        });

        latch.await();
    }

}
