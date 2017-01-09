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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Flowable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.LongConsumer;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.*;

public class FlowableMergeDelayErrorTest {

    Subscriber<String> stringObserver;

    @Before
    public void before() {
        stringObserver = TestHelper.mockSubscriber();
    }

    @Test
    public void testErrorDelayed1() {
        final Flowable<String> o1 = Flowable.unsafeCreate(new TestErrorFlowable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
        final Flowable<String> o2 = Flowable.unsafeCreate(new TestErrorFlowable("one", "two", "three"));

        Flowable<String> m = Flowable.mergeDelayError(o1, o2);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(NullPointerException.class));
        verify(stringObserver, never()).onComplete();
        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, times(1)).onNext("two");
        verify(stringObserver, times(1)).onNext("three");
        verify(stringObserver, times(1)).onNext("four");
        verify(stringObserver, times(0)).onNext("five");
        // despite not expecting it ... we don't do anything to prevent it if the source Flowable keeps sending after onError
        // inner Flowable errors are considered terminal for that source
//        verify(stringObserver, times(1)).onNext("six");
        // inner Flowable errors are considered terminal for that source
    }

    @Test
    public void testErrorDelayed2() {
        final Flowable<String> o1 = Flowable.unsafeCreate(new TestErrorFlowable("one", "two", "three"));
        final Flowable<String> o2 = Flowable.unsafeCreate(new TestErrorFlowable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
        final Flowable<String> o3 = Flowable.unsafeCreate(new TestErrorFlowable("seven", "eight", null));
        final Flowable<String> o4 = Flowable.unsafeCreate(new TestErrorFlowable("nine"));

        Flowable<String> m = Flowable.mergeDelayError(o1, o2, o3, o4);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(CompositeException.class));
        verify(stringObserver, never()).onComplete();
        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, times(1)).onNext("two");
        verify(stringObserver, times(1)).onNext("three");
        verify(stringObserver, times(1)).onNext("four");
        verify(stringObserver, times(0)).onNext("five");
        // despite not expecting it ... we don't do anything to prevent it if the source Flowable keeps sending after onError
        // inner Flowable errors are considered terminal for that source
//        verify(stringObserver, times(1)).onNext("six");
        verify(stringObserver, times(1)).onNext("seven");
        verify(stringObserver, times(1)).onNext("eight");
        verify(stringObserver, times(1)).onNext("nine");
    }

    @Test
    public void testErrorDelayed3() {
        final Flowable<String> o1 = Flowable.unsafeCreate(new TestErrorFlowable("one", "two", "three"));
        final Flowable<String> o2 = Flowable.unsafeCreate(new TestErrorFlowable("four", "five", "six"));
        final Flowable<String> o3 = Flowable.unsafeCreate(new TestErrorFlowable("seven", "eight", null));
        final Flowable<String> o4 = Flowable.unsafeCreate(new TestErrorFlowable("nine"));

        Flowable<String> m = Flowable.mergeDelayError(o1, o2, o3, o4);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(NullPointerException.class));
        verify(stringObserver, never()).onComplete();
        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, times(1)).onNext("two");
        verify(stringObserver, times(1)).onNext("three");
        verify(stringObserver, times(1)).onNext("four");
        verify(stringObserver, times(1)).onNext("five");
        verify(stringObserver, times(1)).onNext("six");
        verify(stringObserver, times(1)).onNext("seven");
        verify(stringObserver, times(1)).onNext("eight");
        verify(stringObserver, times(1)).onNext("nine");
    }

    @Test
    public void testErrorDelayed4() {
        final Flowable<String> o1 = Flowable.unsafeCreate(new TestErrorFlowable("one", "two", "three"));
        final Flowable<String> o2 = Flowable.unsafeCreate(new TestErrorFlowable("four", "five", "six"));
        final Flowable<String> o3 = Flowable.unsafeCreate(new TestErrorFlowable("seven", "eight"));
        final Flowable<String> o4 = Flowable.unsafeCreate(new TestErrorFlowable("nine", null));

        Flowable<String> m = Flowable.mergeDelayError(o1, o2, o3, o4);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(NullPointerException.class));
        verify(stringObserver, never()).onComplete();
        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, times(1)).onNext("two");
        verify(stringObserver, times(1)).onNext("three");
        verify(stringObserver, times(1)).onNext("four");
        verify(stringObserver, times(1)).onNext("five");
        verify(stringObserver, times(1)).onNext("six");
        verify(stringObserver, times(1)).onNext("seven");
        verify(stringObserver, times(1)).onNext("eight");
        verify(stringObserver, times(1)).onNext("nine");
    }

    @Test
    public void testErrorDelayed4WithThreading() {
        final TestAsyncErrorFlowable o1 = new TestAsyncErrorFlowable("one", "two", "three");
        final TestAsyncErrorFlowable o2 = new TestAsyncErrorFlowable("four", "five", "six");
        final TestAsyncErrorFlowable o3 = new TestAsyncErrorFlowable("seven", "eight");
        // throw the error at the very end so no onComplete will be called after it
        final TestAsyncErrorFlowable o4 = new TestAsyncErrorFlowable("nine", null);

        Flowable<String> m = Flowable.mergeDelayError(Flowable.unsafeCreate(o1), Flowable.unsafeCreate(o2), Flowable.unsafeCreate(o3), Flowable.unsafeCreate(o4));
        m.subscribe(stringObserver);

        try {
            o1.t.join();
            o2.t.join();
            o3.t.join();
            o4.t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, times(1)).onNext("two");
        verify(stringObserver, times(1)).onNext("three");
        verify(stringObserver, times(1)).onNext("four");
        verify(stringObserver, times(1)).onNext("five");
        verify(stringObserver, times(1)).onNext("six");
        verify(stringObserver, times(1)).onNext("seven");
        verify(stringObserver, times(1)).onNext("eight");
        verify(stringObserver, times(1)).onNext("nine");
        verify(stringObserver, times(1)).onError(any(NullPointerException.class));
        verify(stringObserver, never()).onComplete();
    }

    @Test
    public void testCompositeErrorDelayed1() {
        final Flowable<String> o1 = Flowable.unsafeCreate(new TestErrorFlowable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
        final Flowable<String> o2 = Flowable.unsafeCreate(new TestErrorFlowable("one", "two", null));

        Flowable<String> m = Flowable.mergeDelayError(o1, o2);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(Throwable.class));
        verify(stringObserver, never()).onComplete();
        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, times(1)).onNext("two");
        verify(stringObserver, times(0)).onNext("three");
        verify(stringObserver, times(1)).onNext("four");
        verify(stringObserver, times(0)).onNext("five");
        // despite not expecting it ... we don't do anything to prevent it if the source Flowable keeps sending after onError
        // inner Flowable errors are considered terminal for that source
//        verify(stringObserver, times(1)).onNext("six");
    }

    @Test
    public void testCompositeErrorDelayed2() {
        final Flowable<String> o1 = Flowable.unsafeCreate(new TestErrorFlowable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
        final Flowable<String> o2 = Flowable.unsafeCreate(new TestErrorFlowable("one", "two", null));

        Flowable<String> m = Flowable.mergeDelayError(o1, o2);
        CaptureObserver w = new CaptureObserver();
        m.subscribe(w);

        assertNotNull(w.e);

        int size = ((CompositeException)w.e).size();
        if (size != 2) {
            w.e.printStackTrace();
        }
        assertEquals(2, size);

//        if (w.e instanceof CompositeException) {
//            assertEquals(2, ((CompositeException) w.e).getExceptions().size());
//            w.e.printStackTrace();
//        } else {
//            fail("Expecting CompositeException");
//        }

    }

    /**
     * The unit tests below are from OperationMerge and should ensure the normal merge functionality is correct.
     */

    @Test
    public void testMergeFlowableOfFlowables() {
        final Flowable<String> o1 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        final Flowable<String> o2 = Flowable.unsafeCreate(new TestSynchronousFlowable());

        Flowable<Flowable<String>> FlowableOfFlowables = Flowable.unsafeCreate(new Publisher<Flowable<String>>() {

            @Override
            public void subscribe(Subscriber<? super Flowable<String>> observer) {
                observer.onSubscribe(new BooleanSubscription());
                // simulate what would happen in a Flowable
                observer.onNext(o1);
                observer.onNext(o2);
                observer.onComplete();
            }

        });
        Flowable<String> m = Flowable.mergeDelayError(FlowableOfFlowables);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onComplete();
        verify(stringObserver, times(2)).onNext("hello");
    }

    @Test
    public void testMergeArray() {
        final Flowable<String> o1 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        final Flowable<String> o2 = Flowable.unsafeCreate(new TestSynchronousFlowable());

        Flowable<String> m = Flowable.mergeDelayError(o1, o2);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(2)).onNext("hello");
        verify(stringObserver, times(1)).onComplete();
    }

    @Test
    public void testMergeList() {
        final Flowable<String> o1 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        final Flowable<String> o2 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        List<Flowable<String>> listOfFlowables = new ArrayList<Flowable<String>>();
        listOfFlowables.add(o1);
        listOfFlowables.add(o2);

        Flowable<String> m = Flowable.mergeDelayError(Flowable.fromIterable(listOfFlowables));
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onComplete();
        verify(stringObserver, times(2)).onNext("hello");
    }

    @Test
    public void testMergeArrayWithThreading() {
        final TestASynchronousFlowable o1 = new TestASynchronousFlowable();
        final TestASynchronousFlowable o2 = new TestASynchronousFlowable();

        Flowable<String> m = Flowable.mergeDelayError(Flowable.unsafeCreate(o1), Flowable.unsafeCreate(o2));
        m.subscribe(stringObserver);

        try {
            o1.t.join();
            o2.t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(2)).onNext("hello");
        verify(stringObserver, times(1)).onComplete();
    }

    @Test(timeout = 1000L)
    public void testSynchronousError() {
        final Flowable<Flowable<String>> o1 = Flowable.error(new RuntimeException("unit test"));

        final CountDownLatch latch = new CountDownLatch(1);
        Flowable.mergeDelayError(o1).subscribe(new DefaultSubscriber<String>() {
            @Override
            public void onComplete() {
                fail("Expected onError path");
            }

            @Override
            public void onError(Throwable e) {
                latch.countDown();
            }

            @Override
            public void onNext(String s) {
                fail("Expected onError path");
            }
        });

        try {
            latch.await();
        } catch (InterruptedException ex) {
            fail("interrupted");
        }
    }

    private static class TestSynchronousFlowable implements Publisher<String> {

        @Override
        public void subscribe(Subscriber<? super String> observer) {
            observer.onSubscribe(new BooleanSubscription());
            observer.onNext("hello");
            observer.onComplete();
        }
    }

    private static class TestASynchronousFlowable implements Publisher<String> {
        Thread t;

        @Override
        public void subscribe(final Subscriber<? super String> observer) {
            observer.onSubscribe(new BooleanSubscription());
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    observer.onNext("hello");
                    observer.onComplete();
                }

            });
            t.start();
        }
    }

    private static class TestErrorFlowable implements Publisher<String> {

        String[] valuesToReturn;

        TestErrorFlowable(String... values) {
            valuesToReturn = values;
        }

        @Override
        public void subscribe(Subscriber<? super String> observer) {
            observer.onSubscribe(new BooleanSubscription());
            boolean errorThrown = false;
            for (String s : valuesToReturn) {
                if (s == null) {
                    System.out.println("throwing exception");
                    observer.onError(new NullPointerException());
                    errorThrown = true;
                    // purposefully not returning here so it will continue calling onNext
                    // so that we also test that we handle bad sequences like this
                } else {
                    observer.onNext(s);
                }
            }
            if (!errorThrown) {
                observer.onComplete();
            }
        }
    }

    private static class TestAsyncErrorFlowable implements Publisher<String> {

        String[] valuesToReturn;

        TestAsyncErrorFlowable(String... values) {
            valuesToReturn = values;
        }

        Thread t;

        @Override
        public void subscribe(final Subscriber<? super String> observer) {
            observer.onSubscribe(new BooleanSubscription());
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    for (String s : valuesToReturn) {
                        if (s == null) {
                            System.out.println("throwing exception");
                            try {
                                Thread.sleep(100);
                            } catch (Throwable e) {

                            }
                            observer.onError(new NullPointerException());
                            return;
                        } else {
                            observer.onNext(s);
                        }
                    }
                    System.out.println("subscription complete");
                    observer.onComplete();
                }

            });
            t.start();
        }
    }

    private static class CaptureObserver extends DefaultSubscriber<String> {
        volatile Throwable e;

        @Override
        public void onComplete() {

        }

        @Override
        public void onError(Throwable e) {
            this.e = e;
        }

        @Override
        public void onNext(String args) {

        }

    }
    @Test
    @Ignore("Subscribers should not throw")
    public void testMergeSourceWhichDoesntPropagateExceptionBack() {
        Flowable<Integer> source = Flowable.unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> t1) {
                t1.onSubscribe(new BooleanSubscription());
                try {
                    t1.onNext(0);
                } catch (Throwable swallow) {

                }
                t1.onNext(1);
                t1.onComplete();
            }
        });

        Flowable<Integer> result = Flowable.mergeDelayError(source, Flowable.just(2));

        final Subscriber<Integer> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);

        result.subscribe(new DefaultSubscriber<Integer>() {
            int calls;
            @Override
            public void onNext(Integer t) {
                if (calls++ == 0) {
                    throw new TestException();
                }
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

        /*
         * If the child onNext throws, why would we keep accepting values from
         * other sources?
         */
        inOrder.verify(o).onNext(2);
        inOrder.verify(o, never()).onNext(0);
        inOrder.verify(o, never()).onNext(1);
        inOrder.verify(o, never()).onNext(anyInt());
        inOrder.verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
    }

    @Test
    public void testErrorInParentFlowable() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.mergeDelayError(
                Flowable.just(Flowable.just(1), Flowable.just(2))
                        .startWith(Flowable.<Integer> error(new RuntimeException()))
                ).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertTerminated();
        ts.assertValues(1, 2);
        assertEquals(1, ts.errorCount());

    }

    @Test
    public void testErrorInParentFlowableDelayed() throws Exception {
        for (int i = 0; i < 50; i++) {
            final TestASynchronous1sDelayedFlowable o1 = new TestASynchronous1sDelayedFlowable();
            final TestASynchronous1sDelayedFlowable o2 = new TestASynchronous1sDelayedFlowable();
            Flowable<Flowable<String>> parentFlowable = Flowable.unsafeCreate(new Publisher<Flowable<String>>() {
                @Override
                public void subscribe(Subscriber<? super Flowable<String>> op) {
                    op.onSubscribe(new BooleanSubscription());
                    op.onNext(Flowable.unsafeCreate(o1));
                    op.onNext(Flowable.unsafeCreate(o2));
                    op.onError(new NullPointerException("throwing exception in parent"));
                }
            });

            Subscriber<String> stringObserver = TestHelper.mockSubscriber();

            TestSubscriber<String> ts = new TestSubscriber<String>(stringObserver);
            Flowable<String> m = Flowable.mergeDelayError(parentFlowable);
            m.subscribe(ts);
            System.out.println("testErrorInParentFlowableDelayed | " + i);
            ts.awaitTerminalEvent(2000, TimeUnit.MILLISECONDS);
            ts.assertTerminated();

            verify(stringObserver, times(2)).onNext("hello");
            verify(stringObserver, times(1)).onError(any(NullPointerException.class));
            verify(stringObserver, never()).onComplete();
        }
    }

    private static class TestASynchronous1sDelayedFlowable implements Publisher<String> {
        Thread t;

        @Override
        public void subscribe(final Subscriber<? super String> observer) {
            observer.onSubscribe(new BooleanSubscription());
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        observer.onError(e);
                    }
                    observer.onNext("hello");
                    observer.onComplete();
                }

            });
            t.start();
        }
    }
    @Test
    public void testDelayErrorMaxConcurrent() {
        final List<Long> requests = new ArrayList<Long>();
        Flowable<Integer> source = Flowable.mergeDelayError(Flowable.just(
                Flowable.just(1).hide(),
                Flowable.<Integer>error(new TestException()))
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long t1) {
                        requests.add(t1);
                    }
                }), 1);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.subscribe(ts);

        ts.assertValue(1);
        ts.assertTerminated();
        ts.assertError(TestException.class);
        assertEquals(Arrays.asList(1L, 1L, 1L), requests);
    }

    // This is pretty much a clone of testMergeList but with the overloaded MergeDelayError for Iterables
    @Test
    public void mergeIterable() {
        final Flowable<String> o1 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        final Flowable<String> o2 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        List<Flowable<String>> listOfFlowables = new ArrayList<Flowable<String>>();
        listOfFlowables.add(o1);
        listOfFlowables.add(o2);

        Flowable<String> m = Flowable.mergeDelayError(listOfFlowables);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onComplete();
        verify(stringObserver, times(2)).onNext("hello");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void iterableMaxConcurrent() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> ps1 = PublishProcessor.create();
        PublishProcessor<Integer> ps2 = PublishProcessor.create();

        Flowable.mergeDelayError(Arrays.asList(ps1, ps2), 1).subscribe(ts);

        assertTrue("ps1 has no subscribers?!", ps1.hasSubscribers());
        assertFalse("ps2 has subscribers?!", ps2.hasSubscribers());

        ps1.onNext(1);
        ps1.onComplete();

        assertFalse("ps1 has subscribers?!", ps1.hasSubscribers());
        assertTrue("ps2 has no subscribers?!", ps2.hasSubscribers());

        ps2.onNext(2);
        ps2.onComplete();

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void iterableMaxConcurrentError() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> ps1 = PublishProcessor.create();
        PublishProcessor<Integer> ps2 = PublishProcessor.create();

        Flowable.mergeDelayError(Arrays.asList(ps1, ps2), 1).subscribe(ts);

        assertTrue("ps1 has no subscribers?!", ps1.hasSubscribers());
        assertFalse("ps2 has subscribers?!", ps2.hasSubscribers());

        ps1.onNext(1);
        ps1.onError(new TestException());

        assertFalse("ps1 has subscribers?!", ps1.hasSubscribers());
        assertTrue("ps2 has no subscribers?!", ps2.hasSubscribers());

        ps2.onNext(2);
        ps2.onError(new TestException());

        ts.assertValues(1, 2);
        ts.assertError(CompositeException.class);
        ts.assertNotComplete();

        CompositeException ce = (CompositeException)ts.errors().get(0);

        assertEquals(2, ce.getExceptions().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    @Ignore("No 2-9 parameter mergeDelayError() overloads")
    public void mergeMany() throws Exception {
        for (int i = 2; i < 10; i++) {
            Class<?>[] clazz = new Class[i];
            Arrays.fill(clazz, Flowable.class);

            Flowable<Integer>[] obs = new Flowable[i];
            Arrays.fill(obs, Flowable.just(1));

            Integer[] expected = new Integer[i];
            Arrays.fill(expected, 1);

            Method m = Flowable.class.getMethod("mergeDelayError", clazz);

            TestSubscriber<Integer> ts = TestSubscriber.create();

            ((Flowable<Integer>)m.invoke(null, (Object[])obs)).subscribe(ts);

            ts.assertValues(expected);
            ts.assertNoErrors();
            ts.assertComplete();
        }
    }

    static <T> Flowable<T> withError(Flowable<T> source) {
        return source.concatWith(Flowable.<T>error(new TestException()));
    }

    @SuppressWarnings("unchecked")
    @Test
    @Ignore("No 2-9 parameter mergeDelayError() overloads")
    public void mergeManyError() throws Exception {
        for (int i = 2; i < 10; i++) {
            Class<?>[] clazz = new Class[i];
            Arrays.fill(clazz, Flowable.class);

            Flowable<Integer>[] obs = new Flowable[i];
            for (int j = 0; j < i; j++) {
                obs[j] = withError(Flowable.just(1));
            }

            Integer[] expected = new Integer[i];
            Arrays.fill(expected, 1);

            Method m = Flowable.class.getMethod("mergeDelayError", clazz);

            TestSubscriber<Integer> ts = TestSubscriber.create();

            ((Flowable<Integer>)m.invoke(null, (Object[])obs)).subscribe(ts);

            ts.assertValues(expected);
            ts.assertError(CompositeException.class);
            ts.assertNotComplete();

            CompositeException ce = (CompositeException)ts.errors().get(0);

            assertEquals(i, ce.getExceptions().size());
        }
    }

    @Test
    public void array() {
        for (int i = 1; i < 100; i++) {

            @SuppressWarnings("unchecked")
            Flowable<Integer>[] sources = new Flowable[i];
            Arrays.fill(sources, Flowable.just(1));
            Integer[] expected = new Integer[i];
            for (int j = 0; j < i; j++) {
                expected[j] = 1;
            }

            Flowable.mergeArrayDelayError(sources)
            .test()
            .assertResult(expected);
        }
    }


    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayDelayError() {
        Flowable.mergeArrayDelayError(Flowable.just(1), Flowable.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableDelayErrorWithError() {
        Flowable.mergeDelayError(
                Arrays.asList(Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException())),
                Flowable.just(2)))
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void mergeDelayError() {
        Flowable.mergeDelayError(
                Flowable.just(Flowable.just(1),
                Flowable.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void mergeDelayErrorWithError() {
        Flowable.mergeDelayError(
                Flowable.just(Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException())),
                Flowable.just(2)))
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void mergeDelayErrorMaxConcurrency() {
        Flowable.mergeDelayError(
                Flowable.just(Flowable.just(1),
                Flowable.just(2)), 1)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void mergeDelayErrorWithErrorMaxConcurrency() {
        Flowable.mergeDelayError(
                Flowable.just(Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException())),
                Flowable.just(2)), 1)
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableDelayErrorMaxConcurrency() {
        Flowable.mergeDelayError(
                Arrays.asList(Flowable.just(1),
                Flowable.just(2)), 1)
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableDelayErrorWithErrorMaxConcurrency() {
        Flowable.mergeDelayError(
                Arrays.asList(Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException())),
                Flowable.just(2)), 1)
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void mergeDelayError3() {
        Flowable.mergeDelayError(
                Flowable.just(1),
                Flowable.just(2),
                Flowable.just(3)
        )
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void mergeDelayError3WithError() {
        Flowable.mergeDelayError(
                Flowable.just(1),
                Flowable.just(2).concatWith(Flowable.<Integer>error(new TestException())),
                Flowable.just(3)
        )
        .test()
        .assertFailure(TestException.class, 1, 2, 3);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableDelayError() {
        Flowable.mergeDelayError(Arrays.asList(Flowable.just(1), Flowable.just(2)))
        .test()
        .assertResult(1, 2);
    }
}
