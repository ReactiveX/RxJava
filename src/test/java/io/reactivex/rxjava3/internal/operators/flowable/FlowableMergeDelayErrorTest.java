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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.LongConsumer;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableMergeDelayErrorTest extends RxJavaTest {

    Subscriber<String> stringSubscriber;

    @Before
    public void before() {
        stringSubscriber = TestHelper.mockSubscriber();
    }

    @Test
    public void errorDelayed1() {
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestErrorFlowable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestErrorFlowable("one", "two", "three"));

        Flowable<String> m = Flowable.mergeDelayError(f1, f2);
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, times(1)).onError(any(NullPointerException.class));
        verify(stringSubscriber, never()).onComplete();
        verify(stringSubscriber, times(1)).onNext("one");
        verify(stringSubscriber, times(1)).onNext("two");
        verify(stringSubscriber, times(1)).onNext("three");
        verify(stringSubscriber, times(1)).onNext("four");
        verify(stringSubscriber, times(0)).onNext("five");
        // despite not expecting it ... we don't do anything to prevent it if the source Flowable keeps sending after onError
        // inner Flowable errors are considered terminal for that source
//        verify(stringSubscriber, times(1)).onNext("six");
        // inner Flowable errors are considered terminal for that source
    }

    @Test
    public void errorDelayed2() {
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestErrorFlowable("one", "two", "three"));
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestErrorFlowable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
        final Flowable<String> f3 = Flowable.unsafeCreate(new TestErrorFlowable("seven", "eight", null));
        final Flowable<String> f4 = Flowable.unsafeCreate(new TestErrorFlowable("nine"));

        Flowable<String> m = Flowable.mergeDelayError(f1, f2, f3, f4);
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, times(1)).onError(any(CompositeException.class));
        verify(stringSubscriber, never()).onComplete();
        verify(stringSubscriber, times(1)).onNext("one");
        verify(stringSubscriber, times(1)).onNext("two");
        verify(stringSubscriber, times(1)).onNext("three");
        verify(stringSubscriber, times(1)).onNext("four");
        verify(stringSubscriber, times(0)).onNext("five");
        // despite not expecting it ... we don't do anything to prevent it if the source Flowable keeps sending after onError
        // inner Flowable errors are considered terminal for that source
//        verify(stringSubscriber, times(1)).onNext("six");
        verify(stringSubscriber, times(1)).onNext("seven");
        verify(stringSubscriber, times(1)).onNext("eight");
        verify(stringSubscriber, times(1)).onNext("nine");
    }

    @Test
    public void errorDelayed3() {
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestErrorFlowable("one", "two", "three"));
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestErrorFlowable("four", "five", "six"));
        final Flowable<String> f3 = Flowable.unsafeCreate(new TestErrorFlowable("seven", "eight", null));
        final Flowable<String> f4 = Flowable.unsafeCreate(new TestErrorFlowable("nine"));

        Flowable<String> m = Flowable.mergeDelayError(f1, f2, f3, f4);
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, times(1)).onError(any(NullPointerException.class));
        verify(stringSubscriber, never()).onComplete();
        verify(stringSubscriber, times(1)).onNext("one");
        verify(stringSubscriber, times(1)).onNext("two");
        verify(stringSubscriber, times(1)).onNext("three");
        verify(stringSubscriber, times(1)).onNext("four");
        verify(stringSubscriber, times(1)).onNext("five");
        verify(stringSubscriber, times(1)).onNext("six");
        verify(stringSubscriber, times(1)).onNext("seven");
        verify(stringSubscriber, times(1)).onNext("eight");
        verify(stringSubscriber, times(1)).onNext("nine");
    }

    @Test
    public void errorDelayed4() {
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestErrorFlowable("one", "two", "three"));
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestErrorFlowable("four", "five", "six"));
        final Flowable<String> f3 = Flowable.unsafeCreate(new TestErrorFlowable("seven", "eight"));
        final Flowable<String> f4 = Flowable.unsafeCreate(new TestErrorFlowable("nine", null));

        Flowable<String> m = Flowable.mergeDelayError(f1, f2, f3, f4);
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, times(1)).onError(any(NullPointerException.class));
        verify(stringSubscriber, never()).onComplete();
        verify(stringSubscriber, times(1)).onNext("one");
        verify(stringSubscriber, times(1)).onNext("two");
        verify(stringSubscriber, times(1)).onNext("three");
        verify(stringSubscriber, times(1)).onNext("four");
        verify(stringSubscriber, times(1)).onNext("five");
        verify(stringSubscriber, times(1)).onNext("six");
        verify(stringSubscriber, times(1)).onNext("seven");
        verify(stringSubscriber, times(1)).onNext("eight");
        verify(stringSubscriber, times(1)).onNext("nine");
    }

    @Test
    public void errorDelayed4WithThreading() {
        final TestAsyncErrorFlowable f1 = new TestAsyncErrorFlowable("one", "two", "three");
        final TestAsyncErrorFlowable f2 = new TestAsyncErrorFlowable("four", "five", "six");
        final TestAsyncErrorFlowable f3 = new TestAsyncErrorFlowable("seven", "eight");
        // throw the error at the very end so no onComplete will be called after it
        final TestAsyncErrorFlowable f4 = new TestAsyncErrorFlowable("nine", null);

        Flowable<String> m = Flowable.mergeDelayError(Flowable.unsafeCreate(f1), Flowable.unsafeCreate(f2), Flowable.unsafeCreate(f3), Flowable.unsafeCreate(f4));
        m.subscribe(stringSubscriber);

        try {
            f1.t.join();
            f2.t.join();
            f3.t.join();
            f4.t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        verify(stringSubscriber, times(1)).onNext("one");
        verify(stringSubscriber, times(1)).onNext("two");
        verify(stringSubscriber, times(1)).onNext("three");
        verify(stringSubscriber, times(1)).onNext("four");
        verify(stringSubscriber, times(1)).onNext("five");
        verify(stringSubscriber, times(1)).onNext("six");
        verify(stringSubscriber, times(1)).onNext("seven");
        verify(stringSubscriber, times(1)).onNext("eight");
        verify(stringSubscriber, times(1)).onNext("nine");
        verify(stringSubscriber, times(1)).onError(any(NullPointerException.class));
        verify(stringSubscriber, never()).onComplete();
    }

    @Test
    public void compositeErrorDelayed1() {
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestErrorFlowable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestErrorFlowable("one", "two", null));

        Flowable<String> m = Flowable.mergeDelayError(f1, f2);
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, times(1)).onError(any(Throwable.class));
        verify(stringSubscriber, never()).onComplete();
        verify(stringSubscriber, times(1)).onNext("one");
        verify(stringSubscriber, times(1)).onNext("two");
        verify(stringSubscriber, times(0)).onNext("three");
        verify(stringSubscriber, times(1)).onNext("four");
        verify(stringSubscriber, times(0)).onNext("five");
        // despite not expecting it ... we don't do anything to prevent it if the source Flowable keeps sending after onError
        // inner Flowable errors are considered terminal for that source
//        verify(stringSubscriber, times(1)).onNext("six");
    }

    @Test
    public void compositeErrorDelayed2() {
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestErrorFlowable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestErrorFlowable("one", "two", null));

        Flowable<String> m = Flowable.mergeDelayError(f1, f2);
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
    public void mergeFlowableOfFlowables() {
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestSynchronousFlowable());

        Flowable<Flowable<String>> flowableOfFlowables = Flowable.unsafeCreate(new Publisher<Flowable<String>>() {

            @Override
            public void subscribe(Subscriber<? super Flowable<String>> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                // simulate what would happen in a Flowable
                subscriber.onNext(f1);
                subscriber.onNext(f2);
                subscriber.onComplete();
            }

        });
        Flowable<String> m = Flowable.mergeDelayError(flowableOfFlowables);
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, never()).onError(any(Throwable.class));
        verify(stringSubscriber, times(1)).onComplete();
        verify(stringSubscriber, times(2)).onNext("hello");
    }

    @Test
    public void mergeArray() {
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestSynchronousFlowable());

        Flowable<String> m = Flowable.mergeDelayError(f1, f2);
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, never()).onError(any(Throwable.class));
        verify(stringSubscriber, times(2)).onNext("hello");
        verify(stringSubscriber, times(1)).onComplete();
    }

    @Test
    public void mergeList() {
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        List<Flowable<String>> listOfFlowables = new ArrayList<>();
        listOfFlowables.add(f1);
        listOfFlowables.add(f2);

        Flowable<String> m = Flowable.mergeDelayError(Flowable.fromIterable(listOfFlowables));
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, never()).onError(any(Throwable.class));
        verify(stringSubscriber, times(1)).onComplete();
        verify(stringSubscriber, times(2)).onNext("hello");
    }

    @Test
    public void mergeArrayWithThreading() {
        final TestASynchronousFlowable f1 = new TestASynchronousFlowable();
        final TestASynchronousFlowable f2 = new TestASynchronousFlowable();

        Flowable<String> m = Flowable.mergeDelayError(Flowable.unsafeCreate(f1), Flowable.unsafeCreate(f2));
        m.subscribe(stringSubscriber);

        try {
            f1.t.join();
            f2.t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        verify(stringSubscriber, never()).onError(any(Throwable.class));
        verify(stringSubscriber, times(2)).onNext("hello");
        verify(stringSubscriber, times(1)).onComplete();
    }

    @Test
    public void synchronousError() {
        final Flowable<Flowable<String>> f1 = Flowable.error(new RuntimeException("unit test"));

        final CountDownLatch latch = new CountDownLatch(1);
        Flowable.mergeDelayError(f1).subscribe(new DefaultSubscriber<String>() {
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
        public void subscribe(Subscriber<? super String> subscriber) {
            subscriber.onSubscribe(new BooleanSubscription());
            subscriber.onNext("hello");
            subscriber.onComplete();
        }
    }

    private static class TestASynchronousFlowable implements Publisher<String> {
        Thread t;

        @Override
        public void subscribe(final Subscriber<? super String> subscriber) {
            subscriber.onSubscribe(new BooleanSubscription());
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    subscriber.onNext("hello");
                    subscriber.onComplete();
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
        public void subscribe(Subscriber<? super String> subscriber) {
            subscriber.onSubscribe(new BooleanSubscription());
            boolean errorThrown = false;
            for (String s : valuesToReturn) {
                if (s == null) {
                    System.out.println("throwing exception");
                    subscriber.onError(new NullPointerException());
                    errorThrown = true;
                    // purposefully not returning here so it will continue calling onNext
                    // so that we also test that we handle bad sequences like this
                } else {
                    subscriber.onNext(s);
                }
            }
            if (!errorThrown) {
                subscriber.onComplete();
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
        public void subscribe(final Subscriber<? super String> subscriber) {
            subscriber.onSubscribe(new BooleanSubscription());
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
                            subscriber.onError(new NullPointerException());
                            return;
                        } else {
                            subscriber.onNext(s);
                        }
                    }
                    System.out.println("subscription complete");
                    subscriber.onComplete();
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
    public void errorInParentFlowable() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        Flowable.mergeDelayError(
                Flowable.just(Flowable.just(1), Flowable.just(2))
                        .startWithItem(Flowable.<Integer> error(new RuntimeException()))
                ).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertTerminated();
        ts.assertValues(1, 2);
        assertEquals(1, ts.errors().size());

    }

    @Test
    public void errorInParentFlowableDelayed() throws Exception {
        for (int i = 0; i < 50; i++) {
            final TestASynchronous1sDelayedFlowable f1 = new TestASynchronous1sDelayedFlowable();
            final TestASynchronous1sDelayedFlowable f2 = new TestASynchronous1sDelayedFlowable();
            Flowable<Flowable<String>> parentFlowable = Flowable.unsafeCreate(new Publisher<Flowable<String>>() {
                @Override
                public void subscribe(Subscriber<? super Flowable<String>> op) {
                    op.onSubscribe(new BooleanSubscription());
                    op.onNext(Flowable.unsafeCreate(f1));
                    op.onNext(Flowable.unsafeCreate(f2));
                    op.onError(new NullPointerException("throwing exception in parent"));
                }
            });

            stringSubscriber = TestHelper.mockSubscriber();

            TestSubscriberEx<String> ts = new TestSubscriberEx<>(stringSubscriber);
            Flowable<String> m = Flowable.mergeDelayError(parentFlowable);
            m.subscribe(ts);
            System.out.println("testErrorInParentFlowableDelayed | " + i);
            ts.awaitDone(2000, TimeUnit.MILLISECONDS);
            ts.assertTerminated();

            verify(stringSubscriber, times(2)).onNext("hello");
            verify(stringSubscriber, times(1)).onError(any(NullPointerException.class));
            verify(stringSubscriber, never()).onComplete();
        }
    }

    private static class TestASynchronous1sDelayedFlowable implements Publisher<String> {
        Thread t;

        @Override
        public void subscribe(final Subscriber<? super String> subscriber) {
            subscriber.onSubscribe(new BooleanSubscription());
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        subscriber.onError(e);
                    }
                    subscriber.onNext("hello");
                    subscriber.onComplete();
                }

            });
            t.start();
        }
    }

    @Test
    public void delayErrorMaxConcurrent() {
        final List<Long> requests = new ArrayList<>();
        Flowable<Integer> source = Flowable.mergeDelayError(Flowable.just(
                Flowable.just(1).hide(),
                Flowable.<Integer>error(new TestException()))
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long t1) {
                        requests.add(t1);
                    }
                }), 1);

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        source.subscribe(ts);

        ts.assertValue(1);
        ts.assertTerminated();
        ts.assertError(TestException.class);
        assertEquals(Arrays.asList(1L, 1L, 1L), requests);
    }

    // This is pretty much a clone of testMergeList but with the overloaded MergeDelayError for Iterables
    @Test
    public void mergeIterable() {
        final Flowable<String> f1 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        final Flowable<String> f2 = Flowable.unsafeCreate(new TestSynchronousFlowable());
        List<Flowable<String>> listOfFlowables = new ArrayList<>();
        listOfFlowables.add(f1);
        listOfFlowables.add(f2);

        Flowable<String> m = Flowable.mergeDelayError(listOfFlowables);
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, never()).onError(any(Throwable.class));
        verify(stringSubscriber, times(1)).onComplete();
        verify(stringSubscriber, times(2)).onNext("hello");
    }

    @Test
    public void iterableMaxConcurrent() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        Flowable.mergeDelayError(Arrays.asList(pp1, pp2), 1).subscribe(ts);

        assertTrue("ps1 has no subscribers?!", pp1.hasSubscribers());
        assertFalse("ps2 has subscribers?!", pp2.hasSubscribers());

        pp1.onNext(1);
        pp1.onComplete();

        assertFalse("ps1 has subscribers?!", pp1.hasSubscribers());
        assertTrue("ps2 has no subscribers?!", pp2.hasSubscribers());

        pp2.onNext(2);
        pp2.onComplete();

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void iterableMaxConcurrentError() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        Flowable.mergeDelayError(Arrays.asList(pp1, pp2), 1).subscribe(ts);

        assertTrue("ps1 has no subscribers?!", pp1.hasSubscribers());
        assertFalse("ps2 has subscribers?!", pp2.hasSubscribers());

        pp1.onNext(1);
        pp1.onError(new TestException());

        assertFalse("ps1 has subscribers?!", pp1.hasSubscribers());
        assertTrue("ps2 has no subscribers?!", pp2.hasSubscribers());

        pp2.onNext(2);
        pp2.onError(new TestException());

        ts.assertValues(1, 2);
        ts.assertError(CompositeException.class);
        ts.assertNotComplete();

        CompositeException ce = (CompositeException)ts.errors().get(0);

        assertEquals(2, ce.getExceptions().size());
    }

    static <T> Flowable<T> withError(Flowable<T> source) {
        return source.concatWith(Flowable.<T>error(new TestException()));
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

    @Test
    public void mergeArrayDelayError() {
        Flowable.mergeArrayDelayError(Flowable.just(1), Flowable.just(2))
        .test()
        .assertResult(1, 2);
    }

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

    @Test
    public void mergeIterableDelayErrorMaxConcurrency() {
        Flowable.mergeDelayError(
                Arrays.asList(Flowable.just(1),
                Flowable.just(2)), 1)
        .test()
        .assertResult(1, 2);
    }

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

    @Test
    public void mergeIterableDelayError() {
        Flowable.mergeDelayError(Arrays.asList(Flowable.just(1), Flowable.just(2)))
        .test()
        .assertResult(1, 2);
    }
}
