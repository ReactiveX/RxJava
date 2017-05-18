package rx.singles;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import rx.*;
import rx.exceptions.CompositeException;
import rx.exceptions.TestException;
import rx.functions.Action0;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class TestSingleSubscriberTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void assertValue() {
        Single<String> s = Single.just("foo");
        TestSingleSubscriber<String> o = new TestSingleSubscriber<String>();
        s.subscribe(o);

        o.assertValue("foo");
        assertEquals("foo", o.getOnSuccessEvent());
        o.assertTerminalEvent();
    }

    @Test
    public void assertNotMatchValue() {
        Single<String> s = Single.just("foo");
        TestSingleSubscriber<String> o = new TestSingleSubscriber<String>();
        s.subscribe(o);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Value at index: 0 expected to be [bar] (String) but was: [foo] (String)");

        o.assertValue("bar");
        assertEquals("bar", o.getOnSuccessEvent());
        o.assertTerminalEvent();
    }

    @Test
    public void assertTerminalEventNotReceived() {
        Single<String> s = Single.create(new Single.OnSubscribe<String>() {
            @Override
            public void call(SingleSubscriber<? super String> singleSubscriber) {
            }
        });

        TestSingleSubscriber<String> o = new TestSingleSubscriber<String>();
        s.subscribe(o);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("No terminal events received.");

        o.assertTerminalEvent();
    }

    @Test
    public void wrappingMock() {
        Single<Integer> s = Single.just(1);
        @SuppressWarnings("unchecked")
        Observer<Integer> mockObserver = mock(Observer.class);
        s.subscribe(new TestSingleSubscriber<Integer>(mockObserver));

        InOrder inOrder = inOrder(mockObserver);
        inOrder.verify(mockObserver, times(1)).onNext(1);
        inOrder.verify(mockObserver, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void assertError() {
        RuntimeException e = new RuntimeException("Oops");
        TestSingleSubscriber<Object> subscriber = new TestSingleSubscriber<Object>();
        Single.error(e).subscribe(subscriber);
        subscriber.assertError(e);
    }

    @Test
    public void awaitTerminalEventWithDuration() {
        TestSingleSubscriber<Object> ts = new TestSingleSubscriber<Object>();
        Single.just(1).subscribe(ts);
        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertTerminalEvent();
    }

    @Test
    public void awaitTerminalEventWithDurationAndUnsubscribeOnTimeout() {
        TestSingleSubscriber<Object> ts = new TestSingleSubscriber<Object>();
        final AtomicBoolean unsub = new AtomicBoolean(false);
        Single.just(1)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        unsub.set(true);
                    }
                })
                .delay(1000, TimeUnit.MILLISECONDS).subscribe(ts);
        ts.awaitTerminalEventAndUnsubscribeOnTimeout(100, TimeUnit.MILLISECONDS);
        assertTrue(unsub.get());
    }

    @Test(expected = NullPointerException.class)
    public void nullDelegate1() {
        TestSingleSubscriber<Integer> ts = new TestSingleSubscriber<Integer>((Observer<Integer>) null);
        ts.onSuccess(1);
    }

    @Test(expected = NullPointerException.class)
    public void nullDelegate2() {
        TestSingleSubscriber<Integer> ts = new TestSingleSubscriber<Integer>((Subscriber<Integer>) null);
        ts.onSuccess(1);
    }

    @Test
    public void delegate1() {
        TestObserver<Integer> to = new TestObserver<Integer>();
        TestSingleSubscriber<Integer> ts = TestSingleSubscriber.create(to);
        ts.onSuccess(0);

        to.assertTerminalEvent();
    }

    @Test
    public void delegate2() {
        TestSubscriber<Integer> ts1 = TestSubscriber.create();
        TestSingleSubscriber<Integer> ts2 = TestSingleSubscriber.create(ts1);
        ts2.onSuccess(0);

        ts1.assertCompleted();
    }

    @Test
    public void unsubscribed() {
        TestSingleSubscriber<Integer> ts = new TestSingleSubscriber<Integer>();
        try {
            ts.assertUnsubscribed();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Not unsubscribed but not reported!");
    }

    @Test
    public void noErrors() {
        TestSingleSubscriber<Integer> ts = new TestSingleSubscriber<Integer>();
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
    public void notSuccess() {
        TestSingleSubscriber<Integer> ts = new TestSingleSubscriber<Integer>();
        try {
            ts.assertSuccess();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Not success and no assertion error!");
    }

    @Test
    public void success() {
        TestSingleSubscriber<Integer> ts = new TestSingleSubscriber<Integer>();
        ts.onSuccess(0);
        try {
            ts.assertNotSuccess();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Completed and no assertion error!");
    }

    @Test
    public void multipleSuccess() {
        TestSingleSubscriber<Integer> ts = new TestSingleSubscriber<Integer>();
        ts.onSuccess(0);
        ts.onSuccess(1);
        try {
            ts.assertSuccess();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Multiple success and no assertion error!");
    }

    @Test
    public void multipleSuccess2() {
        TestSingleSubscriber<Integer> ts = new TestSingleSubscriber<Integer>();
        ts.onSuccess(0);
        ts.onSuccess(1);
        try {
            ts.assertNotSuccess();
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("Multiple success and no assertion error!");
    }

    @Test
    public void multipleErrors3() {
        TestSingleSubscriber<Integer> ts = new TestSingleSubscriber<Integer>();
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
    public void differentError() {
        TestSingleSubscriber<Integer> ts = new TestSingleSubscriber<Integer>();
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
        TestSingleSubscriber<Integer> ts = new TestSingleSubscriber<Integer>();
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
        TestSingleSubscriber<Integer> ts = new TestSingleSubscriber<Integer>();
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
    public void noError() {
        TestSingleSubscriber<Integer> ts = new TestSingleSubscriber<Integer>();
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
        TestSingleSubscriber<Integer> ts = new TestSingleSubscriber<Integer>();
        try {
            ts.assertError(new TestException());
        } catch (AssertionError ex) {
            // expected
            return;
        }
        fail("No present but no assertion error!");
    }

    @Test
    public void interruptTerminalEventAwait() {
        TestSingleSubscriber<Integer> ts = TestSingleSubscriber.create();

        final Thread t0 = Thread.currentThread();
        Scheduler.Worker w = Schedulers.computation().createWorker();
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
    public void interruptTerminalEventAwaitTimed() {
        TestSingleSubscriber<Integer> ts = TestSingleSubscriber.create();

        final Thread t0 = Thread.currentThread();
        Scheduler.Worker w = Schedulers.computation().createWorker();
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
    public void interruptTerminalEventAwaitAndUnsubscribe() {
        TestSingleSubscriber<Integer> ts = TestSingleSubscriber.create();

        final Thread t0 = Thread.currentThread();
        Scheduler.Worker w = Schedulers.computation().createWorker();
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
    public void noTerminalEventBut1Success() {
        TestSingleSubscriber<Integer> ts = TestSingleSubscriber.create();

        ts.onSuccess(0);

        try {
            ts.assertNoTerminalEvent();
            fail("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void noTerminalEventBut1Error() {
        TestSingleSubscriber<Integer> ts = TestSingleSubscriber.create();

        ts.onError(new TestException());

        try {
            ts.assertNoTerminalEvent();
            fail("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void noTerminalEventBut1Error1Completed() {
        TestSingleSubscriber<Integer> ts = TestSingleSubscriber.create();

        ts.onSuccess(0);
        ts.onError(new TestException());

        try {
            ts.assertNoTerminalEvent();
            fail("Failed to report there were terminal event(s)!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void noTerminalEventBut2Errors() {
        TestSingleSubscriber<Integer> ts = TestSingleSubscriber.create();

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
    public void noValues() {
        TestSingleSubscriber<Integer> ts = TestSingleSubscriber.create();
        ts.onSuccess(1);

        try {
            ts.assertNotSuccess();
            fail("Failed to report there were values!");
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test(timeout = 1000)
    public void onCompletedCrashCountsDownLatch() {
        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onCompleted() {
                throw new TestException();
            }
        };
        TestSingleSubscriber<Integer> ts = TestSingleSubscriber.create(to);

        try {
            ts.onSuccess(0);
        } catch (TestException ex) {
            // expected
        }

        ts.awaitTerminalEvent();
    }

    @Test(timeout = 1000)
    public void onErrorCrashCountsDownLatch() {
        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onError(Throwable e) {
                throw new TestException();
            }
        };
        TestSingleSubscriber<Integer> ts = TestSingleSubscriber.create(to);

        try {
            ts.onError(new RuntimeException());
        } catch (TestException ex) {
            // expected
        }

        ts.awaitTerminalEvent();
    }
}
