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

package io.reactivex.rxjava3.testsupport;

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
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TestSubscriberExTest extends RxJavaTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void assertTestSubscriberEx() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        oi.subscribe(ts);

        ts.assertValues(1, 2);
        ts.assertValueCount(2);
        ts.assertTerminated();
    }

    @Test
    public void assertNotMatchCount() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        oi.subscribe(ts);

        thrown.expect(AssertionError.class);

        ts.assertValues(1);
        ts.assertValueCount(2);
        ts.assertTerminated();
    }

    @Test
    public void assertNotMatchValue() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        oi.subscribe(ts);

        thrown.expect(AssertionError.class);

        ts.assertValues(1, 3);
        ts.assertValueCount(2);
        ts.assertTerminated();
    }

    @Test
    public void assertNeverAtNotMatchingValue() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        oi.subscribe(ts);

        ts.assertNever(3);
        ts.assertValueCount(2);
        ts.assertTerminated();
    }

    @Test
    public void assertNeverAtMatchingValue() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        oi.subscribe(ts);

        ts.assertValues(1, 2);

        thrown.expect(AssertionError.class);

        ts.assertNever(2);
        ts.assertValueCount(2);
        ts.assertTerminated();
    }

    @Test
    public void assertNeverAtMatchingPredicate() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        Flowable.just(1, 2).subscribe(ts);

        ts.assertValues(1, 2);

        thrown.expect(AssertionError.class);

        ts.assertNever(new Predicate<Integer>() {
            @Override
            public boolean test(final Integer o) throws Exception {
                return o == 1;
            }
        });
    }

    @Test
    public void assertNeverAtNotMatchingPredicate() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        Flowable.just(2, 3).subscribe(ts);

        ts.assertNever(new Predicate<Integer>() {
            @Override
            public boolean test(final Integer o) throws Exception {
                return o == 1;
            }
        });
    }

    @Test
    public void assertTerminalEventNotReceived() {
        PublishProcessor<Integer> p = PublishProcessor.create();
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        p.subscribe(ts);

        p.onNext(1);
        p.onNext(2);

        thrown.expect(AssertionError.class);

        ts.assertValues(1, 2);
        ts.assertValueCount(2);
        ts.assertTerminated();
    }

    @Test
    public void wrappingMock() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        Subscriber<Integer> mockSubscriber = TestHelper.mockSubscriber();

        oi.subscribe(new TestSubscriberEx<>(mockSubscriber));

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
        oi.subscribe(new TestSubscriberEx<>(mockSubscriber));

        InOrder inOrder = inOrder(mockSubscriber);
        inOrder.verify(mockSubscriber, times(1)).onNext(1);
        inOrder.verify(mockSubscriber, times(1)).onNext(2);
        inOrder.verify(mockSubscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void assertError() {
        RuntimeException e = new RuntimeException("Oops");
        TestSubscriberEx<Object> subscriber = new TestSubscriberEx<>();
        Flowable.error(e).subscribe(subscriber);
        subscriber.assertError(e);
    }

    @Test
    public void awaitTerminalEventWithDurationAndUnsubscribeOnTimeout() {
        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(null);
        ts.onComplete();
    }

    @Test(expected = NullPointerException.class)
    public void nullDelegate2() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(null);
        ts.onComplete();
    }

    @Test(expected = NullPointerException.class)
    public void nullDelegate3() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(null, 0L);
        ts.onComplete();
    }

    @Test
    public void delegate1() {
        TestSubscriberEx<Integer> ts0 = new TestSubscriberEx<>();
        ts0.onSubscribe(EmptySubscription.INSTANCE);

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(ts0);
        ts.onComplete();

        ts0.assertTerminated();
    }

    @Test
    public void delegate2() {
        TestSubscriberEx<Integer> ts1 = new TestSubscriberEx<>();
        TestSubscriberEx<Integer> ts2 = new TestSubscriberEx<>(ts1);
        ts2.onComplete();

        ts1.assertComplete();
    }

    @Test
    public void delegate3() {
        TestSubscriberEx<Integer> ts1 = new TestSubscriberEx<>();
        TestSubscriberEx<Integer> ts2 = new TestSubscriberEx<>(ts1, 0L);
        ts2.onComplete();
        ts1.assertComplete();
    }

    @Test
    public void unsubscribed() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        assertFalse(ts.isCancelled());
    }

    @Test
    public void noErrors() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        ts.onComplete();

        try {
            ts.assertNotTerminated();
            throw new RuntimeException("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void noTerminalEventBut1Error() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        ts.onError(new TestException());

        try {
            ts.assertNotTerminated();
            throw new RuntimeException("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void noTerminalEventBut1Error1Completed() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        ts.onComplete();
        ts.onError(new TestException());

        try {
            ts.assertNotTerminated();
            throw new RuntimeException("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void noTerminalEventBut2Errors() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        ts.onSubscribe(EmptySubscription.INSTANCE);

        ts.onError(new TestException());
        ts.onError(new TestException());

        try {
            ts.assertNotTerminated();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts0 = new TestSubscriberEx<Integer>() {
            @Override
            public void onComplete() {
                throw new TestException();
            }
        };
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(ts0);

        try {
            ts.onComplete();
        } catch (TestException ex) {
            // expected
        }

        ts.awaitDone(5, TimeUnit.SECONDS);
    }

    @Test
    public void onErrorCrashCountsDownLatch() {
        TestSubscriberEx<Integer> ts0 = new TestSubscriberEx<Integer>() {
            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }
        };
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(ts0);

        try {
            ts.onError(new RuntimeException());
        } catch (TestException ex) {
            // expected
        }

        ts.awaitDone(5, TimeUnit.SECONDS);
    }

    @Test
    public void createDelegate() {
        TestSubscriberEx<Integer> ts1 = new TestSubscriberEx<>();

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(ts1);

        ts.assertNotSubscribed();

        assertFalse(ts.hasSubscription());

        ts.onSubscribe(new BooleanSubscription());

        try {
            ts.assertNotSubscribed();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        assertTrue(ts.hasSubscription());

        assertFalse(ts.isDisposed());

        ts.onNext(1);
        ts.onError(new TestException());
        ts.onComplete();

        ts1.assertValue(1).assertError(TestException.class).assertComplete();

        ts.dispose();

        assertTrue(ts.isDisposed());

        assertSame(Thread.currentThread(), ts.lastThread());

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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
            ts.assertErrorMessage("");
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
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

        try {
            ts.assertTerminated();
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

        ts.assertErrorMessage("Forced failure");

        ts.assertError(Functions.<Throwable>alwaysTrue());

        ts.assertError(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable t) {
                return t.getMessage() != null && t.getMessage().contains("Forced");
            }
        });

        try {
            ts.assertErrorMessage("");
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

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

        ts.assertTerminated();

        ts.assertValueCount(0);

        ts.assertNoValues();
    }

    @Test
    public void emptyObserverEnum() {
        assertEquals(1, TestSubscriberEx.EmptySubscriber.values().length);
        assertNotNull(TestSubscriberEx.EmptySubscriber.valueOf("INSTANCE"));
    }

    @Test
    public void valueAndClass() {
        assertEquals("null", TestSubscriberEx.valueAndClass(null));
        assertEquals("1 (class: Integer)", TestSubscriberEx.valueAndClass(1));
    }

    @Test
    public void assertFailure() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        ts.onSubscribe(new BooleanSubscription());

        ts.onError(new TestException("Forced failure"));

        ts.assertFailure(TestException.class);

        ts.assertFailure(Functions.<Throwable>alwaysTrue());

        ts.assertFailureAndMessage(TestException.class, "Forced failure");

        ts.onNext(1);

        ts.assertFailure(TestException.class, 1);

        ts.assertFailure(Functions.<Throwable>alwaysTrue(), 1);

        ts.assertFailureAndMessage(TestException.class, "Forced failure", 1);
    }

    @Test
    public void assertFuseable() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        ts.onSubscribe(new BooleanSubscription());

        ts.assertNotFuseable();

        try {
            ts.assertFuseable();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            ts.assertFusionMode(QueueFuseable.SYNC);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }
        ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.ANY);

        ts.onSubscribe(new ScalarSubscription<>(ts, 1));

        ts.assertFuseable();

        ts.assertFusionMode(QueueFuseable.SYNC);

        try {
            ts.assertFusionMode(QueueFuseable.NONE);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            ts.assertNotFuseable();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

    }

    @Test
    public void assertTerminated() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        ts.assertNotTerminated();

        ts.onError(null);

        try {
            ts.assertNotTerminated();
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void assertResult() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        ts.onSubscribe(new BooleanSubscription());

        assertFalse(ts.await(100, TimeUnit.MILLISECONDS));

        ts.awaitDone(100, TimeUnit.MILLISECONDS);

        assertTrue(ts.isDisposed());

        assertFalse(ts.await(100, TimeUnit.MILLISECONDS));

        ts.assertNotComplete().assertNoErrors();

        ts.onComplete();

        assertTrue(ts.await(100, TimeUnit.MILLISECONDS));

        ts.await();

        ts.awaitDone(5, TimeUnit.SECONDS);

        ts.assertComplete().assertNoErrors();

        assertTrue(ts.await(5, TimeUnit.SECONDS));

        final TestSubscriberEx<Integer> ts1 = new TestSubscriberEx<>();

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
    public void errors() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        ts.onSubscribe(new BooleanSubscription());

        assertEquals(0, ts.errors().size());

        ts.onError(new TestException());

        assertEquals(1, ts.errors().size());

        TestHelper.assertError(ts.errors(), 0, TestException.class);
    }

    @Test
    public void onNext() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
    public void fusionModeToString() {
        assertEquals("NONE", TestSubscriberEx.fusionModeToString(QueueFuseable.NONE));
        assertEquals("SYNC", TestSubscriberEx.fusionModeToString(QueueFuseable.SYNC));
        assertEquals("ASYNC", TestSubscriberEx.fusionModeToString(QueueFuseable.ASYNC));
        assertEquals("Unknown(100)", TestSubscriberEx.fusionModeToString(100));
    }

    @Test
    public void multipleTerminals() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        ts.onSubscribe(new BooleanSubscription());

        ts.assertNotComplete();

        ts.onComplete();

        try {
            ts.assertNotComplete();
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }

        ts.assertTerminated();

        ts.onComplete();

        try {
            ts.assertComplete();
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }

        try {
            ts.assertTerminated();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        ts.onNext(1);

        ts.assertError(IllegalStateException.class);

        ts = new TestSubscriberEx<>();

        ts.onSubscribe(new BooleanSubscription());

        ts.onNext(null);

        ts.assertFailure(NullPointerException.class, (Integer)null);
    }

    @Test
    public void awaitTerminalEventInterrupt() {
        final TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        ts.onSubscribe(new BooleanSubscription());

        ts.assertNotTerminated();

        ts.onError(new TestException());

        ts.assertTerminated();

        ts.onError(new IOException());

        try {
            ts.assertTerminated();
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

        ts = new TestSubscriberEx<>();

        ts.onSubscribe(new BooleanSubscription());

        ts.onError(new TestException());
        ts.onComplete();

        try {
            ts.assertTerminated();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void onSubscribe() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        ts.onSubscribe(null);

        ts.assertError(NullPointerException.class);

        ts = new TestSubscriberEx<>();

        ts.onSubscribe(new BooleanSubscription());

        BooleanSubscription bs1 = new BooleanSubscription();

        ts.onSubscribe(bs1);

        assertTrue(bs1.isCancelled());

        ts.assertError(IllegalStateException.class);

        ts = new TestSubscriberEx<>();
        ts.dispose();

        bs1 = new BooleanSubscription();

        ts.onSubscribe(bs1);

        assertTrue(bs1.isCancelled());

    }

    @Test
    public void assertValueSequence() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        Thread.currentThread().interrupt();

        try {
            ts.awaitDone(5, TimeUnit.SECONDS);
        } catch (RuntimeException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof InterruptedException);
        }
    }

    @Test
    public void assertNotSubscribed() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        ts.assertNotSubscribed();

        ts.errors().add(new TestException());

        try {
            ts.assertNotSubscribed();
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void assertErrorMultiple() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        TestException e = new TestException();
        ts.errors().add(e);
        ts.errors().add(new TestException());

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
        try {
            ts.assertErrorMessage("");
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void assertComplete() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        ts.onComplete();

        ts.assertError(IllegalStateException.class);
    }

    @Test
    public void completeDelegateThrows() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(new FlowableSubscriber<Integer>() {

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
            ts.assertTerminated();
        }
    }

    @Test
    public void errorDelegateThrows() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(new FlowableSubscriber<Integer>() {

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
            ts.assertTerminated();
        }
    }

    @Test
    public void syncQueueThrows() {
        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.SYNC);

        Flowable.range(1, 5)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception { throw new TestException(); }
        })
        .subscribe(ts);

        ts.assertSubscribed()
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertFailure(TestException.class);
    }

    @Test
    public void asyncQueueThrows() {
        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.ANY);

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception { throw new TestException(); }
        })
        .subscribe(ts);

        up.onNext(1);

        ts.assertSubscribed()
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertFailure(TestException.class);
    }

    @Test
    public void assertValuePredicateEmpty() {
        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();

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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        Flowable.just(1).subscribe(ts);

        ts.assertValue(new Predicate<Integer>() {
            @Override public boolean test(final Integer o) throws Exception {
                return o == 1;
            }
        });
    }

    @Test
    public void assertValuePredicateNoMatch() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();

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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        Flowable.just(1, 2).subscribe(ts);

        ts.assertValueAt(1, new Predicate<Integer>() {
            @Override public boolean test(final Integer o) throws Exception {
                return o == 2;
            }
        });
    }

    @Test
    public void assertValueAtPredicateNoMatch() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

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

        TestSubscriberEx<Object> ts = Flowable.never()
        .to(TestHelper.<Object>testConsumer());
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
        TestSubscriberEx<Object> ts = Flowable.never()
            .to(TestHelper.<Object>testConsumer());
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
        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        ts.onSubscribe(new BooleanSubscription());
        ts.assertValuesOnly();

        ts.onNext(5);
        ts.assertValuesOnly(5);

        ts.onNext(-1);
        ts.assertValuesOnly(5, -1);
    }

    @Test
    public void assertValuesOnlyThrowsOnUnexpectedValue() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
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
