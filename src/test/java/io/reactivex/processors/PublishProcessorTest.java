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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.*;

public class PublishProcessorTest extends FlowableProcessorTest<Object> {

    @Override
    protected FlowableProcessor<Object> create() {
        return PublishProcessor.create();
    }

    @Test
    public void testCompleted() {
        PublishProcessor<String> subject = PublishProcessor.create();

        Subscriber<String> observer = TestHelper.mockSubscriber();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onComplete();

        Subscriber<String> anotherSubscriber = TestHelper.mockSubscriber();
        subject.subscribe(anotherSubscriber);

        subject.onNext("four");
        subject.onComplete();
        subject.onError(new Throwable());

        assertCompletedSubscriber(observer);
        // todo bug?            assertNeverSubscriber(anotherSubscriber);
    }

    @Test
    public void testCompletedStopsEmittingData() {
        PublishProcessor<Object> channel = PublishProcessor.create();
        Subscriber<Object> observerA = TestHelper.mockSubscriber();
        Subscriber<Object> observerB = TestHelper.mockSubscriber();
        Subscriber<Object> observerC = TestHelper.mockSubscriber();

        TestSubscriber<Object> ts = new TestSubscriber<Object>(observerA);

        channel.subscribe(ts);
        channel.subscribe(observerB);

        InOrder inOrderA = inOrder(observerA);
        InOrder inOrderB = inOrder(observerB);
        InOrder inOrderC = inOrder(observerC);

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

    private void assertCompletedSubscriber(Subscriber<String> observer) {
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testError() {
        PublishProcessor<String> subject = PublishProcessor.create();

        Subscriber<String> observer = TestHelper.mockSubscriber();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onError(testException);

        Subscriber<String> anotherSubscriber = TestHelper.mockSubscriber();
        subject.subscribe(anotherSubscriber);

        subject.onNext("four");
        subject.onError(new Throwable());
        subject.onComplete();

        assertErrorSubscriber(observer);
        // todo bug?            assertNeverSubscriber(anotherSubscriber);
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
        PublishProcessor<String> subject = PublishProcessor.create();

        Subscriber<String> observer = TestHelper.mockSubscriber();
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");

        assertObservedUntilTwo(observer);

        Subscriber<String> anotherSubscriber = TestHelper.mockSubscriber();
        subject.subscribe(anotherSubscriber);

        subject.onNext("three");
        subject.onComplete();

        assertCompletedSubscriber(observer);
        assertCompletedStartingWithThreeSubscriber(anotherSubscriber);
    }

    private void assertCompletedStartingWithThreeSubscriber(Subscriber<String> observer) {
        verify(observer, Mockito.never()).onNext("one");
        verify(observer, Mockito.never()).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testUnsubscribeFirstSubscriber() {
        PublishProcessor<String> subject = PublishProcessor.create();

        Subscriber<String> observer = TestHelper.mockSubscriber();
        TestSubscriber<String> ts = new TestSubscriber<String>(observer);
        subject.subscribe(ts);

        subject.onNext("one");
        subject.onNext("two");

        ts.dispose();
        assertObservedUntilTwo(observer);

        Subscriber<String> anotherSubscriber = TestHelper.mockSubscriber();
        subject.subscribe(anotherSubscriber);

        subject.onNext("three");
        subject.onComplete();

        assertObservedUntilTwo(observer);
        assertCompletedStartingWithThreeSubscriber(anotherSubscriber);
    }

    private void assertObservedUntilTwo(Subscriber<String> observer) {
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, Mockito.never()).onComplete();
    }

    @Test
    public void testNestedSubscribe() {
        final PublishProcessor<Integer> s = PublishProcessor.create();

        final AtomicInteger countParent = new AtomicInteger();
        final AtomicInteger countChildren = new AtomicInteger();
        final AtomicInteger countTotal = new AtomicInteger();

        final ArrayList<String> list = new ArrayList<String>();

        s.flatMap(new Function<Integer, Flowable<String>>() {

            @Override
            public Flowable<String> apply(final Integer v) {
                countParent.incrementAndGet();

                // then subscribe to subject again (it will not receive the previous value)
                return s.map(new Function<Integer, String>() {

                    @Override
                    public String apply(Integer v2) {
                        countChildren.incrementAndGet();
                        return "Parent: " + v + " Child: " + v2;
                    }

                });
            }

        }).subscribe(new Consumer<String>() {

            @Override
            public void accept(String v) {
                countTotal.incrementAndGet();
                list.add(v);
            }

        });

        for (int i = 0; i < 10; i++) {
            s.onNext(i);
        }
        s.onComplete();

        //            System.out.println("countParent: " + countParent.get());
        //            System.out.println("countChildren: " + countChildren.get());
        //            System.out.println("countTotal: " + countTotal.get());

        // 9+8+7+6+5+4+3+2+1+0 == 45
        assertEquals(45, list.size());
    }

    /**
     * Should be able to unsubscribe all Subscribers, have it stop emitting, then subscribe new ones and it start emitting again.
     */
    @Test
    public void testReSubscribe() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();

        Subscriber<Integer> o1 = TestHelper.mockSubscriber();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(o1);
        ps.subscribe(ts);

        // emit
        ps.onNext(1);

        // validate we got it
        InOrder inOrder1 = inOrder(o1);
        inOrder1.verify(o1, times(1)).onNext(1);
        inOrder1.verifyNoMoreInteractions();

        // unsubscribe
        ts.dispose();

        // emit again but nothing will be there to receive it
        ps.onNext(2);

        Subscriber<Integer> o2 = TestHelper.mockSubscriber();
        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>(o2);
        ps.subscribe(ts2);

        // emit
        ps.onNext(3);

        // validate we got it
        InOrder inOrder2 = inOrder(o2);
        inOrder2.verify(o2, times(1)).onNext(3);
        inOrder2.verifyNoMoreInteractions();

        ts2.dispose();
    }

    private final Throwable testException = new Throwable();

    @Test(timeout = 1000)
    public void testUnsubscriptionCase() {
        PublishProcessor<String> src = PublishProcessor.create();

        for (int i = 0; i < 10; i++) {
            final Subscriber<Object> o = TestHelper.mockSubscriber();
            InOrder inOrder = inOrder(o);
            String v = "" + i;
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
            src.onNext(v);

            inOrder.verify(o).onNext(v + ", " + v);
            inOrder.verify(o).onComplete();
            verify(o, never()).onError(any(Throwable.class));
        }
    }


    // FIXME RS subscribers are not allowed to throw
//    @Test
//    public void testOnErrorThrowsDoesntPreventDelivery() {
//        PublishSubject<String> ps = PublishSubject.create();
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
//        assertEquals(1, ts.getOnErrorEvents().size());
//    }

    // FIXME RS subscribers are not allowed to throw
//    /**
//     * This one has multiple failures so should get a CompositeException
//     */
//    @Test
//    public void testOnErrorThrowsDoesntPreventDelivery2() {
//        PublishSubject<String> ps = PublishSubject.create();
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
        PublishProcessor<Object> as = PublishProcessor.create();

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
        PublishProcessor<Object> as = PublishProcessor.create();

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
        PublishProcessor<Object> as = PublishProcessor.create();

        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getThrowable());

        as.onError(new TestException());

        assertTrue(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertTrue(as.getThrowable() instanceof TestException);
    }

    @Test
    public void subscribeTo() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        Flowable.range(1, 10).subscribe(pp);

        assertTrue(pp.hasComplete());

        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        pp2.subscribe(pp);

        assertFalse(pp2.hasSubscribers());
    }

    @Test
    public void requestValidation() {
        TestHelper.assertBadRequestReported(PublishProcessor.create());
    }

    @Test
    public void crossCancel() {
        final TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>();
        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                ts1.cancel();
            }
        };

        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.subscribe(ts2);
        pp.subscribe(ts1);

        pp.onNext(1);

        ts2.assertValue(1);

        ts1.assertNoValues();
    }

    @Test
    public void crossCancelOnError() {
        final TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>();
        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable t) {
                super.onError(t);
                ts1.cancel();
            }
        };

        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.subscribe(ts2);
        pp.subscribe(ts1);

        pp.onError(new TestException());

        ts2.assertError(TestException.class);

        ts1.assertNoErrors();
    }

    @Test
    public void crossCancelOnComplete() {
        final TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>();
        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>() {
            @Override
            public void onComplete() {
                super.onComplete();
                ts1.cancel();
            }
        };

        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.subscribe(ts2);
        pp.subscribe(ts1);

        pp.onComplete();

        ts2.assertComplete();

        ts1.assertNotComplete();
    }

    @Test
    public void backpressureOverflow() {

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.test(0L);

        pp.onNext(1);

        ts.assertNoValues()
        .assertNotComplete()
        .assertError(MissingBackpressureException.class)
        ;
    }

    @Test
    public void onSubscribeCancelsImmediately() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.test();

        pp.subscribe(new FlowableSubscriber<Integer>() {

            @Override
            public void onSubscribe(Subscription s) {
                s.cancel();
            }

            @Override
            public void onNext(Integer t) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }

        });

        ts.cancel();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void terminateRace() throws Exception {

        for (int i = 0; i < 100; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            TestSubscriber<Integer> ts = pp.test();

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };

            TestHelper.race(task, task, Schedulers.io());

            ts
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult();
        }
    }

    @Test
    public void addRemoveRance() throws Exception {

        for (int i = 0; i < 100; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = pp.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.subscribe();
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2, Schedulers.io());
        }
    }

    @Test
    public void offer() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.test(0);

        assertFalse(pp.offer(1));

        ts.request(1);

        assertTrue(pp.offer(1));

        assertFalse(pp.offer(2));

        ts.cancel();

        assertTrue(pp.offer(2));

        ts = pp.test(0);

        assertTrue(pp.offer(null));

        ts.assertFailure(NullPointerException.class);

        assertTrue(pp.hasThrowable());
        assertTrue(pp.getThrowable().toString(), pp.getThrowable() instanceof NullPointerException);
    }

    @Test
    public void offerAsync() throws Exception {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        Schedulers.single().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                while (!pp.hasSubscribers()) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }

                for (int i = 1; i <= 10; i++) {
                    while (!pp.offer(i)) { }
                }
                pp.onComplete();
            }
        });

        Thread.sleep(1);

        pp.test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }
}
