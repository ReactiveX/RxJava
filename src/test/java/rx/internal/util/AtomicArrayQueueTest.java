package rx.internal.util;

import static org.junit.Assert.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class AtomicArrayQueueTest {
    @Test
    public void testSimpleOfferPoll() {
        AtomicArrayQueue saq = new AtomicArrayQueue();
        for (int i = 0; i < 10000; i++) {
            saq.offer(i);
            assertEquals(i, saq.poll());
        }
    }
    @Test
    public void testTriggerOneGrowth() {
        AtomicArrayQueue saq = new AtomicArrayQueue();
        for (int i = 0; i < 16; i++) {
            saq.offer(i);
        }
        for (int i = 0; i < 16; i++) {
            assertEquals(i, saq.poll());
        }       
    }
    @Test
    public void testTriggerGrowthHalfwayReading() {
        AtomicArrayQueue saq = new AtomicArrayQueue();
        for (int i = 0; i < 4; i++) {
            saq.offer(i);
        }
        for (int i = 0; i < 4; i++) {
            assertEquals(i, saq.poll());
        }
        for (int i = 4; i < 16; i++) {
            saq.offer(i);
        }
        for (int i = 4; i < 16; i++) {
            assertEquals(i, saq.poll());
        }
    }

    @Test
    public void testCapacityLimit() {
        AtomicArrayQueue aaq = new AtomicArrayQueue(8, 16);
        for (int i = 0; i < 16; i++) {
            assertTrue(aaq.offer(i));
        }
        assertFalse(aaq.offer(16));
        
        assertEquals(0, aaq.poll());

        assertTrue(aaq.offer(16));

    }
    
    static void await(CyclicBarrier cb) {
        try {
            cb.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } catch (BrokenBarrierException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Test(timeout = 10000)
    public void testConcurrentBehavior() throws InterruptedException {
        for (int j = 0; j < 50; j++) {
            final AtomicArrayQueue aaq = new AtomicArrayQueue();
            
            final AtomicBoolean continuous = new AtomicBoolean(true);
            
            final int m = 1000 * 1000;
            
            final CyclicBarrier cb = new CyclicBarrier(2);
            
            Thread t1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    await(cb);
                    for (int i = 0; i < m; i++) {
                        aaq.offer(i);
                    }
                    System.out.println("Offer done.");
                }
            });
    
            Thread t2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    await(cb);
                    int last = -1;
                    while (!Thread.currentThread().isInterrupted() && last + 1 == m) {
                        Integer o = (Integer)aaq.poll();
                        if (o != null) {
                            int last0 = last;
                            last = o;
                            if (last0 + 1 != o) {
                                System.out.println("Discontinuity! " + last0 + " -> " + last);
                                continuous.set(false);
                                return;
                            }
                        }
                    }
                    System.out.println("Poll done.");
                }
            });
            
            t1.start();
            t2.start();
    
            t1.join();
            t2.join();
            
            assertTrue("Discontinuity!", continuous.get());
        }
    }
}
