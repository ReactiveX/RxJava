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

package io.reactivex.processors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.*;

public class ReplayProcessorTest extends FlowableProcessorTest<Object> {

    private final Throwable testException = new Throwable();

    @Override
    protected FlowableProcessor<Object> create() {
        return ReplayProcessor.create();
    }

    @Test
    public void testCompleted() {
        ReplayProcessor<String> subject = ReplayProcessor.create();

        Subscriber<String> o1 = TestHelper.mockSubscriber();
        subject.subscribe(o1);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onComplete();

        subject.onNext("four");
        subject.onComplete();
        subject.onError(new Throwable());

        assertCompletedSubscriber(o1);

        // assert that subscribing a 2nd time gets the same data
        Subscriber<String> o2 = TestHelper.mockSubscriber();
        subject.subscribe(o2);
        assertCompletedSubscriber(o2);
    }

    @Test
    public void testCompletedStopsEmittingData() {
        ReplayProcessor<Integer> channel = ReplayProcessor.create();
        Subscriber<Object> observerA = TestHelper.mockSubscriber();
        Subscriber<Object> observerB = TestHelper.mockSubscriber();
        Subscriber<Object> observerC = TestHelper.mockSubscriber();
        Subscriber<Object> observerD = TestHelper.mockSubscriber();
        TestSubscriber<Object> ts = new TestSubscriber<Object>(observerA);

        channel.subscribe(ts);
        channel.subscribe(observerB);

        InOrder inOrderA = inOrder(observerA);
        InOrder inOrderB = inOrder(observerB);
        InOrder inOrderC = inOrder(observerC);
        InOrder inOrderD = inOrder(observerD);

        channel.onNext(42);

        // both A and B should have received 42 from before subscription
        inOrderA.verify(observerA).onNext(42);
        inOrderB.verify(observerB).onNext(42);

        ts.dispose();

        // a should receive no more
        inOrderA.verifyNoMoreInteractions();

        channel.onNext(4711);

        // only be should receive 4711 at this point
        inOrderB.verify(observerB).onNext(4711);

        channel.onComplete();

        // B is subscribed so should receive onComplete
        inOrderB.verify(observerB).onComplete();

        channel.subscribe(observerC);

        // when C subscribes it should receive 42, 4711, onComplete
        inOrderC.verify(observerC).onNext(42);
        inOrderC.verify(observerC).onNext(4711);
        inOrderC.verify(observerC).onComplete();

        // if further events are propagated they should be ignored
        channel.onNext(13);
        channel.onNext(14);
        channel.onNext(15);
        channel.onError(new RuntimeException());

        // a new subscription should only receive what was emitted prior to terminal state onComplete
        channel.subscribe(observerD);

        inOrderD.verify(observerD).onNext(42);
        inOrderD.verify(observerD).onNext(4711);
        inOrderD.verify(observerD).onComplete();

        verify(observerA).onSubscribe((Subscription)notNull());
        verify(observerB).onSubscribe((Subscription)notNull());
        verify(observerC).onSubscribe((Subscription)notNull());
        verify(observerD).onSubscribe((Subscription)notNull());
        Mockito.verifyNoMoreInteractions(observerA);
        Mockito.verifyNoMoreInteractions(observerB);
        Mockito.verifyNoMoreInteractions(observerC);
        Mockito.verifyNoMoreInteractions(observerD);

    }

    @Test
    public void testCompletedAfterError() {
        ReplayProcessor<String> subject = ReplayProcessor.create();

        Subscriber<String> observer = TestHelper.mockSubscriber();

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onComplete();
        subject.onError(new RuntimeException());

        subject.subscribe(observer);
        verify(observer).onSubscribe((Subscription)notNull());
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onError(testException);
        verifyNoMoreInteractions(observer);
    }

    private void assertCompletedSubscriber(Subscriber<String> observer) {
        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, Mockito.never()).onError(any(Throwable.class));
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testError() {
        ReplayProcessor<String> subject = ReplayProcessor.create();

        Subscriber<String> observer = TestHelper.mockSubscriber();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onError(testException);

        subject.onNext("four");
        subject.onError(new Throwable());
        subject.onComplete();

        assertErrorSubscriber(observer);

        observer = TestHelper.mockSubscriber();
        subject.subscribe(observer);
        assertErrorSubscriber(observer);
    }

    private void assertErrorSubscriber(Subscriber<String> observer) {
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, times(1)).onError(testException);
        verify(observer, Mockito.never()).onComplete();
    }

    @Test
    public void testSubscribeMidSequence() {
        ReplayProcessor<String> subject = ReplayProcessor.create();

        Subscriber<String> observer = TestHelper.mockSubscriber();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");

        assertObservedUntilTwo(observer);

        Subscriber<String> anotherSubscriber = TestHelper.mockSubscriber();
        subject.subscribe(anotherSubscriber);
        assertObservedUntilTwo(anotherSubscriber);

        subject.onNext("three");
        subject.onComplete();

        assertCompletedSubscriber(observer);
        assertCompletedSubscriber(anotherSubscriber);
    }

    @Test
    public void testUnsubscribeFirstSubscriber() {
        ReplayProcessor<String> subject = ReplayProcessor.create();

        Subscriber<String> observer = TestHelper.mockSubscriber();
        TestSubscriber<String> ts = new TestSubscriber<String>(observer);
        subject.subscribe(ts);

        subject.onNext("one");
        subject.onNext("two");

        ts.dispose();
        assertObservedUntilTwo(observer);

        Subscriber<String> anotherSubscriber = TestHelper.mockSubscriber();
        subject.subscribe(anotherSubscriber);
        assertObservedUntilTwo(anotherSubscriber);

        subject.onNext("three");
        subject.onComplete();

        assertObservedUntilTwo(observer);
        assertCompletedSubscriber(anotherSubscriber);
    }

    private void assertObservedUntilTwo(Subscriber<String> observer) {
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, Mockito.never()).onComplete();
    }

    @Test(timeout = 2000)
    public void testNewSubscriberDoesntBlockExisting() throws InterruptedException {

        final AtomicReference<String> lastValueForSubscriber1 = new AtomicReference<String>();
        Subscriber<String> observer1 = new DefaultSubscriber<String>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String v) {
                System.out.println("observer1: " + v);
                lastValueForSubscriber1.set(v);
            }

        };

        final AtomicReference<String> lastValueForSubscriber2 = new AtomicReference<String>();
        final CountDownLatch oneReceived = new CountDownLatch(1);
        final CountDownLatch makeSlow = new CountDownLatch(1);
        final CountDownLatch completed = new CountDownLatch(1);
        Subscriber<String> observer2 = new DefaultSubscriber<String>() {

            @Override
            public void onComplete() {
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
                    lastValueForSubscriber2.set(v);
                }
            }

        };

        ReplayProcessor<String> subject = ReplayProcessor.create();
        subject.subscribe(observer1);
        subject.onNext("one");
        assertEquals("one", lastValueForSubscriber1.get());
        subject.onNext("two");
        assertEquals("two", lastValueForSubscriber1.get());

        // use subscribeOn to make this async otherwise we deadlock as we are using CountDownLatches
        subject.subscribeOn(Schedulers.newThread()).subscribe(observer2);

        System.out.println("before waiting for one");

        // wait until observer2 starts having replay occur
        oneReceived.await();

        System.out.println("after waiting for one");

        subject.onNext("three");

        System.out.println("sent three");

        // if subscription blocked existing subscribers then 'makeSlow' would cause this to not be there yet
        assertEquals("three", lastValueForSubscriber1.get());

        System.out.println("about to send onComplete");

        subject.onComplete();

        System.out.println("completed subject");

        // release
        makeSlow.countDown();

        System.out.println("makeSlow released");

        completed.await();
        // all of them should be emitted with the last being "three"
        assertEquals("three", lastValueForSubscriber2.get());

    }
    @Test
    public void testSubscriptionLeak() {
        ReplayProcessor<Object> replaySubject = ReplayProcessor.create();

        Disposable s = replaySubject.subscribe();

        assertEquals(1, replaySubject.subscriberCount());

        s.dispose();

        assertEquals(0, replaySubject.subscriberCount());
    }

    @Test(timeout = 1000)
    public void testUnsubscriptionCase() {
        ReplayProcessor<String> src = ReplayProcessor.create();

        for (int i = 0; i < 10; i++) {
            final Subscriber<Object> o = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(o);
            String v = "" + i;
            src.onNext(v);
            System.out.printf("Turn: %d%n", i);
            src.firstElement().toFlowable()
                .flatMap(new Function<String, Flowable<String>>() {

                    @Override
                    public Flowable<String> apply(String t1) {
                        return Flowable.just(t1 + ", " + t1);
                    }
                })
                .subscribe(new DefaultSubscriber<String>() {
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
                    public void onComplete() {
                        o.onComplete();
                    }
                });
            inOrder.verify(o).onNext("0, 0");
            inOrder.verify(o).onComplete();
            verify(o, never()).onError(any(Throwable.class));
        }
    }
    @Test
    public void testTerminateOnce() {
        ReplayProcessor<Integer> source = ReplayProcessor.create();
        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        final Subscriber<Integer> o = TestHelper.mockSubscriber();

        source.subscribe(new DefaultSubscriber<Integer>() {

            @Override
            public void onNext(Integer t) {
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

        verify(o).onNext(1);
        verify(o).onNext(2);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testReplay1AfterTermination() {
        ReplayProcessor<Integer> source = ReplayProcessor.createWithSize(1);

        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        for (int i = 0; i < 1; i++) {
            Subscriber<Integer> o = TestHelper.mockSubscriber();

            source.subscribe(o);

            verify(o, never()).onNext(1);
            verify(o).onNext(2);
            verify(o).onComplete();
            verify(o, never()).onError(any(Throwable.class));
        }
    }
    @Test
    public void testReplay1Directly() {
        ReplayProcessor<Integer> source = ReplayProcessor.createWithSize(1);

        Subscriber<Integer> o = TestHelper.mockSubscriber();

        source.onNext(1);
        source.onNext(2);

        source.subscribe(o);

        source.onNext(3);
        source.onComplete();

        verify(o, never()).onNext(1);
        verify(o).onNext(2);
        verify(o).onNext(3);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testReplayTimestampedAfterTermination() {
        TestScheduler scheduler = new TestScheduler();
        ReplayProcessor<Integer> source = ReplayProcessor.createWithTime(1, TimeUnit.SECONDS, scheduler);

        source.onNext(1);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        source.onNext(2);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        source.onNext(3);
        source.onComplete();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        Subscriber<Integer> o = TestHelper.mockSubscriber();

        source.subscribe(o);

        verify(o, never()).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onNext(3);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testReplayTimestampedDirectly() {
        TestScheduler scheduler = new TestScheduler();
        ReplayProcessor<Integer> source = ReplayProcessor.createWithTime(1, TimeUnit.SECONDS, scheduler);

        source.onNext(1);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        Subscriber<Integer> o = TestHelper.mockSubscriber();

        source.subscribe(o);

        source.onNext(2);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        source.onNext(3);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        source.onComplete();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onNext(1);
        verify(o).onNext(2);
        verify(o).onNext(3);
        verify(o).onComplete();
    }

    // FIXME RS subscribers can't throw
//    @Test
//    public void testOnErrorThrowsDoesntPreventDelivery() {
//        ReplaySubject<String> ps = ReplaySubject.create();
//
//        ps.subscribe();
//        TestSubscriber<String> ts = new TestSubscriber<String>();
//        ps.subscribe(ts);
//
//        try {
//            ps.onError(new RuntimeException("an exception"));
//            fail("expect OnErrorNotImplementedException");
//        } catch (OnErrorNotImplementedException e) {
//            // ignore
//        }
//        // even though the onError above throws we should still receive it on the other subscriber
//        assertEquals(1, ts.errors().size());
//    }

    // FIXME RS subscribers can't throw
//    /**
//     * This one has multiple failures so should get a CompositeException
//     */
//    @Test
//    public void testOnErrorThrowsDoesntPreventDelivery2() {
//        ReplaySubject<String> ps = ReplaySubject.create();
//
//        ps.subscribe();
//        ps.subscribe();
//        TestSubscriber<String> ts = new TestSubscriber<String>();
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
        ReplayProcessor<Object> as = ReplayProcessor.create();

        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getThrowable());

        as.onNext(1);

        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getThrowable());

        as.onComplete();

        assertFalse(as.hasThrowable());
        assertTrue(as.hasComplete());
        assertNull(as.getThrowable());
    }

    @Test
    public void testCurrentStateMethodsEmpty() {
        ReplayProcessor<Object> as = ReplayProcessor.create();

        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getThrowable());

        as.onComplete();

        assertFalse(as.hasThrowable());
        assertTrue(as.hasComplete());
        assertNull(as.getThrowable());
    }
    @Test
    public void testCurrentStateMethodsError() {
        ReplayProcessor<Object> as = ReplayProcessor.create();

        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getThrowable());

        as.onError(new TestException());

        assertTrue(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertTrue(as.getThrowable() instanceof TestException);
    }
    @Test
    public void testSizeAndHasAnyValueUnbounded() {
        ReplayProcessor<Object> rs = ReplayProcessor.create();

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());

        rs.onNext(1);

        assertEquals(1, rs.size());
        assertTrue(rs.hasValue());

        rs.onNext(1);

        assertEquals(2, rs.size());
        assertTrue(rs.hasValue());

        rs.onComplete();

        assertEquals(2, rs.size());
        assertTrue(rs.hasValue());
    }
    @Test
    public void testSizeAndHasAnyValueEffectivelyUnbounded() {
        ReplayProcessor<Object> rs = ReplayProcessor.createUnbounded();

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());

        rs.onNext(1);

        assertEquals(1, rs.size());
        assertTrue(rs.hasValue());

        rs.onNext(1);

        assertEquals(2, rs.size());
        assertTrue(rs.hasValue());

        rs.onComplete();

        assertEquals(2, rs.size());
        assertTrue(rs.hasValue());
    }

    @Test
    public void testSizeAndHasAnyValueUnboundedError() {
        ReplayProcessor<Object> rs = ReplayProcessor.create();

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());

        rs.onNext(1);

        assertEquals(1, rs.size());
        assertTrue(rs.hasValue());

        rs.onNext(1);

        assertEquals(2, rs.size());
        assertTrue(rs.hasValue());

        rs.onError(new TestException());

        assertEquals(2, rs.size());
        assertTrue(rs.hasValue());
    }
    @Test
    public void testSizeAndHasAnyValueEffectivelyUnboundedError() {
        ReplayProcessor<Object> rs = ReplayProcessor.createUnbounded();

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());

        rs.onNext(1);

        assertEquals(1, rs.size());
        assertTrue(rs.hasValue());

        rs.onNext(1);

        assertEquals(2, rs.size());
        assertTrue(rs.hasValue());

        rs.onError(new TestException());

        assertEquals(2, rs.size());
        assertTrue(rs.hasValue());
    }

    @Test
    public void testSizeAndHasAnyValueUnboundedEmptyError() {
        ReplayProcessor<Object> rs = ReplayProcessor.create();

        rs.onError(new TestException());

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());
    }
    @Test
    public void testSizeAndHasAnyValueEffectivelyUnboundedEmptyError() {
        ReplayProcessor<Object> rs = ReplayProcessor.createUnbounded();

        rs.onError(new TestException());

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());
    }

    @Test
    public void testSizeAndHasAnyValueUnboundedEmptyCompleted() {
        ReplayProcessor<Object> rs = ReplayProcessor.create();

        rs.onComplete();

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());
    }
    @Test
    public void testSizeAndHasAnyValueEffectivelyUnboundedEmptyCompleted() {
        ReplayProcessor<Object> rs = ReplayProcessor.createUnbounded();

        rs.onComplete();

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());
    }

    @Test
    public void testSizeAndHasAnyValueSizeBounded() {
        ReplayProcessor<Object> rs = ReplayProcessor.createWithSize(1);

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());

        for (int i = 0; i < 1000; i++) {
            rs.onNext(i);

            assertEquals(1, rs.size());
            assertTrue(rs.hasValue());
        }

        rs.onComplete();

        assertEquals(1, rs.size());
        assertTrue(rs.hasValue());
    }

    @Test
    public void testSizeAndHasAnyValueTimeBounded() {
        TestScheduler ts = new TestScheduler();
        ReplayProcessor<Object> rs = ReplayProcessor.createWithTime(1, TimeUnit.SECONDS, ts);

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());

        for (int i = 0; i < 1000; i++) {
            rs.onNext(i);
            assertEquals(1, rs.size());
            assertTrue(rs.hasValue());
            ts.advanceTimeBy(2, TimeUnit.SECONDS);
            assertEquals(0, rs.size());
            assertFalse(rs.hasValue());
        }

        rs.onComplete();

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());
    }
    @Test
    public void testGetValues() {
        ReplayProcessor<Object> rs = ReplayProcessor.create();
        Object[] expected = new Object[10];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = i;
            rs.onNext(i);
            assertArrayEquals(Arrays.copyOf(expected, i + 1), rs.getValues());
        }
        rs.onComplete();

        assertArrayEquals(expected, rs.getValues());

    }
    @Test
    public void testGetValuesUnbounded() {
        ReplayProcessor<Object> rs = ReplayProcessor.createUnbounded();
        Object[] expected = new Object[10];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = i;
            rs.onNext(i);
            assertArrayEquals(Arrays.copyOf(expected, i + 1), rs.getValues());
        }
        rs.onComplete();

        assertArrayEquals(expected, rs.getValues());

    }

    @Test
    public void testBackpressureHonored() {
        ReplayProcessor<Integer> rs = ReplayProcessor.create();
        rs.onNext(1);
        rs.onNext(2);
        rs.onNext(3);
        rs.onComplete();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

        rs.subscribe(ts);

        ts.request(1);
        ts.assertValue(1);
        ts.assertNotComplete();
        ts.assertNoErrors();


        ts.request(1);
        ts.assertValues(1, 2);
        ts.assertNotComplete();
        ts.assertNoErrors();

        ts.request(1);
        ts.assertValues(1, 2, 3);
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test
    public void testBackpressureHonoredSizeBound() {
        ReplayProcessor<Integer> rs = ReplayProcessor.createWithSize(100);
        rs.onNext(1);
        rs.onNext(2);
        rs.onNext(3);
        rs.onComplete();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

        rs.subscribe(ts);

        ts.request(1);
        ts.assertValue(1);
        ts.assertNotComplete();
        ts.assertNoErrors();


        ts.request(1);
        ts.assertValues(1, 2);
        ts.assertNotComplete();
        ts.assertNoErrors();

        ts.request(1);
        ts.assertValues(1, 2, 3);
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test
    public void testBackpressureHonoredTimeBound() {
        ReplayProcessor<Integer> rs = ReplayProcessor.createWithTime(1, TimeUnit.DAYS, Schedulers.trampoline());
        rs.onNext(1);
        rs.onNext(2);
        rs.onNext(3);
        rs.onComplete();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

        rs.subscribe(ts);

        ts.request(1);
        ts.assertValue(1);
        ts.assertNotComplete();
        ts.assertNoErrors();


        ts.request(1);
        ts.assertValues(1, 2);
        ts.assertNotComplete();
        ts.assertNoErrors();

        ts.request(1);
        ts.assertValues(1, 2, 3);
        ts.assertComplete();
        ts.assertNoErrors();
    }

    @Test
    public void createInvalidCapacity() {
        try {
            ReplayProcessor.create(-99);
            fail("Didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("capacityHint > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void createWithSizeInvalidCapacity() {
        try {
            ReplayProcessor.createWithSize(-99);
            fail("Didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("maxSize > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void createWithTimeAndSizeInvalidCapacity() {
        try {
            ReplayProcessor.createWithTimeAndSize(1, TimeUnit.DAYS, Schedulers.computation(), -99);
            fail("Didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("maxSize > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void hasSubscribers() {
        ReplayProcessor<Integer> rp = ReplayProcessor.create();

        assertFalse(rp.hasSubscribers());

        TestSubscriber<Integer> ts = rp.test();

        assertTrue(rp.hasSubscribers());

        ts.cancel();

        assertFalse(rp.hasSubscribers());
    }

    @Test
    public void peekStateUnbounded() {
        ReplayProcessor<Integer> rp = ReplayProcessor.create();

        rp.onNext(1);

        assertEquals((Integer)1, rp.getValue());

        assertEquals(1, rp.getValues()[0]);
    }

    @Test
    public void peekStateTimeAndSize() {
        ReplayProcessor<Integer> rp = ReplayProcessor.createWithTimeAndSize(1, TimeUnit.DAYS, Schedulers.computation(), 1);

        rp.onNext(1);

        assertEquals((Integer)1, rp.getValue());

        assertEquals(1, rp.getValues()[0]);

        rp.onNext(2);

        assertEquals((Integer)2, rp.getValue());

        assertEquals(2, rp.getValues()[0]);

        assertEquals((Integer)2, rp.getValues(new Integer[0])[0]);

        assertEquals((Integer)2, rp.getValues(new Integer[1])[0]);

        Integer[] a = new Integer[2];
        assertEquals((Integer)2, rp.getValues(a)[0]);
        assertNull(a[1]);
    }

    @Test
    public void peekStateTimeAndSizeValue() {
        ReplayProcessor<Integer> rp = ReplayProcessor.createWithTimeAndSize(1, TimeUnit.DAYS, Schedulers.computation(), 1);

        assertNull(rp.getValue());

        assertEquals(0, rp.getValues().length);

        assertNull(rp.getValues(new Integer[2])[0]);

        rp.onComplete();

        assertNull(rp.getValue());

        assertEquals(0, rp.getValues().length);

        assertNull(rp.getValues(new Integer[2])[0]);

        rp = ReplayProcessor.createWithTimeAndSize(1, TimeUnit.DAYS, Schedulers.computation(), 1);
        rp.onError(new TestException());

        assertNull(rp.getValue());

        assertEquals(0, rp.getValues().length);

        assertNull(rp.getValues(new Integer[2])[0]);
    }

    @Test
    public void peekStateTimeAndSizeValueExpired() {
        TestScheduler scheduler = new TestScheduler();
        ReplayProcessor<Integer> rp = ReplayProcessor.createWithTime(1, TimeUnit.DAYS, scheduler);

        assertNull(rp.getValue());
        assertNull(rp.getValues(new Integer[2])[0]);

        rp.onNext(2);

        assertEquals((Integer)2, rp.getValue());
        assertEquals(2, rp.getValues()[0]);

        scheduler.advanceTimeBy(2, TimeUnit.DAYS);

        assertEquals(null, rp.getValue());
        assertEquals(0, rp.getValues().length);
        assertNull(rp.getValues(new Integer[2])[0]);
    }

    @Test
    public void capacityHint() {
        ReplayProcessor<Integer> rp = ReplayProcessor.create(8);

        for (int i = 0; i < 15; i++) {
            rp.onNext(i);
        }
        rp.onComplete();

        rp.test().assertResult(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
    }

    @Test
    public void subscribeCancelRace() {
        for (int i = 0; i < 500; i++) {
            final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

            final ReplayProcessor<Integer> rp = ReplayProcessor.create();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    rp.subscribe(ts);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void subscribeAfterDone() {
        ReplayProcessor<Integer> rp = ReplayProcessor.create();
        rp.onComplete();

        BooleanSubscription bs = new BooleanSubscription();

        rp.onSubscribe(bs);

        assertTrue(bs.isCancelled());
    }

    @Test
    public void subscribeRace() {
        for (int i = 0; i < 500; i++) {
            final ReplayProcessor<Integer> rp = ReplayProcessor.create();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    rp.test();
                }
            };

            TestHelper.race(r1, r1, Schedulers.single());
        }
    }

    @Test
    public void cancelUpfront() {
        ReplayProcessor<Integer> rp = ReplayProcessor.create();
        rp.test();
        rp.test();

        TestSubscriber<Integer> ts = rp.test(0L, true);

        assertEquals(2, rp.subscriberCount());

        ts.assertEmpty();
    }

    @Test
    public void cancelRace() {
        for (int i = 0; i < 500; i++) {

            final ReplayProcessor<Integer> rp = ReplayProcessor.create();
            final TestSubscriber<Integer> ts1 = rp.test();
            final TestSubscriber<Integer> ts2 = rp.test();

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

            assertFalse(rp.hasSubscribers());
        }
    }

    @Test
    public void sizeboundReplayError() {
        ReplayProcessor<Integer> rp = ReplayProcessor.createWithSize(2);

        rp.onNext(1);
        rp.onNext(2);
        rp.onNext(3);
        rp.onNext(4);
        rp.onError(new TestException());

        rp.test()
        .assertFailure(TestException.class, 3, 4);
    }

    @Test
    public void sizeAndTimeBoundReplayError() {
        ReplayProcessor<Integer> rp = ReplayProcessor.createWithTimeAndSize(1, TimeUnit.DAYS, Schedulers.single(), 2);

        rp.onNext(1);
        rp.onNext(2);
        rp.onNext(3);
        rp.onNext(4);
        rp.onError(new TestException());

        rp.test()
        .assertFailure(TestException.class, 3, 4);
    }

    @Test
    public void replayRequestRace() {
        for (int i = 0; i < 500; i++) {

            final ReplayProcessor<Integer> rp = ReplayProcessor.createWithTimeAndSize(1, TimeUnit.DAYS, Schedulers.single(), 2);
            final TestSubscriber<Integer> ts = rp.test(0L);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    rp.onNext(1);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void timedSkipOld() {
        TestScheduler scheduler = new TestScheduler();

        ReplayProcessor<Integer> rp = ReplayProcessor.createWithTimeAndSize(1, TimeUnit.SECONDS, scheduler, 2);

        rp.onNext(1);
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        rp.test()
        .assertEmpty();
    }

    @Test
    public void takeSizeAndTime() {
        TestScheduler scheduler = new TestScheduler();

        ReplayProcessor<Integer> rp = ReplayProcessor.createWithTimeAndSize(1, TimeUnit.SECONDS, scheduler, 2);

        rp.onNext(1);
        rp.onNext(2);
        rp.onNext(3);

        rp
        .take(1)
        .test()
        .assertResult(2);
    }

    @Test
    public void takeSize() {
        ReplayProcessor<Integer> rp = ReplayProcessor.createWithSize(2);

        rp.onNext(1);
        rp.onNext(2);
        rp.onNext(3);

        rp
        .take(1)
        .test()
        .assertResult(2);
    }

    @Test
    public void reentrantDrain() {
        TestScheduler scheduler = new TestScheduler();

        final ReplayProcessor<Integer> rp = ReplayProcessor.createWithTimeAndSize(1, TimeUnit.SECONDS, scheduler, 2);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    rp.onNext(2);
                }
                super.onNext(t);
            }
        };

        rp.subscribe(ts);

        rp.onNext(1);
        rp.onComplete();

        ts.assertResult(1, 2);
    }

    @Test
    public void reentrantDrainBackpressured() {
        TestScheduler scheduler = new TestScheduler();

        final ReplayProcessor<Integer> rp = ReplayProcessor.createWithTimeAndSize(1, TimeUnit.SECONDS, scheduler, 2);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1L) {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    rp.onNext(2);
                }
                super.onNext(t);
            }
        };

        rp.subscribe(ts);

        rp.onNext(1);
        rp.onComplete();

        ts.request(1);

        ts.assertResult(1, 2);
    }

    @Test
    public void timedNoOutdatedData() {
        TestScheduler scheduler = new TestScheduler();

        ReplayProcessor<Integer> source = ReplayProcessor.createWithTime(2, TimeUnit.SECONDS, scheduler);
        source.onNext(1);
        source.onComplete();

        source.test().assertResult(1);

        source.test().assertResult(1);

        scheduler.advanceTimeBy(3, TimeUnit.SECONDS);

        source.test().assertResult();
    }

    int raceLoop = 10000;

    @Test
    public void unboundedRequestCompleteRace() {
        for (int i = 0; i < raceLoop; i++) {
            final ReplayProcessor<Integer> source = ReplayProcessor.create();

            final TestSubscriber<Integer> ts = source.test(0);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    source.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
                }
            };

            TestHelper.race(r1, r2);

            ts.assertResult();
        }
    }

    @Test
    public void sizeRequestCompleteRace() {
        for (int i = 0; i < raceLoop; i++) {
            final ReplayProcessor<Integer> source = ReplayProcessor.createWithSize(10);

            final TestSubscriber<Integer> ts = source.test(0);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    source.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
                }
            };

            TestHelper.race(r1, r2);

            ts.assertResult();
        }
    }

    @Test
    public void timedRequestCompleteRace() {
        for (int i = 0; i < raceLoop; i++) {
            final ReplayProcessor<Integer> source = ReplayProcessor.createWithTime(2, TimeUnit.HOURS, Schedulers.single());

            final TestSubscriber<Integer> ts = source.test(0);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    source.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
                }
            };

            TestHelper.race(r1, r2);

            ts.assertResult();
        }
    }

    @Test
    public void timeAndSizeRequestCompleteRace() {
        for (int i = 0; i < raceLoop; i++) {
            final ReplayProcessor<Integer> source = ReplayProcessor.createWithTimeAndSize(2, TimeUnit.HOURS, Schedulers.single(), 100);

            final TestSubscriber<Integer> ts = source.test(0);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    source.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
                }
            };

            TestHelper.race(r1, r2);

            ts.assertResult();
        }
    }

    @Test
    public void unboundedZeroRequestComplete() {
        final ReplayProcessor<Integer> source = ReplayProcessor.create();

        source.onComplete();

        source.test(0).assertResult();
    }

    @Test
    public void unboundedZeroRequestError() {
        final ReplayProcessor<Integer> source = ReplayProcessor.create();

        source.onError(new TestException());

        source.test(0).assertFailure(TestException.class);
    }

    @Test
    public void sizeBoundZeroRequestComplete() {
        final ReplayProcessor<Integer> source = ReplayProcessor.createWithSize(16);

        source.onComplete();

        source.test(0).assertResult();
    }

    @Test
    public void sizeBoundZeroRequestError() {
        final ReplayProcessor<Integer> source = ReplayProcessor.createWithSize(16);

        source.onError(new TestException());

        source.test(0).assertFailure(TestException.class);
    }

    @Test
    public void timeBoundZeroRequestComplete() {
        final ReplayProcessor<Integer> source = ReplayProcessor.createWithTime(1, TimeUnit.MINUTES, Schedulers.single());

        source.onComplete();

        source.test(0).assertResult();
    }

    @Test
    public void timeBoundZeroRequestError() {
        final ReplayProcessor<Integer> source = ReplayProcessor.createWithTime(1, TimeUnit.MINUTES, Schedulers.single());

        source.onError(new TestException());

        source.test(0).assertFailure(TestException.class);
    }

    @Test
    public void timeAndSizeBoundZeroRequestComplete() {
        final ReplayProcessor<Integer> source = ReplayProcessor.createWithTimeAndSize(1, TimeUnit.MINUTES, Schedulers.single(), 16);

        source.onComplete();

        source.test(0).assertResult();
    }

    @Test
    public void timeAndSizeBoundZeroRequestError() {
        final ReplayProcessor<Integer> source = ReplayProcessor.createWithTimeAndSize(1, TimeUnit.MINUTES, Schedulers.single(), 16);

        source.onError(new TestException());

        source.test(0).assertFailure(TestException.class);
    }

    TestSubscriber<Integer> take1AndCancel() {
        return new TestSubscriber<Integer>(1) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };
    }

    @Test
    public void unboundedCancelAfterOne() {
        ReplayProcessor<Integer> source = ReplayProcessor.create();
        source.onNext(1);

        source.subscribeWith(take1AndCancel())
        .assertResult(1);
    }

    @Test
    public void sizeBoundCancelAfterOne() {
        ReplayProcessor<Integer> source = ReplayProcessor.createWithSize(16);
        source.onNext(1);

        source.subscribeWith(take1AndCancel())
        .assertResult(1);
    }

    @Test
    public void timeBoundCancelAfterOne() {
        ReplayProcessor<Integer> source = ReplayProcessor.createWithTime(1, TimeUnit.MINUTES, Schedulers.single());
        source.onNext(1);

        source.subscribeWith(take1AndCancel())
        .assertResult(1);
    }
    @Test
    public void timeAndSizeBoundCancelAfterOne() {
        ReplayProcessor<Integer> source = ReplayProcessor.createWithTimeAndSize(1, TimeUnit.MINUTES, Schedulers.single(), 16);
        source.onNext(1);

        source.subscribeWith(take1AndCancel())
        .assertResult(1);
    }
}
