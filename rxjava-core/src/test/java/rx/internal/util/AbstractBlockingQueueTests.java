/*
 * Written by Doug Lea and Martin Buchholz with assistance from members
 * of JCP JSR-166 Expert Group and released to the public domain, as
 * explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 *
 * Original locations:
 *  http://g.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/tck/JSR166TestCase.java
 *  http://g.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/tck/BlockingQueueTest.java
 *  http://g.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/tck/LinkedBlockingQueueTest.java
 *  http://g.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/tck/LinkedTransferQueueTest.java
 */
package rx.internal.util;

import junit.framework.AssertionFailedError;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public abstract class AbstractBlockingQueueTests extends AbstractQueueTests {
    public static long SHORT_DELAY_MS = 50;
    public static long SMALL_DELAY_MS = SHORT_DELAY_MS * 5;
    public static long MEDIUM_DELAY_MS = SHORT_DELAY_MS * 10;
    public static long LONG_DELAY_MS = SHORT_DELAY_MS * 200;

    // Some convenient Integer constants

    public static final Integer zero  = 0;
    public static final Integer one   = 1;
    public static final Integer two   = 2;
    public static final Integer three = 3;
    public static final Integer four  = 4;
    public static final Integer five  = 5;
    public static final Integer six   = 6;

    @Override
    protected Queue<Integer> getQueue() {
        return getBlockingQueue();
    }

    /**
     * The queue to test
     */
    protected abstract BlockingQueue<Integer> getBlockingQueue();

    /**
     * offer(null) throws NullPointerException
     */
    @Test
    public void testOfferNull() {
        final BlockingQueue<Integer> q = getBlockingQueue();
        try {
            q.offer(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * add(null) throws NullPointerException
     */
    @Test
    public void testAddNull() {
        final BlockingQueue<Integer> q = getBlockingQueue();
        try {
            q.add(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * timed offer(null) throws NullPointerException
     */
    @Test
    public void testTimedOfferNull() throws InterruptedException {
        final BlockingQueue<Integer> q = getBlockingQueue();
        long startTime = System.nanoTime();
        try {
            q.offer(null, LONG_DELAY_MS, MILLISECONDS);
            shouldThrow();
        } catch (NullPointerException success) {}
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
    }

    /**
     * put(null) throws NullPointerException
     */
    @Test
    public void testPutNull() throws InterruptedException {
        final BlockingQueue<Integer> q = getBlockingQueue();
        try {
            q.put(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * all elements successfully put are contained
     */
    @Test
    public void testPut() {
        LinkedTransferQueue<Integer> q = new LinkedTransferQueue<Integer>();
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.size());
            q.put(i);
            assertTrue(q.contains(i));
        }
    }

    /**
     * timed poll before a delayed offer times out; after offer succeeds;
     * on interruption throws
     */
    @Test
    public void testTimedPollWithOffer() throws InterruptedException {
        final BlockingQueue<Integer> q = getBlockingQueue();
        final CheckedBarrier barrier = new CheckedBarrier(2);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                assertNull(q.poll(timeoutMillis(), MILLISECONDS));
                assertTrue("Poll waited for timeout", millisElapsedSince(startTime) >= timeoutMillis());

                barrier.await();

                assertSame(zero, q.poll(LONG_DELAY_MS, MILLISECONDS));

                Thread.currentThread().interrupt();
                try {
                    q.poll(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse("Thread was not interrupted (1)", Thread.interrupted());

                barrier.await();
                try {
                    q.poll(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse("Thread was not interrupted (2)", Thread.interrupted());
            }});

        barrier.await();
        long startTime = System.nanoTime();
        assertTrue(q.offer(zero, LONG_DELAY_MS, MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);

        barrier.await();
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * take() blocks interruptibly when empty
     */
    @Test
    public void testTakeFromEmptyBlocksInterruptibly() {
        final BlockingQueue<Integer> q = getBlockingQueue();
        final CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                threadStarted.countDown();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse("Thread was not interrupted", Thread.interrupted());
            }});

        await(threadStarted);
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * take() throws InterruptedException immediately if interrupted
     * before waiting
     */
    @Test
    public void testTakeFromEmptyAfterInterrupt() {
        final BlockingQueue<Integer> q = getBlockingQueue();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                Thread.currentThread().interrupt();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        awaitTermination(t);
    }

    /**
     * timed poll() blocks interruptibly when empty
     */
    @Test
    public void testTimedPollFromEmptyBlocksInterruptibly() {
        final BlockingQueue<Integer> q = getBlockingQueue();
        final CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                threadStarted.countDown();
                try {
                    q.poll(2 * LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(threadStarted);
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * timed poll() throws InterruptedException immediately if
     * interrupted before waiting
     */
    @Test
    public void testTimedPollFromEmptyAfterInterrupt() {
        final BlockingQueue<Integer> q = getBlockingQueue();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                Thread.currentThread().interrupt();
                try {
                    q.poll(2 * LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        awaitTermination(t);
    }

    /**
     * take retrieves elements in FIFO order
     */
    @Test
    public void testTake() throws InterruptedException {
        BlockingQueue<Integer> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals((Integer)i, q.take());
        }
    }

    /**
     * Take removes existing elements until empty, then blocks interruptibly
     */
    @Test
    public void testBlockingTake() throws InterruptedException {
        final BlockingQueue q = populatedQueue(SIZE);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i) {
                    assertEquals(i, q.take());
                }

                Thread.currentThread().interrupt();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse("Thread shouldn't have been interrupted (1)", Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse("Thread shouldn't have been interrupted (2)", Thread.interrupted());
            }});

        await(pleaseInterrupt);
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * poll succeeds unless empty
     */
    @Test
    public void testPoll() {
        BlockingQueue<Integer> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals((Integer)i, q.poll());
        }
        assertNull(q.poll());
    }

    /**
     * timed poll with zero timeout succeeds when non-empty, else times out
     */
    @Test
    public void testTimedPoll0() throws InterruptedException {
        BlockingQueue<Integer> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals((Integer)i, q.poll(0, MILLISECONDS));
        }
        assertNull(q.poll(0, MILLISECONDS));
    }

    /**
     * timed poll with nonzero timeout succeeds when non-empty, else times out
     */
    @Test
    public void testTimedPoll() throws InterruptedException {
        BlockingQueue<Integer> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            long startTime = System.nanoTime();
            assertEquals(i, (int) q.poll(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
        long startTime = System.nanoTime();
        assertNull(q.poll(timeoutMillis(), MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
        checkEmpty(q);
    }

    /**
     * Interrupted timed poll throws InterruptedException instead of
     * returning timeout status
     */
    @Test
    public void testInterruptedTimedPoll() throws InterruptedException {
        final BlockingQueue<Integer> q = populatedQueue(SIZE);
        final CountDownLatch aboutToWait = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i) {
                    long t0 = System.nanoTime();
                    assertEquals(i, (int) q.poll(LONG_DELAY_MS, MILLISECONDS));
                    assertTrue(millisElapsedSince(t0) < SMALL_DELAY_MS);
                }
                long t0 = System.nanoTime();
                aboutToWait.countDown();
                try {
                    q.poll(MEDIUM_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {
                    assertTrue(millisElapsedSince(t0) < MEDIUM_DELAY_MS);
                }
            }});

        aboutToWait.await();
        waitForThreadToEnterWaitState(t, SMALL_DELAY_MS);
        t.interrupt();
        awaitTermination(t, MEDIUM_DELAY_MS);
        checkEmpty(q);
    }

    /**
     * timed poll after thread interrupted throws InterruptedException
     * instead of returning timeout status
     */
    @Test
    public void testTimedPollAfterInterrupt() throws InterruptedException {
        final BlockingQueue<Integer> q = populatedQueue(SIZE);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                Thread.currentThread().interrupt();
                for (int i = 0; i < SIZE; ++i) {
                    long t0 = System.nanoTime();
                    assertEquals(i, (int) q.poll(LONG_DELAY_MS, MILLISECONDS));
                    assertTrue(millisElapsedSince(t0) < SMALL_DELAY_MS);
                }
                try {
                    q.poll(MEDIUM_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {
                }
            }
        });

        awaitTermination(t, MEDIUM_DELAY_MS);
        checkEmpty(q);
    }

    /**
     * peek returns next element, or null if empty
     */
    @Test
    public void testPeek() {
        BlockingQueue<Integer> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals((Integer)i, q.peek());
            assertEquals((Integer)i, q.poll());
            assertTrue(q.peek() == null ||
                    !q.peek().equals(i));
        }
        assertNull(q.peek());
    }

    /**
     * element returns next element, or throws NSEE if empty
     */
    @Test
    public void testElement() {
        BlockingQueue<Integer> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals((Integer)i, q.element());
            assertEquals((Integer)i, q.poll());
        }
        try {
            q.element();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * remove removes next element, or throws NSEE if empty
     */
    @Test
    public void testRemove() {
        BlockingQueue<Integer> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals((Integer)i, q.remove());
        }
        try {
            q.remove();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * contains(x) reports true when elements added but not yet removed
     */
    @Test
    public void testContains() {
        BlockingQueue<Integer> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(q.contains(new Integer(i)));
            q.poll();
            assertFalse(q.contains(new Integer(i)));
        }
    }

    /**
     * clear removes all elements
     */
    @Test
    public void testClear() {
        BlockingQueue<Integer> q = populatedQueue(SIZE);
        q.clear();
        assertTrue(q.isEmpty());
        assertEquals(0, q.size());
        q.add(one);
        assertFalse(q.isEmpty());
        assertTrue(q.contains(one));
        q.clear();
        assertTrue(q.isEmpty());
    }

    /**
     * timed poll retrieves elements across Executor threads
     */
    @Test
    public void testPollInExecutor() {
        final BlockingQueue<Integer> q = getBlockingQueue();
        final CheckedBarrier threadsStarted = new CheckedBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertNull(q.poll());
                threadsStarted.await();
                assertSame(one, q.poll(LONG_DELAY_MS, MILLISECONDS));
                checkEmpty(q);
            }});

        executor.execute(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                threadsStarted.await();
                q.put(one);
            }});

        joinPool(executor);
    }

    /**
     * Fails with message "should throw exception".
     */
    public void shouldThrow() {
        fail("Should throw exception");
    }

    /**
     * Returns a new started daemon Thread running the given runnable.
     */
    Thread newStartedThread(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.start();
        return t;
    }

    /**
     * Returns the number of milliseconds since time given by
     * startNanoTime, which must have been previously returned from a
     * call to System.nanoTime.
     */
    static long millisElapsedSince(long startNanoTime) {
        return NANOSECONDS.toMillis(System.nanoTime() - startNanoTime);
    }

    /**
     * Waits for LONG_DELAY_MS milliseconds for the thread to
     * terminate (using {@link Thread#join(long)}), else interrupts
     * the thread (in the hope that it may terminate later) and fails.
     */
    void awaitTermination(Thread t) {
        awaitTermination(t, LONG_DELAY_MS);
    }

    /**
     * Waits for the specified time (in milliseconds) for the thread
     * to terminate (using {@link Thread#join(long)}), else interrupts
     * the thread (in the hope that it may terminate later) and fails.
     */
    void awaitTermination(Thread t, long timeoutMillis) {
        try {
            t.join(timeoutMillis);
        } catch (InterruptedException ie) {
            fail("Unexpected exception: " + ie);
        } finally {
            if (t.getState() != Thread.State.TERMINATED) {
                t.interrupt();
                fail("Test timed out");
            }
        }
    }

    public void await(CountDownLatch latch) {
        try {
            assertTrue("Await timed out", latch.await(LONG_DELAY_MS, MILLISECONDS));
        } catch (Throwable t) {
            fail("Unexpected exception: " + t);
        }
    }

    public void await(Semaphore semaphore) {
        try {
            assertTrue(semaphore.tryAcquire(LONG_DELAY_MS, MILLISECONDS));
        } catch (Throwable t) {
            fail("Unexpected exception: " + t);
        }
    }

    /**
     * Checks that thread does not terminate within the default
     * millisecond delay of {@code timeoutMillis()}.
     */
    void assertThreadStaysAlive(Thread thread) {
        assertThreadStaysAlive(thread, timeoutMillis());
    }

    /**
     * Checks that thread does not terminate within the given millisecond delay.
     */
    void assertThreadStaysAlive(Thread thread, long millis) {
        try {
            // No need to optimize the failing case via Thread.join.
            delay(millis);
            assertTrue("Thread should be alive", thread.isAlive());
        } catch (InterruptedException ie) {
            fail("Unexpected InterruptedException");
        }
    }

    /**
     * Returns a timeout in milliseconds to be used in tests that
     * verify that operations block or time out.
     */
    long timeoutMillis() {
        return SHORT_DELAY_MS / 4;
    }

    /**
     * Delays, via Thread.sleep, for the given millisecond delay, but
     * if the sleep is shorter than specified, may re-sleep or yield
     * until time elapses.
     */
    static void delay(long millis) throws InterruptedException {
        long startTime = System.nanoTime();
        long ns = millis * 1000 * 1000;
        for (;;) {
            if (millis > 0L)
                Thread.sleep(millis);
            else // too short to sleep
                Thread.yield();
            long d = ns - (System.nanoTime() - startTime);
            if (d > 0L)
                millis = d / (1000 * 1000);
            else
                break;
        }
    }

    public abstract class CheckedRunnable implements Runnable {
        protected abstract void realRun() throws Throwable;

        public final void run() {
            try {
                realRun();
            } catch (Throwable t) {
                fail("Unexpected exception: "+ t);
            }
        }
    }

    /**
     * A CyclicBarrier that uses timed await and fails with
     * AssertionFailedErrors instead of throwing checked exceptions.
     */
    public class CheckedBarrier extends CyclicBarrier {
        public CheckedBarrier(int parties) { super(parties); }

        public int await() {
            try {
                return super.await(2 * LONG_DELAY_MS, MILLISECONDS);
            } catch (TimeoutException e) {
                throw new AssertionFailedError("timed out");
            } catch (Exception e) {
                AssertionFailedError afe =
                        new AssertionFailedError("Unexpected exception: " + e);
                afe.initCause(e);
                throw afe;
            }
        }
    }

    /**
     * Returns a new queue of given size containing consecutive
     * Integers 0 ... n.
     */
    private BlockingQueue<Integer> populatedQueue(int n) {
        final BlockingQueue<Integer> q = getBlockingQueue();
        assertTrue(q.isEmpty());
        for (int i = 0; i < n; i++)
            assertTrue(q.offer(i));
        assertFalse(q.isEmpty());
        assertEquals(n, q.size());
        return q;
    }

    /**
     * Waits out termination of a thread pool or fails doing so.
     */
    void joinPool(ExecutorService exec) {
        try {
            exec.shutdown();
            if (!exec.awaitTermination(2 * LONG_DELAY_MS, MILLISECONDS))
                fail("ExecutorService " + exec +
                        " did not terminate in a timely manner");
        } catch (SecurityException ok) {
            // Allowed in case test doesn't have privs
        } catch (InterruptedException ie) {
            fail("Unexpected InterruptedException");
        }
    }

    void checkEmpty(BlockingQueue q) throws InterruptedException {
        assertTrue(q.isEmpty());
        assertEquals(0, q.size());
        assertNull(q.peek());
        assertNull(q.poll());
        assertNull(q.poll(0, MILLISECONDS));
        try {
            q.element();
            shouldThrow();
        } catch (NoSuchElementException success) {}
        try {
            q.remove();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * Spin-waits up to the specified number of milliseconds for the given
     * thread to enter a wait state: BLOCKED, WAITING, or TIMED_WAITING.
     */
    void waitForThreadToEnterWaitState(Thread thread, long timeoutMillis) {
        long startTime = System.nanoTime();
        for (;;) {
            Thread.State s = thread.getState();
            if (s == Thread.State.BLOCKED ||
                    s == Thread.State.WAITING ||
                    s == Thread.State.TIMED_WAITING)
                return;
            else if (s == Thread.State.TERMINATED)
                fail("Unexpected thread termination");
            else if (millisElapsedSince(startTime) > timeoutMillis) {
                assertTrue(thread.isAlive());
                return;
            }
            Thread.yield();
        }
    }
}
