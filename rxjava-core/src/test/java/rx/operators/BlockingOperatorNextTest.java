/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static rx.operators.BlockingOperatorNext.next;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import rx.Observable;
import rx.Subscriber;
import rx.exceptions.TestException;
import rx.observables.BlockingObservable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

public class BlockingOperatorNextTest {

    private void fireOnNextInNewThread(final Subject<String, String> o, final String value) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // ignore
                }
                o.onNext(value);
            }
        }.start();
    }

    private void fireOnErrorInNewThread(final Subject<String, String> o) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // ignore
                }
                o.onError(new TestException());
            }
        }.start();
    }

    @Test
    public void testNext() {
        Subject<String, String> obs = PublishSubject.create();
        Iterator<String> it = next(obs).iterator();
        fireOnNextInNewThread(obs, "one");
        assertTrue(it.hasNext());
        assertEquals("one", it.next());

        fireOnNextInNewThread(obs, "two");
        assertTrue(it.hasNext());
        assertEquals("two", it.next());

        obs.onCompleted();
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("At the end of an iterator should throw a NoSuchElementException");
        } catch (NoSuchElementException e) {
        }

        // If the observable is completed, hasNext always returns false and next always throw a NoSuchElementException.
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("At the end of an iterator should throw a NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testNextWithError() {
        Subject<String, String> obs = PublishSubject.create();
        Iterator<String> it = next(obs).iterator();
        fireOnNextInNewThread(obs, "one");
        assertTrue(it.hasNext());
        assertEquals("one", it.next());

        fireOnErrorInNewThread(obs);
        try {
            it.hasNext();
            fail("Expected an TestException");
        } catch (TestException e) {
        }

        assertErrorAfterObservableFail(it);
    }

    @Test
    public void testNextWithEmpty() {
        Observable<String> obs = Observable.<String> empty().observeOn(Schedulers.newThread());
        Iterator<String> it = next(obs).iterator();

        assertFalse(it.hasNext());
        try {
            it.next();
            fail("At the end of an iterator should throw a NoSuchElementException");
        } catch (NoSuchElementException e) {
        }

        // If the observable is completed, hasNext always returns false and next always throw a NoSuchElementException.
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("At the end of an iterator should throw a NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testOnError() throws Throwable {
        Subject<String, String> obs = PublishSubject.create();
        Iterator<String> it = next(obs).iterator();

        obs.onError(new TestException());
        try {
            it.hasNext();
            fail("Expected an TestException");
        } catch (TestException e) {
            // successful
        }

        assertErrorAfterObservableFail(it);
    }

    @Test
    public void testOnErrorInNewThread() {
        Subject<String, String> obs = PublishSubject.create();
        Iterator<String> it = next(obs).iterator();

        fireOnErrorInNewThread(obs);

        try {
            it.hasNext();
            fail("Expected an TestException");
        } catch (TestException e) {
            // successful
        }

        assertErrorAfterObservableFail(it);
    }

    private void assertErrorAfterObservableFail(Iterator<String> it) {
        // After the observable fails, hasNext and next always throw the exception.
        try {
            it.hasNext();
            fail("hasNext should throw a TestException");
        } catch (TestException e) {
        }
        try {
            it.next();
            fail("next should throw a TestException");
        } catch (TestException e) {
        }
    }

    @Test
    public void testNextWithOnlyUsingNextMethod() {
        Subject<String, String> obs = PublishSubject.create();
        Iterator<String> it = next(obs).iterator();
        fireOnNextInNewThread(obs, "one");
        assertEquals("one", it.next());

        fireOnNextInNewThread(obs, "two");
        assertEquals("two", it.next());

        obs.onCompleted();
        try {
            it.next();
            fail("At the end of an iterator should throw a NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testNextWithCallingHasNextMultipleTimes() {
        Subject<String, String> obs = PublishSubject.create();
        Iterator<String> it = next(obs).iterator();
        fireOnNextInNewThread(obs, "one");
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertEquals("one", it.next());

        obs.onCompleted();
        try {
            it.next();
            fail("At the end of an iterator should throw a NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }

    /**
     * Confirm that no buffering or blocking of the Observable onNext calls occurs and it just grabs the next emitted value.
     * <p/>
     * This results in output such as => a: 1 b: 2 c: 89
     * 
     * @throws Throwable
     */
    @Test
    public void testNoBufferingOrBlockingOfSequence() throws Throwable {
        final CountDownLatch finished = new CountDownLatch(1);
        final int COUNT = 30;
        final CountDownLatch timeHasPassed = new CountDownLatch(COUNT);
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicInteger count = new AtomicInteger(0);
        final Observable<Integer> obs = Observable.create(new Observable.OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> o) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            while (running.get()) {
                                o.onNext(count.incrementAndGet());
                                timeHasPassed.countDown();
                            }
                            o.onCompleted();
                        } catch (Throwable e) {
                            o.onError(e);
                        } finally {
                            finished.countDown();
                        }
                    }
                }).start();
            }

        });

        Iterator<Integer> it = next(obs).iterator();

        assertTrue(it.hasNext());
        int a = it.next();
        assertTrue(it.hasNext());
        int b = it.next();
        // we should have a different value
        assertTrue("a and b should be different", a != b);

        // wait for some time (if times out we are blocked somewhere so fail ... set very high for very slow, constrained machines)
        timeHasPassed.await(8000, TimeUnit.MILLISECONDS);

        assertTrue(it.hasNext());
        int c = it.next();

        assertTrue("c should not just be the next in sequence", c != (b + 1));
        assertTrue("expected that c [" + c + "] is higher than or equal to " + COUNT, c >= COUNT);

        assertTrue(it.hasNext());
        int d = it.next();
        assertTrue(d > c);

        // shut down the thread
        running.set(false);

        finished.await();

        assertFalse(it.hasNext());

        System.out.println("a: " + a + " b: " + b + " c: " + c);
    }

    @Test /* (timeout = 8000) */
    public void testSingleSourceManyIterators() throws InterruptedException {
        PublishSubject<Long> ps = PublishSubject.create();
        BlockingObservable<Long> source = ps.take(10).toBlocking();

        Iterable<Long> iter = source.next();

        for (int j = 0; j < 3; j++) {
            BlockingOperatorNext.NextIterator<Long> it = (BlockingOperatorNext.NextIterator<Long>)iter.iterator();

            for (long i = 0; i < 9; i++) {
                // hasNext has to set the waiting to true, otherwise, all onNext will be skipped
                it.setWaiting(true);
                ps.onNext(i);
                Assert.assertEquals(true, it.hasNext());
                Assert.assertEquals(j + "th iteration", Long.valueOf(i), it.next());
            }
            it.setWaiting(true);
            ps.onNext(9L);

            Assert.assertEquals(j + "th iteration", false, it.hasNext());
        }

    }
}
