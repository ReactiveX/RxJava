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
package rx.observers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.exceptions.*;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class TestSubscriberTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testAssert() {
        Observable<Integer> oi = Observable.from(Arrays.asList(1, 2));
        TestSubscriber<Integer> o = new TestSubscriber<Integer>();
        oi.subscribe(o);

        o.assertReceivedOnNext(Arrays.asList(1, 2));
        assertEquals(2, o.getOnNextEvents().size());
        o.assertTerminalEvent();
    }

    @Test
    public void testAssertNotMatchCount() {
        Observable<Integer> oi = Observable.from(Arrays.asList(1, 2));
        TestSubscriber<Integer> o = new TestSubscriber<Integer>();
        oi.subscribe(o);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Number of items does not match. Provided: 1  Actual: 2");

        o.assertReceivedOnNext(Arrays.asList(1));
        assertEquals(2, o.getOnNextEvents().size());
        o.assertTerminalEvent();
    }

    @Test
    public void testAssertNotMatchValue() {
        Observable<Integer> oi = Observable.from(Arrays.asList(1, 2));
        TestSubscriber<Integer> o = new TestSubscriber<Integer>();
        oi.subscribe(o);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Value at index: 1 expected to be [3] (Integer) but was: [2] (Integer)");


        o.assertReceivedOnNext(Arrays.asList(1, 3));
        assertEquals(2, o.getOnNextEvents().size());
        o.assertTerminalEvent();
    }

    @Test
    public void testAssertTerminalEventNotReceived() {
        PublishSubject<Integer> p = PublishSubject.create();
        TestSubscriber<Integer> o = new TestSubscriber<Integer>();
        p.subscribe(o);

        p.onNext(1);
        p.onNext(2);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("No terminal events received.");

        o.assertReceivedOnNext(Arrays.asList(1, 2));
        assertEquals(2, o.getOnNextEvents().size());
        o.assertTerminalEvent();
    }

    @Test
    public void testWrappingMock() {
        Observable<Integer> oi = Observable.from(Arrays.asList(1, 2));
        @SuppressWarnings("unchecked")
        Observer<Integer> mockObserver = mock(Observer.class);
        oi.subscribe(new TestSubscriber<Integer>(mockObserver));

        InOrder inOrder = inOrder(mockObserver);
        inOrder.verify(mockObserver, times(1)).onNext(1);
        inOrder.verify(mockObserver, times(1)).onNext(2);
        inOrder.verify(mockObserver, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testWrappingMockWhenUnsubscribeInvolved() {
        Observable<Integer> oi = Observable.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9)).take(2);
        @SuppressWarnings("unchecked")
        Observer<Integer> mockObserver = mock(Observer.class);
        oi.subscribe(new TestSubscriber<Integer>(mockObserver));

        InOrder inOrder = inOrder(mockObserver);
        inOrder.verify(mockObserver, times(1)).onNext(1);
        inOrder.verify(mockObserver, times(1)).onNext(2);
        inOrder.verify(mockObserver, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testAssertError() {
        RuntimeException e = new RuntimeException("Oops");
        TestSubscriber<Object> subscriber = new TestSubscriber<Object>();
        Observable.error(e).subscribe(subscriber);
        subscriber.assertError(e);
    }
    
    @Test
    public void testAwaitTerminalEventWithDuration() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        Observable.just(1).subscribe(ts);
        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertTerminalEvent();
    }
    
    @Test
    public void testAwaitTerminalEventWithDurationAndUnsubscribeOnTimeout() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        final AtomicBoolean unsub = new AtomicBoolean(false);
        Observable.just(1)
        //
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        unsub.set(true);
                    }
                })
                //
                .delay(1000, TimeUnit.MILLISECONDS).subscribe(ts);
        ts.awaitTerminalEventAndUnsubscribeOnTimeout(100, TimeUnit.MILLISECONDS);
        assertTrue(unsub.get());
    }

    @Test(expected = NullPointerException.class)
    public void testNullDelegate1() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>((Observer<Integer>)null);
        ts.onCompleted();
    }
    
    @Test(expected = NullPointerException.class)
    public void testNullDelegate2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>((Subscriber<Integer>)null);
        ts.onCompleted();
    }
    
    @Test(expected = NullPointerException.class)
    public void testNullDelegate3() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>((Subscriber<Integer>)null, 0);
        ts.onCompleted();
    }
    
    @Test
    public void testDelegate1() {
        TestObserver<Integer> to = new TestObserver<Integer>();
        TestSubscriber<Integer> ts = TestSubscriber.create(to);
        ts.onCompleted();
        
        to.assertTerminalEvent();
    }
    
    @Test
    public void testDelegate2() {
        TestSubscriber<Integer> ts1 = TestSubscriber.create();
        TestSubscriber<Integer> ts2 = TestSubscriber.create(ts1);
        ts2.onCompleted();
        
        ts1.assertCompleted();
    }
    
    @Test
    public void testDelegate3() {
        TestSubscriber<Integer> ts1 = TestSubscriber.create();
        TestSubscriber<Integer> ts2 = TestSubscriber.create(ts1, 0);
        ts2.onCompleted();
        ts1.assertCompleted();
    }
    
    @Test
    public void testUnsubscribed() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        try {
            ts.assertUnsubscribed();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Not unsubscribed but not reported!");
    }
    
    @Test
    public void testNoErrors() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
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
    public void testNotCompleted() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        try {
            ts.assertCompleted();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Not completed and no assertion error!");
    }
    
    @Test
    public void testMultipleCompletions() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.onCompleted();
        ts.onCompleted();
        try {
            ts.assertCompleted();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Multiple completions and no assertion error!");
    }
    
    @Test
    public void testCompleted() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.onCompleted();
        try {
            ts.assertNotCompleted();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Completed and no assertion error!");
    }
    
    @Test
    public void testMultipleCompletions2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.onCompleted();
        ts.onCompleted();
        try {
            ts.assertNotCompleted();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Multiple completions and no assertion error!");
    }
    
    @Test
    public void testMultipleErrors() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.onError(new TestException());
        ts.onError(new TestException());
        try {
            ts.assertNoErrors();
        } catch (AssertionError ex) {
            if (!(ex.getCause() instanceof CompositeException)) {
                fail("Multiple Error present but the reported error doesn't have a composite cause!");
            }
            // expected
            return;
        }
        fail("Multiple Error present but no assertion error!");
    }
    
    @Test
    public void testMultipleErrors2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.onError(new TestException());
        ts.onError(new TestException());
        try {
            ts.assertError(TestException.class);
        } catch (AssertionError ex) {
            if (!(ex.getCause() instanceof CompositeException)) {
                fail("Multiple Error present but the reported error doesn't have a composite cause!");
            }
            // expected
            return;
        }
        fail("Multiple Error present but no assertion error!");
    }
    
    @Test
    public void testMultipleErrors3() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.onError(new TestException());
        ts.onError(new TestException());
        try {
            ts.assertError(new TestException());
        } catch (AssertionError ex) {
            if (!(ex.getCause() instanceof CompositeException)) {
                fail("Multiple Error present but the reported error doesn't have a composite cause!");
            }
            // expected
            return;
        }
        fail("Multiple Error present but no assertion error!");
    }
    
    @Test
    public void testDifferentError() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
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
    public void testDifferentError2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
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
    public void testDifferentError3() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.onError(new RuntimeException());
        try {
            ts.assertError(TestException.class);
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Different Error present but no assertion error!");
    }
    
    @Test
    public void testNoError() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        try {
            ts.assertError(TestException.class);
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("No present but no assertion error!");
    }

    @Test
    public void testNoError2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        try {
            ts.assertError(new TestException());
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("No present but no assertion error!");
    }
    
    @Test
    public void testInterruptTerminalEventAwait() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        final Thread t0 = Thread.currentThread();
        Worker w = Schedulers.computation().createWorker();
        try {
            w.schedule(new Action0() {
                @Override
                public void call() {
                    t0.interrupt();
                }
            }, 200, TimeUnit.MILLISECONDS);
            
            try {
                ts.awaitTerminalEvent();
                fail("Did not interrupt wait!");
            } catch (RuntimeException ex) {
                if (!(ex.getCause() instanceof InterruptedException)) {
                    fail("The cause is not InterruptedException! " + ex.getCause());
                }
            }
        } finally {
            w.unsubscribe();
        }
    }
    
    @Test
    public void testInterruptTerminalEventAwaitTimed() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        final Thread t0 = Thread.currentThread();
        Worker w = Schedulers.computation().createWorker();
        try {
            w.schedule(new Action0() {
                @Override
                public void call() {
                    t0.interrupt();
                }
            }, 200, TimeUnit.MILLISECONDS);
            
            try {
                ts.awaitTerminalEvent(5, TimeUnit.SECONDS);
                fail("Did not interrupt wait!");
            } catch (RuntimeException ex) {
                if (!(ex.getCause() instanceof InterruptedException)) {
                    fail("The cause is not InterruptedException! " + ex.getCause());
                }
            }
        } finally {
            w.unsubscribe();
        }
    }
    
    @Test
    public void testInterruptTerminalEventAwaitAndUnsubscribe() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        final Thread t0 = Thread.currentThread();
        Worker w = Schedulers.computation().createWorker();
        try {
            w.schedule(new Action0() {
                @Override
                public void call() {
                    t0.interrupt();
                }
            }, 200, TimeUnit.MILLISECONDS);
            
            ts.awaitTerminalEventAndUnsubscribeOnTimeout(5, TimeUnit.SECONDS);
            if (!ts.isUnsubscribed()) {
                fail("Did not unsubscribe!");
            }
        } finally {
            w.unsubscribe();
        }
    }
    
    @Test
    public void testNoTerminalEventBut1Completed() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        ts.onCompleted();
        
        try {
            ts.assertNoTerminalEvent();
            fail("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }
    
    @Test
    public void testNoTerminalEventBut1Error() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        ts.onError(new TestException());
        
        try {
            ts.assertNoTerminalEvent();
            fail("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }
    
    @Test
    public void testNoTerminalEventBut1Error1Completed() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        ts.onCompleted();
        ts.onError(new TestException());
        
        try {
            ts.assertNoTerminalEvent();
            fail("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }
    
    @Test
    public void testNoTerminalEventBut2Errors() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        ts.onError(new TestException());
        ts.onError(new TestException());
        
        try {
            ts.assertNoTerminalEvent();
            fail("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
            if (!(ex.getCause() instanceof CompositeException)) {
                fail("Did not report a composite exception cause: " + ex.getCause());
            }
        }
    }
    
    @Test
    public void testNoValues() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        ts.onNext(1);
        
        try {
            ts.assertNoValues();
            fail("Failed to report there were values!");
        } catch (AssertionError ex) {
            // expected
        }
    }
    
    @Test
    public void testValueCount() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        ts.onNext(1);
        ts.onNext(2);
        
        try {
            ts.assertValueCount(3);
            fail("Failed to report there were values!");
        } catch (AssertionError ex) {
            // expected
        }
    }
    
    @Test(timeout = 1000)
    public void testOnCompletedCrashCountsDownLatch() {
        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onCompleted() {
                throw new TestException();
            }
        };
        TestSubscriber<Integer> ts = TestSubscriber.create(to);
        
        try {
            ts.onCompleted();
        } catch (TestException ex) {
            // expected
        }
        
        ts.awaitTerminalEvent();
    }
    
    @Test(timeout = 1000)
    public void testOnErrorCrashCountsDownLatch() {
        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }
        };
        TestSubscriber<Integer> ts = TestSubscriber.create(to);
        
        try {
            ts.onError(new RuntimeException());
        } catch (TestException ex) {
            // expected
        }
        
        ts.awaitTerminalEvent();
    }

    @Test
    public void assertValuesShouldThrowIfNumberOfItemsDoesNotMatch() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        ts.onNext("a");
        ts.onNext("b");
        ts.onNext("c");

        try {
            ts.assertValues("1", "2");
            fail();
        } catch (AssertionError expected) {
            assertEquals("Number of items does not match. Provided: 2  Actual: 3.\n" +
                    "Provided values: [1, 2]\n" +
                    "Actual values: [a, b, c]\n (0 completions)",
                    expected.getMessage()
            );
        }
    }
    
    @Test
    public void assertionFailureGivesActiveDetails() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        ts.onNext("a");
        ts.onNext("b");
        ts.onNext("c");
        ts.onError(new TestException("forced failure"));

        try {
            ts.assertValues("1", "2");
            fail();
        } catch (AssertionError expected) {
            assertEquals("Number of items does not match. Provided: 2  Actual: 3.\n" +
                    "Provided values: [1, 2]\n" +
                    "Actual values: [a, b, c]\n (0 completions) (+1 error)",
                    expected.getMessage()
            );
            Throwable ex = expected.getCause();
            assertEquals(TestException.class, ex.getClass());
            assertEquals("forced failure", ex.getMessage());
        }
    }
    
    @Test
    public void assertionFailureShowsMultipleErrors() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        ts.onNext("a");
        ts.onNext("b");
        ts.onNext("c");
        ts.onError(new TestException("forced failure"));
        ts.onError(new TestException("forced failure 2"));

        try {
            ts.assertValues("1", "2");
            fail();
        } catch (AssertionError expected) {
            assertEquals("Number of items does not match. Provided: 2  Actual: 3.\n" +
                    "Provided values: [1, 2]\n" +
                    "Actual values: [a, b, c]\n (0 completions) (+2 errors)",
                    expected.getMessage()
            );
            Throwable ex = expected.getCause();
            assertEquals(CompositeException.class, ex.getClass());
            List<Throwable> list = ((CompositeException)ex).getExceptions();
            assertEquals(2, list.size());
            assertEquals("forced failure", list.get(0).getMessage());
            assertEquals("forced failure 2", list.get(1).getMessage());
        }
    }

    @Test
    public void assertionFailureShowsCompletion() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        ts.onNext("a");
        ts.onNext("b");
        ts.onNext("c");
        ts.onCompleted();

        try {
            ts.assertValues("1", "2");
            fail();
        } catch (AssertionError expected) {
            assertEquals("Number of items does not match. Provided: 2  Actual: 3.\n" +
                    "Provided values: [1, 2]\n" +
                    "Actual values: [a, b, c]\n (1 completion)",
                    expected.getMessage()
            );
        }
    }

    @Test
    public void assertionFailureShowsMultipleCompletions() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        ts.onNext("a");
        ts.onNext("b");
        ts.onNext("c");
        ts.onCompleted();
        ts.onCompleted();

        try {
            ts.assertValues("1", "2");
            fail();
        } catch (AssertionError expected) {
            assertEquals("Number of items does not match. Provided: 2  Actual: 3.\n" +
                    "Provided values: [1, 2]\n" +
                    "Actual values: [a, b, c]\n (2 completions)",
                    expected.getMessage()
            );
        }
    }

}
