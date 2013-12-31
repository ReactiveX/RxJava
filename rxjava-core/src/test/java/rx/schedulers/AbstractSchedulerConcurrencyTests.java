package rx.schedulers;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
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
