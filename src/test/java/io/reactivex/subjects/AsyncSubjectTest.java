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

package io.reactivex.subjects;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;
import org.mockito.*;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.observers.*;
import io.reactivex.schedulers.Schedulers;

public class AsyncSubjectTest {

    private final Throwable testException = new Throwable();

    @Test
    public void testNeverCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");

        verify(observer, Mockito.never()).onNext(anyString());
        verify(observer, Mockito.never()).onError(testException);
        verify(observer, Mockito.never()).onComplete();
    }

    @Test
    public void testCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onComplete();

        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    @Ignore("Null values not allowed")
    public void testNull() {
        AsyncSubject<String> subject = AsyncSubject.create();

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        subject.onNext(null);
        subject.onComplete();

        verify(observer, times(1)).onNext(null);
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSubscribeAfterCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        Observer<String> observer = TestHelper.mockObserver();

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onComplete();

        subject.subscribe(observer);

        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSubscribeAfterError() {
        AsyncSubject<String> subject = AsyncSubject.create();

        Observer<String> observer = TestHelper.mockObserver();

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");

        RuntimeException re = new RuntimeException("failed");
        subject.onError(re);

        subject.subscribe(observer);

        verify(observer, times(1)).onError(re);
        verify(observer, Mockito.never()).onNext(any(String.class));
        verify(observer, Mockito.never()).onComplete();
    }

    @Test
    public void testError() {
        AsyncSubject<String> subject = AsyncSubject.create();

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onError(testException);
        subject.onNext("four");
        subject.onError(new Throwable());
        subject.onComplete();

        verify(observer, Mockito.never()).onNext(anyString());
        verify(observer, times(1)).onError(testException);
        verify(observer, Mockito.never()).onComplete();
    }

    @Test
    public void testUnsubscribeBeforeCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);
        subject.subscribe(ts);

        subject.onNext("one");
        subject.onNext("two");

        ts.dispose();

        verify(observer, Mockito.never()).onNext(anyString());
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, Mockito.never()).onComplete();

        subject.onNext("three");
        subject.onComplete();

        verify(observer, Mockito.never()).onNext(anyString());
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, Mockito.never()).onComplete();
    }

    @Test
    public void testEmptySubjectCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        subject.onComplete();

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, never()).onNext(null);
        inOrder.verify(observer, never()).onNext(any(String.class));
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Can receive timeout if subscribe never receives an onError/onComplete ... which reveals a race condition.
     */
    @Test(timeout = 10000)
    public void testSubscribeCompletionRaceCondition() {
        /*
         * With non-threadsafe code this fails most of the time on my dev laptop and is non-deterministic enough
         * to act as a unit test to the race conditions.
         *
         * With the synchronization code in place I can not get this to fail on my laptop.
         */
        for (int i = 0; i < 50; i++) {
            final AsyncSubject<String> subject = AsyncSubject.create();
            final AtomicReference<String> value1 = new AtomicReference<String>();

            subject.subscribe(new Consumer<String>() {

                @Override
                public void accept(String t1) {
                    try {
                        // simulate a slow observer
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    value1.set(t1);
                }

            });

            Thread t1 = new Thread(new Runnable() {

                @Override
                public void run() {
                    subject.onNext("value");
                    subject.onComplete();
                }
            });

            SubjectSubscriberThread t2 = new SubjectSubscriberThread(subject);
            SubjectSubscriberThread t3 = new SubjectSubscriberThread(subject);
            SubjectSubscriberThread t4 = new SubjectSubscriberThread(subject);
            SubjectSubscriberThread t5 = new SubjectSubscriberThread(subject);

            t2.start();
            t3.start();
            t1.start();
            t4.start();
            t5.start();
            try {
                t1.join();
                t2.join();
                t3.join();
                t4.join();
                t5.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertEquals("value", value1.get());
            assertEquals("value", t2.value.get());
            assertEquals("value", t3.value.get());
            assertEquals("value", t4.value.get());
            assertEquals("value", t5.value.get());
        }

    }

    private static class SubjectSubscriberThread extends Thread {

        private final AsyncSubject<String> subject;
        private final AtomicReference<String> value = new AtomicReference<String>();

        SubjectSubscriberThread(AsyncSubject<String> subject) {
            this.subject = subject;
        }

        @Override
        public void run() {
            try {
                // a timeout exception will happen if we don't get a terminal state
                String v = subject.timeout(2000, TimeUnit.MILLISECONDS).blockingSingle();
                value.set(v);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // FIXME subscriber methods are not allowed to throw
//    @Test
//    public void testOnErrorThrowsDoesntPreventDelivery() {
//        AsyncSubject<String> ps = AsyncSubject.create();
//
//        ps.subscribe();
//        TestObserver<String> ts = new TestObserver<String>();
//        ps.subscribe(ts);
//
//        try {
//            ps.onError(new RuntimeException("an exception"));
//            fail("expect OnErrorNotImplementedException");
//        } catch (OnErrorNotImplementedException e) {
//            // ignore
//        }
//        // even though the onError above throws we should still receive it on the other subscriber
//        assertEquals(1, ts.getOnErrorEvents().size());
//    }


    // FIXME subscriber methods are not allowed to throw
//    /**
//     * This one has multiple failures so should get a CompositeException
//     */
//    @Test
//    public void testOnErrorThrowsDoesntPreventDelivery2() {
//        AsyncSubject<String> ps = AsyncSubject.create();
//
//        ps.subscribe();
//        ps.subscribe();
//        TestObserver<String> ts = new TestObserver<String>();
//        ps.subscribe(ts);
//        ps.subscribe();
//        ps.subscribe();
//        ps.subscribe();
//
//        try {
//            ps.onError(new RuntimeException("an exception"));
//            fail("expect OnErrorNotImplementedException");
//        } catch (CompositeException e) {
//            // we should have 5 of them
//            assertEquals(5, e.getExceptions().size());
//        }
//        // even though the onError above throws we should still receive it on the other subscriber
//        assertEquals(1, ts.getOnErrorEvents().size());
//    }

    @Test
    public void testCurrentStateMethodsNormal() {
        AsyncSubject<Object> as = AsyncSubject.create();

        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());

        as.onNext(1);

        assertFalse(as.hasValue()); // AS no longer reports a value until it has completed
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue()); // AS no longer reports a value until it has completed
        assertNull(as.getThrowable());

        as.onComplete();
        assertTrue(as.hasValue());
        assertFalse(as.hasThrowable());
        assertTrue(as.hasComplete());
        assertEquals(1, as.getValue());
        assertNull(as.getThrowable());
    }

    @Test
    public void testCurrentStateMethodsEmpty() {
        AsyncSubject<Object> as = AsyncSubject.create();

        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());

        as.onComplete();

        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertTrue(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());
    }
    @Test
    public void testCurrentStateMethodsError() {
        AsyncSubject<Object> as = AsyncSubject.create();

        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());

        as.onError(new TestException());

        assertFalse(as.hasValue());
        assertTrue(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue());
        assertTrue(as.getThrowable() instanceof TestException);
    }


    @Test
    public void fusionLive() {
        AsyncSubject<Integer> ap = new AsyncSubject<Integer>();

        TestObserver<Integer> ts = ObserverFusion.newTest(QueueSubscription.ANY);

        ap.subscribe(ts);

        ts
        .assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueSubscription.ASYNC));

        ts.assertNoValues().assertNoErrors().assertNotComplete();

        ap.onNext(1);

        ts.assertNoValues().assertNoErrors().assertNotComplete();

        ap.onComplete();

        ts.assertResult(1);
    }

    @Test
    public void fusionOfflie() {
        AsyncSubject<Integer> ap = new AsyncSubject<Integer>();
        ap.onNext(1);
        ap.onComplete();

        TestObserver<Integer> ts = ObserverFusion.newTest(QueueSubscription.ANY);

        ap.subscribe(ts);

        ts
        .assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueSubscription.ASYNC))
        .assertResult(1);
    }

    @Test
    public void onNextNull() {
        final AsyncSubject<Object> s = AsyncSubject.create();

        s.onNext(null);

        s.test()
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void onErrorNull() {
        final AsyncSubject<Object> s = AsyncSubject.create();

        s.onError(null);

        s.test()
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void onNextNullDelayed() {
        final AsyncSubject<Object> p = AsyncSubject.create();

        TestObserver<Object> ts = p.test();

        p.onNext(null);

        ts
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void onErrorNullDelayed() {
        final AsyncSubject<Object> p = AsyncSubject.create();

        TestObserver<Object> ts = p.test();

        p.onError(null);

        ts
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void onSubscribeAfterDone() {
        AsyncSubject<Object> p = AsyncSubject.create();

        Disposable bs = Disposables.empty();
        p.onSubscribe(bs);

        assertFalse(bs.isDisposed());

        p.onComplete();

        bs = Disposables.empty();
        p.onSubscribe(bs);

        assertTrue(bs.isDisposed());

        p.test().assertResult();
    }

    @Test
    public void cancelUpfront() {
        AsyncSubject<Object> p = AsyncSubject.create();

        assertFalse(p.hasObservers());

        p.test().assertEmpty();
        p.test().assertEmpty();

        p.test(true).assertEmpty();

        assertTrue(p.hasObservers());
    }

    @Test
    public void cancelRace() {
        AsyncSubject<Object> p = AsyncSubject.create();

        for (int i = 0; i < 500; i++) {
            final TestObserver<Object> ts1 = p.test();
            final TestObserver<Object> ts2 = p.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts2.cancel();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void onErrorCancelRace() {

        for (int i = 0; i < 500; i++) {
            final AsyncSubject<Object> p = AsyncSubject.create();

            final TestObserver<Object> ts1 = p.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            final TestException ex = new TestException();

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    p.onError(ex);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            if (ts1.errorCount() != 0) {
                ts1.assertFailure(TestException.class);
            } else {
                ts1.assertEmpty();
            }
        }
    }

    @Test
    public void onNextCrossCancel() {
        AsyncSubject<Object> p = AsyncSubject.create();

        final TestObserver<Object> ts2 = new TestObserver<Object>();
        TestObserver<Object> ts1 = new TestObserver<Object>() {
            @Override
            public void onNext(Object t) {
                ts2.cancel();
                super.onNext(t);
            }
        };

        p.subscribe(ts1);
        p.subscribe(ts2);

        p.onNext(1);
        p.onComplete();

        ts1.assertResult(1);
        ts2.assertEmpty();
    }

    @Test
    public void onErrorCrossCancel() {
        AsyncSubject<Object> p = AsyncSubject.create();

        final TestObserver<Object> ts2 = new TestObserver<Object>();
        TestObserver<Object> ts1 = new TestObserver<Object>() {
            @Override
            public void onError(Throwable t) {
                ts2.cancel();
                super.onError(t);
            }
        };

        p.subscribe(ts1);
        p.subscribe(ts2);

        p.onError(new TestException());

        ts1.assertFailure(TestException.class);
        ts2.assertEmpty();
    }

    @Test
    public void onCompleteCrossCancel() {
        AsyncSubject<Object> p = AsyncSubject.create();

        final TestObserver<Object> ts2 = new TestObserver<Object>();
        TestObserver<Object> ts1 = new TestObserver<Object>() {
            @Override
            public void onComplete() {
                ts2.cancel();
                super.onComplete();
            }
        };

        p.subscribe(ts1);
        p.subscribe(ts2);

        p.onComplete();

        ts1.assertResult();
        ts2.assertEmpty();
    }
}
