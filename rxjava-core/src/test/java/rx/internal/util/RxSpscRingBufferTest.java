package rx.internal.util;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Producer;
import rx.Scheduler;
import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class RxSpscRingBufferTest extends RxRingBufferTest {

    @Override
    protected RxRingBuffer createRingBuffer() {
        return new RxSpscRingBuffer();
    }

    /**
     * Single producer, single consumer. The request() ensures it gets scheduled back on the same Producer thread.
     */
    @Test(timeout = 2000)
    public void testConcurrency() throws InterruptedException {
        final RxRingBuffer b = createRingBuffer();
        final CountDownLatch latch = new CountDownLatch(255);

        final Scheduler.Worker w1 = Schedulers.newThread().createWorker();
        final Scheduler.Worker w2 = Schedulers.newThread().createWorker();

        final AtomicInteger emit = new AtomicInteger();
        final AtomicInteger poll = new AtomicInteger();
        final AtomicInteger backpressureExceptions = new AtomicInteger();

        final Producer p = new Producer() {

            AtomicInteger c = new AtomicInteger();

            @Override
            public void request(final int n) {
                System.out.println("request[" + c.incrementAndGet() + "]: " + n + "  Thread: " + Thread.currentThread());
                w1.schedule(new Action0() {

                    @Override
                    public void call() {
                        if (latch.getCount() == 0) {
                            return;
                        }
                        for (int i = 0; i < n; i++) {
                            try {
                                emit.incrementAndGet();
                                b.onNext("one");
                            } catch (MissingBackpressureException e) {
                                System.out.println("BackpressureException => item: " + i + "  requested: " + n + " emit: " + emit.get() + "  poll: " + poll.get());
                                backpressureExceptions.incrementAndGet();
                            }
                        }
                        // we'll release after n batches
                        latch.countDown();
                    }

                });
            }

        };
        final Subscriber<String> s = new Subscriber<String>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String t) {

            }

        };

        w1.schedule(new Action0() {

            @Override
            public void call() {
                b.requestIfNeeded(s);
                s.setProducer(p);
            }

        });

        w2.schedule(new Action0() {

            @Override
            public void call() {
                while (true) {
                    Object o = b.poll();
                    if (o == null) {
                        b.requestIfNeeded(s);
                    } else {
                        poll.incrementAndGet();
                    }
                }
            }

        });

        latch.await();
        w1.unsubscribe();
        w2.unsubscribe();

        System.out.println("emit: " + emit.get() + " poll: " + poll.get());
        assertEquals(0, backpressureExceptions.get());
        assertEquals(emit.get(), poll.get());
    }

}
