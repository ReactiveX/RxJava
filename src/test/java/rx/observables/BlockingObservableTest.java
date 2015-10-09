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
package rx.observables;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;
import org.mockito.*;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.exceptions.*;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class BlockingObservableTest {

    @Mock
    Subscriber<Integer> w;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testLast() {
        BlockingObservable<String> obs = BlockingObservable.from(Observable.just("one", "two", "three"));

        assertEquals("three", obs.last());
    }

    @Test(expected = NoSuchElementException.class)
    public void testLastEmptyObservable() {
        BlockingObservable<Object> obs = BlockingObservable.from(Observable.empty());
        obs.last();
    }

    @Test
    public void testLastOrDefault() {
        BlockingObservable<Integer> observable = BlockingObservable.from(Observable.just(1, 0, -1));
        int last = observable.lastOrDefault(-100, new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer args) {
                return args >= 0;
            }
        });
        assertEquals(0, last);
    }

    @Test
    public void testLastOrDefault1() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.just("one", "two", "three"));
        assertEquals("three", observable.lastOrDefault("default"));
    }

    @Test
    public void testLastOrDefault2() {
        BlockingObservable<Object> observable = BlockingObservable.from(Observable.empty());
        assertEquals("default", observable.lastOrDefault("default"));
    }

    @Test
    public void testLastOrDefaultWithPredicate() {
        BlockingObservable<Integer> observable = BlockingObservable.from(Observable.just(1, 0, -1));
        int last = observable.lastOrDefault(0, new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer args) {
                return args < 0;
            }
        });

        assertEquals(-1, last);
    }

    @Test
    public void testLastOrDefaultWrongPredicate() {
        BlockingObservable<Integer> observable = BlockingObservable.from(Observable.just(-1, -2, -3));
        int last = observable.lastOrDefault(0, new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer args) {
                return args >= 0;
            }
        });
        assertEquals(0, last);
    }

    @Test
    public void testLastWithPredicate() {
        BlockingObservable<String> obs = BlockingObservable.from(Observable.just("one", "two", "three"));
        assertEquals("two", obs.last(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String s) {
                return s.length() == 3;
            }
        }));
    }

    @Test
    public void testSingle() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.just("one"));
        assertEquals("one", observable.single());
    }

    @Test
    public void testSingleDefault() {
        BlockingObservable<Object> observable = BlockingObservable.from(Observable.empty());
        assertEquals("default", observable.singleOrDefault("default"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSingleDefaultPredicateMatchesMoreThanOne() {
        BlockingObservable.from(Observable.just("one", "two")).singleOrDefault("default", new Func1<String, Boolean>() {
            @Override
            public Boolean call(String args) {
                return args.length() == 3;
            }
        });
    }

    @Test
    public void testSingleDefaultPredicateMatchesNothing() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.just("one", "two"));
        String result = observable.singleOrDefault("default", new Func1<String, Boolean>() {
            @Override
            public Boolean call(String args) {
                return args.length() == 4;
            }
        });
        assertEquals("default", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSingleDefaultWithMoreThanOne() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.just("one", "two", "three"));
        observable.singleOrDefault("default");
    }

    @Test
    public void testSingleWithPredicateDefault() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.just("one", "two", "four"));
        assertEquals("four", observable.single(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String s) {
                return s.length() == 4;
            }
        }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSingleWrong() {
        BlockingObservable<Integer> observable = BlockingObservable.from(Observable.just(1, 2));
        observable.single();
    }

    @Test(expected = NoSuchElementException.class)
    public void testSingleWrongPredicate() {
        BlockingObservable<Integer> observable = BlockingObservable.from(Observable.just(-1));
        observable.single(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer args) {
                return args > 0;
            }
        });
    }

    @Test
    public void testToIterable() {
        BlockingObservable<String> obs = BlockingObservable.from(Observable.just("one", "two", "three"));

        Iterator<String> it = obs.toIterable().iterator();

        assertEquals(true, it.hasNext());
        assertEquals("one", it.next());

        assertEquals(true, it.hasNext());
        assertEquals("two", it.next());

        assertEquals(true, it.hasNext());
        assertEquals("three", it.next());

        assertEquals(false, it.hasNext());

    }

    @Test(expected = NoSuchElementException.class)
    public void testToIterableNextOnly() {
        BlockingObservable<Integer> obs = BlockingObservable.from(Observable.just(1, 2, 3));

        Iterator<Integer> it = obs.toIterable().iterator();

        Assert.assertEquals((Integer) 1, it.next());
        Assert.assertEquals((Integer) 2, it.next());
        Assert.assertEquals((Integer) 3, it.next());

        it.next();
    }

    @Test(expected = NoSuchElementException.class)
    public void testToIterableNextOnlyTwice() {
        BlockingObservable<Integer> obs = BlockingObservable.from(Observable.just(1, 2, 3));

        Iterator<Integer> it = obs.toIterable().iterator();

        Assert.assertEquals((Integer) 1, it.next());
        Assert.assertEquals((Integer) 2, it.next());
        Assert.assertEquals((Integer) 3, it.next());

        boolean exc = false;
        try {
            it.next();
        } catch (NoSuchElementException ex) {
            exc = true;
        }
        Assert.assertEquals(true, exc);

        it.next();
    }

    @Test
    public void testToIterableManyTimes() {
        BlockingObservable<Integer> obs = BlockingObservable.from(Observable.just(1, 2, 3));

        Iterable<Integer> iter = obs.toIterable();

        for (int j = 0; j < 3; j++) {
            Iterator<Integer> it = iter.iterator();

            Assert.assertTrue(it.hasNext());
            Assert.assertEquals((Integer) 1, it.next());
            Assert.assertTrue(it.hasNext());
            Assert.assertEquals((Integer) 2, it.next());
            Assert.assertTrue(it.hasNext());
            Assert.assertEquals((Integer) 3, it.next());
            Assert.assertFalse(it.hasNext());
        }
    }

    @Test(expected = TestException.class)
    public void testToIterableWithException() {
        BlockingObservable<String> obs = BlockingObservable.from(Observable.create(new Observable.OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> observer) {
                observer.onNext("one");
                observer.onError(new TestException());
            }
        }));

        Iterator<String> it = obs.toIterable().iterator();

        assertEquals(true, it.hasNext());
        assertEquals("one", it.next());

        assertEquals(true, it.hasNext());
        it.next();

    }

    @Test
    public void testForEachWithError() {
        try {
            BlockingObservable.from(Observable.create(new Observable.OnSubscribe<String>() {

                @Override
                public void call(final Subscriber<? super String> observer) {
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            observer.onNext("one");
                            observer.onNext("two");
                            observer.onNext("three");
                            observer.onCompleted();
                        }
                    }).start();
                }
            })).forEach(new Action1<String>() {

                @Override
                public void call(String t1) {
                    throw new RuntimeException("fail");
                }
            });
            fail("we expect an exception to be thrown");
        } catch (Throwable e) {
            // do nothing as we expect this
        }
    }

    @Test
    public void testFirst() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.just("one", "two", "three"));
        assertEquals("one", observable.first());
    }

    @Test(expected = NoSuchElementException.class)
    public void testFirstWithEmpty() {
        BlockingObservable.from(Observable.<String> empty()).first();
    }

    @Test
    public void testFirstWithPredicate() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.just("one", "two", "three"));
        String first = observable.first(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String args) {
                return args.length() > 3;
            }
        });
        assertEquals("three", first);
    }

    @Test(expected = NoSuchElementException.class)
    public void testFirstWithPredicateAndEmpty() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.just("one", "two", "three"));
        observable.first(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String args) {
                return args.length() > 5;
            }
        });
    }

    @Test
    public void testFirstOrDefault() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.just("one", "two", "three"));
        assertEquals("one", observable.firstOrDefault("default"));
    }

    @Test
    public void testFirstOrDefaultWithEmpty() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.<String>empty());
        assertEquals("default", observable.firstOrDefault("default"));
    }

    @Test
    public void testFirstOrDefaultWithPredicate() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.just("one", "two", "three"));
        String first = observable.firstOrDefault("default", new Func1<String, Boolean>() {
            @Override
            public Boolean call(String args) {
                return args.length() > 3;
            }
        });
        assertEquals("three", first);
    }

    @Test
    public void testFirstOrDefaultWithPredicateAndEmpty() {
        BlockingObservable<String> observable = BlockingObservable.from(Observable.just("one", "two", "three"));
        String first = observable.firstOrDefault("default", new Func1<String, Boolean>() {
            @Override
            public Boolean call(String args) {
                return args.length() > 5;
            }
        });
        assertEquals("default", first);
    }

    @Test
    public void testSingleOrDefaultUnsubscribe() throws InterruptedException {
        final CountDownLatch unsubscribe = new CountDownLatch(1);
        Observable<Integer> o = Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        unsubscribe.countDown();
                    }
                }));
                subscriber.onNext(1);
                subscriber.onNext(2);
                // Don't call `onCompleted` to emulate an infinite stream
            }
        }).subscribeOn(Schedulers.newThread());
        try {
            o.toBlocking().singleOrDefault(-1);
            fail("Expected IllegalArgumentException because there are 2 elements");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        assertTrue("Timeout means `unsubscribe` is not called", unsubscribe.await(30, TimeUnit.SECONDS));
    }

    private Action1<BlockingObservable<Void>> singleAction = new Action1<BlockingObservable<Void>>() {
        @Override
        public void call(final BlockingObservable<Void> o) {
            o.single();
        }
    };

    @Test
    public void testUnsubscribeFromSingleWhenInterrupted() throws InterruptedException {
        new InterruptionTests().assertUnsubscribeIsInvoked("single()", singleAction);
    }

    @Test
    public void testNoInterruptedExceptionWhenInterruptedWhileSingleOnSynchronousObservable() throws InterruptedException {
        new InterruptionTests().assertNoInterruptedExceptionWhenSynchronous("single()", singleAction);
    }

    private Action1<BlockingObservable<Void>> forEachAction = new Action1<BlockingObservable<Void>>() {
        @Override
        public void call(final BlockingObservable<Void> o) {
            o.forEach(new Action1<Void>() {
                @Override
                public void call(final Void aVoid) {
                    // nothing
                }
            });
        }
    };

    @Test
    public void testUnsubscribeFromForEachWhenInterrupted() throws InterruptedException {
        new InterruptionTests().assertUnsubscribeIsInvoked("forEach()", forEachAction);
    }

    @Test
    public void testNoInterruptedExceptionWhenInterruptedWhileForEachOnSynchronousObservable() throws InterruptedException {
        new InterruptionTests().assertNoInterruptedExceptionWhenSynchronous("forEach()", forEachAction);
    }

    private Action1<BlockingObservable<Void>> firstAction = new Action1<BlockingObservable<Void>>() {
        @Override
        public void call(final BlockingObservable<Void> o) {
            o.first();
        }
    };

    @Test
    public void testUnsubscribeFromFirstWhenInterrupted() throws InterruptedException {
        new InterruptionTests().assertUnsubscribeIsInvoked("first()", firstAction);
    }

    @Test
    public void testNoInterruptedExceptionWhenInterruptedWhileFirstOnSynchronousObservable() throws InterruptedException {
        new InterruptionTests().assertNoInterruptedExceptionWhenSynchronous("first()", firstAction);
    }

    private Action1<BlockingObservable<Void>> lastAction = new Action1<BlockingObservable<Void>>() {
        @Override
        public void call(final BlockingObservable<Void> o) {
            o.last();
        }
    };

    @Test
    public void testUnsubscribeFromLastWhenInterrupted() throws InterruptedException {
        new InterruptionTests().assertUnsubscribeIsInvoked("last()", lastAction);
    }

    @Test
    public void testNoInterruptedExceptionWhenInterruptedWhileLastOnSynchronousObservable() throws InterruptedException {
        new InterruptionTests().assertNoInterruptedExceptionWhenSynchronous("last()", lastAction);
    }

    private Action1<BlockingObservable<Void>> latestAction = new Action1<BlockingObservable<Void>>() {
        @Override
        public void call(final BlockingObservable<Void> o) {
            o.latest().iterator().next();
        }
    };

    @Test
    public void testUnsubscribeFromLatestWhenInterrupted() throws InterruptedException {
        new InterruptionTests().assertUnsubscribeIsInvoked("latest()", latestAction);
    }

    // NOTE: latest() is intended to be async, so InterruptedException will be thrown even if synchronous

    private Action1<BlockingObservable<Void>> nextAction = new Action1<BlockingObservable<Void>>() {
        @Override
        public void call(final BlockingObservable<Void> o) {
            o.next().iterator().next();
        }
    };

    @Test
    public void testUnsubscribeFromNextWhenInterrupted() throws InterruptedException {
        new InterruptionTests().assertUnsubscribeIsInvoked("next()", nextAction);
    }

    // NOTE: next() is intended to be async, so InterruptedException will be thrown even if synchronous

    private Action1<BlockingObservable<Void>> getIteratorAction = new Action1<BlockingObservable<Void>>() {
        @Override
        public void call(final BlockingObservable<Void> o) {
            o.getIterator().next();
        }
    };

    @Test
    public void testUnsubscribeFromGetIteratorWhenInterrupted() throws InterruptedException {
        new InterruptionTests().assertUnsubscribeIsInvoked("getIterator()", getIteratorAction);
    }

    @Test
    public void testNoInterruptedExceptionWhenInterruptedWhileGetIteratorOnSynchronousObservable() throws InterruptedException {
        new InterruptionTests().assertNoInterruptedExceptionWhenSynchronous("getIterator()", getIteratorAction);
    }

    private Action1<BlockingObservable<Void>> toIterableAction = new Action1<BlockingObservable<Void>>() {
        @Override
        public void call(final BlockingObservable<Void> o) {
            o.toIterable().iterator().next();
        }
    };

    @Test
    public void testUnsubscribeFromToIterableWhenInterrupted() throws InterruptedException {
        new InterruptionTests().assertUnsubscribeIsInvoked("toIterable()", toIterableAction);
    }

    @Test
    public void testNoInterruptedExceptionWhenInterruptedWhileToIterableOnSynchronousObservable() throws InterruptedException {
        new InterruptionTests().assertNoInterruptedExceptionWhenSynchronous("toIterable()", toIterableAction);
    }

    /** Utilities set for interruption behaviour tests. */
    private static class InterruptionTests {

        private boolean isUnSubscribed;
        private final AtomicReference<RuntimeException> errorRef = new AtomicReference<RuntimeException>();
        private CountDownLatch latch = new CountDownLatch(1);

        private Action0 createOnUnsubscribe() {
            return new Action0() {
                @Override
                public void call() {
                    isUnSubscribed = true;
                }
            };
        }

        private Observable<Void> createNeverObservable() {
            return Observable.<Void>never().doOnUnsubscribe(createOnUnsubscribe());
        }

        private Observable<Void> createSynchronousObservable() {
            return Observable.from(new Iterable<Void>() {
                @Override
                public Iterator<Void> iterator() {
                    return new Iterator<Void>() {
                        private boolean nextCalled = false;

                        @Override
                        public boolean hasNext() {
                            return !(nextCalled && Thread.currentThread().isInterrupted());
                        }

                        @Override
                        public Void next() {
                            nextCalled = true;
                            return null;
                        }

                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException("Read-only iterator.");
                        }
                    };
                }
            }).takeLast(1).doOnUnsubscribe(createOnUnsubscribe());
        }

        private <T> void startBlockingAndInterrupt(final Observable<T> observable, final Action1<BlockingObservable<T>> blockingAction) {
            Thread subscriptionThread = new Thread() {
                @Override
                public void run() {
                    try {
                        blockingAction.call(observable.toBlocking());
                    } catch (RuntimeException e) {
                        errorRef.set(e);
                    }
                    latch.countDown();
                }
            };
            subscriptionThread.start();
            subscriptionThread.interrupt();
        }

        void assertUnsubscribeIsInvoked(final String method, final Action1<BlockingObservable<Void>> blockingAction)
            throws InterruptedException {
            startBlockingAndInterrupt(createNeverObservable(), blockingAction);
            assertTrue("Timeout means interruption is not performed", latch.await(30, TimeUnit.SECONDS));
            assertNotNull("InterruptedException is not thrown", getInterruptedExceptionOrNull());
            assertTrue("'unsubscribe' is not invoked when thread is interrupted for " + method, isUnSubscribed);
        }

        void assertNoInterruptedExceptionWhenSynchronous(final String method, final Action1<BlockingObservable<Void>> blockingAction)
            throws InterruptedException {
            startBlockingAndInterrupt(createSynchronousObservable(), blockingAction);
            assertTrue("Timeout means interruption is not performed", latch.await(30, TimeUnit.SECONDS));
            assertNull("'InterruptedException' is thrown when observable is synchronous for " + method, getInterruptedExceptionOrNull());
        }

        private InterruptedException getInterruptedExceptionOrNull() {
            RuntimeException error = errorRef.get();
            if (error == null) {
                return null;
            }
            Throwable cause = error.getCause();
            if (cause instanceof InterruptedException) {
                return (InterruptedException) cause;
            }
            throw error;
        }

    }

    @Test
    public void testRun() {
        Observable.just(1).observeOn(Schedulers.computation()).toBlocking().subscribe();
    }
    
    @Test(expected = TestException.class)
    public void testRunException() {
        Observable.error(new TestException()).observeOn(Schedulers.computation()).toBlocking().subscribe();
    }
    
    @Test
    public void testRunIOException() {
        try {
            Observable.error(new IOException()).observeOn(Schedulers.computation()).toBlocking().subscribe();
            fail("No exception thrown");
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException) {
                return;
            }
            fail("Bad exception type: " + ex + ", " + ex.getCause());
        }
    }
    
    @Test
    public void testSubscriberBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onStart() {
                request(2);
            }
            
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                unsubscribe();
            }
        };
        
        Observable.range(1, 10).observeOn(Schedulers.computation()).toBlocking().subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertNotCompleted();
        ts.assertValue(1);
    }
    
    @Test(expected = OnErrorNotImplementedException.class)
    public void testOnErrorNotImplemented() {
        Observable.error(new TestException()).observeOn(Schedulers.computation()).toBlocking().subscribe(Actions.empty());
    }
    
    @Test
    public void testSubscribeCallback1() {
        final boolean[] valueReceived = { false };
        Observable.just(1).observeOn(Schedulers.computation()).toBlocking().subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                valueReceived[0] = true;
                assertEquals((Integer)1, t);
            }
        });
        
        assertTrue(valueReceived[0]);
    }
    
    @Test
    public void testSubscribeCallback2() {
        final boolean[] received = { false };
        Observable.error(new TestException()).observeOn(Schedulers.computation()).toBlocking()
        .subscribe(new Action1<Object>() {
            @Override
            public void call(Object t) {
                fail("Value emitted: " + t);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {
                received[0] = true;
                assertEquals(TestException.class, t.getClass());
            }
        });
        
        assertTrue(received[0]);
    }
    
    @Test
    public void testSubscribeCallback3() {
        final boolean[] received = { false, false };
        Observable.just(1).observeOn(Schedulers.computation()).toBlocking().subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                received[0] = true;
                assertEquals((Integer)1, t);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {
                t.printStackTrace();
                fail("Exception received!");
            }
        }, new Action0() {
            @Override
            public void call() {
                received[1] = true;
            }
        });
        
        assertTrue(received[0]);
        assertTrue(received[1]);
    }
    @Test
    public void testSubscribeCallback3Error() {
        final TestSubscriber<Object> ts = TestSubscriber.create();
        Observable.error(new TestException()).observeOn(Schedulers.computation()).toBlocking().subscribe(new Action1<Object>() {
            @Override
            public void call(Object t) {
                ts.onNext(t);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {
                ts.onError(t);
            }
        }, new Action0() {
            @Override
            public void call() {
                ts.onCompleted();
            }
        });
        
        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(TestException.class);
    }
}
