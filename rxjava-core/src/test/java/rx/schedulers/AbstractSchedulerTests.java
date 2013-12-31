package rx.schedulers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func2;

/**
 * Base tests for all schedulers including Immediate/Current.
 */
public abstract class AbstractSchedulerTests {

    /**
     * The scheduler to test
     */
    protected abstract Scheduler getScheduler();

    @Test
    public final void unsubscribeWithFastProducerWithSlowConsumerCausingQueuing() throws InterruptedException {
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

        if (getScheduler() instanceof CurrentThreadScheduler || getScheduler() instanceof ImmediateScheduler) {
            // since there is no concurrency it will block and only emit as many as it can process
            assertEquals(10, countEmitted.get());
        } else {
            // they will all emit because the consumer is running slow
            assertEquals(100, countEmitted.get());
        }
        // number received after take (but take will filter any extra)
        assertEquals(10, value);
        // so we also want to check the doOnNext after observeOn to see if it got unsubscribed
        Thread.sleep(200); // let time pass to see if the scheduler is still doing work
        // we expect only 10 to make it through the observeOn side
        assertEquals(10, countTaken.get());
    }

    @Test
    public void testNestedActions() throws InterruptedException {
        final Scheduler scheduler = getScheduler();
        final CountDownLatch latch = new CountDownLatch(1);

        final Action0 firstStepStart = mock(Action0.class);
        final Action0 firstStepEnd = mock(Action0.class);

        final Action0 secondStepStart = mock(Action0.class);
        final Action0 secondStepEnd = mock(Action0.class);

        final Action0 thirdStepStart = mock(Action0.class);
        final Action0 thirdStepEnd = mock(Action0.class);

        final Action0 firstAction = new Action0() {
            @Override
            public void call() {
                firstStepStart.call();
                firstStepEnd.call();
                latch.countDown();
            }
        };
        final Action0 secondAction = new Action0() {
            @Override
            public void call() {
                secondStepStart.call();
                scheduler.schedule(firstAction);
                secondStepEnd.call();

            }
        };
        final Action0 thirdAction = new Action0() {
            @Override
            public void call() {
                thirdStepStart.call();
                scheduler.schedule(secondAction);
                thirdStepEnd.call();
            }
        };

        InOrder inOrder = inOrder(firstStepStart, firstStepEnd, secondStepStart, secondStepEnd, thirdStepStart, thirdStepEnd);

        scheduler.schedule(thirdAction);
        
        latch.await();

        inOrder.verify(thirdStepStart, times(1)).call();
        inOrder.verify(thirdStepEnd, times(1)).call();
        inOrder.verify(secondStepStart, times(1)).call();
        inOrder.verify(secondStepEnd, times(1)).call();
        inOrder.verify(firstStepStart, times(1)).call();
        inOrder.verify(firstStepEnd, times(1)).call();
    }

    @Test
    public final void testSequenceOfActions() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        final CountDownLatch latch = new CountDownLatch(1);
        final Action0 first = mock(Action0.class);
        final Action0 second = mock(Action0.class);

        scheduler.schedule(first);
        scheduler.schedule(second);
        scheduler.schedule(new Action0() {

            @Override
            public void call() {
                latch.countDown();
            }
        });

        latch.await();

        verify(first, times(1)).call();
        verify(second, times(1)).call();

    }

    @Test
    public void testSequenceOfDelayedActions() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        final CountDownLatch latch = new CountDownLatch(1);
        final Action0 first = mock(Action0.class);
        final Action0 second = mock(Action0.class);

        scheduler.schedule(new Action0() {
            @Override
            public void call() {
                scheduler.schedule(first, 30, TimeUnit.MILLISECONDS);
                scheduler.schedule(second, 10, TimeUnit.MILLISECONDS);
                scheduler.schedule(new Action0() {

                    @Override
                    public void call() {
                        latch.countDown();
                    }
                }, 40, TimeUnit.MILLISECONDS);
            }
        });

        latch.await();
        InOrder inOrder = inOrder(first, second);

        inOrder.verify(second, times(1)).call();
        inOrder.verify(first, times(1)).call();

    }

    @Test
    public void testMixOfDelayedAndNonDelayedActions() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        final CountDownLatch latch = new CountDownLatch(1);
        final Action0 first = mock(Action0.class);
        final Action0 second = mock(Action0.class);
        final Action0 third = mock(Action0.class);
        final Action0 fourth = mock(Action0.class);

        scheduler.schedule(new Action0() {
            @Override
            public void call() {
                scheduler.schedule(first);
                scheduler.schedule(second, 300, TimeUnit.MILLISECONDS);
                scheduler.schedule(third, 100, TimeUnit.MILLISECONDS);
                scheduler.schedule(fourth);
                scheduler.schedule(new Action0() {

                    @Override
                    public void call() {
                        latch.countDown();
                    }
                }, 400, TimeUnit.MILLISECONDS);
            }
        });

        latch.await();
        InOrder inOrder = inOrder(first, second, third, fourth);

        inOrder.verify(first, times(1)).call();
        inOrder.verify(fourth, times(1)).call();
        inOrder.verify(third, times(1)).call();
        inOrder.verify(second, times(1)).call();
    }

    @Test
    public final void testRecursiveExecutionWithAction0() throws InterruptedException {
        final Scheduler scheduler = getScheduler();
        final AtomicInteger i = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        scheduler.schedule(new Action1<Action0>() {

            @Override
            public void call(Action0 self) {
                if (i.incrementAndGet() < 100) {
                    self.call();
                } else {
                    latch.countDown();
                }
            }
        });

        latch.await();
        assertEquals(100, i.get());
    }

    @Test
    public final void testRecursiveExecutionWithFunc2() throws InterruptedException {
        final Scheduler scheduler = getScheduler();
        final AtomicInteger i = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);

        scheduler.schedule(0, new Func2<Scheduler, Integer, Subscription>() {

            @Override
            public Subscription call(Scheduler innerScheduler, Integer state) {
                i.set(state);
                if (state < 100) {
                    return innerScheduler.schedule(state + 1, this);
                } else {
                    latch.countDown();
                    return Subscriptions.empty();
                }
            }

        });

        latch.await();
        assertEquals(100, i.get());
    }

    @Test
    public final void testRecursiveExecutionWithFunc2AndDelayTime() throws InterruptedException {
        final Scheduler scheduler = getScheduler();
        final AtomicInteger i = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);

        scheduler.schedule(0, new Func2<Scheduler, Integer, Subscription>() {

            @Override
            public Subscription call(Scheduler innerScheduler, Integer state) {
                i.set(state);
                if (state < 100) {
                    return innerScheduler.schedule(state + 1, this, 5, TimeUnit.MILLISECONDS);
                } else {
                    latch.countDown();
                    return Subscriptions.empty();
                }
            }

        }, 50, TimeUnit.MILLISECONDS);

        latch.await();
        assertEquals(100, i.get());
    }

}
