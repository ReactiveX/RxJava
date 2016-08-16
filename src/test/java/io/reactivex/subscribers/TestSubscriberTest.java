/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.subscribers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Action;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

public class TestSubscriberTest {

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
        // FIXME different message pattern
        // thrown.expectMessage("Number of items does not match. Provided: 1  Actual: 2");

        o.assertValues(1);
        o.assertValueCount(2);
        o.assertTerminated();
    }

    @Test
    public void testAssertNotMatchValue() {
        Flowable<Integer> oi = Flowable.fromIterable(Arrays.asList(1, 2));
        TestSubscriber<Integer> o = new TestSubscriber<Integer>();
        oi.subscribe(o);

        thrown.expect(AssertionError.class);
        // FIXME different message pattern
        // thrown.expectMessage("Value at index: 1 expected to be [3] (Integer) but was: [2] (Integer)");


        o.assertValues(1, 3);
        o.assertValueCount(2);
        o.assertTerminated();
    }

    @Test
    public void testAssertTerminalEventNotReceived() {
        PublishProcessor<Integer> p = PublishProcessor.create();
        TestSubscriber<Integer> o = new TestSubscriber<Integer>();
        p.subscribe(o);

        p.onNext(1);
        p.onNext(2);

        thrown.expect(AssertionError.class);
        // FIXME different message pattern
        // thrown.expectMessage("No terminal events received.");

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
    public void testAssertError() {
        RuntimeException e = new RuntimeException("Oops");
        TestSubscriber<Object> subscriber = new TestSubscriber<Object>();
        Flowable.error(e).subscribe(subscriber);
        subscriber.assertError(e);
    }
    
    @Test
    public void testAwaitTerminalEventWithDuration() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        Flowable.just(1).subscribe(ts);
        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertTerminated();
    }
    
    @Test
    public void testAwaitTerminalEventWithDurationAndUnsubscribeOnTimeout() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
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
        ts.awaitTerminalEvent(100, TimeUnit.MILLISECONDS);
        ts.dispose();
        assertTrue(unsub.get());
    }

    @Test(expected = NullPointerException.class)
    public void testNullDelegate1() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(null);
        ts.onComplete();
    }
    
    @Test(expected = NullPointerException.class)
    public void testNullDelegate2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(null);
        ts.onComplete();
    }
    
    @Test(expected = NullPointerException.class)
    public void testNullDelegate3() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(null, 0L);
        ts.onComplete();
    }
    
    @Test
    public void testDelegate1() {
        TestSubscriber<Integer> to = new TestSubscriber<Integer>();
        to.onSubscribe(EmptySubscription.INSTANCE);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(to);
        ts.onComplete();
        
        to.assertTerminated();
    }
    
    @Test
    public void testDelegate2() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>();
        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>(ts1);
        ts2.onComplete();
        
        ts1.assertComplete();
    }
    
    @Test
    public void testDelegate3() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>();
        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>(ts1, 0L);
        ts2.onComplete();
        ts1.assertComplete();
    }
    
    @Test
    public void testUnsubscribed() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        assertFalse(ts.isCancelled());
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
            ts.assertComplete();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Not completed and no assertion error!");
    }
    
    @Test
    public void testMultipleCompletions() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
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
    public void testCompleted() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
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
    public void testMultipleCompletions2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
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
    public void testMultipleErrors() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
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
    public void testMultipleErrors2() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
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
    public void testMultipleErrors3() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
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
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

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
                if (ts.awaitTerminalEvent()) {
                    fail("Did not interrupt wait!");
                }
            } catch (RuntimeException ex) {
                if (!(ex.getCause() instanceof InterruptedException)) {
                    fail("The cause is not InterruptedException! " + ex.getCause());
                }
            }
        } finally {
            w.dispose();
        }
    }
    
    @Test
    public void testInterruptTerminalEventAwaitTimed() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

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
                if (ts.awaitTerminalEvent(5, TimeUnit.SECONDS)) {
                    fail("Did not interrupt wait!");
                }
            } catch (RuntimeException ex) {
                if (!(ex.getCause() instanceof InterruptedException)) {
                    fail("The cause is not InterruptedException! " + ex.getCause());
                }
            }
        } finally {
            Thread.interrupted(); // clear interrupted flag
            w.dispose();
        }
    }
    
    @Test
    public void testInterruptTerminalEventAwaitAndUnsubscribe() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        final Thread t0 = Thread.currentThread();
        Worker w = Schedulers.computation().createWorker();
        try {
            w.schedule(new Runnable() {
                @Override
                public void run() {
                    t0.interrupt();
                }
            }, 200, TimeUnit.MILLISECONDS);
            
            ts.awaitTerminalEvent(5, TimeUnit.SECONDS);
            ts.dispose();
            if (!ts.isCancelled()) {
                fail("Did not unsubscribe!");
            }
        } finally {
            w.dispose();
        }
    }
    
    @Test
    public void testNoTerminalEventBut1Completed() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        ts.onComplete();
        
        try {
            ts.assertNotTerminated();
            fail("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }
    
    @Test
    public void testNoTerminalEventBut1Error() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        ts.onError(new TestException());
        
        try {
            ts.assertNotTerminated();
            fail("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }
    
    @Test
    public void testNoTerminalEventBut1Error1Completed() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        ts.onComplete();
        ts.onError(new TestException());
        
        try {
            ts.assertNotTerminated();
            fail("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }
    
    @Test
    public void testNoTerminalEventBut2Errors() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.onSubscribe(EmptySubscription.INSTANCE);
        
        ts.onError(new TestException());
        ts.onError(new TestException());
        
        try {
            ts.assertNotTerminated();
            fail("Failed to report there were terminal event(s)!");
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
    public void testNoValues() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
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
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
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
        TestSubscriber<Integer> to = new TestSubscriber<Integer>() {
            @Override
            public void onComplete() {
                throw new TestException();
            }
        };
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(to);
        
        try {
            ts.onComplete();
        } catch (TestException ex) {
            // expected
        }
        
        ts.awaitTerminalEvent();
    }
    
    @Test(timeout = 1000)
    public void testOnErrorCrashCountsDownLatch() {
        TestSubscriber<Integer> to = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }
        };
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(to);
        
        try {
            ts.onError(new RuntimeException());
        } catch (TestException ex) {
            // expected
        }
        
        ts.awaitTerminalEvent();
    }
}