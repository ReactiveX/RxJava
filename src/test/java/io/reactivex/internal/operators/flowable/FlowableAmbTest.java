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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.util.CrashingMappedIterable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.*;

public class FlowableAmbTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    public void setUp() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
    }

    private Flowable<String> createFlowable(final String[] values,
            final long interval, final Throwable e) {
        return Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(final Subscriber<? super String> subscriber) {
                final CompositeDisposable parentSubscription = new CompositeDisposable();

                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {

                    }

                    @Override
                    public void cancel() {
                        parentSubscription.dispose();
                    }
                });

                long delay = interval;
                for (final String value : values) {
                    parentSubscription.add(innerScheduler.schedule(new Runnable() {
                        @Override
                        public void run() {
                            subscriber.onNext(value);
                        }
                    }
                    , delay, TimeUnit.MILLISECONDS));
                    delay += interval;
                }
                parentSubscription.add(innerScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                            if (e == null) {
                                subscriber.onComplete();
                            } else {
                                subscriber.onError(e);
                            }
                    }
                }, delay, TimeUnit.MILLISECONDS));
            }
        });
    }

    @Test
    public void testAmb() {
        Flowable<String> Flowable1 = createFlowable(new String[] {
                "1", "11", "111", "1111" }, 2000, null);
        Flowable<String> Flowable2 = createFlowable(new String[] {
                "2", "22", "222", "2222" }, 1000, null);
        Flowable<String> Flowable3 = createFlowable(new String[] {
                "3", "33", "333", "3333" }, 3000, null);

        @SuppressWarnings("unchecked")
        Flowable<String> o = Flowable.ambArray(Flowable1,
                Flowable2, Flowable3);

        @SuppressWarnings("unchecked")
        DefaultSubscriber<String> observer = mock(DefaultSubscriber.class);
        o.subscribe(observer);

        scheduler.advanceTimeBy(100000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("2");
        inOrder.verify(observer, times(1)).onNext("22");
        inOrder.verify(observer, times(1)).onNext("222");
        inOrder.verify(observer, times(1)).onNext("2222");
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testAmb2() {
        IOException expectedException = new IOException(
                "fake exception");
        Flowable<String> Flowable1 = createFlowable(new String[] {},
                2000, new IOException("fake exception"));
        Flowable<String> Flowable2 = createFlowable(new String[] {
                "2", "22", "222", "2222" }, 1000, expectedException);
        Flowable<String> Flowable3 = createFlowable(new String[] {},
                3000, new IOException("fake exception"));

        @SuppressWarnings("unchecked")
        Flowable<String> o = Flowable.ambArray(Flowable1,
                Flowable2, Flowable3);

        @SuppressWarnings("unchecked")
        DefaultSubscriber<String> observer = mock(DefaultSubscriber.class);
        o.subscribe(observer);

        scheduler.advanceTimeBy(100000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("2");
        inOrder.verify(observer, times(1)).onNext("22");
        inOrder.verify(observer, times(1)).onNext("222");
        inOrder.verify(observer, times(1)).onNext("2222");
        inOrder.verify(observer, times(1)).onError(expectedException);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testAmb3() {
        Flowable<String> Flowable1 = createFlowable(new String[] {
                "1" }, 2000, null);
        Flowable<String> Flowable2 = createFlowable(new String[] {},
                1000, null);
        Flowable<String> Flowable3 = createFlowable(new String[] {
                "3" }, 3000, null);

        @SuppressWarnings("unchecked")
        Flowable<String> o = Flowable.ambArray(Flowable1,
                Flowable2, Flowable3);

        @SuppressWarnings("unchecked")
        DefaultSubscriber<String> observer = mock(DefaultSubscriber.class);
        o.subscribe(observer);

        scheduler.advanceTimeBy(100000, TimeUnit.MILLISECONDS);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProducerRequestThroughAmb() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);
        ts.request(3);
        final AtomicLong requested1 = new AtomicLong();
        final AtomicLong requested2 = new AtomicLong();
        Flowable<Integer> o1 = Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        System.out.println("1-requested: " + n);
                        requested1.set(n);
                    }

                    @Override
                    public void cancel() {

                    }
                });
            }

        });
        Flowable<Integer> o2 = Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        System.out.println("2-requested: " + n);
                        requested2.set(n);
                    }

                    @Override
                    public void cancel() {

                    }
                });
            }

        });
        Flowable.ambArray(o1, o2).subscribe(ts);
        assertEquals(3, requested1.get());
        assertEquals(3, requested2.get());
    }

    @Test
    public void testBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.range(0, Flowable.bufferSize() * 2)
                .ambWith(Flowable.range(0, Flowable.bufferSize() * 2))
                .observeOn(Schedulers.computation()) // observeOn has a backpressured RxRingBuffer
                .delay(1, TimeUnit.MICROSECONDS) // make it a slightly slow consumer
                .subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.values().size());
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testSubscriptionOnlyHappensOnce() throws InterruptedException {
        final AtomicLong count = new AtomicLong();
        Consumer<Subscription> incrementer = new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                count.incrementAndGet();
            }
        };

        //this aync stream should emit first
        Flowable<Integer> o1 = Flowable.just(1).doOnSubscribe(incrementer)
                .delay(100, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation());
        //this stream emits second
        Flowable<Integer> o2 = Flowable.just(1).doOnSubscribe(incrementer)
                .delay(100, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation());
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.ambArray(o1, o2).subscribe(ts);
        ts.request(1);
        ts.awaitTerminalEvent(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals(2, count.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSecondaryRequestsPropagatedToChildren() throws InterruptedException {
        //this aync stream should emit first
        Flowable<Integer> o1 = Flowable.fromArray(1, 2, 3)
                .delay(100, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation());
        //this stream emits second
        Flowable<Integer> o2 = Flowable.fromArray(4, 5, 6)
                .delay(200, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation());
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1L);

        Flowable.ambArray(o1, o2).subscribe(ts);
        // before first emission request 20 more
        // this request should suffice to emit all
        ts.request(20);
        //ensure stream does not hang
        ts.awaitTerminalEvent(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
    }

    @Test
    public void testSynchronousSources() {
        // under async subscription the second Flowable would complete before
        // the first but because this is a synchronous subscription to sources
        // then second Flowable does not get subscribed to before first
        // subscription completes hence first Flowable emits result through
        // amb
        int result = Flowable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        //
                    }
            }
        }).ambWith(Flowable.just(2)).blockingSingle();
        assertEquals(1, result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAmbCancelsOthers() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();
        PublishProcessor<Integer> source3 = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.ambArray(source1, source2, source3).subscribe(ts);

        assertTrue("Source 1 doesn't have subscribers!", source1.hasSubscribers());
        assertTrue("Source 2 doesn't have subscribers!", source2.hasSubscribers());
        assertTrue("Source 3 doesn't have subscribers!", source3.hasSubscribers());

        source1.onNext(1);

        assertTrue("Source 1 doesn't have subscribers!", source1.hasSubscribers());
        assertFalse("Source 2 still has subscribers!", source2.hasSubscribers());
        assertFalse("Source 2 still has subscribers!", source3.hasSubscribers());

    }

    @Test(timeout = 1000)
    public void testMultipleUse() {
        TestSubscriber<Long> ts1 = new TestSubscriber<Long>();
        TestSubscriber<Long> ts2 = new TestSubscriber<Long>();

        Flowable<Long> amb = Flowable.timer(100, TimeUnit.MILLISECONDS).ambWith(Flowable.timer(200, TimeUnit.MILLISECONDS));

        amb.subscribe(ts1);
        amb.subscribe(ts2);

        ts1.awaitTerminalEvent();
        ts2.awaitTerminalEvent();

        ts1.assertValue(0L);
        ts1.assertComplete();
        ts1.assertNoErrors();

        ts2.assertValue(0L);
        ts2.assertComplete();
        ts2.assertNoErrors();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable() {
        PublishProcessor<Integer> ps1 = PublishProcessor.create();
        PublishProcessor<Integer> ps2 = PublishProcessor.create();

        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.amb(Arrays.asList(ps1, ps2)).subscribe(ts);

        ts.assertNoValues();

        ps1.onNext(1);
        ps1.onComplete();

        assertFalse(ps1.hasSubscribers());
        assertFalse(ps2.hasSubscribers());

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable2() {
        PublishProcessor<Integer> ps1 = PublishProcessor.create();
        PublishProcessor<Integer> ps2 = PublishProcessor.create();

        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.amb(Arrays.asList(ps1, ps2)).subscribe(ts);

        ts.assertNoValues();

        ps2.onNext(2);
        ps2.onComplete();

        assertFalse(ps1.hasSubscribers());
        assertFalse(ps2.hasSubscribers());

        ts.assertValue(2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Ignore("No 2-9 arg overloads")
    @SuppressWarnings("unchecked")
    @Test
    public void ambMany() throws Exception {
        for (int i = 2; i < 10; i++) {
            Class<?>[] clazz = new Class[i];
            Arrays.fill(clazz, Publisher.class);

            PublishProcessor<Integer>[] ps = new PublishProcessor[i];

            for (int j = 0; j < i; j++) {

                for (int k = 0; k < i; k++) {
                    ps[k] = PublishProcessor.create();
                }

                Method m = Flowable.class.getMethod("amb", clazz);

                Flowable<Integer> obs = (Flowable<Integer>)m.invoke(null, (Object[])ps);

                TestSubscriber<Integer> ts = TestSubscriber.create();

                obs.subscribe(ts);

                for (int k = 0; k < i; k++) {
                    assertTrue("@" + i + "/" + k + " has no observers?", ps[k].hasSubscribers());
                }

                ps[j].onNext(j);
                ps[j].onComplete();

                for (int k = 0; k < i; k++) {
                    assertFalse("@" + i + "/" + k + " has observers?", ps[k].hasSubscribers());
                }

                ts.assertValue(j);
                ts.assertNoErrors();
                ts.assertComplete();
            }
        }
    }

    @Ignore("No 2-9 arg overloads")
    @SuppressWarnings("unchecked")
    @Test
    public void ambManyError() throws Exception {
        for (int i = 2; i < 10; i++) {
            Class<?>[] clazz = new Class[i];
            Arrays.fill(clazz, Publisher.class);

            PublishProcessor<Integer>[] ps = new PublishProcessor[i];

            for (int j = 0; j < i; j++) {

                for (int k = 0; k < i; k++) {
                    ps[k] = PublishProcessor.create();
                }

                Method m = Flowable.class.getMethod("amb", clazz);

                Flowable<Integer> obs = (Flowable<Integer>)m.invoke(null, (Object[])ps);

                TestSubscriber<Integer> ts = TestSubscriber.create();

                obs.subscribe(ts);

                for (int k = 0; k < i; k++) {
                    assertTrue("@" + i + "/" + k + " has no observers?", ps[k].hasSubscribers());
                }

                ps[j].onError(new TestException(Integer.toString(j)));

                for (int k = 0; k < i; k++) {
                    assertFalse("@" + i + "/" + k + " has observers?", ps[k].hasSubscribers());
                }

                ts.assertNoValues();
                ts.assertError(TestException.class);
                ts.assertNotComplete();

                assertEquals(Integer.toString(j), ts.errors().get(0).getMessage());
            }
        }
    }

    @Ignore("No 2-9 arg overloads")
    @SuppressWarnings("unchecked")
    @Test
    public void ambManyComplete() throws Exception {
        for (int i = 2; i < 10; i++) {
            Class<?>[] clazz = new Class[i];
            Arrays.fill(clazz, Publisher.class);

            PublishProcessor<Integer>[] ps = new PublishProcessor[i];

            for (int j = 0; j < i; j++) {

                for (int k = 0; k < i; k++) {
                    ps[k] = PublishProcessor.create();
                }

                Method m = Flowable.class.getMethod("amb", clazz);

                Flowable<Integer> obs = (Flowable<Integer>)m.invoke(null, (Object[])ps);

                TestSubscriber<Integer> ts = TestSubscriber.create();

                obs.subscribe(ts);

                for (int k = 0; k < i; k++) {
                    assertTrue("@" + i + "/" + k + " has no observers?", ps[k].hasSubscribers());
                }

                ps[j].onComplete();

                for (int k = 0; k < i; k++) {
                    assertFalse("@" + i + "/" + k + " has observers?", ps[k].hasSubscribers());
                }

                ts.assertNoValues();
                ts.assertNoErrors();
                ts.assertComplete();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArrayEmpty() {
        assertSame(Flowable.empty(), Flowable.ambArray());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArraySingleElement() {
        assertSame(Flowable.never(), Flowable.ambArray(Flowable.never()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void disposed() {
        TestHelper.checkDisposed(Flowable.ambArray(Flowable.never(), Flowable.never()));
    }

    @Test
    public void manySources() {
        Flowable<?>[] a = new Flowable[32];
        Arrays.fill(a, Flowable.never());
        a[31] = Flowable.just(1);

        Flowable.amb(Arrays.asList(a))
        .test()
        .assertResult(1);
    }

    @Test
    public void emptyIterable() {
        Flowable.amb(Collections.<Flowable<Integer>>emptyList())
        .test()
        .assertResult();
    }

    @Test
    public void singleIterable() {
        Flowable.amb(Collections.singletonList(Flowable.just(1)))
        .test()
        .assertResult(1);
    }

    @Test
    public void onNextRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> ps1 = PublishProcessor.create();
            final PublishProcessor<Integer> ps2 = PublishProcessor.create();

            @SuppressWarnings("unchecked")
            TestSubscriber<Integer> to = Flowable.ambArray(ps1, ps2).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps1.onNext(1);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps2.onNext(1);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            to.assertSubscribed().assertNoErrors()
            .assertNotComplete().assertValueCount(1);
        }
    }

    @Test
    public void onCompleteRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> ps1 = PublishProcessor.create();
            final PublishProcessor<Integer> ps2 = PublishProcessor.create();

            @SuppressWarnings("unchecked")
            TestSubscriber<Integer> to = Flowable.ambArray(ps1, ps2).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps1.onComplete();
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps2.onComplete();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            to.assertResult();
        }
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> ps1 = PublishProcessor.create();
            final PublishProcessor<Integer> ps2 = PublishProcessor.create();

            @SuppressWarnings("unchecked")
            TestSubscriber<Integer> to = Flowable.ambArray(ps1, ps2).test();

            final Throwable ex = new TestException();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps1.onError(ex);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps2.onError(ex);
                }
            };

            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestHelper.race(r1, r2, Schedulers.single());
            } finally {
                RxJavaPlugins.reset();
            }

            to.assertFailure(TestException.class);
            if (!errors.isEmpty()) {
                TestHelper.assertUndeliverable(errors, 0, TestException.class);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nullIterableElement() {
        Flowable.amb(Arrays.asList(Flowable.never(), null, Flowable.never()))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void iteratorThrows() {
        Flowable.amb(new CrashingMappedIterable<Flowable<Integer>>(1, 100, 100, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                return Flowable.never();
            }
        }))
        .test()
        .assertFailureAndMessage(TestException.class, "iterator()");

        Flowable.amb(new CrashingMappedIterable<Flowable<Integer>>(100, 1, 100, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                return Flowable.never();
            }
        }))
        .test()
        .assertFailureAndMessage(TestException.class, "hasNext()");

        Flowable.amb(new CrashingMappedIterable<Flowable<Integer>>(100, 100, 1, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                return Flowable.never();
            }
        }))
        .test()
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test
    public void ambWithOrder() {
        Flowable<Integer> error = Flowable.error(new RuntimeException());
        Flowable.just(1).ambWith(error).test().assertValue(1).assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterableOrder() {
        Flowable<Integer> error = Flowable.error(new RuntimeException());
        Flowable.amb(Arrays.asList(Flowable.just(1), error)).test().assertValue(1).assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArrayOrder() {
        Flowable<Integer> error = Flowable.error(new RuntimeException());
        Flowable.ambArray(Flowable.just(1), error).test().assertValue(1).assertComplete();
    }
}
