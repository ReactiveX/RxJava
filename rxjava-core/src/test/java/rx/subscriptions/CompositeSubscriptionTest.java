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
package rx.subscriptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Subscription;
import rx.exceptions.CompositeException;

public class CompositeSubscriptionTest {

    @Test
    public void testSuccess() {
        final AtomicInteger counter = new AtomicInteger();
        CompositeSubscription s = new CompositeSubscription();
        s.add(new Subscription() {

            @Override
            public void unsubscribe() {
                counter.incrementAndGet();
            }

            @Override
            public boolean isUnsubscribed() {
                return false;
            }
        });

        s.add(new Subscription() {

            @Override
            public void unsubscribe() {
                counter.incrementAndGet();
            }

            @Override
            public boolean isUnsubscribed() {
                return false;
            }
        });

        s.unsubscribe();

        assertEquals(2, counter.get());
    }

    @Test(timeout = 1000)
    public void shouldUnsubscribeAll() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        final CompositeSubscription s = new CompositeSubscription();

        final int count = 10;
        final CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < count; i++) {
            s.add(new Subscription() {

                @Override
                public void unsubscribe() {
                    counter.incrementAndGet();
                }

                @Override
                public boolean isUnsubscribed() {
                    return false;
                }
            });
        }

        final List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < count; i++) {
            final Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        start.await();
                        s.unsubscribe();
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
        CompositeSubscription s = new CompositeSubscription();
        s.add(new Subscription() {

            @Override
            public void unsubscribe() {
                throw new RuntimeException("failed on first one");
            }

            @Override
            public boolean isUnsubscribed() {
                return false;
            }
        });

        s.add(new Subscription() {

            @Override
            public void unsubscribe() {
                counter.incrementAndGet();
            }

            @Override
            public boolean isUnsubscribed() {
                return false;
            }
        });

        try {
            s.unsubscribe();
            fail("Expecting an exception");
        } catch (RuntimeException e) {
            // we expect this
            assertEquals(e.getMessage(), "failed on first one");
        }

        // we should still have unsubscribed to the second one
        assertEquals(1, counter.get());
    }

    @Test
    public void testCompositeException() {
        final AtomicInteger counter = new AtomicInteger();
        CompositeSubscription s = new CompositeSubscription();
        s.add(new Subscription() {

            @Override
            public void unsubscribe() {
                throw new RuntimeException("failed on first one");
            }

            @Override
            public boolean isUnsubscribed() {
                return false;
            }
        });

        s.add(new Subscription() {

            @Override
            public void unsubscribe() {
                throw new RuntimeException("failed on second one too");
            }

            @Override
            public boolean isUnsubscribed() {
                return false;
            }
        });

        s.add(new Subscription() {

            @Override
            public void unsubscribe() {
                counter.incrementAndGet();
            }

            @Override
            public boolean isUnsubscribed() {
                return false;
            }
        });

        try {
            s.unsubscribe();
            fail("Expecting an exception");
        } catch (CompositeException e) {
            // we expect this
            assertEquals(e.getExceptions().size(), 2);
        }

        // we should still have unsubscribed to the second one
        assertEquals(1, counter.get());
    }

    @Test
    public void testRemoveUnsubscribes() {
        BooleanSubscription s1 = new BooleanSubscription();
        BooleanSubscription s2 = new BooleanSubscription();

        CompositeSubscription s = new CompositeSubscription();
        s.add(s1);
        s.add(s2);

        s.remove(s1);

        assertTrue(s1.isUnsubscribed());
        assertFalse(s2.isUnsubscribed());
    }

    @Test
    public void testClear() {
        BooleanSubscription s1 = new BooleanSubscription();
        BooleanSubscription s2 = new BooleanSubscription();

        CompositeSubscription s = new CompositeSubscription();
        s.add(s1);
        s.add(s2);

        assertFalse(s1.isUnsubscribed());
        assertFalse(s2.isUnsubscribed());

        s.clear();

        assertTrue(s1.isUnsubscribed());
        assertTrue(s2.isUnsubscribed());
        assertFalse(s.isUnsubscribed());

        BooleanSubscription s3 = new BooleanSubscription();

        s.add(s3);
        s.unsubscribe();

        assertTrue(s3.isUnsubscribed());
        assertTrue(s.isUnsubscribed());
    }

    @Test
    public void testUnsubscribeIdempotence() {
        final AtomicInteger counter = new AtomicInteger();
        CompositeSubscription s = new CompositeSubscription();
        s.add(new Subscription() {

            @Override
            public void unsubscribe() {
                counter.incrementAndGet();
            }

            @Override
            public boolean isUnsubscribed() {
                return false;
            }
        });

        s.unsubscribe();
        s.unsubscribe();
        s.unsubscribe();

        // we should have only unsubscribed once
        assertEquals(1, counter.get());
    }

    @Test(timeout = 1000)
    public void testUnsubscribeIdempotenceConcurrently()
            throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        final CompositeSubscription s = new CompositeSubscription();

        final int count = 10;
        final CountDownLatch start = new CountDownLatch(1);
        s.add(new Subscription() {

            @Override
            public void unsubscribe() {
                counter.incrementAndGet();
            }

            @Override
            public boolean isUnsubscribed() {
                return false;
            }
        });

        final List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < count; i++) {
            final Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        start.await();
                        s.unsubscribe();
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

        // we should have only unsubscribed once
        assertEquals(1, counter.get());
    }
    @Test
    public void testTryRemoveIfNotIn() {
        CompositeSubscription csub = new CompositeSubscription();
        
        CompositeSubscription csub1 = new CompositeSubscription();
        CompositeSubscription csub2 = new CompositeSubscription();
        
        csub.add(csub1);
        csub.remove(csub1);
        csub.add(csub2);
        
        csub.remove(csub1); // try removing agian
    }
}
