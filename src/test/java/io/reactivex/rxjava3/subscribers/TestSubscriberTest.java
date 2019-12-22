/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.rxjava3.subscribers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class TestSubscriberTest extends RxJavaTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void assertTestSubscriber() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        oi.subscribe(ts);

        ts.assertValues(1, 2);
        ts.assertValueCount(2);
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test
    public void assertNotMatchCount() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        oi.subscribe(ts);

        thrown.expect(AssertionError.class);
        // FIXME different message pattern
        // thrown.expectMessage("Number of items does not match. Provided: 1  Actual: 2");

        ts.assertValues(1);
        ts.assertValueCount(2);
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test
    public void assertNotMatchValue() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        oi.subscribe(ts);

        thrown.expect(AssertionError.class);
        // FIXME different message pattern
        // thrown.expectMessage("Value at index: 1 expected to be [3] (Integer) but was: [2] (Integer)");

        ts.assertValues(1, 3);
        ts.assertValueCount(2);
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test
    public void assertTerminalEventNotReceived() {
        PublishProcessor<Integer> p = PublishProcessor.create();
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        p.subscribe(ts);

        p.onNext(1);
        p.onNext(2);

        thrown.expect(AssertionError.class);
        // FIXME different message pattern
        // thrown.expectMessage("No terminal events received.");

        ts.assertValues(1, 2);
        ts.assertValueCount(2);
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test
    public void wrappingMock() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        Subscriber<Integer> mockSubscriber = TestHelper.mockSubscriber();

        oi.subscribe(new TestSubscriber<>(mockSubscriber));

        InOrder inOrder = inOrder(mockSubscriber);
        inOrder.verify(mockSubscriber, times(1)).onNext(1);
        inOrder.verify(mockSubscriber, times(1)).onNext(2);
        inOrder.verify(mockSubscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void wrappingMockWhenUnsubscribeInvolved() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9)).take(2);
        Subscriber<Integer> mockSubscriber = TestHelper.mockSubscriber();
        oi.subscribe(new TestSubscriber<>(mockSubscriber));

        InOrder inOrder = inOrder(mockSubscriber);
        inOrder.verify(mockSubscriber, times(1)).onNext(1);
        inOrder.verify(mockSubscriber, times(1)).onNext(2);
        inOrder.verify(mockSubscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void assertError() {
        RuntimeException e = new RuntimeException("Oops");
        TestSubscriber<Object> subscriber = new TestSubscriber<>();
        Flowable.error(e).subscribe(subscriber);
        subscriber.assertError(e);
    }

    @Test
    public void awaitTerminalEventWithDuration() {
        TestSubscriber<Object> ts = new TestSubscriber<>();
        Flowable.just(1).subscribe(ts);
        ts.awaitDone(1, TimeUnit.SECONDS);
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test
    public void awaitTerminalEventWithDurationAndUnsubscribeOnTimeout() {
        TestSubscriber<Object> ts = new TestSubscriber<>();
        final AtomicBoolean unsub = new AtomicBoolean(false);
        Flowable.just(1)
        //
                .doOnCancel(new Action() {
                    @Override
                    public void run() {
                        unsub.set(true);
                    }
                })
                //
                .delay(1000, TimeUnit.MILLISECONDS).subscribe(ts);
        ts.awaitDone(100, TimeUnit.MILLISECONDS);
        ts.dispose();
        assertTrue(unsub.get());
    }

    @Test(expected = NullPointerException.class)
    public void nullDelegate1() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(null);
        ts.onComplete();
    }

    @Test(expected = NullPointerException.class)
    public void nullDelegate2() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(null);
        ts.onComplete();
    }

    @Test(expected = NullPointerException.class)
    public void nullDelegate3() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(null, 0L);
        ts.onComplete();
    }

    @Test
    public void delegate1() {
        TestSubscriber<Integer> ts0 = new TestSubscriber<>();
        ts0.onSubscribe(EmptySubscription.INSTANCE);

        TestSubscriber<Integer> ts = new TestSubscriber<>(ts0);
        ts.onComplete();

        ts0.assertComplete();
        ts0.assertNoErrors();
    }

    @Test
    public void delegate2() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<>();
        TestSubscriber<Integer> ts2 = new TestSubscriber<>(ts1);
        ts2.onComplete();

        ts1.assertComplete();
    }

    @Test
    public void delegate3() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<>();
        TestSubscriber<Integer> ts2 = new TestSubscriber<>(ts1, 0L);
        ts2.onComplete();
        ts1.assertComplete();
    }

    @Test
    public void unsubscribed() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        assertFalse(ts.isCancelled());
    }

    @Test
    public void noErrors() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onError(new TestException());
        try {
            ts.assertNoErrors();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Error present but no assertion error!");
    }

    @Test
    public void notCompleted() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        try {
            ts.assertComplete();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Not completed and no assertion error!");
    }

    @Test
    public void multipleCompletions() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onComplete();
        ts.onComplete();
        try {
            ts.assertComplete();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Multiple completions and no assertion error!");
    }

    @Test
    public void completed() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onComplete();
        try {
            ts.assertNotComplete();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Completed and no assertion error!");
    }

    @Test
    public void multipleCompletions2() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onComplete();
        ts.onComplete();
        try {
            ts.assertNotComplete();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Multiple completions and no assertion error!");
    }

    @Test
    public void multipleErrors() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onSubscribe(EmptySubscription.INSTANCE);
        ts.onError(new TestException());
        ts.onError(new TestException());
        try {
            ts.assertNoErrors();
        } catch (AssertionError ex) {
            Throwable e = ex.getCause();
            if (!(e instanceof CompositeException)) {
                fail("Multiple Error present but the reported error doesn't have a composite cause!");
            }
            CompositeException ce = (CompositeException)e;
            if (ce.size() != 2) {
                ce.printStackTrace();
            }
            assertEquals(2, ce.size());
            // expected
            return;
        }
        fail("Multiple Error present but no assertion error!");
    }

    @Test
    public void multipleErrors2() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onSubscribe(EmptySubscription.INSTANCE);
        ts.onError(new TestException());
        ts.onError(new TestException());
        try {
            ts.assertError(TestException.class);
        } catch (AssertionError ex) {
            Throwable e = ex.getCause();
            if (!(e instanceof CompositeException)) {
                fail("Multiple Error present but the reported error doesn't have a composite cause!");
            }
            CompositeException ce = (CompositeException)e;
            assertEquals(2, ce.size());
            // expected
            return;
        }
        fail("Multiple Error present but no assertion error!");
    }

    @Test
    public void multipleErrors3() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onSubscribe(EmptySubscription.INSTANCE);
        ts.onError(new TestException());
        ts.onError(new TestException());
        try {
            ts.assertError(new TestException());
        } catch (AssertionError ex) {
            Throwable e = ex.getCause();
            if (!(e instanceof CompositeException)) {
                fail("Multiple Error present but the reported error doesn't have a composite cause!");
            }
            CompositeException ce = (CompositeException)e;
            assertEquals(2, ce.size());
            // expected
            return;
        }
        fail("Multiple Error present but no assertion error!");
    }

    @Test
    public void multipleErrors4() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onSubscribe(EmptySubscription.INSTANCE);
        ts.onError(new TestException());
        ts.onError(new TestException());
        try {
            ts.assertError(Functions.<Throwable>alwaysTrue());
        } catch (AssertionError ex) {
            Throwable e = ex.getCause();
            if (!(e instanceof CompositeException)) {
                fail("Multiple Error present but the reported error doesn't have a composite cause!");
            }
            CompositeException ce = (CompositeException)e;
            assertEquals(2, ce.size());
            // expected
            return;
        }
        fail("Multiple Error present but no assertion error!");
    }

    @Test
    public void differentError() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onError(new TestException());
        try {
            ts.assertError(new TestException());
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Different Error present but no assertion error!");
    }

    @Test
    public void differentError2() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onError(new RuntimeException());
        try {
            ts.assertError(new TestException());
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Different Error present but no assertion error!");
    }

    @Test
    public void differentError3() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onError(new RuntimeException());
        try {
            ts.assertError(TestException.class);
        }
        catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Different Error present but no assertion error!");
    }

    @Test
    public void differentError4() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onError(new RuntimeException());
        try {
            ts.assertError(Functions.<Throwable>alwaysFalse());
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Different Error present but no assertion error!");
    }

    @Test
    public void errorInPredicate() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onError(new RuntimeException());
        try {
            ts.assertError(new Predicate<Throwable>() {
                @Override
                public boolean test(Throwable throwable) throws Exception {
                    throw new TestException();
                }
            });
        } catch (TestException ex) {
            // expected
            return;
        }
        fail("Error in predicate but not thrown!");
    }

    @Test
    public void noError() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        try {
            ts.assertError(TestException.class);
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("No present but no assertion error!");
    }

    @Test
    public void noError2() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        try {
            ts.assertError(new TestException());
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("No present but no assertion error!");
    }

    @Test
    public void noError3() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        try {
            ts.assertError(Functions.<Throwable>alwaysTrue());
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("No present but no assertion error!");
    }

    @Test
    public void interruptTerminalEventAwait() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        final Thread t0 = Thread.currentThread();
        Worker w = Schedulers.computation().createWorker();
        try {
            w.schedule(new Runnable() {
                @Override
                public void run() {
                    t0.interrupt();
                }
            }, 200, TimeUnit.MILLISECONDS);

            try {
                if (ts.await(5, TimeUnit.SECONDS)) {
                    fail("Did not interrupt wait!");
                }
            } catch (InterruptedException expected) {
                // expected
            }
        } finally {
            w.dispose();
        }
    }

    @Test
    public void interruptTerminalEventAwaitTimed() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        final Thread t0 = Thread.currentThread();
        Worker w = Schedulers.computation().createWorker();
        try {
            w.schedule(new Runnable() {
                @Override
                public void run() {
                    t0.interrupt();
                }
            }, 200, TimeUnit.MILLISECONDS);

            try {
                if (ts.await(5, TimeUnit.SECONDS)) {
                    fail("Did not interrupt wait!");
                }
            } catch (InterruptedException expected) {
                // expected
            }
        } finally {
            Thread.interrupted(); // clear interrupted flag
            w.dispose();
        }
    }

    @Test
    public void interruptTerminalEventAwaitAndUnsubscribe() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        final Thread t0 = Thread.currentThread();
        Worker w = Schedulers.computation().createWorker();
        try {
            w.schedule(new Runnable() {
                @Override
                public void run() {
                    t0.interrupt();
                }
            }, 200, TimeUnit.MILLISECONDS);

            try {
                ts.awaitDone(5, TimeUnit.SECONDS);
            } catch (RuntimeException allowed) {
                assertTrue(allowed.toString(), allowed.getCause() instanceof InterruptedException);
            }
            ts.dispose();
            if (!ts.isCancelled()) {
                fail("Did not unsubscribe!");
            }
        } finally {
            w.dispose();
        }
    }

    @Test
    public void noTerminalEventBut1Completed() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        ts.onComplete();

        try {
            ts.assertNotComplete();
            throw new RuntimeException("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void noTerminalEventBut1Error() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        ts.onError(new TestException());

        try {
            ts.assertNoErrors();
            throw new RuntimeException("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void noTerminalEventBut1Error1Complete() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        ts.onComplete();
        ts.onError(new TestException());

        try {
            ts.assertNotComplete();
            throw new RuntimeException("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            ts.assertNoErrors();
            throw new RuntimeException("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void noTerminalEventBut2Errors() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onSubscribe(EmptySubscription.INSTANCE);

        ts.onError(new TestException());
        ts.onError(new TestException());

        try {
            ts.assertNoErrors();
            throw new RuntimeException("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
            Throwable e = ex.getCause();
            if (!(e instanceof CompositeException)) {
                fail("Multiple Error present but the reported error doesn't have a composite cause!");
            }
            CompositeException ce = (CompositeException)e;
            assertEquals(2, ce.size());
        }
    }

    @Test
    public void noValues() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onNext(1);

        try {
            ts.assertNoValues();
            throw new RuntimeException("Failed to report there were values!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void valueCount() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onNext(1);
        ts.onNext(2);

        try {
            ts.assertValueCount(3);
            throw new RuntimeException("Failed to report there were values!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void onCompletedCrashCountsDownLatch() {
        TestSubscriber<Integer> ts0 = new TestSubscriber<Integer>() {
            @Override
            public void onComplete() {
                throw new TestException();
            }
        };
        TestSubscriber<Integer> ts = new TestSubscriber<>(ts0);

        try {
            ts.onComplete();
        } catch (TestException ex) {
            // expected
        }

        ts.awaitDone(5, TimeUnit.SECONDS);
    }

    @Test
    public void onErrorCrashCountsDownLatch() {
        TestSubscriber<Integer> ts0 = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }
        };
        TestSubscriber<Integer> ts = new TestSubscriber<>(ts0);

        try {
            ts.onError(new RuntimeException());
        } catch (TestException ex) {
            // expected
        }

        ts.awaitDone(5, TimeUnit.SECONDS);
    }

    @Test
    public void createDelegate() {
        TestSubscriber<Integer> ts1 = TestSubscriber.create();

        TestSubscriber<Integer> ts = TestSubscriber.create(ts1);

        assertFalse(ts.hasSubscription());

        ts.onSubscribe(new BooleanSubscription());

        assertTrue(ts.hasSubscription());

        assertFalse(ts.isDisposed());

        ts.onNext(1);
        ts.onError(new TestException());
        ts.onComplete();

        ts1.assertValue(1).assertError(TestException.class).assertComplete();

        ts.dispose();

        assertTrue(ts.isDisposed());

        try {
            ts.assertNoValues();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        try {
            ts.assertValueCount(0);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        ts.assertValueSequence(Collections.singletonList(1));

        try {
            ts.assertValueSequence(Collections.singletonList(2));
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }
    }

    @Test
    public void assertError2() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        try {
            ts.assertError(TestException.class);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            ts.assertError(new TestException());
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            ts.assertError(Functions.<Throwable>alwaysTrue());
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            ts.assertSubscribed();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        ts.onSubscribe(new BooleanSubscription());

        ts.assertSubscribed();

        ts.assertNoErrors();

        TestException ex = new TestException("Forced failure");

        ts.onError(ex);

        ts.assertError(ex);

        ts.assertError(TestException.class);

        ts.assertError(Functions.<Throwable>alwaysTrue());

        ts.assertError(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable t) {
                return t.getMessage() != null && t.getMessage().contains("Forced");
            }
        });

        try {
            ts.assertError(new RuntimeException());
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        try {
            ts.assertError(IOException.class);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        try {
            ts.assertNoErrors();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        try {
            ts.assertError(Functions.<Throwable>alwaysFalse());
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        ts.assertValueCount(0);

        ts.assertNoValues();
    }

    @Test
    public void emptyObserverEnum() {
        assertEquals(1, TestSubscriber.EmptySubscriber.values().length);
        assertNotNull(TestSubscriber.EmptySubscriber.valueOf("INSTANCE"));
    }

    @Test
    public void valueAndClass() {
        assertEquals("null", TestSubscriber.valueAndClass(null));
        assertEquals("1 (class: Integer)", TestSubscriber.valueAndClass(1));
    }

    @Test
    public void assertFailure() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        ts.onSubscribe(new BooleanSubscription());

        ts.onError(new TestException("Forced failure"));

        ts.assertFailure(TestException.class);

        ts.onNext(1);

        ts.assertFailure(TestException.class, 1);
    }

    @Test
    public void assertResult() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        ts.onSubscribe(new BooleanSubscription());

        ts.onComplete();

        ts.assertResult();

        try {
            ts.assertResult(1);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        ts.onNext(1);

        ts.assertResult(1);

        try {
            ts.assertResult(2);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            ts.assertResult();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

    }

    @Test
    public void await() throws Exception {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        ts.onSubscribe(new BooleanSubscription());

        assertFalse(ts.await(100, TimeUnit.MILLISECONDS));

        ts.awaitDone(100, TimeUnit.MILLISECONDS);

        assertTrue(ts.isDisposed());

        assertFalse(ts.await(100, TimeUnit.MILLISECONDS));

        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.onComplete();

        assertTrue(ts.await(100, TimeUnit.MILLISECONDS));

        ts.await();

        ts.awaitDone(5, TimeUnit.SECONDS);

        ts.assertComplete();
        ts.assertNoErrors();

        assertTrue(ts.await(5, TimeUnit.SECONDS));

        final TestSubscriber<Integer> ts1 = TestSubscriber.create();

        ts1.onSubscribe(new BooleanSubscription());

        Schedulers.single().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                ts1.onComplete();
            }
        }, 200, TimeUnit.MILLISECONDS);

        ts1.await();
    }

    @Test
    public void onNext() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        ts.onSubscribe(new BooleanSubscription());

        ts.assertNoValues();

        assertEquals(Collections.emptyList(), ts.values());

        ts.onNext(1);

        assertEquals(Collections.singletonList(1), ts.values());

        ts.cancel();

        assertTrue(ts.isCancelled());
        assertTrue(ts.isDisposed());

        ts.assertValue(1);

        ts.onComplete();
    }

    @Test
    public void multipleTerminals() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        ts.onSubscribe(new BooleanSubscription());

        ts.assertNotComplete();

        ts.onComplete();

        try {
            ts.assertNotComplete();
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }

        ts.assertComplete();

        ts.onComplete();

        try {
            ts.assertComplete();
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }

        try {
            ts.assertComplete();
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }

        try {
            ts.assertNotComplete();
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }
    }

    @Test
    public void assertValue() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        ts.onSubscribe(new BooleanSubscription());

        try {
            ts.assertValue(1);
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }

        ts.onNext(1);

        ts.assertValue(1);

        try {
            ts.assertValue(2);
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }

        ts.onNext(2);

        try {
            ts.assertValue(1);
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }
    }

    @Test
    public void onNextMisbehave() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        ts.onNext(1);

        ts.assertError(IllegalStateException.class);

        ts = TestSubscriber.create();

        ts.onSubscribe(new BooleanSubscription());

        ts.onNext(null);

        ts.assertFailure(NullPointerException.class, (Integer)null);
    }

    @Test
    public void awaitTerminalEventInterrupt() {
        final TestSubscriber<Integer> ts = TestSubscriber.create();

        ts.onSubscribe(new BooleanSubscription());

        Thread.currentThread().interrupt();

        try {
            ts.awaitDone(5, TimeUnit.SECONDS);
        } catch (RuntimeException allowed) {
            assertTrue(allowed.toString(), allowed.getCause() instanceof InterruptedException);
        }

        // FIXME ? catch consumes this flag
        // assertTrue(Thread.interrupted());

        Thread.currentThread().interrupt();

        try {
            ts.awaitDone(5, TimeUnit.SECONDS);
        } catch (RuntimeException allowed) {
            assertTrue(allowed.toString(), allowed.getCause() instanceof InterruptedException);
        }

        // FIXME ? catch consumes this flag
        // assertTrue(Thread.interrupted());
    }

    @Test
    public void assertTerminated2() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        ts.onSubscribe(new BooleanSubscription());

        ts.assertNotComplete().assertNoErrors();

        ts.onError(new TestException());

        ts.assertError(TestException.class);

        ts.onError(new IOException());

        try {
            ts.assertNoErrors();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            ts.assertError(TestException.class);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        ts = TestSubscriber.create();

        ts.onSubscribe(new BooleanSubscription());

        ts.onError(new TestException());
        ts.onComplete();

        try {
            ts.assertComplete()
            .assertNoErrors();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            ts.assertError(Throwable.class)
            .assertNotComplete();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void onSubscribe() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        ts.onSubscribe(null);

        ts.assertError(NullPointerException.class);

        ts = TestSubscriber.create();

        ts.onSubscribe(new BooleanSubscription());

        BooleanSubscription bs1 = new BooleanSubscription();

        ts.onSubscribe(bs1);

        assertTrue(bs1.isCancelled());

        ts.assertError(IllegalStateException.class);

        ts = TestSubscriber.create();
        ts.dispose();

        bs1 = new BooleanSubscription();

        ts.onSubscribe(bs1);

        assertTrue(bs1.isCancelled());

    }

    @Test
    public void assertValueSequence() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        ts.onSubscribe(new BooleanSubscription());

        ts.onNext(1);
        ts.onNext(2);

        try {
            ts.assertValueSequence(Collections.<Integer>emptyList());
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            ts.assertValueSequence(Collections.singletonList(1));
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        ts.assertValueSequence(Arrays.asList(1, 2));

        try {
            ts.assertValueSequence(Arrays.asList(1, 2, 3));
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void assertEmpty() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        try {
            ts.assertEmpty();
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }

        ts.onSubscribe(new BooleanSubscription());

        ts.assertEmpty();

        ts.onNext(1);

        try {
            ts.assertEmpty();
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void awaitDoneTimed() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Thread.currentThread().interrupt();

        try {
            ts.awaitDone(5, TimeUnit.SECONDS);
        } catch (RuntimeException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof InterruptedException);
        }
    }

    @Test
    public void assertErrorMultiple() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        TestException e = new TestException();
        ts.onError(e);
        ts.onError(new TestException());

        try {
            ts.assertError(TestException.class);
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
        try {
            ts.assertError(e);
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void assertComplete() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        ts.onSubscribe(new BooleanSubscription());

        try {
            ts.assertComplete();
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }

        ts.onComplete();

        ts.assertComplete();

        ts.onComplete();

        try {
            ts.assertComplete();
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void completeWithoutOnSubscribe() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        ts.onComplete();

        ts.assertError(IllegalStateException.class);
    }

    @Test
    public void completeDelegateThrows() throws Exception {
        TestSubscriber<Integer> ts = new TestSubscriber<>(new FlowableSubscriber<Integer>() {

            @Override
            public void onSubscribe(Subscription s) {

            }

            @Override
            public void onNext(Integer value) {

            }

            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }

            @Override
            public void onComplete() {
                throw new TestException();
            }

        });

        ts.onSubscribe(new BooleanSubscription());

        try {
            ts.onComplete();
            throw new RuntimeException("Should have thrown!");
        } catch (TestException ex) {
            assertTrue(ts.await(1, TimeUnit.SECONDS));
        }
    }

    @Test
    public void errorDelegateThrows() throws Exception {
        TestSubscriber<Integer> ts = new TestSubscriber<>(new FlowableSubscriber<Integer>() {

            @Override
            public void onSubscribe(Subscription s) {

            }

            @Override
            public void onNext(Integer value) {

            }

            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }

            @Override
            public void onComplete() {
                throw new TestException();
            }

        });

        ts.onSubscribe(new BooleanSubscription());

        try {
            ts.onError(new IOException());
            throw new RuntimeException("Should have thrown!");
        } catch (TestException ex) {
            assertTrue(ts.await(1, TimeUnit.SECONDS));
        }
    }

    @Test
    public void assertValuePredicateEmpty() {
        TestSubscriber<Object> ts = new TestSubscriber<>();

        Flowable.empty().subscribe(ts);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("No values");
        ts.assertValue(new Predicate<Object>() {
            @Override public boolean test(final Object o) throws Exception {
                return false;
            }
        });
    }

    @Test
    public void assertValuePredicateMatch() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.just(1).subscribe(ts);

        ts.assertValue(new Predicate<Integer>() {
            @Override public boolean test(final Integer o) throws Exception {
                return o == 1;
            }
        });
    }

    @Test
    public void assertValuePredicateNoMatch() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.just(1).subscribe(ts);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Value not present");
        ts.assertValue(new Predicate<Integer>() {
            @Override public boolean test(final Integer o) throws Exception {
                return o != 1;
            }
        });
    }

    @Test
    public void assertValuePredicateMatchButMore() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.just(1, 2).subscribe(ts);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Value present but other values as well");
        ts.assertValue(new Predicate<Integer>() {
            @Override public boolean test(final Integer o) throws Exception {
                return o == 1;
            }
        });
    }

    @Test
    public void assertValueAtPredicateEmpty() {
        TestSubscriber<Object> ts = new TestSubscriber<>();

        Flowable.empty().subscribe(ts);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("No values");
        ts.assertValueAt(0, new Predicate<Object>() {
            @Override public boolean test(final Object o) throws Exception {
                return false;
            }
        });
    }

    @Test
    public void assertValueAtPredicateMatch() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.just(1, 2).subscribe(ts);

        ts.assertValueAt(1, new Predicate<Integer>() {
            @Override public boolean test(final Integer o) throws Exception {
                return o == 2;
            }
        });
    }

    @Test
    public void assertValueAtPredicateNoMatch() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.just(1, 2, 3).subscribe(ts);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Value not present");
        ts.assertValueAt(2, new Predicate<Integer>() {
            @Override public boolean test(final Integer o) throws Exception {
                return o != 3;
            }
        });
    }

    @Test
    public void assertValueAtInvalidIndex() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.just(1, 2).subscribe(ts);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Invalid index: 2 (latch = 0, values = 2, errors = 0, completions = 1)");
        ts.assertValueAt(2, new Predicate<Integer>() {
            @Override public boolean test(final Integer o) throws Exception {
                return o == 1;
            }
        });
    }

    @Test
    public void requestMore() {
        Flowable.range(1, 5)
        .test(0)
        .requestMore(1)
        .assertValue(1)
        .requestMore(2)
        .assertValues(1, 2, 3)
        .requestMore(3)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void withTag() {
        try {
            for (int i = 1; i < 3; i++) {
                Flowable.just(i)
                .test()
                .withTag("testing with item=" + i)
                .assertResult(1)
                ;
            }
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            assertTrue(ex.toString(), ex.toString().contains("testing with item=2"));
        }
    }

    @Test
    public void timeoutIndicated() throws InterruptedException {
        Thread.interrupted(); // clear flag

        TestSubscriber<Object> ts = Flowable.never()
        .test();
        assertFalse(ts.await(1, TimeUnit.MILLISECONDS));

        try {
            ts.assertResult(1);
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            assertTrue(ex.toString(), ex.toString().contains("timeout!"));
        }
    }

    @Test
    public void timeoutIndicated2() throws InterruptedException {
        try {
            Flowable.never()
            .test()
            .awaitDone(1, TimeUnit.MILLISECONDS)
            .assertResult(1);

            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            assertTrue(ex.toString(), ex.toString().contains("timeout!"));
        }
    }

    @Test
    public void timeoutIndicated3() throws InterruptedException {
        TestSubscriber<Object> ts = Flowable.never()
        .test();
        assertFalse(ts.await(1, TimeUnit.MILLISECONDS));

        try {
            ts.assertResult(1);
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            assertTrue(ex.toString(), ex.toString().contains("timeout!"));
        }
    }

    @Test
    public void disposeIndicated() {
        TestSubscriber<Object> ts = new TestSubscriber<>();
        ts.cancel();

        try {
            ts.assertResult(1);
            throw new RuntimeException("Should have thrown!");
        } catch (Throwable ex) {
            assertTrue(ex.toString(), ex.toString().contains("disposed!"));
        }
    }

    @Test
    public void awaitCount() {
        Flowable.range(1, 10).delay(100, TimeUnit.MILLISECONDS)
        .test(5)
        .awaitCount(5)
        .assertValues(1, 2, 3, 4, 5)
        .requestMore(5)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void awaitCountLess() {
        Flowable.range(1, 4)
        .test()
        .awaitCount(5)
        .assertResult(1, 2, 3, 4);
    }

    @Test
    public void assertValueAtPredicateThrows() {
        try {
            Flowable.just(1)
            .test()
            .assertValueAt(0, new Predicate<Integer>() {
                @Override
                public boolean test(Integer t) throws Exception {
                    throw new IllegalArgumentException();
                }
            });
            throw new RuntimeException("Should have thrown!");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void assertValuesOnly() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        ts.onSubscribe(new BooleanSubscription());
        ts.assertValuesOnly();

        ts.onNext(5);
        ts.assertValuesOnly(5);

        ts.onNext(-1);
        ts.assertValuesOnly(5, -1);
    }

    @Test
    public void assertValuesOnlyThrowsOnUnexpectedValue() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        ts.onSubscribe(new BooleanSubscription());
        ts.assertValuesOnly();

        ts.onNext(5);
        ts.assertValuesOnly(5);

        ts.onNext(-1);

        try {
            ts.assertValuesOnly(5);
            throw new RuntimeException();
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void assertValuesOnlyThrowsWhenCompleted() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        ts.onSubscribe(new BooleanSubscription());

        ts.onComplete();

        try {
            ts.assertValuesOnly();
            throw new RuntimeException();
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void assertValuesOnlyThrowsWhenErrored() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        ts.onSubscribe(new BooleanSubscription());

        ts.onError(new TestException());

        try {
            ts.assertValuesOnly();
            throw new RuntimeException();
        } catch (AssertionError ex) {
            // expected
        }
    }
}
