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

package io.reactivex.observers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.operators.observable.ObservableScalarXMap.ScalarDisposable;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.UnicastSubject;
import io.reactivex.subscribers.TestSubscriber;

public class TestObserverTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testAssert() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        TestSubscriber<Integer> o = new TestSubscriber<Integer>();
        oi.subscribe(o);

        o.assertValues(1, 2);
        o.assertValueCount(2);
        o.assertTerminated();
    }

    @Test
    public void testAssertNotMatchCount() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        TestSubscriber<Integer> o = new TestSubscriber<Integer>();
        oi.subscribe(o);

        thrown.expect(AssertionError.class);
        // FIXME different message format
//        thrown.expectMessage("Number of items does not match. Provided: 1  Actual: 2");

        o.assertValue(1);
        o.assertValueCount(2);
        o.assertTerminated();
    }

    @Test
    public void testAssertNotMatchValue() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        TestSubscriber<Integer> o = new TestSubscriber<Integer>();
        oi.subscribe(o);

        thrown.expect(AssertionError.class);
        // FIXME different message format
//        thrown.expectMessage("Value at index: 1 expected to be [3] (Integer) but was: [2] (Integer)");

        o.assertValues(1, 3);
        o.assertValueCount(2);
        o.assertTerminated();
    }

    @Test
    public void assertNeverAtNotMatchingValue() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        TestSubscriber<Integer> o = new TestSubscriber<Integer>();
        oi.subscribe(o);

        o.assertNever(3);
        o.assertValueCount(2);
        o.assertTerminated();
    }

    @Test
    public void assertNeverAtMatchingValue() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        TestSubscriber<Integer> o = new TestSubscriber<Integer>();
        oi.subscribe(o);

        o.assertValues(1, 2);

        thrown.expect(AssertionError.class);

        o.assertNever(2);
        o.assertValueCount(2);
        o.assertTerminated();
    }

    @Test
    public void assertNeverAtMatchingPredicate() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

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
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.just(2, 3).subscribe(ts);

        ts.assertNever(new Predicate<Integer>() {
            @Override
            public boolean test(final Integer o) throws Exception {
                return o == 1;
            }
        });
    }

    @Test
    public void testAssertTerminalEventNotReceived() {
        PublishProcessor<Integer> p = PublishProcessor.create();
        TestSubscriber<Integer> o = new TestSubscriber<Integer>();
        p.subscribe(o);

        p.onNext(1);
        p.onNext(2);

        thrown.expect(AssertionError.class);
        // FIXME different message format
//        thrown.expectMessage("No terminal events received.");

        o.assertValues(1, 2);
        o.assertValueCount(2);
        o.assertTerminated();
    }

    @Test
    public void testWrappingMock() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));

        Subscriber<Integer> mockObserver = TestHelper.mockSubscriber();

        oi.subscribe(new TestSubscriber<Integer>(mockObserver));

        InOrder inOrder = inOrder(mockObserver);
        inOrder.verify(mockObserver, times(1)).onNext(1);
        inOrder.verify(mockObserver, times(1)).onNext(2);
        inOrder.verify(mockObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testWrappingMockWhenUnsubscribeInvolved() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9)).take(2);
        Subscriber<Integer> mockObserver = TestHelper.mockSubscriber();
        oi.subscribe(new TestSubscriber<Integer>(mockObserver));

        InOrder inOrder = inOrder(mockObserver);
        inOrder.verify(mockObserver, times(1)).onNext(1);
        inOrder.verify(mockObserver, times(1)).onNext(2);
        inOrder.verify(mockObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testErrorSwallowed() {
        Flowable.error(new RuntimeException()).subscribe(new TestSubscriber<Object>());
    }

    @Test
    public void testGetEvents() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.onSubscribe(EmptySubscription.INSTANCE);
        ts.onNext(1);
        ts.onNext(2);

        assertEquals(Arrays.<Object>asList(Arrays.asList(1, 2),
                Collections.emptyList(),
                Collections.emptyList()), ts.getEvents());

        ts.onComplete();

        assertEquals(Arrays.<Object>asList(Arrays.asList(1, 2), Collections.emptyList(),
                Collections.singletonList(Notification.createOnComplete())), ts.getEvents());

        TestException ex = new TestException();
        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>();
        ts2.onSubscribe(EmptySubscription.INSTANCE);
        ts2.onNext(1);
        ts2.onNext(2);

        assertEquals(Arrays.<Object>asList(Arrays.asList(1, 2),
                Collections.emptyList(),
                Collections.emptyList()), ts2.getEvents());

        ts2.onError(ex);

        assertEquals(Arrays.<Object>asList(
                Arrays.asList(1, 2),
                Collections.singletonList(ex),
                Collections.emptyList()),
                    ts2.getEvents());
    }

    @Test
    public void testNullExpected() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.onNext(1);

        try {
            ts.assertValue((Integer) null);
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Null element check assertion didn't happen!");
    }

    @Test
    public void testNullActual() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.onNext(null);

        try {
            ts.assertValue(1);
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Null element check assertion didn't happen!");
    }

    @Test
    public void testTerminalErrorOnce() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.onError(new TestException());
        ts.onError(new TestException());

        try {
            ts.assertTerminated();
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Failed to report multiple onError terminal events!");
    }
    @Test
    public void testTerminalCompletedOnce() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.onComplete();
        ts.onComplete();

        try {
            ts.assertTerminated();
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Failed to report multiple onComplete terminal events!");
    }

    @Test
    public void testTerminalOneKind() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.onError(new TestException());
        ts.onComplete();

        try {
            ts.assertTerminated();
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Failed to report multiple kinds of events!");
    }

    @Test
    public void createDelegate() {
        TestObserver<Integer> to1 = TestObserver.create();

        TestObserver<Integer> to = TestObserver.create(to1);

        to.assertNotSubscribed();

        assertFalse(to.hasSubscription());

        to.onSubscribe(Disposables.empty());

        try {
            to.assertNotSubscribed();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        assertTrue(to.hasSubscription());

        assertFalse(to.isDisposed());

        to.onNext(1);
        to.onError(new TestException());
        to.onComplete();

        to1.assertValue(1).assertError(TestException.class).assertComplete();

        to.dispose();

        assertTrue(to.isDisposed());

        assertTrue(to.isTerminated());

        assertSame(Thread.currentThread(), to.lastThread());

        try {
            to.assertNoValues();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        try {
            to.assertValueCount(0);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        to.assertValueSequence(Collections.singletonList(1));

        try {
            to.assertValueSequence(Collections.singletonList(2));
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        to.assertValueSet(Collections.singleton(1));

        try {
            to.assertValueSet(Collections.singleton(2));
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

    }

    @Test
    public void assertError() {
        TestObserver<Integer> to = TestObserver.create();

        try {
            to.assertError(TestException.class);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            to.assertError(new TestException());
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            to.assertError(Functions.<Throwable>alwaysTrue());
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            to.assertErrorMessage("");
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        try {
            to.assertSubscribed();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        try {
            to.assertTerminated();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        to.onSubscribe(Disposables.empty());

        to.assertSubscribed();

        to.assertNoErrors();

        TestException ex = new TestException("Forced failure");

        to.onError(ex);

        to.assertError(ex);

        to.assertError(TestException.class);

        to.assertError(Functions.<Throwable>alwaysTrue());

        to.assertError(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable t) throws Exception {
                return t.getMessage() != null && t.getMessage().contains("Forced");
            }
        });

        to.assertErrorMessage("Forced failure");

        try {
            to.assertErrorMessage("");
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        try {
            to.assertError(new RuntimeException());
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        try {
            to.assertError(IOException.class);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        try {
            to.assertError(Functions.<Throwable>alwaysFalse());
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        try {
            to.assertNoErrors();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError exc) {
            // expected
        }

        to.assertTerminated();

        to.assertValueCount(0);

        to.assertNoValues();


    }

    @Test
    public void emptyObserverEnum() {
        assertEquals(1, TestObserver.EmptyObserver.values().length);
        assertNotNull(TestObserver.EmptyObserver.valueOf("INSTANCE"));
    }

    @Test
    public void valueAndClass() {
        assertEquals("null", TestObserver.valueAndClass(null));
        assertEquals("1 (class: Integer)", TestObserver.valueAndClass(1));
    }

    @Test
    public void assertFailure() {
        TestObserver<Integer> to = TestObserver.create();

        to.onSubscribe(Disposables.empty());

        to.onError(new TestException("Forced failure"));

        to.assertFailure(TestException.class);

        to.assertFailure(Functions.<Throwable>alwaysTrue());

        to.assertFailureAndMessage(TestException.class, "Forced failure");

        to.onNext(1);

        to.assertFailure(TestException.class, 1);

        to.assertFailure(Functions.<Throwable>alwaysTrue(), 1);

        to.assertFailureAndMessage(TestException.class, "Forced failure", 1);
    }

    @Test
    public void assertFuseable() {
        TestObserver<Integer> to = TestObserver.create();

        to.onSubscribe(Disposables.empty());

        to.assertNotFuseable();

        try {
            to.assertFuseable();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            to.assertFusionMode(QueueFuseable.SYNC);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        to = TestObserver.create();
        to.setInitialFusionMode(QueueFuseable.ANY);

        to.onSubscribe(new ScalarDisposable<Integer>(to, 1));

        to.assertFuseable();

        to.assertFusionMode(QueueFuseable.SYNC);

        try {
            to.assertFusionMode(QueueFuseable.NONE);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            to.assertNotFuseable();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

    }

    @Test
    public void assertTerminated() {
        TestObserver<Integer> to = TestObserver.create();

        to.assertNotTerminated();

        to.onError(null);

        try {
            to.assertNotTerminated();
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void assertOf() {
        TestObserver<Integer> to = TestObserver.create();

        to.assertOf(new Consumer<TestObserver<Integer>>() {
            @Override
            public void accept(TestObserver<Integer> f) throws Exception {
                f.assertNotSubscribed();
            }
        });

        try {
            to.assertOf(new Consumer<TestObserver<Integer>>() {
                @Override
                public void accept(TestObserver<Integer> f) throws Exception {
                    f.assertSubscribed();
                }
            });
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            to.assertOf(new Consumer<TestObserver<Integer>>() {
                @Override
                public void accept(TestObserver<Integer> f) throws Exception {
                    throw new IllegalArgumentException();
                }
            });
            throw new RuntimeException("Should have thrown");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void assertResult() {
        TestObserver<Integer> to = TestObserver.create();

        to.onSubscribe(Disposables.empty());

        to.onComplete();

        to.assertResult();

        try {
            to.assertResult(1);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        to.onNext(1);

        to.assertResult(1);

        try {
            to.assertResult(2);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            to.assertResult();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

    }

    @Test(timeout = 5000)
    public void await() throws Exception {
        TestObserver<Integer> to = TestObserver.create();

        to.onSubscribe(Disposables.empty());

        assertFalse(to.await(100, TimeUnit.MILLISECONDS));

        to.awaitDone(100, TimeUnit.MILLISECONDS);

        assertTrue(to.isDisposed());

        assertFalse(to.awaitTerminalEvent(100, TimeUnit.MILLISECONDS));

        assertEquals(0, to.completions());
        assertEquals(0, to.errorCount());

        to.onComplete();

        assertTrue(to.await(100, TimeUnit.MILLISECONDS));

        to.await();

        to.awaitDone(5, TimeUnit.SECONDS);

        assertEquals(1, to.completions());
        assertEquals(0, to.errorCount());

        assertTrue(to.awaitTerminalEvent());

        final TestObserver<Integer> to1 = TestObserver.create();

        to1.onSubscribe(Disposables.empty());

        Schedulers.single().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                to1.onComplete();
            }
        }, 200, TimeUnit.MILLISECONDS);

        to1.await();

        to1.assertValueSet(Collections.<Integer>emptySet());
    }

    @Test
    public void errors() {
        TestObserver<Integer> to = TestObserver.create();

        to.onSubscribe(Disposables.empty());

        assertEquals(0, to.errors().size());

        to.onError(new TestException());

        assertEquals(1, to.errors().size());

        TestHelper.assertError(to.errors(), 0, TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onNext() {
        TestObserver<Integer> to = TestObserver.create();

        to.onSubscribe(Disposables.empty());

        assertEquals(0, to.valueCount());

        assertEquals(Collections.emptyList(), to.values());

        to.onNext(1);

        assertEquals(Collections.singletonList(1), to.values());

        to.cancel();

        assertTrue(to.isCancelled());
        assertTrue(to.isDisposed());

        to.assertValue(1);

        assertEquals(Arrays.asList(Collections.singletonList(1), Collections.emptyList(), Collections.emptyList()), to.getEvents());

        to.onComplete();

        assertEquals(Arrays.asList(Collections.singletonList(1), Collections.emptyList(), Collections.singletonList(Notification.createOnComplete())), to.getEvents());
    }

    @Test
    public void fusionModeToString() {
        assertEquals("NONE", TestObserver.fusionModeToString(QueueFuseable.NONE));
        assertEquals("SYNC", TestObserver.fusionModeToString(QueueFuseable.SYNC));
        assertEquals("ASYNC", TestObserver.fusionModeToString(QueueFuseable.ASYNC));
        assertEquals("Unknown(100)", TestObserver.fusionModeToString(100));
    }

    @Test
    public void multipleTerminals() {
        TestObserver<Integer> to = TestObserver.create();

        to.onSubscribe(Disposables.empty());

        to.assertNotComplete();

        to.onComplete();

        try {
            to.assertNotComplete();
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }

        to.assertTerminated();

        to.onComplete();

        try {
            to.assertComplete();
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }

        try {
            to.assertTerminated();
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }

        try {
            to.assertNotComplete();
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }
    }

    @Test
    public void assertValue() {
        TestObserver<Integer> to = TestObserver.create();

        to.onSubscribe(Disposables.empty());

        try {
            to.assertValue(1);
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }

        to.onNext(1);

        to.assertValue(1);

        try {
            to.assertValue(2);
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }

        to.onNext(2);

        try {
            to.assertValue(1);
            throw new RuntimeException("Should have thrown");
        } catch (Throwable ex) {
            // expected
        }
    }

    @Test
    public void onNextMisbehave() {
        TestObserver<Integer> to = TestObserver.create();

        to.onNext(1);

        to.assertError(IllegalStateException.class);

        to = TestObserver.create();

        to.onSubscribe(Disposables.empty());

        to.onNext(null);

        to.assertFailure(NullPointerException.class, (Integer)null);
    }

    @Test
    public void awaitTerminalEventInterrupt() {
        final TestObserver<Integer> to = TestObserver.create();

        to.onSubscribe(Disposables.empty());

        Thread.currentThread().interrupt();

        to.awaitTerminalEvent();

        assertTrue(Thread.interrupted());

        Thread.currentThread().interrupt();

        to.awaitTerminalEvent(5, TimeUnit.SECONDS);

        assertTrue(Thread.interrupted());
    }

    @Test
    public void assertTerminated2() {
        TestObserver<Integer> to = TestObserver.create();

        to.onSubscribe(Disposables.empty());

        assertFalse(to.isTerminated());

        to.onError(new TestException());
        to.onError(new IOException());

        assertTrue(to.isTerminated());

        try {
            to.assertTerminated();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }

        try {
            to.assertError(TestException.class);
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }


        to = TestObserver.create();

        to.onSubscribe(Disposables.empty());

        to.onError(new TestException());
        to.onComplete();

        try {
            to.assertTerminated();
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void onSubscribe() {
        TestObserver<Integer> to = TestObserver.create();

        to.onSubscribe(null);

        to.assertError(NullPointerException.class);

        to = TestObserver.create();

        to.onSubscribe(Disposables.empty());

        Disposable d1 = Disposables.empty();

        to.onSubscribe(d1);

        assertTrue(d1.isDisposed());

        to.assertError(IllegalStateException.class);

        to = TestObserver.create();
        to.dispose();

        d1 = Disposables.empty();

        to.onSubscribe(d1);

        assertTrue(d1.isDisposed());

    }

    @Test
    public void assertValueSequence() {
        TestObserver<Integer> to = TestObserver.create();

        to.onSubscribe(Disposables.empty());

        to.onNext(1);
        to.onNext(2);

        try {
            to.assertValueSequence(Collections.<Integer>emptyList());
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith("More values received than expected (0)"));
        }

        try {
            to.assertValueSequence(Collections.singletonList(1));
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith("More values received than expected (1)"));
        }

        to.assertValueSequence(Arrays.asList(1, 2));

        try {
            to.assertValueSequence(Arrays.asList(1, 2, 3));
            throw new RuntimeException("Should have thrown");
        } catch (AssertionError expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith("Fewer values received than expected (2)"));
        }
    }

    @Test
    public void assertEmpty() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        try {
            to.assertEmpty();
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }

        to.onSubscribe(Disposables.empty());

        to.assertEmpty();

        to.onNext(1);

        try {
            to.assertEmpty();
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void awaitDoneTimed() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        Thread.currentThread().interrupt();

        try {
            to.awaitDone(5, TimeUnit.SECONDS);
        } catch (RuntimeException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof InterruptedException);
        }
    }

    @Test
    public void assertNotSubscribed() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        to.assertNotSubscribed();

        to.errors().add(new TestException());

        try {
            to.assertNotSubscribed();
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void assertErrorMultiple() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        TestException e = new TestException();
        to.errors().add(e);
        to.errors().add(new TestException());

        try {
            to.assertError(TestException.class);
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
        try {
            to.assertError(e);
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
        try {
            to.assertError(Functions.<Throwable>alwaysTrue());
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
        try {
            to.assertErrorMessage("");
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void testErrorInPredicate() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.onError(new RuntimeException());
        try {
            to.assertError(new Predicate<Throwable>() {
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
    public void assertComplete() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        to.onSubscribe(Disposables.empty());

        try {
            to.assertComplete();
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }

        to.onComplete();

        to.assertComplete();

        to.onComplete();

        try {
            to.assertComplete();
            throw new RuntimeException("Should have thrown!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void completeWithoutOnSubscribe() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        to.onComplete();

        to.assertError(IllegalStateException.class);
    }

    @Test
    public void completeDelegateThrows() {
        TestObserver<Integer> to = new TestObserver<Integer>(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {

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

        to.onSubscribe(Disposables.empty());

        try {
            to.onComplete();
            throw new RuntimeException("Should have thrown!");
        } catch (TestException ex) {
            assertTrue(to.isTerminated());
        }
    }

    @Test
    public void errorDelegateThrows() {
        TestObserver<Integer> to = new TestObserver<Integer>(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {

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

        to.onSubscribe(Disposables.empty());

        try {
            to.onError(new IOException());
            throw new RuntimeException("Should have thrown!");
        } catch (TestException ex) {
            assertTrue(to.isTerminated());
        }
    }

    @Test
    public void syncQueueThrows() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.setInitialFusionMode(QueueFuseable.SYNC);

        Observable.range(1, 5)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception { throw new TestException(); }
        })
        .subscribe(to);

        to.assertSubscribed()
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertFailure(TestException.class);
    }

    @Test
    public void asyncQueueThrows() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.setInitialFusionMode(QueueFuseable.ANY);

        UnicastSubject<Integer> up = UnicastSubject.create();

        up
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception { throw new TestException(); }
        })
        .subscribe(to);

        up.onNext(1);

        to.assertSubscribed()
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertFailure(TestException.class);
    }

    @Test
    public void completedMeansDisposed() {
        // 2.0.2 - a terminated TestObserver no longer reports isDisposed
        assertFalse(Observable.just(1)
                .test()
                .assertResult(1).isDisposed());
    }

    @Test
    public void errorMeansDisposed() {
        // 2.0.2 - a terminated TestObserver no longer reports isDisposed
        assertFalse(Observable.error(new TestException())
                .test()
                .assertFailure(TestException.class).isDisposed());
    }

    @Test
    public void asyncFusion() {
        TestObserver<Object> to = new TestObserver<Object>();
        to.setInitialFusionMode(QueueFuseable.ANY);

        UnicastSubject<Integer> up = UnicastSubject.create();

        up
        .subscribe(to);

        up.onNext(1);
        up.onComplete();

        to.assertSubscribed()
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1);
    }

    @Test
    public void assertValuePredicateEmpty() {
        TestObserver<Object> to = new TestObserver<Object>();

        Observable.empty().subscribe(to);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("No values");
        to.assertValue(new Predicate<Object>() {
            @Override public boolean test(final Object o) throws Exception {
                return false;
            }
        });
    }

    @Test
    public void assertValuePredicateMatch() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        Observable.just(1).subscribe(to);

        to.assertValue(new Predicate<Integer>() {
            @Override public boolean test(final Integer o) throws Exception {
                return o == 1;
            }
        });
    }

    @Test
    public void assertValuePredicateNoMatch() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        Observable.just(1).subscribe(to);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Value not present");
        to.assertValue(new Predicate<Integer>() {
            @Override public boolean test(final Integer o) throws Exception {
                return o != 1;
            }
        });
    }

    @Test
    public void assertValuePredicateMatchButMore() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        Observable.just(1, 2).subscribe(to);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Value present but other values as well");
        to.assertValue(new Predicate<Integer>() {
            @Override public boolean test(final Integer o) throws Exception {
                return o == 1;
            }
        });
    }

    @Test
    public void assertValueAtPredicateEmpty() {
        TestObserver<Object> to = new TestObserver<Object>();

        Observable.empty().subscribe(to);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("No values");
        to.assertValueAt(0, new Predicate<Object>() {
            @Override public boolean test(final Object o) throws Exception {
                return false;
            }
        });
    }

    @Test
    public void assertValueAtPredicateMatch() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        Observable.just(1, 2).subscribe(to);

        to.assertValueAt(1, new Predicate<Integer>() {
            @Override public boolean test(final Integer o) throws Exception {
                return o == 2;
            }
        });
    }

    @Test
    public void assertValueAtPredicateNoMatch() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        Observable.just(1, 2, 3).subscribe(to);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Value not present");
        to.assertValueAt(2, new Predicate<Integer>() {
            @Override public boolean test(final Integer o) throws Exception {
                return o != 3;
            }
        });
    }

    @Test
    public void assertValueAtInvalidIndex() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        Observable.just(1, 2).subscribe(to);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Invalid index: 2 (latch = 0, values = 2, errors = 0, completions = 1)");
        to.assertValueAt(2, new Predicate<Integer>() {
            @Override public boolean test(final Integer o) throws Exception {
                return o == 1;
            }
        });
    }

    @Test
    public void assertValueAtIndexEmpty() {
        TestObserver<Object> to = new TestObserver<Object>();

        Observable.empty().subscribe(to);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("No values");
        to.assertValueAt(0, "a");
    }

    @Test
    public void assertValueAtIndexMatch() {
        TestObserver<String> to = new TestObserver<String>();

        Observable.just("a", "b").subscribe(to);

        to.assertValueAt(1, "b");
    }

    @Test
    public void assertValueAtIndexNoMatch() {
        TestObserver<String> to = new TestObserver<String>();

        Observable.just("a", "b", "c").subscribe(to);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Expected: b (class: String), Actual: c (class: String) (latch = 0, values = 3, errors = 0, completions = 1)");
        to.assertValueAt(2, "b");
    }

    @Test
    public void assertValueAtIndexInvalidIndex() {
        TestObserver<String> to = new TestObserver<String>();

        Observable.just("a", "b").subscribe(to);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Invalid index: 2 (latch = 0, values = 2, errors = 0, completions = 1)");
        to.assertValueAt(2, "c");
    }

    @Test
    public void withTag() {
        try {
            for (int i = 1; i < 3; i++) {
                Observable.just(i)
                .test()
                .withTag("testing with item=" + i)
                .assertResult(1)
                ;
            }
            fail("Should have thrown!");
        } catch (AssertionError ex) {
            assertTrue(ex.toString(), ex.toString().contains("testing with item=2"));
        }
    }

    @Test
    public void assertValuesOnly() {
        TestObserver<Integer> to = TestObserver.create();
        to.onSubscribe(Disposables.empty());
        to.assertValuesOnly();

        to.onNext(5);
        to.assertValuesOnly(5);

        to.onNext(-1);
        to.assertValuesOnly(5, -1);
    }

    @Test
    public void assertValuesOnlyThrowsOnUnexpectedValue() {
        TestObserver<Integer> to = TestObserver.create();
        to.onSubscribe(Disposables.empty());
        to.assertValuesOnly();

        to.onNext(5);
        to.assertValuesOnly(5);

        to.onNext(-1);

        try {
            to.assertValuesOnly(5);
            fail();
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void assertValuesOnlyThrowsWhenCompleted() {
        TestObserver<Integer> to = TestObserver.create();
        to.onSubscribe(Disposables.empty());

        to.onComplete();

        try {
            to.assertValuesOnly();
            fail();
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void assertValuesOnlyThrowsWhenErrored() {
        TestObserver<Integer> to = TestObserver.create();
        to.onSubscribe(Disposables.empty());

        to.onError(new TestException());

        try {
            to.assertValuesOnly();
            fail();
        } catch (AssertionError ex) {
            // expected
        }
    }
}
