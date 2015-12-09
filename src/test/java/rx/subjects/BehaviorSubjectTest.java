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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;
import org.mockito.InOrder;

import rx.*;
import rx.exceptions.CompositeException;
import rx.exceptions.OnErrorNotImplementedException;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class BehaviorSubjectTest {

    private final Throwable testException = new Throwable();

    @Test
    public void testThatObserverReceivesDefaultValueAndSubsequentEvents() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testThatObserverReceivesLatestAndThenSubsequentEvents() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        subject.onNext("one");

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext("two");
        subject.onNext("three");

        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testSubscribeThenOnComplete() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onCompleted();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testSubscribeToCompletedOnlyEmitsOnComplete() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");
        subject.onNext("one");
        subject.onCompleted();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        verify(observer, times(1)).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testSubscribeToErrorOnlyEmitsOnError() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");
        subject.onNext("one");
        RuntimeException re = new RuntimeException("test error");
        subject.onError(re);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        verify(observer, times(1)).onError(re);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testCompletedStopsEmittingData() {
        BehaviorSubject<Integer> channel = BehaviorSubject.create(2013);
        @SuppressWarnings("unchecked")
        Observer<Object> observerA = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerB = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerC = mock(Observer.class);

        Subscription a = channel.subscribe(observerA);
        channel.subscribe(observerB);

        InOrder inOrderA = inOrder(observerA);
        InOrder inOrderB = inOrder(observerB);
        InOrder inOrderC = inOrder(observerC);

        inOrderA.verify(observerA).onNext(2013);
        inOrderB.verify(observerB).onNext(2013);

        channel.onNext(42);

        inOrderA.verify(observerA).onNext(42);
        inOrderB.verify(observerB).onNext(42);

        a.unsubscribe();
        inOrderA.verifyNoMoreInteractions();

        channel.onNext(4711);

        inOrderB.verify(observerB).onNext(4711);

        channel.onCompleted();

        inOrderB.verify(observerB).onCompleted();

        channel.subscribe(observerC);

        inOrderC.verify(observerC).onCompleted();

        channel.onNext(13);

        inOrderB.verifyNoMoreInteractions();
        inOrderC.verifyNoMoreInteractions();
    }

    @Test
    public void testCompletedAfterErrorIsNotSent() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onCompleted();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onError(testException);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testCompletedAfterErrorIsNotSent2() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onCompleted();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onError(testException);
        verifyNoMoreInteractions(observer);

        @SuppressWarnings("unchecked")
        Observer<Object> o2 = mock(Observer.class);
        subject.subscribe(o2);
        verify(o2, times(1)).onError(testException);
        verifyNoMoreInteractions(o2);
    }

    @Test
    public void testCompletedAfterErrorIsNotSent3() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onCompleted();
        subject.onNext("two");
        subject.onCompleted();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onCompleted();
        verifyNoMoreInteractions(observer);

        @SuppressWarnings("unchecked")
        Observer<Object> o2 = mock(Observer.class);
        subject.subscribe(o2);
        verify(o2, times(1)).onCompleted();
        verifyNoMoreInteractions(o2);
    }
    @Test(timeout = 1000)
    public void testUnsubscriptionCase() {
        BehaviorSubject<String> src = BehaviorSubject.create((String)null);
        
        for (int i = 0; i < 10; i++) {
            @SuppressWarnings("unchecked")
            final Observer<Object> o = mock(Observer.class);
            InOrder inOrder = inOrder(o);
            String v = "" + i;
            src.onNext(v);
            System.out.printf("Turn: %d%n", i);
            src.first()
                .flatMap(new Func1<String, Observable<String>>() {

                    @Override
                    public Observable<String> call(String t1) {
                        return Observable.just(t1 + ", " + t1);
                    }
                })
                .subscribe(o);

            inOrder.verify(o).onNext(v + ", " + v);
            inOrder.verify(o).onCompleted();
            verify(o, never()).onError(any(Throwable.class));
        }
    }
    @Test
    public void testStartEmpty() {
        BehaviorSubject<Integer> source = BehaviorSubject.create();
        @SuppressWarnings("unchecked")
        final Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        source.subscribe(o);
        
        inOrder.verify(o, never()).onNext(any());
        inOrder.verify(o, never()).onCompleted();
        
        source.onNext(1);
        
        source.onCompleted();
        
        source.onNext(2);
        
        verify(o, never()).onError(any(Throwable.class));

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
        
        
    }
    @Test
    public void testStartEmptyThenAddOne() {
        BehaviorSubject<Integer> source = BehaviorSubject.create();
        @SuppressWarnings("unchecked")
        final Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.onNext(1);

        source.subscribe(o);

        inOrder.verify(o).onNext(1);

        source.onCompleted();

        source.onNext(2);

        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
        
        verify(o, never()).onError(any(Throwable.class));
        
    }
    @Test
    public void testStartEmptyCompleteWithOne() {
        BehaviorSubject<Integer> source = BehaviorSubject.create();
        @SuppressWarnings("unchecked")
        final Observer<Object> o = mock(Observer.class);

        source.onNext(1);
        source.onCompleted();

        source.onNext(2);

        source.subscribe(o);

        verify(o).onCompleted();
        verifyNoMoreInteractions(o);
    }
    
    @Test
    public void testTakeOneSubscriber() {
        BehaviorSubject<Integer> source = BehaviorSubject.create(1);
        @SuppressWarnings("unchecked")
        final Observer<Object> o = mock(Observer.class);
        
        source.take(1).subscribe(o);
        
        verify(o).onNext(1);
        verify(o).onCompleted();
        verifyNoMoreInteractions(o);

        assertEquals(0, source.subscriberCount());
        assertFalse(source.hasObservers());
    }
    
    @Test
    public void testOnErrorThrowsDoesntPreventDelivery() {
        BehaviorSubject<String> ps = BehaviorSubject.create();

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
        BehaviorSubject<String> ps = BehaviorSubject.create();

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
    public void testEmissionSubscriptionRace() throws Exception {
        Scheduler s = Schedulers.io();
        Scheduler.Worker worker = Schedulers.io().createWorker();
        try {
            for (int i = 0; i < 50000; i++) {
                if (i % 1000 == 0) {
                    System.out.println(i);
                }
                final BehaviorSubject<Object> rs = BehaviorSubject.create();
                
                final CountDownLatch finish = new CountDownLatch(1); 
                final CountDownLatch start = new CountDownLatch(1); 
                
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        try {
                            start.await();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        rs.onNext(1);
                    }
                });
                
                final AtomicReference<Object> o = new AtomicReference<Object>();
                
                rs.subscribeOn(s).observeOn(Schedulers.io())
                .subscribe(new Observer<Object>() {
    
                    @Override
                    public void onCompleted() {
                        o.set(-1);
                        finish.countDown();
                    }
    
                    @Override
                    public void onError(Throwable e) {
                        o.set(e);
                        finish.countDown();
                    }
    
                    @Override
                    public void onNext(Object t) {
                        o.set(t);
                        finish.countDown();
                    }
                    
                });
                start.countDown();
                
                if (!finish.await(5, TimeUnit.SECONDS)) {
                    System.out.println(o.get());
                    System.out.println(rs.hasObservers());
                    rs.onCompleted();
                    Assert.fail("Timeout @ " + i);
                    break;
                } else {
                    Assert.assertEquals(1, o.get());
                    worker.schedule(new Action0() {
                        @Override
                        public void call() {
                            rs.onCompleted();
                        }
                    });
                }
            }
        } finally {
            worker.unsubscribe();
        }
    }
    
    @Test
    public void testCurrentStateMethodsNormalEmptyStart() {
        BehaviorSubject<Object> as = BehaviorSubject.create();
        
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
        
        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertTrue(as.hasCompleted());
        assertNull(as.getValue());
        assertNull(as.getThrowable());
    }
    
    @Test
    public void testCurrentStateMethodsNormalSomeStart() {
        BehaviorSubject<Object> as = BehaviorSubject.create((Object)1);
        
        assertTrue(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasCompleted());
        assertEquals(1, as.getValue());
        assertNull(as.getThrowable());
        
        as.onNext(2);
        
        assertTrue(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasCompleted());
        assertEquals(2, as.getValue());
        assertNull(as.getThrowable());
        
        as.onCompleted();
        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertTrue(as.hasCompleted());
        assertNull(as.getValue());
        assertNull(as.getThrowable());
    }
    
    @Test
    public void testCurrentStateMethodsEmpty() {
        BehaviorSubject<Object> as = BehaviorSubject.create();
        
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
        BehaviorSubject<Object> as = BehaviorSubject.create();
        
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
    public void testBehaviorSubjectValueRelay() {
        BehaviorSubject<Integer> async = BehaviorSubject.create();
        async.onNext(1);
        async.onCompleted();
        
        assertFalse(async.hasObservers());
        assertTrue(async.hasCompleted());
        assertFalse(async.hasThrowable());
        assertNull(async.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectValueRelayIncomplete() {
        BehaviorSubject<Integer> async = BehaviorSubject.create();
        async.onNext(1);
        
        assertFalse(async.hasObservers());
        assertFalse(async.hasCompleted());
        assertFalse(async.hasThrowable());
        assertNull(async.getThrowable());
        assertEquals((Integer)1, async.getValue());
        assertTrue(async.hasValue());
        assertArrayEquals(new Object[] { 1 }, async.getValues());
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectIncompleteEmpty() {
        BehaviorSubject<Integer> async = BehaviorSubject.create();
        
        assertFalse(async.hasObservers());
        assertFalse(async.hasCompleted());
        assertFalse(async.hasThrowable());
        assertNull(async.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectEmpty() {
        BehaviorSubject<Integer> async = BehaviorSubject.create();
        async.onCompleted();
        
        assertFalse(async.hasObservers());
        assertTrue(async.hasCompleted());
        assertFalse(async.hasThrowable());
        assertNull(async.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectError() {
        BehaviorSubject<Integer> async = BehaviorSubject.create();
        TestException te = new TestException();
        async.onError(te);
        
        assertFalse(async.hasObservers());
        assertFalse(async.hasCompleted());
        assertTrue(async.hasThrowable());
        assertSame(te, async.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
}
