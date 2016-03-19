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

package io.reactivex.subjects;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;
import org.mockito.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.Function;
import io.reactivex.observers.*;
import io.reactivex.schedulers.Schedulers;

public class NbpBehaviorSubjectTest {

    private final Throwable testException = new Throwable();

    @Test
    public void testThatSubscriberReceivesDefaultValueAndSubsequentEvents() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");

        Observer<String> observer = TestHelper.mockNbpSubscriber();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(testException);
        verify(observer, Mockito.never()).onComplete();
    }

    @Test
    public void testThatSubscriberReceivesLatestAndThenSubsequentEvents() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");

        subject.onNext("one");

        Observer<String> observer = TestHelper.mockNbpSubscriber();
        subject.subscribe(observer);

        subject.onNext("two");
        subject.onNext("three");

        verify(observer, Mockito.never()).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(testException);
        verify(observer, Mockito.never()).onComplete();
    }

    @Test
    public void testSubscribeThenOnComplete() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");

        Observer<String> observer = TestHelper.mockNbpSubscriber();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onComplete();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSubscribeToCompletedOnlyEmitsOnComplete() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");
        subject.onNext("one");
        subject.onComplete();

        Observer<String> observer = TestHelper.mockNbpSubscriber();
        subject.subscribe(observer);

        verify(observer, never()).onNext("default");
        verify(observer, never()).onNext("one");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSubscribeToErrorOnlyEmitsOnError() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");
        subject.onNext("one");
        RuntimeException re = new RuntimeException("test error");
        subject.onError(re);

        Observer<String> observer = TestHelper.mockNbpSubscriber();
        subject.subscribe(observer);

        verify(observer, never()).onNext("default");
        verify(observer, never()).onNext("one");
        verify(observer, times(1)).onError(re);
        verify(observer, never()).onComplete();
    }

    @Test
    public void testCompletedStopsEmittingData() {
        BehaviorSubject<Integer> channel = BehaviorSubject.createDefault(2013);
        Observer<Object> observerA = TestHelper.mockNbpSubscriber();
        Observer<Object> observerB = TestHelper.mockNbpSubscriber();
        Observer<Object> observerC = TestHelper.mockNbpSubscriber();

        TestObserver<Object> ts = new TestObserver<Object>(observerA);
        
        channel.subscribe(ts);
        channel.subscribe(observerB);

        InOrder inOrderA = inOrder(observerA);
        InOrder inOrderB = inOrder(observerB);
        InOrder inOrderC = inOrder(observerC);

        inOrderA.verify(observerA).onNext(2013);
        inOrderB.verify(observerB).onNext(2013);

        channel.onNext(42);

        inOrderA.verify(observerA).onNext(42);
        inOrderB.verify(observerB).onNext(42);

        ts.dispose();
        inOrderA.verifyNoMoreInteractions();

        channel.onNext(4711);

        inOrderB.verify(observerB).onNext(4711);

        channel.onComplete();

        inOrderB.verify(observerB).onComplete();

        channel.subscribe(observerC);

        inOrderC.verify(observerC).onComplete();

        channel.onNext(13);

        inOrderB.verifyNoMoreInteractions();
        inOrderC.verifyNoMoreInteractions();
    }

    @Test
    public void testCompletedAfterErrorIsNotSent() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");

        Observer<String> observer = TestHelper.mockNbpSubscriber();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onComplete();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onError(testException);
        verify(observer, never()).onNext("two");
        verify(observer, never()).onComplete();
    }

    @Test
    public void testCompletedAfterErrorIsNotSent2() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");

        Observer<String> observer = TestHelper.mockNbpSubscriber();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onComplete();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onError(testException);
        verify(observer, never()).onNext("two");
        verify(observer, never()).onComplete();

        Observer<Object> o2 = TestHelper.mockNbpSubscriber();
        subject.subscribe(o2);
        verify(o2, times(1)).onError(testException);
        verify(o2, never()).onNext(any());
        verify(o2, never()).onComplete();
    }

    @Test
    public void testCompletedAfterErrorIsNotSent3() {
        BehaviorSubject<String> subject = BehaviorSubject.createDefault("default");

        Observer<String> observer = TestHelper.mockNbpSubscriber();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onComplete();
        subject.onNext("two");
        subject.onComplete();

        verify(observer, times(1)).onNext("default");
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext("two");

        Observer<Object> o2 = TestHelper.mockNbpSubscriber();
        subject.subscribe(o2);
        verify(o2, times(1)).onComplete();
        verify(o2, never()).onNext(any());
        verify(observer, never()).onError(any(Throwable.class));
    }
    
    @Test(timeout = 1000)
    public void testUnsubscriptionCase() {
        BehaviorSubject<String> src = BehaviorSubject.createDefault("null"); // FIXME was plain null which is not allowed
        
        for (int i = 0; i < 10; i++) {
            final Observer<Object> o = TestHelper.mockNbpSubscriber();
            InOrder inOrder = inOrder(o);
            String v = "" + i;
            src.onNext(v);
            System.out.printf("Turn: %d%n", i);
            src.first()
                .flatMap(new Function<String, Observable<String>>() {

                    @Override
                    public Observable<String> apply(String t1) {
                        return Observable.just(t1 + ", " + t1);
                    }
                })
                .subscribe(new DefaultObserver<String>() {
                    @Override
                    public void onNext(String t) {
                        o.onNext(t);
                    }

                    @Override
                    public void onError(Throwable e) {
                        o.onError(e);
                    }

                    @Override
                    public void onComplete() {
                        o.onComplete();
                    }
                });
            inOrder.verify(o).onNext(v + ", " + v);
            inOrder.verify(o).onComplete();
            verify(o, never()).onError(any(Throwable.class));
        }
    }
    @Test
    public void testStartEmpty() {
        BehaviorSubject<Integer> source = BehaviorSubject.create();
        final Observer<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);
        
        source.subscribe(o);
        
        inOrder.verify(o, never()).onNext(any());
        inOrder.verify(o, never()).onComplete();
        
        source.onNext(1);
        
        source.onComplete();
        
        source.onNext(2);
        
        verify(o, never()).onError(any(Throwable.class));

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();
        
        
    }
    @Test
    public void testStartEmptyThenAddOne() {
        BehaviorSubject<Integer> source = BehaviorSubject.create();
        final Observer<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        source.onNext(1);

        source.subscribe(o);

        inOrder.verify(o).onNext(1);

        source.onComplete();

        source.onNext(2);

        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();
        
        verify(o, never()).onError(any(Throwable.class));
        
    }
    @Test
    public void testStartEmptyCompleteWithOne() {
        BehaviorSubject<Integer> source = BehaviorSubject.create();
        final Observer<Object> o = TestHelper.mockNbpSubscriber();

        source.onNext(1);
        source.onComplete();

        source.onNext(2);

        source.subscribe(o);

        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onNext(any());
    }
    
    @Test
    public void testTakeOneSubscriber() {
        BehaviorSubject<Integer> source = BehaviorSubject.createDefault(1);
        final Observer<Object> o = TestHelper.mockNbpSubscriber();
        
        source.take(1).subscribe(o);
        
        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
        
        assertEquals(0, source.subscriberCount());
        assertFalse(source.hasSubscribers());
    }
    
    // FIXME RS subscribers are not allowed to throw
//    @Test
//    public void testOnErrorThrowsDoesntPreventDelivery() {
//        NbpBehaviorSubject<String> ps = NbpBehaviorSubject.create();
//
//        ps.subscribe();
//        TestNbpSubscriber<String> ts = new TestNbpSubscriber<T>();
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
    
    // FIXME RS subscribers are not allowed to throw
//    /**
//     * This one has multiple failures so should get a CompositeException
//     */
//    @Test
//    public void testOnErrorThrowsDoesntPreventDelivery2() {
//        NbpBehaviorSubject<String> ps = NbpBehaviorSubject.create();
//
//        ps.subscribe();
//        ps.subscribe();
//        TestNbpSubscriber<String> ts = new TestNbpSubscriber<String>();
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
                
                worker.schedule(new Runnable() {
                    @Override
                    public void run() {
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
                .subscribe(new DefaultObserver<Object>() {
    
                    @Override
                    public void onComplete() {
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
                    System.out.println(rs.hasSubscribers());
                    rs.onComplete();
                    Assert.fail("Timeout @ " + i);
                    break;
                } else {
                    Assert.assertEquals(1, o.get());
                    worker.schedule(new Runnable() {
                        @Override
                        public void run() {
                            rs.onComplete();
                        }
                    });
                }
            }
        } finally {
            worker.dispose();
        }
    }
    
    @Test
    public void testCurrentStateMethodsNormalEmptyStart() {
        BehaviorSubject<Object> as = BehaviorSubject.create();
        
        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());
        
        as.onNext(1);
        
        assertTrue(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertEquals(1, as.getValue());
        assertNull(as.getThrowable());
        
        as.onComplete();
        
        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertTrue(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());
    }
    
    @Test
    public void testCurrentStateMethodsNormalSomeStart() {
        BehaviorSubject<Object> as = BehaviorSubject.createDefault((Object)1);
        
        assertTrue(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertEquals(1, as.getValue());
        assertNull(as.getThrowable());
        
        as.onNext(2);
        
        assertTrue(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertEquals(2, as.getValue());
        assertNull(as.getThrowable());
        
        as.onComplete();
        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertTrue(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());
    }
    
    @Test
    public void testCurrentStateMethodsEmpty() {
        BehaviorSubject<Object> as = BehaviorSubject.create();
        
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
        BehaviorSubject<Object> as = BehaviorSubject.create();
        
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
}