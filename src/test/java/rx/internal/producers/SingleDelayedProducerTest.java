package rx.internal.producers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import rx.Scheduler;
import rx.functions.Action0;
import rx.observers.*;
import rx.schedulers.Schedulers;

public class SingleDelayedProducerTest {

    @Test
    public void negativeRequestThrows() {
        SingleDelayedProducer<Integer> pa = new SingleDelayedProducer<Integer>(Subscribers.empty());
        try {
            pa.request(-99);
            Assert.fail("Failed to throw on invalid request amount");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("n >= 0 required", ex.getMessage());
        }
    }

    @Test
    public void requestCompleteRace() throws Exception {
        Scheduler.Worker w = Schedulers.computation().createWorker();
        try {
            for (int i = 0; i < 10000; i++) {
                final AtomicInteger waiter = new AtomicInteger(2);

                TestSubscriber<Integer> ts = TestSubscriber.create();
                
                final SingleDelayedProducer<Integer> pa = new SingleDelayedProducer<Integer>(ts);

                final CountDownLatch cdl = new CountDownLatch(1);
                
                w.schedule(new Action0() {
                    @Override
                    public void call() {
                        waiter.decrementAndGet();
                        while (waiter.get() != 0) ;
                        pa.request(1);
                        cdl.countDown();
                    }
                });
                
                waiter.decrementAndGet();
                while (waiter.get() != 0) ;
                pa.setValue(1);
                if (!cdl.await(5, TimeUnit.SECONDS)) {
                    Assert.fail("The wait for completion timed out");
                }
                
                ts.assertValue(1);
                ts.assertCompleted();
            }
        } finally {
            w.unsubscribe();
        }
    }

}
