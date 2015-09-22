/**
 * Copyright 2015 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.nbp;

import static io.reactivex.internal.operators.nbp.NbpBlockingOperatorNext.next;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.observables.nbp.NbpBlockingObservable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.nbp.*;

public class NbpBlockingOperatorNextTest {

    private void fireOnNextInNewThread(final NbpSubject<String, String> o, final String value) {
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

    private void fireOnErrorInNewThread(final NbpSubject<String, String> o) {
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
        NbpSubject<String, String> obs = NbpPublishSubject.create();
        Iterator<String> it = next(obs).iterator();
        fireOnNextInNewThread(obs, "one");
        assertTrue(it.hasNext());
        assertEquals("one", it.next());

        fireOnNextInNewThread(obs, "two");
        assertTrue(it.hasNext());
        assertEquals("two", it.next());

        fireOnNextInNewThread(obs, "three");
        try {
            assertEquals("three", it.next());
        } catch (NoSuchElementException e) {
            fail("Calling next() without hasNext() should wait for next fire");
        }

        obs.onComplete();
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("At the end of an iterator should throw a NoSuchElementException");
        } catch (NoSuchElementException e) {
        }

        // If the NbpObservable is completed, hasNext always returns false and next always throw a NoSuchElementException.
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("At the end of an iterator should throw a NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testNextWithError() {
        NbpSubject<String, String> obs = NbpPublishSubject.create();
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
        NbpObservable<String> obs = NbpObservable.<String> empty().observeOn(Schedulers.newThread());
        Iterator<String> it = next(obs).iterator();

        assertFalse(it.hasNext());
        try {
            it.next();
            fail("At the end of an iterator should throw a NoSuchElementException");
        } catch (NoSuchElementException e) {
        }

        // If the NbpObservable is completed, hasNext always returns false and next always throw a NoSuchElementException.
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("At the end of an iterator should throw a NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testOnError() throws Throwable {
        NbpSubject<String, String> obs = NbpPublishSubject.create();
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
        NbpSubject<String, String> obs = NbpPublishSubject.create();
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
        // After the NbpObservable fails, hasNext and next always throw the exception.
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
        NbpSubject<String, String> obs = NbpPublishSubject.create();
        Iterator<String> it = next(obs).iterator();
        fireOnNextInNewThread(obs, "one");
        assertEquals("one", it.next());

        fireOnNextInNewThread(obs, "two");
        assertEquals("two", it.next());

        obs.onComplete();
        try {
            it.next();
            fail("At the end of an iterator should throw a NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testNextWithCallingHasNextMultipleTimes() {
        NbpSubject<String, String> obs = NbpPublishSubject.create();
        Iterator<String> it = next(obs).iterator();
        fireOnNextInNewThread(obs, "one");
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertEquals("one", it.next());

        obs.onComplete();
        try {
            it.next();
            fail("At the end of an iterator should throw a NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }

    /**
     * Confirm that no buffering or blocking of the NbpObservable onNext calls occurs and it just grabs the next emitted value.
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
        final NbpObservable<Integer> obs = NbpObservable.create(new NbpOnSubscribe<Integer>() {

            @Override
            public void accept(final NbpSubscriber<? super Integer> o) {
                o.onSubscribe(EmptyDisposable.INSTANCE);
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            while (running.get()) {
                                o.onNext(count.incrementAndGet());
                                timeHasPassed.countDown();
                            }
                            o.onComplete();
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
        NbpObservable<Long> o = NbpObservable.interval(100, TimeUnit.MILLISECONDS);
        NbpPublishSubject<Integer> terminal = NbpPublishSubject.create();
        NbpBlockingObservable<Long> source = o.takeUntil(terminal).toBlocking();

        Iterable<Long> iter = source.next();

        for (int j = 0; j < 3; j++) {
            NbpBlockingOperatorNext.NextIterator<Long> it = (NbpBlockingOperatorNext.NextIterator<Long>)iter.iterator();

            for (long i = 0; i < 10; i++) {
                Assert.assertEquals(true, it.hasNext());
                Assert.assertEquals(j + "th iteration next", Long.valueOf(i), it.next());
            }
            terminal.onNext(1);
        }
    }
    
    @Test
    public void testSynchronousNext() {
        assertEquals(1, BehaviorSubject.createDefault(1).take(1).toBlocking().single().intValue());
        assertEquals(2, BehaviorSubject.createDefault(2).toBlocking().iterator().next().intValue());
        assertEquals(3, BehaviorSubject.createDefault(3).toBlocking().next().iterator().next().intValue());
    }
}