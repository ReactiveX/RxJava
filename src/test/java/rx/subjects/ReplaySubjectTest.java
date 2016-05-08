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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.exceptions.OnErrorNotImplementedException;
import rx.exceptions.TestException;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

public class ReplaySubjectTest {

    private final Throwable testException = new Throwable();

    @SuppressWarnings("unchecked")
    @Test
    public void testCompleted() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> o1 = mock(Observer.class);
        subject.subscribe(o1);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onCompleted();

        subject.onNext("four");
        subject.onCompleted();
        subject.onError(new Throwable());

        assertCompletedObserver(o1);

        // assert that subscribing a 2nd time gets the same data
        Observer<String> o2 = mock(Observer.class);
        subject.subscribe(o2);
        assertCompletedObserver(o2);
    }

    @Test
    public void testCompletedStopsEmittingData() {
        ReplaySubject<Integer> channel = ReplaySubject.create();
        @SuppressWarnings("unchecked")
        Observer<Object> observerA = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerB = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerC = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerD = mock(Observer.class);

        Subscription a = channel.subscribe(observerA);
        channel.subscribe(observerB);

        InOrder inOrderA = inOrder(observerA);
        InOrder inOrderB = inOrder(observerB);
        InOrder inOrderC = inOrder(observerC);
        InOrder inOrderD = inOrder(observerD);

        channel.onNext(42);

        // both A and B should have received 42 from before subscription
        inOrderA.verify(observerA).onNext(42);
        inOrderB.verify(observerB).onNext(42);

        a.unsubscribe();

        // a should receive no more
        inOrderA.verifyNoMoreInteractions();

        channel.onNext(4711);

        // only be should receive 4711 at this point
        inOrderB.verify(observerB).onNext(4711);

        channel.onCompleted();

        // B is subscribed so should receive onCompleted
        inOrderB.verify(observerB).onCompleted();

        channel.subscribe(observerC);

        // when C subscribes it should receive 42, 4711, onCompleted
        inOrderC.verify(observerC).onNext(42);
        inOrderC.verify(observerC).onNext(4711);
        inOrderC.verify(observerC).onCompleted();

        // if further events are propagated they should be ignored
        channel.onNext(13);
        channel.onNext(14);
        channel.onNext(15);
        channel.onError(new RuntimeException());

        // a new subscription should only receive what was emitted prior to terminal state onCompleted
        channel.subscribe(observerD);

        inOrderD.verify(observerD).onNext(42);
        inOrderD.verify(observerD).onNext(4711);
        inOrderD.verify(observerD).onCompleted();

        verifyNoMoreInteractions(observerA);
        verifyNoMoreInteractions(observerB);
        verifyNoMoreInteractions(observerC);
        verifyNoMoreInteractions(observerD);
    }

    @Test
    public void testCompletedAfterError() {
        ReplaySubject<String> subject = ReplaySubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onCompleted();
        subject.onError(new RuntimeException());

        subject.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onError(testException);
        verifyNoMoreInteractions(observer);
    }

    private void assertCompletedObserver(Observer<String> observer) {
        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testError() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onError(testException);

        subject.onNext("four");
        subject.onError(new Throwable());
        subject.onCompleted();

        assertErrorObserver(observer);

        observer = mock(Observer.class);
        subject.subscribe(observer);
        assertErrorObserver(observer);
    }

    private void assertErrorObserver(Observer<String> observer) {
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, times(1)).onError(testException);
        verify(observer, never()).onCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSubscribeMidSequence() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");

        assertObservedUntilTwo(observer);

        Observer<String> anotherObserver = mock(Observer.class);
        subject.subscribe(anotherObserver);
        assertObservedUntilTwo(anotherObserver);

        subject.onNext("three");
        subject.onCompleted();

        assertCompletedObserver(observer);
        assertCompletedObserver(anotherObserver);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnsubscribeFirstObserver() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> observer = mock(Observer.class);
        Subscription subscription = subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");

        subscription.unsubscribe();
        assertObservedUntilTwo(observer);

        Observer<String> anotherObserver = mock(Observer.class);
        subject.subscribe(anotherObserver);
        assertObservedUntilTwo(anotherObserver);

        subject.onNext("three");
        subject.onCompleted();

        assertObservedUntilTwo(observer);
        assertCompletedObserver(anotherObserver);
    }

    private void assertObservedUntilTwo(Observer<String> observer) {
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
    }

    @Test(timeout = 2000)
    public void testNewSubscriberDoesntBlockExisting() throws InterruptedException {

        final AtomicReference<String> lastValueForObserver1 = new AtomicReference<String>();
        Subscriber<String> observer1 = new Subscriber<String>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String v) {
                System.out.println("observer1: " + v);
                lastValueForObserver1.set(v);
            }

        };

        final AtomicReference<String> lastValueForObserver2 = new AtomicReference<String>();
        final CountDownLatch oneReceived = new CountDownLatch(1);
        final CountDownLatch makeSlow = new CountDownLatch(1);
        final CountDownLatch completed = new CountDownLatch(1);
        Subscriber<String> observer2 = new Subscriber<String>() {

            @Override
            public void onCompleted() {
                completed.countDown();
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String v) {
                System.out.println("observer2: " + v);
                if (v.equals("one")) {
                    oneReceived.countDown();
                } else {
                    try {
                        makeSlow.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    lastValueForObserver2.set(v);
                }
            }

        };

        ReplaySubject<String> subject = ReplaySubject.create();
        subject.subscribe(observer1);
        subject.onNext("one");
        assertEquals("one", lastValueForObserver1.get());
        subject.onNext("two");
        assertEquals("two", lastValueForObserver1.get());

        // use subscribeOn to make this async otherwise we deadlock as we are using CountDownLatches
        subject.subscribeOn(Schedulers.newThread()).subscribe(observer2);

        System.out.println("before waiting for one");

        // wait until observer2 starts having replay occur
        oneReceived.await();

        System.out.println("after waiting for one");

        subject.onNext("three");
        
        System.out.println("sent three");
        
        // if subscription blocked existing subscribers then 'makeSlow' would cause this to not be there yet 
        assertEquals("three", lastValueForObserver1.get());
        
        System.out.println("about to send onCompleted");
        
        subject.onCompleted();

        System.out.println("completed subject");
        
        // release 
        makeSlow.countDown();
        
        System.out.println("makeSlow released");
        
        completed.await();
        // all of them should be emitted with the last being "three"
        assertEquals("three", lastValueForObserver2.get());

    }
    @Test
    public void testSubscriptionLeak() {
        ReplaySubject<Object> replaySubject = ReplaySubject.create();
        
        Subscription s = replaySubject.subscribe();

        assertEquals(1, replaySubject.subscriberCount());

        s.unsubscribe();
        
        assertEquals(0, replaySubject.subscriberCount());
    }
    @Test(timeout = 1000)
    public void testUnsubscriptionCase() {
        ReplaySubject<String> src = ReplaySubject.create();
        
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
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String t) {
                        System.out.println(t);
                        o.onNext(t);
                    }

                    @Override
                    public void onError(Throwable e) {
                        o.onError(e);
                    }

                    @Override
                    public void onCompleted() {
                        o.onCompleted();
                    }
                });
            inOrder.verify(o).onNext("0, 0");
            inOrder.verify(o).onCompleted();
            verify(o, never()).onError(any(Throwable.class));
        }
    }
    @Test
    public void testTerminateOnce() {
        ReplaySubject<Integer> source = ReplaySubject.create();
        source.onNext(1);
        source.onNext(2);
        source.onCompleted();
        
        @SuppressWarnings("unchecked")
        final Observer<Integer> o = mock(Observer.class);
        
        source.unsafeSubscribe(new Subscriber<Integer>() {

            @Override
            public void onNext(Integer t) {
                o.onNext(t);
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onCompleted() {
                o.onCompleted();
            }
        });
        
        verify(o).onNext(1);
        verify(o).onNext(2);
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void testReplay1AfterTermination() {
        ReplaySubject<Integer> source = ReplaySubject.createWithSize(1);
        
        source.onNext(1);
        source.onNext(2);
        source.onCompleted();
        
        for (int i = 0; i < 1; i++) {
            @SuppressWarnings("unchecked")
            Observer<Integer> o = mock(Observer.class);

            source.subscribe(o);

            verify(o, never()).onNext(1);
            verify(o).onNext(2);
            verify(o).onCompleted();
            verify(o, never()).onError(any(Throwable.class));
        }
    }
    @Test
    public void testReplay1Directly() {
        ReplaySubject<Integer> source = ReplaySubject.createWithSize(1);

        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);

        source.onNext(1);
        source.onNext(2);

        source.subscribe(o);

        source.onNext(3);
        source.onCompleted();

        verify(o, never()).onNext(1);
        verify(o).onNext(2);
        verify(o).onNext(3);
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testReplayTimestampedAfterTermination() {
        TestScheduler scheduler = new TestScheduler();
        ReplaySubject<Integer> source = ReplaySubject.createWithTime(1, TimeUnit.SECONDS, scheduler);
        
        source.onNext(1);
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        source.onNext(2);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        source.onNext(3);
        source.onCompleted();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);

        source.subscribe(o);
        
        verify(o, never()).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onNext(3); // late subscribers no longer replay stale data
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testReplayTimestampedDirectly() {
        TestScheduler scheduler = new TestScheduler();
        ReplaySubject<Integer> source = ReplaySubject.createWithTime(1, TimeUnit.SECONDS, scheduler);

        source.onNext(1);
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);

        source.subscribe(o);

        source.onNext(2);
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        source.onNext(3);
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        source.onCompleted();
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onNext(1);
        verify(o).onNext(2);
        verify(o).onNext(3);
        verify(o).onCompleted();
    }
    
    @Test
    public void testOnErrorThrowsDoesntPreventDelivery() {
        ReplaySubject<String> ps = ReplaySubject.create();

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
        ReplaySubject<String> ps = ReplaySubject.create();

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
        ReplaySubject<Object> as = ReplaySubject.create();
        
        assertFalse(as.hasThrowable());
        assertFalse(as.hasCompleted());
        assertNull(as.getThrowable());
        
        as.onNext(1);
        
        assertFalse(as.hasThrowable());
        assertFalse(as.hasCompleted());
        assertNull(as.getThrowable());
        
        as.onCompleted();
        
        assertFalse(as.hasThrowable());
        assertTrue(as.hasCompleted());
        assertNull(as.getThrowable());
    }
    
    @Test
    public void testCurrentStateMethodsEmpty() {
        ReplaySubject<Object> as = ReplaySubject.create();
        
        assertFalse(as.hasThrowable());
        assertFalse(as.hasCompleted());
        assertNull(as.getThrowable());
        
        as.onCompleted();
        
        assertFalse(as.hasThrowable());
        assertTrue(as.hasCompleted());
        assertNull(as.getThrowable());
    }
    @Test
    public void testCurrentStateMethodsError() {
        ReplaySubject<Object> as = ReplaySubject.create();
        
        assertFalse(as.hasThrowable());
        assertFalse(as.hasCompleted());
        assertNull(as.getThrowable());
        
        as.onError(new TestException());
        
        assertTrue(as.hasThrowable());
        assertFalse(as.hasCompleted());
        assertTrue(as.getThrowable() instanceof TestException);
    }
    @Test
    public void testSizeAndHasAnyValueUnbounded() {
        ReplaySubject<Object> rs = ReplaySubject.create();
        
        assertEquals(0, rs.size());
        assertFalse(rs.hasAnyValue());
        
        rs.onNext(1);
        
        assertEquals(1, rs.size());
        assertTrue(rs.hasAnyValue());
        
        rs.onNext(1);

        assertEquals(2, rs.size());
        assertTrue(rs.hasAnyValue());
        
        rs.onCompleted();

        assertEquals(2, rs.size());
        assertTrue(rs.hasAnyValue());
    }
    @Test
    public void testSizeAndHasAnyValueEffectivelyUnbounded() {
        ReplaySubject<Object> rs = ReplaySubject.createUnbounded();
        
        assertEquals(0, rs.size());
        assertFalse(rs.hasAnyValue());
        
        rs.onNext(1);
        
        assertEquals(1, rs.size());
        assertTrue(rs.hasAnyValue());
        
        rs.onNext(1);

        assertEquals(2, rs.size());
        assertTrue(rs.hasAnyValue());
        
        rs.onCompleted();

        assertEquals(2, rs.size());
        assertTrue(rs.hasAnyValue());
    }
    
    @Test
    public void testSizeAndHasAnyValueUnboundedError() {
        ReplaySubject<Object> rs = ReplaySubject.create();
        
        assertEquals(0, rs.size());
        assertFalse(rs.hasAnyValue());
        
        rs.onNext(1);
        
        assertEquals(1, rs.size());
        assertTrue(rs.hasAnyValue());
        
        rs.onNext(1);

        assertEquals(2, rs.size());
        assertTrue(rs.hasAnyValue());
        
        rs.onError(new TestException());

        assertEquals(2, rs.size());
        assertTrue(rs.hasAnyValue());
    }
    @Test
    public void testSizeAndHasAnyValueEffectivelyUnboundedError() {
        ReplaySubject<Object> rs = ReplaySubject.createUnbounded();
        
        assertEquals(0, rs.size());
        assertFalse(rs.hasAnyValue());
        
        rs.onNext(1);
        
        assertEquals(1, rs.size());
        assertTrue(rs.hasAnyValue());
        
        rs.onNext(1);

        assertEquals(2, rs.size());
        assertTrue(rs.hasAnyValue());
        
        rs.onError(new TestException());

        assertEquals(2, rs.size());
        assertTrue(rs.hasAnyValue());
    }
    
    @Test
    public void testSizeAndHasAnyValueUnboundedEmptyError() {
        ReplaySubject<Object> rs = ReplaySubject.create();
        
        rs.onError(new TestException());

        assertEquals(0, rs.size());
        assertFalse(rs.hasAnyValue());
    }
    @Test
    public void testSizeAndHasAnyValueEffectivelyUnboundedEmptyError() {
        ReplaySubject<Object> rs = ReplaySubject.createUnbounded();
        
        rs.onError(new TestException());

        assertEquals(0, rs.size());
        assertFalse(rs.hasAnyValue());
    }
    
    @Test
    public void testSizeAndHasAnyValueUnboundedEmptyCompleted() {
        ReplaySubject<Object> rs = ReplaySubject.create();
        
        rs.onCompleted();

        assertEquals(0, rs.size());
        assertFalse(rs.hasAnyValue());
    }
    @Test
    public void testSizeAndHasAnyValueEffectivelyUnboundedEmptyCompleted() {
        ReplaySubject<Object> rs = ReplaySubject.createUnbounded();
        
        rs.onCompleted();

        assertEquals(0, rs.size());
        assertFalse(rs.hasAnyValue());
    }
    
    @Test
    public void testSizeAndHasAnyValueSizeBounded() {
        ReplaySubject<Object> rs = ReplaySubject.createWithSize(1);
        
        assertEquals(0, rs.size());
        assertFalse(rs.hasAnyValue());
        
        for (int i = 0; i < 1000; i++) {
            rs.onNext(i);

            assertEquals(1, rs.size());
            assertTrue(rs.hasAnyValue());
        }
        
        rs.onCompleted();

        assertEquals(1, rs.size());
        assertTrue(rs.hasAnyValue());
    }
    
    @Test
    public void testSizeAndHasAnyValueTimeBounded() {
        TestScheduler ts = new TestScheduler();
        ReplaySubject<Object> rs = ReplaySubject.createWithTime(1, TimeUnit.SECONDS, ts);
        
        assertEquals(0, rs.size());
        assertFalse(rs.hasAnyValue());
        
        for (int i = 0; i < 1000; i++) {
            rs.onNext(i);
            ts.advanceTimeBy(500, TimeUnit.MILLISECONDS);
            assertEquals(1, rs.size());
            assertTrue(rs.hasAnyValue());
            ts.advanceTimeBy(1500, TimeUnit.MILLISECONDS);
            assertEquals(0, rs.size()); // stale data no longer peekable
            assertFalse(rs.hasAnyValue());
        }
        
        rs.onCompleted();

        assertEquals(0, rs.size());
        assertFalse(rs.hasAnyValue());
    }
    @Test
    public void testGetValues() {
        ReplaySubject<Object> rs = ReplaySubject.create();
        Object[] expected = new Object[10];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = i;
            rs.onNext(i);
            assertArrayEquals(Arrays.copyOf(expected, i + 1), rs.getValues());
        }
        rs.onCompleted();
        
        assertArrayEquals(expected, rs.getValues());
        
    }
    @Test
    public void testGetValuesUnbounded() {
        ReplaySubject<Object> rs = ReplaySubject.createUnbounded();
        Object[] expected = new Object[10];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = i;
            rs.onNext(i);
            assertArrayEquals(Arrays.copyOf(expected, i + 1), rs.getValues());
        }
        rs.onCompleted();
        
        assertArrayEquals(expected, rs.getValues());
        
    }
    
    @Test
    public void testReplaySubjectValueRelay() {
        ReplaySubject<Integer> async = ReplaySubject.create();
        async.onNext(1);
        async.onCompleted();
        
        assertFalse(async.hasObservers());
        assertTrue(async.hasCompleted());
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
    public void testReplaySubjectValueRelayIncomplete() {
        ReplaySubject<Integer> async = ReplaySubject.create();
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
    public void testReplaySubjectValueRelayBounded() {
        ReplaySubject<Integer> async = ReplaySubject.createWithSize(1);
        async.onNext(0);
        async.onNext(1);
        async.onCompleted();
        
        assertFalse(async.hasObservers());
        assertTrue(async.hasCompleted());
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
    public void testReplaySubjectValueRelayBoundedIncomplete() {
        ReplaySubject<Integer> async = ReplaySubject.createWithSize(1);
        async.onNext(0);
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
    public void testReplaySubjectValueRelayBoundedEmptyIncomplete() {
        ReplaySubject<Integer> async = ReplaySubject.createWithSize(1);
        
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
    public void testReplaySubjectValueRelayEmptyIncomplete() {
        ReplaySubject<Integer> async = ReplaySubject.create();
        
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
    public void testReplaySubjectEmpty() {
        ReplaySubject<Integer> async = ReplaySubject.create();
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
    public void testReplaySubjectError() {
        ReplaySubject<Integer> async = ReplaySubject.create();
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
    
    @Test
    public void testReplaySubjectBoundedEmpty() {
        ReplaySubject<Integer> async = ReplaySubject.createWithSize(1);
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
    public void testReplaySubjectBoundedError() {
        ReplaySubject<Integer> async = ReplaySubject.createWithSize(1);
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
    
    void backpressureLive(ReplaySubject<Integer> rs) {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        rs.subscribe(ts);
        
        for (int i = 1; i <= 5; i++) {
            rs.onNext(i);
        }
        
        ts.assertNoValues();
        
        ts.requestMore(2);
        
        ts.assertValues(1, 2);
        
        ts.requestMore(6);

        ts.assertValues(1, 2, 3, 4, 5);
        
        for (int i = 6; i <= 10; i++) {
            rs.onNext(i);
        }

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8);
        
        rs.onCompleted();
        
        ts.assertNotCompleted();
        
        ts.requestMore(2);
        
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    void backpressureOffline(ReplaySubject<Integer> rs) {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        for (int i = 1; i <= 10; i++) {
            rs.onNext(i);
        }
        rs.onCompleted();

        rs.subscribe(ts);

        ts.assertNoValues();
        
        ts.requestMore(2);
        
        ts.assertValues(1, 2);
        
        ts.requestMore(6);
        
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8);
        
        ts.assertNotCompleted();
        
        ts.requestMore(2);
        
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    void backpressureOffline5(ReplaySubject<Integer> rs) {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        for (int i = 1; i <= 10; i++) {
            rs.onNext(i);
        }
        rs.onCompleted();

        rs.subscribe(ts);

        ts.assertNoValues();
        
        ts.requestMore(2);
        
        ts.assertValues(6, 7);
        
        ts.requestMore(2);
        
        ts.assertValues(6, 7, 8, 9);
        
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        
        ts.assertValues(6, 7, 8, 9, 10);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void backpressureUnboundedLive() {
        backpressureLive(ReplaySubject.<Integer>create());
    }

    @Test
    public void backpressureSizeBoundLive() {
        backpressureLive(ReplaySubject.<Integer>createWithSize(1));
        backpressureLive(ReplaySubject.<Integer>createWithSize(5));
        backpressureLive(ReplaySubject.<Integer>createWithSize(10));
        backpressureLive(ReplaySubject.<Integer>createWithSize(100));
    }

    @Test
    public void backpressureSizeAndTimeLive() {
        backpressureLive(ReplaySubject.<Integer>createWithTimeAndSize(1, TimeUnit.DAYS, 1, Schedulers.immediate()));
        backpressureLive(ReplaySubject.<Integer>createWithTimeAndSize(1, TimeUnit.DAYS, 5, Schedulers.immediate()));
        backpressureLive(ReplaySubject.<Integer>createWithTimeAndSize(1, TimeUnit.DAYS, 10, Schedulers.immediate()));
        backpressureLive(ReplaySubject.<Integer>createWithTimeAndSize(1, TimeUnit.DAYS, 100, Schedulers.immediate()));
    }

    @Test
    public void backpressureUnboundedOffline() {
        backpressureOffline(ReplaySubject.<Integer>create());
    }

    @Test
    public void backpressureSizeBoundOffline() {
        backpressureOffline5(ReplaySubject.<Integer>createWithSize(5));
        backpressureOffline(ReplaySubject.<Integer>createWithSize(10));
        backpressureOffline(ReplaySubject.<Integer>createWithSize(100));
    }

    @Test
    public void backpressureSizeAndTimeOffline() {
        backpressureOffline5(ReplaySubject.<Integer>createWithTimeAndSize(1, TimeUnit.DAYS, 5, Schedulers.immediate()));
        backpressureOffline(ReplaySubject.<Integer>createWithTimeAndSize(1, TimeUnit.DAYS, 10, Schedulers.immediate()));
        backpressureOffline(ReplaySubject.<Integer>createWithTimeAndSize(1, TimeUnit.DAYS, 100, Schedulers.immediate()));
    }

}
