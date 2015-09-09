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

package io.reactivex.disposables;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.exceptions.CompositeException;

public class CompositeDisposableTest {

    @Test
    public void testSuccess() {
        final AtomicInteger counter = new AtomicInteger();
        CompositeDisposable s = new CompositeDisposable();
        s.add(new Disposable() {

            @Override
            public void dispose() {
                counter.incrementAndGet();
            }

        });

        s.add(new Disposable() {

            @Override
            public void dispose() {
                counter.incrementAndGet();
            }
        });

        s.dispose();

        assertEquals(2, counter.get());
    }

    @Test(timeout = 1000)
    public void shouldUnsubscribeAll() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        final CompositeDisposable s = new CompositeDisposable();

        final int count = 10;
        final CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < count; i++) {
            s.add(new Disposable() {

                @Override
                public void dispose() {
                    counter.incrementAndGet();
                }
            });
        }

        final List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        start.await();
                        s.dispose();
                    } catch (final InterruptedException e) {
                        fail(e.getMessage());
                    }
                }
            };
            t.start();
            threads.add(t);
        }

        start.countDown();
        for (final Thread t : threads) {
            t.join();
        }

        assertEquals(count, counter.get());
    }

    @Test
    public void testException() {
        final AtomicInteger counter = new AtomicInteger();
        CompositeDisposable s = new CompositeDisposable();
        s.add(new Disposable() {

            @Override
            public void dispose() {
                throw new RuntimeException("failed on first one");
            }

        });

        s.add(new Disposable() {

            @Override
            public void dispose() {
                counter.incrementAndGet();
            }

        });

        try {
            s.dispose();
            fail("Expecting an exception");
        } catch (RuntimeException e) {
            // we expect this
            assertEquals(e.getMessage(), "failed on first one");
        }

        // we should still have disposed to the second one
        assertEquals(1, counter.get());
    }

    @Test
    public void testCompositeException() {
        final AtomicInteger counter = new AtomicInteger();
        CompositeDisposable s = new CompositeDisposable();
        s.add(new Disposable() {

            @Override
            public void dispose() {
                throw new RuntimeException("failed on first one");
            }

        });

        s.add(new Disposable() {

            @Override
            public void dispose() {
                throw new RuntimeException("failed on second one too");
            }
        });

        s.add(new Disposable() {

            @Override
            public void dispose() {
                counter.incrementAndGet();
            }

        });

        try {
            s.dispose();
            fail("Expecting an exception");
        } catch (CompositeException e) {
            // we expect this
            assertEquals(e.getExceptions().size(), 2);
        }

        // we should still have disposed to the second one
        assertEquals(1, counter.get());
    }

    @Test
    public void testRemoveUnsubscribes() {
        BooleanDisposable s1 = new BooleanDisposable();
        BooleanDisposable s2 = new BooleanDisposable();

        CompositeDisposable s = new CompositeDisposable();
        s.add(s1);
        s.add(s2);

        s.remove(s1);

        assertTrue(s1.isDisposed());
        assertFalse(s2.isDisposed());
    }

    @Test
    public void testClear() {
        BooleanDisposable s1 = new BooleanDisposable();
        BooleanDisposable s2 = new BooleanDisposable();

        CompositeDisposable s = new CompositeDisposable();
        s.add(s1);
        s.add(s2);

        assertFalse(s1.isDisposed());
        assertFalse(s2.isDisposed());

        s.clear();

        assertTrue(s1.isDisposed());
        assertTrue(s2.isDisposed());
        assertFalse(s.isDisposed());

        BooleanDisposable s3 = new BooleanDisposable();

        s.add(s3);
        s.dispose();

        assertTrue(s3.isDisposed());
        assertTrue(s.isDisposed());
    }

    @Test
    public void testUnsubscribeIdempotence() {
        final AtomicInteger counter = new AtomicInteger();
        CompositeDisposable s = new CompositeDisposable();
        s.add(new Disposable() {

            @Override
            public void dispose() {
                counter.incrementAndGet();
            }

        });

        s.dispose();
        s.dispose();
        s.dispose();

        // we should have only disposed once
        assertEquals(1, counter.get());
    }

    @Test(timeout = 1000)
    public void testUnsubscribeIdempotenceConcurrently()
            throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        final CompositeDisposable s = new CompositeDisposable();

        final int count = 10;
        final CountDownLatch start = new CountDownLatch(1);
        s.add(new Disposable() {

            @Override
            public void dispose() {
                counter.incrementAndGet();
            }

        });

        final List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        start.await();
                        s.dispose();
                    } catch (final InterruptedException e) {
                        fail(e.getMessage());
                    }
                }
            };
            t.start();
            threads.add(t);
        }

        start.countDown();
        for (final Thread t : threads) {
            t.join();
        }

        // we should have only disposed once
        assertEquals(1, counter.get());
    }
    @Test
    public void testTryRemoveIfNotIn() {
        CompositeDisposable csub = new CompositeDisposable();
        
        CompositeDisposable csub1 = new CompositeDisposable();
        CompositeDisposable csub2 = new CompositeDisposable();
        
        csub.add(csub1);
        csub.remove(csub1);
        csub.add(csub2);
        
        csub.remove(csub1); // try removing agian
    }

    @Test(expected = NullPointerException.class)
    public void testAddingNullDisposableIllegal() {
        CompositeDisposable csub = new CompositeDisposable();
        csub.add(null);
    }

}