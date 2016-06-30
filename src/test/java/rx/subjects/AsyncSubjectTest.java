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
package rx.subjects;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observer;
import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.exceptions.OnErrorNotImplementedException;
import rx.exceptions.TestException;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

public class AsyncSubjectTest {

    private final Throwable testException = new Throwable();

    @Test
    public void testNeverCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");

        verify(observer, never()).onNext(anyString());
        verify(observer, never()).onError(testException);
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onCompleted();

        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testNull() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext(null);
        subject.onCompleted();

        verify(observer, times(1)).onNext(null);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSubscribeAfterCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onCompleted();

        subject.subscribe(observer);

        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSubscribeAfterError() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");

        RuntimeException re = new RuntimeException("failed");
        subject.onError(re);

        subject.subscribe(observer);

        verify(observer, times(1)).onError(re);
        verify(observer, never()).onNext(any(String.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testError() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onError(testException);
        subject.onNext("four");
        subject.onError(new Throwable());
        subject.onCompleted();

        verify(observer, never()).onNext(anyString());
        verify(observer, times(1)).onError(testException);
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testUnsubscribeBeforeCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Subscription subscription = subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");

        subscription.unsubscribe();

        verify(observer, never()).onNext(anyString());
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();

        subject.onNext("three");
        subject.onCompleted();

        verify(observer, never()).onNext(anyString());
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void testEmptySubjectCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onCompleted();

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, never()).onNext(null);
        inOrder.verify(observer, never()).onNext(any(String.class));
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Can receive timeout if subscribe never receives an onError/onCompleted ... which reveals a race condition.
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

            subject.subscribe(new Action1<String>() {

                @Override
                public void call(String t1) {
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
                    subject.onCompleted();
                }
            });

            SubjectObserverThread t2 = new SubjectObserverThread(subject);
            SubjectObserverThread t3 = new SubjectObserverThread(subject);
            SubjectObserverThread t4 = new SubjectObserverThread(subject);
            SubjectObserverThread t5 = new SubjectObserverThread(subject);

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

    private static class SubjectObserverThread extends Thread {

        private final AsyncSubject<String> subject;
        private final AtomicReference<String> value = new AtomicReference<String>();

        public SubjectObserverThread(AsyncSubject<String> subject) {
            this.subject = subject;
        }

        @Override
        public void run() {
            try {
                // a timeout exception will happen if we don't get a terminal state 
                String v = subject.timeout(2000, TimeUnit.MILLISECONDS).toBlocking().single();
                value.set(v);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @Test
    public void testOnErrorThrowsDoesntPreventDelivery() {
        AsyncSubject<String> ps = AsyncSubject.create();

        ps.subscribe();
        TestSubscriber<String> ts = new TestSubscriber<String>();
        ps.subscribe(ts);

        try {
            ps.onError(new RuntimeException("an exception"));
            fail("expect OnErrorNotImplementedException");
        } catch (OnErrorNotImplementedException e) {
            // ignore
        }
        // even though the onError above throws we should still receive it on the other subscriber 
        assertEquals(1, ts.getOnErrorEvents().size());
    }
    
    /**
     * This one has multiple failures so should get a CompositeException
     */
    @Test
    public void testOnErrorThrowsDoesntPreventDelivery2() {
        AsyncSubject<String> ps = AsyncSubject.create();

        ps.subscribe();
        ps.subscribe();
        TestSubscriber<String> ts = new TestSubscriber<String>();
        ps.subscribe(ts);
        ps.subscribe();
        ps.subscribe();
        ps.subscribe();

        try {
            ps.onError(new RuntimeException("an exception"));
            fail("expect OnErrorNotImplementedException");
        } catch (CompositeException e) {
            // we should have 5 of them
            assertEquals(5, e.getExceptions().size());
        }
        // even though the onError above throws we should still receive it on the other subscriber 
        assertEquals(1, ts.getOnErrorEvents().size());
    }

    @Test
    public void testCurrentStateMethodsNormal() {
        AsyncSubject<Object> as = AsyncSubject.create();
        
        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasCompleted());
        assertNull(as.getValue());
        assertNull(as.getThrowable());
        
        as.onNext(1);
        
        assertTrue(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasCompleted());
        assertEquals(1, as.getValue());
        assertNull(as.getThrowable());
        
        as.onCompleted();
        assertTrue(as.hasValue());
        assertFalse(as.hasThrowable());
        assertTrue(as.hasCompleted());
        assertEquals(1, as.getValue());
        assertNull(as.getThrowable());
    }
    
    @Test
    public void testCurrentStateMethodsEmpty() {
        AsyncSubject<Object> as = AsyncSubject.create();
        
        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasCompleted());
        assertNull(as.getValue());
        assertNull(as.getThrowable());
        
        as.onCompleted();
        
        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertTrue(as.hasCompleted());
        assertNull(as.getValue());
        assertNull(as.getThrowable());
    }
    @Test
    public void testCurrentStateMethodsError() {
        AsyncSubject<Object> as = AsyncSubject.create();
        
        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasCompleted());
        assertNull(as.getValue());
        assertNull(as.getThrowable());
        
        as.onError(new TestException());
        
        assertFalse(as.hasValue());
        assertTrue(as.hasThrowable());
        assertFalse(as.hasCompleted());
        assertNull(as.getValue());
        assertTrue(as.getThrowable() instanceof TestException);
    }
    
    @Test
    public void testAsyncSubjectValueRelay() {
        AsyncSubject<Integer> async = AsyncSubject.create();
        async.onNext(1);
        async.onCompleted();
        
        assertFalse(async.hasObservers());
        assertTrue(async.hasCompleted());
        assertFalse(async.hasThrowable());
        assertNull(async.getThrowable());
        assertEquals((Integer)1, async.getValue());
        assertTrue(async.hasValue());
    }
    @Test
    public void testAsyncSubjectValueEmpty() {
        AsyncSubject<Integer> async = AsyncSubject.create();
        async.onCompleted();
        
        assertFalse(async.hasObservers());
        assertTrue(async.hasCompleted());
        assertFalse(async.hasThrowable());
        assertNull(async.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
    }
    @Test
    public void testAsyncSubjectValueError() {
        AsyncSubject<Integer> async = AsyncSubject.create();
        TestException te = new TestException();
        async.onError(te);
        
        assertFalse(async.hasObservers());
        assertFalse(async.hasCompleted());
        assertTrue(async.hasThrowable());
        assertSame(te, async.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
    }
    
    @Test
    public void backpressureOnline() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        AsyncSubject<Integer> subject = AsyncSubject.create();
        
        subject.subscribe(ts);
        
        subject.onNext(1);
        subject.onCompleted();
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        
        ts.assertValue(1);
        ts.assertCompleted();
        ts.assertNoErrors();
    }
    
    @Test
    public void backpressureOffline() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        AsyncSubject<Integer> subject = AsyncSubject.create();
        subject.onNext(1);
        subject.onCompleted();
        
        subject.subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        
        ts.assertValue(1);
        ts.assertCompleted();
        ts.assertNoErrors();
    }
}
