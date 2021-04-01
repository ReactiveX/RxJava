/*
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

package io.reactivex.rxjava3.subjects;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.management.*;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.mockito.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.observers.*;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subjects.ReplaySubject.*;
import io.reactivex.rxjava3.testsupport.*;

public class ReplaySubjectTest extends SubjectTest<Integer> {

    private final Throwable testException = new Throwable();

    @Override
    protected Subject<Integer> create() {
        return ReplaySubject.create();
    }

    @Test
    @SuppressUndeliverable
    public void completed() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> o1 = TestHelper.mockObserver();
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
        Observer<String> o2 = TestHelper.mockObserver();
        subject.subscribe(o2);
        assertCompletedSubscriber(o2);
    }

    @Test
    @SuppressUndeliverable
    public void completedStopsEmittingData() {
        ReplaySubject<Integer> channel = ReplaySubject.create();
        Observer<Object> observerA = TestHelper.mockObserver();
        Observer<Object> observerB = TestHelper.mockObserver();
        Observer<Object> observerC = TestHelper.mockObserver();
        Observer<Object> observerD = TestHelper.mockObserver();
        TestObserver<Object> to = new TestObserver<>(observerA);

        channel.subscribe(to);
        channel.subscribe(observerB);

        InOrder inOrderA = inOrder(observerA);
        InOrder inOrderB = inOrder(observerB);
        InOrder inOrderC = inOrder(observerC);
        InOrder inOrderD = inOrder(observerD);

        channel.onNext(42);

        // both A and B should have received 42 from before subscription
        inOrderA.verify(observerA).onNext(42);
        inOrderB.verify(observerB).onNext(42);

        to.dispose();

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

        verify(observerA).onSubscribe((Disposable)notNull());
        verify(observerB).onSubscribe((Disposable)notNull());
        verify(observerC).onSubscribe((Disposable)notNull());
        verify(observerD).onSubscribe((Disposable)notNull());
        Mockito.verifyNoMoreInteractions(observerA);
        Mockito.verifyNoMoreInteractions(observerB);
        Mockito.verifyNoMoreInteractions(observerC);
        Mockito.verifyNoMoreInteractions(observerD);

    }

    @Test
    @SuppressUndeliverable
    public void completedAfterError() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> observer = TestHelper.mockObserver();

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onComplete();
        subject.onError(new RuntimeException());

        subject.subscribe(observer);
        verify(observer).onSubscribe((Disposable)notNull());
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onError(testException);
        verifyNoMoreInteractions(observer);
    }

    private void assertCompletedSubscriber(Observer<String> observer) {
        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, Mockito.never()).onError(any(Throwable.class));
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @SuppressUndeliverable
    public void error() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onError(testException);

        subject.onNext("four");
        subject.onError(new Throwable());
        subject.onComplete();

        assertErrorSubscriber(observer);

        observer = TestHelper.mockObserver();
        subject.subscribe(observer);
        assertErrorSubscriber(observer);
    }

    private void assertErrorSubscriber(Observer<String> observer) {
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, times(1)).onError(testException);
        verify(observer, Mockito.never()).onComplete();
    }

    @Test
    public void subscribeMidSequence() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> observer = TestHelper.mockObserver();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");

        assertObservedUntilTwo(observer);

        Observer<String> anotherSubscriber = TestHelper.mockObserver();
        subject.subscribe(anotherSubscriber);
        assertObservedUntilTwo(anotherSubscriber);

        subject.onNext("three");
        subject.onComplete();

        assertCompletedSubscriber(observer);
        assertCompletedSubscriber(anotherSubscriber);
    }

    @Test
    public void unsubscribeFirstSubscriber() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> to = new TestObserver<>(observer);
        subject.subscribe(to);

        subject.onNext("one");
        subject.onNext("two");

        to.dispose();
        assertObservedUntilTwo(observer);

        Observer<String> anotherSubscriber = TestHelper.mockObserver();
        subject.subscribe(anotherSubscriber);
        assertObservedUntilTwo(anotherSubscriber);

        subject.onNext("three");
        subject.onComplete();

        assertObservedUntilTwo(observer);
        assertCompletedSubscriber(anotherSubscriber);
    }

    private void assertObservedUntilTwo(Observer<String> observer) {
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, Mockito.never()).onComplete();
    }

    @Test
    public void newSubscriberDoesntBlockExisting() throws InterruptedException {

        final AtomicReference<String> lastValueForSubscriber1 = new AtomicReference<>();
        Observer<String> observer1 = new DefaultObserver<String>() {

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

        final AtomicReference<String> lastValueForSubscriber2 = new AtomicReference<>();
        final CountDownLatch oneReceived = new CountDownLatch(1);
        final CountDownLatch makeSlow = new CountDownLatch(1);
        final CountDownLatch completed = new CountDownLatch(1);
        Observer<String> observer2 = new DefaultObserver<String>() {

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

        ReplaySubject<String> subject = ReplaySubject.create();
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
    public void subscriptionLeak() {
        ReplaySubject<Object> subject = ReplaySubject.create();

        Disposable d = subject.subscribe();

        assertEquals(1, subject.observerCount());

        d.dispose();

        assertEquals(0, subject.observerCount());
    }

    @Test
    public void unsubscriptionCase() {
        ReplaySubject<String> src = ReplaySubject.create();

        for (int i = 0; i < 10; i++) {
            final Observer<Object> o = TestHelper.mockObserver();
            InOrder inOrder = inOrder(o);
            String v = "" + i;
            src.onNext(v);
            System.out.printf("Turn: %d%n", i);
            src.firstElement()
                .toObservable()
                .flatMap(new Function<String, Observable<String>>() {

                    @Override
                    public Observable<String> apply(String t1) {
                        return Observable.just(t1 + ", " + t1);
                    }
                })
                .subscribe(new DefaultObserver<String>() {
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
    public void terminateOnce() {
        ReplaySubject<Integer> source = ReplaySubject.create();
        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        final Observer<Integer> o = TestHelper.mockObserver();

        source.subscribe(new DefaultObserver<Integer>() {

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
    public void replay1AfterTermination() {
        ReplaySubject<Integer> source = ReplaySubject.createWithSize(1);

        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        for (int i = 0; i < 1; i++) {
            Observer<Integer> o = TestHelper.mockObserver();

            source.subscribe(o);

            verify(o, never()).onNext(1);
            verify(o).onNext(2);
            verify(o).onComplete();
            verify(o, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void replay1Directly() {
        ReplaySubject<Integer> source = ReplaySubject.createWithSize(1);

        Observer<Integer> o = TestHelper.mockObserver();

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
    public void replayTimestampedAfterTermination() {
        TestScheduler scheduler = new TestScheduler();
        ReplaySubject<Integer> source = ReplaySubject.createWithTime(1, TimeUnit.SECONDS, scheduler);

        source.onNext(1);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        source.onNext(2);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        source.onNext(3);
        source.onComplete();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        Observer<Integer> o = TestHelper.mockObserver();

        source.subscribe(o);

        verify(o, never()).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onNext(3);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void replayTimestampedDirectly() {
        TestScheduler scheduler = new TestScheduler();
        ReplaySubject<Integer> source = ReplaySubject.createWithTime(1, TimeUnit.SECONDS, scheduler);

        source.onNext(1);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        Observer<Integer> o = TestHelper.mockObserver();

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

    @Test
    public void currentStateMethodsNormal() {
        ReplaySubject<Object> as = ReplaySubject.create();

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
    public void currentStateMethodsEmpty() {
        ReplaySubject<Object> as = ReplaySubject.create();

        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getThrowable());

        as.onComplete();

        assertFalse(as.hasThrowable());
        assertTrue(as.hasComplete());
        assertNull(as.getThrowable());
    }

    @Test
    public void currentStateMethodsError() {
        ReplaySubject<Object> as = ReplaySubject.create();

        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getThrowable());

        as.onError(new TestException());

        assertTrue(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertTrue(as.getThrowable() instanceof TestException);
    }

    @Test
    public void sizeAndHasAnyValueUnbounded() {
        ReplaySubject<Object> rs = ReplaySubject.create();

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
    public void sizeAndHasAnyValueEffectivelyUnbounded() {
        ReplaySubject<Object> rs = ReplaySubject.createUnbounded();

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
    public void sizeAndHasAnyValueUnboundedError() {
        ReplaySubject<Object> rs = ReplaySubject.create();

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
    public void sizeAndHasAnyValueEffectivelyUnboundedError() {
        ReplaySubject<Object> rs = ReplaySubject.createUnbounded();

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
    public void sizeAndHasAnyValueUnboundedEmptyError() {
        ReplaySubject<Object> rs = ReplaySubject.create();

        rs.onError(new TestException());

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());
    }

    @Test
    public void sizeAndHasAnyValueEffectivelyUnboundedEmptyError() {
        ReplaySubject<Object> rs = ReplaySubject.createUnbounded();

        rs.onError(new TestException());

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());
    }

    @Test
    public void sizeAndHasAnyValueUnboundedEmptyCompleted() {
        ReplaySubject<Object> rs = ReplaySubject.create();

        rs.onComplete();

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());
    }

    @Test
    public void sizeAndHasAnyValueEffectivelyUnboundedEmptyCompleted() {
        ReplaySubject<Object> rs = ReplaySubject.createUnbounded();

        rs.onComplete();

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());
    }

    @Test
    public void sizeAndHasAnyValueSizeBounded() {
        ReplaySubject<Object> rs = ReplaySubject.createWithSize(1);

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
    public void sizeAndHasAnyValueTimeBounded() {
        TestScheduler to = new TestScheduler();
        ReplaySubject<Object> rs = ReplaySubject.createWithTime(1, TimeUnit.SECONDS, to);

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());

        for (int i = 0; i < 1000; i++) {
            rs.onNext(i);
            assertEquals(1, rs.size());
            assertTrue(rs.hasValue());
            to.advanceTimeBy(2, TimeUnit.SECONDS);
            assertEquals(0, rs.size());
            assertFalse(rs.hasValue());
        }

        rs.onComplete();

        assertEquals(0, rs.size());
        assertFalse(rs.hasValue());
    }

    @Test
    public void getValues() {
        ReplaySubject<Object> rs = ReplaySubject.create();
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
    public void getValuesUnbounded() {
        ReplaySubject<Object> rs = ReplaySubject.createUnbounded();
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
    public void createInvalidCapacity() {
        try {
            ReplaySubject.create(-99);
            fail("Didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("capacityHint > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void createWithSizeInvalidCapacity() {
        try {
            ReplaySubject.createWithSize(-99);
            fail("Didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("maxSize > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void createWithTimeAndSizeInvalidCapacity() {
        try {
            ReplaySubject.createWithTimeAndSize(1, TimeUnit.DAYS, Schedulers.computation(), -99);
            fail("Didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("maxSize > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void hasSubscribers() {
        ReplaySubject<Integer> rp = ReplaySubject.create();

        assertFalse(rp.hasObservers());

        TestObserver<Integer> to = rp.test();

        assertTrue(rp.hasObservers());

        to.dispose();

        assertFalse(rp.hasObservers());
    }

    @Test
    public void peekStateUnbounded() {
        ReplaySubject<Integer> rp = ReplaySubject.create();

        rp.onNext(1);

        assertEquals((Integer)1, rp.getValue());

        assertEquals(1, rp.getValues()[0]);
    }

    @Test
    public void peekStateTimeAndSize() {
        ReplaySubject<Integer> rp = ReplaySubject.createWithTimeAndSize(1, TimeUnit.DAYS, Schedulers.computation(), 1);

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
        ReplaySubject<Integer> rp = ReplaySubject.createWithTimeAndSize(1, TimeUnit.DAYS, Schedulers.computation(), 1);

        assertNull(rp.getValue());

        assertEquals(0, rp.getValues().length);

        assertNull(rp.getValues(new Integer[2])[0]);

        rp.onComplete();

        assertNull(rp.getValue());

        assertEquals(0, rp.getValues().length);

        assertNull(rp.getValues(new Integer[2])[0]);

        rp = ReplaySubject.createWithTimeAndSize(1, TimeUnit.DAYS, Schedulers.computation(), 1);
        rp.onError(new TestException());

        assertNull(rp.getValue());

        assertEquals(0, rp.getValues().length);

        assertNull(rp.getValues(new Integer[2])[0]);
    }

    @Test
    public void peekStateTimeAndSizeValueExpired() {
        TestScheduler scheduler = new TestScheduler();
        ReplaySubject<Integer> rp = ReplaySubject.createWithTime(1, TimeUnit.DAYS, scheduler);

        assertNull(rp.getValue());
        assertNull(rp.getValues(new Integer[2])[0]);

        rp.onNext(2);

        assertEquals((Integer)2, rp.getValue());
        assertEquals(2, rp.getValues()[0]);

        scheduler.advanceTimeBy(2, TimeUnit.DAYS);

        assertNull(rp.getValue());
        assertEquals(0, rp.getValues().length);
        assertNull(rp.getValues(new Integer[2])[0]);
    }

    @Test
    public void capacityHint() {
        ReplaySubject<Integer> rp = ReplaySubject.create(8);

        for (int i = 0; i < 15; i++) {
            rp.onNext(i);
        }
        rp.onComplete();

        rp.test().assertResult(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
    }

    @Test
    public void subscribeCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestObserver<Integer> to = new TestObserver<>();

            final ReplaySubject<Integer> rp = ReplaySubject.create();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    rp.subscribe(to);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void subscribeAfterDone() {
        ReplaySubject<Integer> rp = ReplaySubject.create();
        rp.onComplete();

        Disposable bs = Disposable.empty();

        rp.onSubscribe(bs);

        assertTrue(bs.isDisposed());
    }

    @Test
    public void subscribeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final ReplaySubject<Integer> rp = ReplaySubject.create();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    rp.test();
                }
            };

            TestHelper.race(r1, r1);
        }
    }

    @Test
    public void cancelUpfront() {
        ReplaySubject<Integer> rp = ReplaySubject.create();
        rp.test();
        rp.test();

        TestObserver<Integer> to = rp.test(true);

        assertEquals(2, rp.observerCount());

        to.assertEmpty();
    }

    @Test
    public void cancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final ReplaySubject<Integer> rp = ReplaySubject.create();
            final TestObserver<Integer> to1 = rp.test();
            final TestObserver<Integer> to2 = rp.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to1.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to2.dispose();
                }
            };

            TestHelper.race(r1, r2);

            assertFalse(rp.hasObservers());
        }
    }

    @Test
    public void sizeboundReplayError() {
        ReplaySubject<Integer> rp = ReplaySubject.createWithSize(2);

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
        ReplaySubject<Integer> rp = ReplaySubject.createWithTimeAndSize(1, TimeUnit.DAYS, Schedulers.single(), 2);

        rp.onNext(1);
        rp.onNext(2);
        rp.onNext(3);
        rp.onNext(4);
        rp.onError(new TestException());

        rp.test()
        .assertFailure(TestException.class, 3, 4);
    }

    @Test
    public void timedSkipOld() {
        TestScheduler scheduler = new TestScheduler();

        ReplaySubject<Integer> rp = ReplaySubject.createWithTimeAndSize(1, TimeUnit.SECONDS, scheduler, 2);

        rp.onNext(1);
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        rp.test()
        .assertEmpty();
    }

    @Test
    public void takeSizeAndTime() {
        TestScheduler scheduler = new TestScheduler();

        ReplaySubject<Integer> rp = ReplaySubject.createWithTimeAndSize(1, TimeUnit.SECONDS, scheduler, 2);

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
        ReplaySubject<Integer> rp = ReplaySubject.createWithSize(2);

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

        final ReplaySubject<Integer> rp = ReplaySubject.createWithTimeAndSize(1, TimeUnit.SECONDS, scheduler, 2);

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (t == 1) {
                    rp.onNext(2);
                }
                super.onNext(t);
            }
        };

        rp.subscribe(to);

        rp.onNext(1);
        rp.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(ReplaySubject.create());

        TestHelper.checkDisposed(ReplaySubject.createUnbounded());

        TestHelper.checkDisposed(ReplaySubject.createWithSize(10));

        TestHelper.checkDisposed(ReplaySubject.createWithTimeAndSize(1, TimeUnit.SECONDS, Schedulers.single(), 10));
    }

    @Test
    public void timedNoOutdatedData() {
        TestScheduler scheduler = new TestScheduler();

        ReplaySubject<Integer> source = ReplaySubject.createWithTime(2, TimeUnit.SECONDS, scheduler);
        source.onNext(1);
        source.onComplete();

        source.test().assertResult(1);

        source.test().assertResult(1);

        scheduler.advanceTimeBy(3, TimeUnit.SECONDS);

        source.test().assertResult();
    }

    @Test
    public void noHeadRetentionCompleteSize() {
        ReplaySubject<Integer> source = ReplaySubject.createWithSize(1);

        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        SizeBoundReplayBuffer<Integer> buf = (SizeBoundReplayBuffer<Integer>)source.buffer;

        assertNull(buf.head.value);

        Object o = buf.head;

        source.cleanupBuffer();

        assertSame(o, buf.head);
    }

    @Test
    public void noHeadRetentionSize() {
        ReplaySubject<Integer> source = ReplaySubject.createWithSize(1);

        source.onNext(1);
        source.onNext(2);

        SizeBoundReplayBuffer<Integer> buf = (SizeBoundReplayBuffer<Integer>)source.buffer;

        assertNotNull(buf.head.value);

        source.cleanupBuffer();

        assertNull(buf.head.value);

        Object o = buf.head;

        source.cleanupBuffer();

        assertSame(o, buf.head);
    }

    @Test
    public void noHeadRetentionCompleteTime() {
        ReplaySubject<Integer> source = ReplaySubject.createWithTime(1, TimeUnit.MINUTES, Schedulers.computation());

        source.onNext(1);
        source.onNext(2);
        source.onComplete();

        SizeAndTimeBoundReplayBuffer<Integer> buf = (SizeAndTimeBoundReplayBuffer<Integer>)source.buffer;

        assertNull(buf.head.value);

        Object o = buf.head;

        source.cleanupBuffer();

        assertSame(o, buf.head);
    }

    @Test
    public void noHeadRetentionTime() {
        TestScheduler sch = new TestScheduler();

        ReplaySubject<Integer> source = ReplaySubject.createWithTime(1, TimeUnit.MILLISECONDS, sch);

        source.onNext(1);

        sch.advanceTimeBy(2, TimeUnit.MILLISECONDS);

        source.onNext(2);

        SizeAndTimeBoundReplayBuffer<Integer> buf = (SizeAndTimeBoundReplayBuffer<Integer>)source.buffer;

        assertNotNull(buf.head.value);

        source.cleanupBuffer();

        assertNull(buf.head.value);

        Object o = buf.head;

        source.cleanupBuffer();

        assertSame(o, buf.head);
    }

    @Test
    public void noBoundedRetentionViaThreadLocal() throws Exception {
        final ReplaySubject<byte[]> rs = ReplaySubject.createWithSize(1);

        Observable<byte[]> source = rs.take(1)
        .concatMap(new Function<byte[], Observable<byte[]>>() {
            @Override
            public Observable<byte[]> apply(byte[] v) throws Exception {
                return rs;
            }
        })
        .takeLast(1)
        ;

        System.out.println("Bounded Replay Leak check: Wait before GC");
        Thread.sleep(1000);

        System.out.println("Bounded Replay Leak check: GC");
        System.gc();

        Thread.sleep(500);

        final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage memHeap = memoryMXBean.getHeapMemoryUsage();
        long initial = memHeap.getUsed();

        System.out.printf("Bounded Replay Leak check: Starting: %.3f MB%n", initial / 1024.0 / 1024.0);

        final AtomicLong after = new AtomicLong();

        source.subscribe(new Consumer<byte[]>() {
            @Override
            public void accept(byte[] v) throws Exception {
                System.out.println("Bounded Replay Leak check: Wait before GC 2");
                Thread.sleep(1000);

                System.out.println("Bounded Replay Leak check:  GC 2");
                System.gc();

                Thread.sleep(500);

                after.set(memoryMXBean.getHeapMemoryUsage().getUsed());
            }
        });

        for (int i = 0; i < 200; i++) {
            rs.onNext(new byte[1024 * 1024]);
        }
        rs.onComplete();

        System.out.printf("Bounded Replay Leak check: After: %.3f MB%n", after.get() / 1024.0 / 1024.0);

        if (initial + 100 * 1024 * 1024 < after.get()) {
            Assert.fail("Bounded Replay Leak check: Memory leak detected: " + (initial / 1024.0 / 1024.0)
                    + " -> " + after.get() / 1024.0 / 1024.0);
        }
    }

    @Test
    public void timeAndSizeNoTerminalTruncationOnTimechange() {
        ReplaySubject<Integer> rs = ReplaySubject.createWithTimeAndSize(1, TimeUnit.SECONDS, new TimesteppingScheduler(), 1);

        TestObserver<Integer> to = rs.test();

        rs.onNext(1);
        rs.cleanupBuffer();
        rs.onComplete();

        to.assertNoErrors()
        .assertComplete();
    }

    @Test
    public void timeAndSizeNoTerminalTruncationOnTimechange2() {
        ReplaySubject<Integer> rs = ReplaySubject.createWithTimeAndSize(1, TimeUnit.SECONDS, new TimesteppingScheduler(), 1);

        TestObserver<Integer> to = rs.test();

        rs.onNext(1);
        rs.cleanupBuffer();
        rs.onNext(2);
        rs.cleanupBuffer();
        rs.onComplete();

        to.assertNoErrors()
        .assertComplete();
    }

    @Test
    public void timeAndSizeNoTerminalTruncationOnTimechange3() {
        ReplaySubject<Integer> rs = ReplaySubject.createWithTimeAndSize(1, TimeUnit.SECONDS, new TimesteppingScheduler(), 1);

        TestObserver<Integer> to = rs.test();

        rs.onNext(1);
        rs.onNext(2);
        rs.onComplete();

        to.assertNoErrors()
        .assertComplete();
    }

    @Test
    public void timeAndSizeNoTerminalTruncationOnTimechange4() {
        ReplaySubject<Integer> rs = ReplaySubject.createWithTimeAndSize(1, TimeUnit.SECONDS, new TimesteppingScheduler(), 10);

        TestObserver<Integer> to = rs.test();

        rs.onNext(1);
        rs.onNext(2);
        rs.onComplete();

        to.assertNoErrors()
        .assertComplete();
    }

    @Test
    public void timeAndSizeRemoveCorrectNumberOfOld() {
        TestScheduler scheduler = new TestScheduler();
        ReplaySubject<Integer> rs = ReplaySubject.createWithTimeAndSize(1, TimeUnit.SECONDS, scheduler, 2);

        rs.onNext(1);
        rs.onNext(2);
        rs.onNext(3); // remove 1 due to maxSize, size == 2

        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        rs.onNext(4); // remove 2 due to maxSize, remove 3 due to age, size == 1
        rs.onNext(5); // size == 2

        rs.test().assertValuesOnly(4, 5);
    }
}
