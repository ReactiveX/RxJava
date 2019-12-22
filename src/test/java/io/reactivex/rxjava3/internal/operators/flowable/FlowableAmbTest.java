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
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.util.CrashingMappedIterable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableAmbTest extends RxJavaTest {

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
    public void amb() {
        Flowable<String> flowable1 = createFlowable(new String[] {
                "1", "11", "111", "1111" }, 2000, null);
        Flowable<String> flowable2 = createFlowable(new String[] {
                "2", "22", "222", "2222" }, 1000, null);
        Flowable<String> flowable3 = createFlowable(new String[] {
                "3", "33", "333", "3333" }, 3000, null);

        Flowable<String> f = Flowable.ambArray(flowable1,
                flowable2, flowable3);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        f.subscribe(subscriber);

        scheduler.advanceTimeBy(100000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onNext("2");
        inOrder.verify(subscriber, times(1)).onNext("22");
        inOrder.verify(subscriber, times(1)).onNext("222");
        inOrder.verify(subscriber, times(1)).onNext("2222");
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void amb2() {
        IOException expectedException = new IOException(
                "fake exception");
        Flowable<String> flowable1 = createFlowable(new String[] {},
                2000, new IOException("fake exception"));
        Flowable<String> flowable2 = createFlowable(new String[] {
                "2", "22", "222", "2222" }, 1000, expectedException);
        Flowable<String> flowable3 = createFlowable(new String[] {},
                3000, new IOException("fake exception"));

        Flowable<String> f = Flowable.ambArray(flowable1,
                flowable2, flowable3);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        f.subscribe(subscriber);

        scheduler.advanceTimeBy(100000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onNext("2");
        inOrder.verify(subscriber, times(1)).onNext("22");
        inOrder.verify(subscriber, times(1)).onNext("222");
        inOrder.verify(subscriber, times(1)).onNext("2222");
        inOrder.verify(subscriber, times(1)).onError(expectedException);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void amb3() {
        Flowable<String> flowable1 = createFlowable(new String[] {
                "1" }, 2000, null);
        Flowable<String> flowable2 = createFlowable(new String[] {},
                1000, null);
        Flowable<String> flowable3 = createFlowable(new String[] {
                "3" }, 3000, null);

        Flowable<String> f = Flowable.ambArray(flowable1,
                flowable2, flowable3);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        f.subscribe(subscriber);

        scheduler.advanceTimeBy(100000, TimeUnit.MILLISECONDS);
        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void producerRequestThroughAmb() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(0L);
        ts.request(3);
        final AtomicLong requested1 = new AtomicLong();
        final AtomicLong requested2 = new AtomicLong();
        Flowable<Integer> f1 = Flowable.unsafeCreate(new Publisher<Integer>() {

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
        Flowable<Integer> f2 = Flowable.unsafeCreate(new Publisher<Integer>() {

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
        Flowable.ambArray(f1, f2).subscribe(ts);
        assertEquals(3, requested1.get());
        assertEquals(3, requested2.get());
    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Flowable.range(0, Flowable.bufferSize() * 2)
                .ambWith(Flowable.range(0, Flowable.bufferSize() * 2))
                .observeOn(Schedulers.computation()) // observeOn has a backpressured RxRingBuffer
                .delay(1, TimeUnit.MICROSECONDS) // make it a slightly slow consumer
                .subscribe(ts);

        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 2, ts.values().size());
    }

    @Test
    public void subscriptionOnlyHappensOnce() throws InterruptedException {
        final AtomicLong count = new AtomicLong();
        Consumer<Subscription> incrementer = new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                count.incrementAndGet();
            }
        };

        //this aync stream should emit first
        Flowable<Integer> f1 = Flowable.just(1).doOnSubscribe(incrementer)
                .delay(100, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation());
        //this stream emits second
        Flowable<Integer> f2 = Flowable.just(1).doOnSubscribe(incrementer)
                .delay(100, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation());
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Flowable.ambArray(f1, f2).subscribe(ts);
        ts.request(1);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals(2, count.get());
    }

    @Test
    public void secondaryRequestsPropagatedToChildren() throws InterruptedException {
        //this aync stream should emit first
        Flowable<Integer> f1 = Flowable.fromArray(1, 2, 3)
                .delay(100, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation());
        //this stream emits second
        Flowable<Integer> f2 = Flowable.fromArray(4, 5, 6)
                .delay(200, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation());
        TestSubscriber<Integer> ts = new TestSubscriber<>(1L);

        Flowable.ambArray(f1, f2).subscribe(ts);
        // before first emission request 20 more
        // this request should suffice to emit all
        ts.request(20);
        //ensure stream does not hang
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
    }

    @Test
    public void synchronousSources() {
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

    @Test
    public void ambCancelsOthers() {
        PublishProcessor<Integer> source1 = PublishProcessor.create();
        PublishProcessor<Integer> source2 = PublishProcessor.create();
        PublishProcessor<Integer> source3 = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.ambArray(source1, source2, source3).subscribe(ts);

        assertTrue("Source 1 doesn't have subscribers!", source1.hasSubscribers());
        assertTrue("Source 2 doesn't have subscribers!", source2.hasSubscribers());
        assertTrue("Source 3 doesn't have subscribers!", source3.hasSubscribers());

        source1.onNext(1);

        assertTrue("Source 1 doesn't have subscribers!", source1.hasSubscribers());
        assertFalse("Source 2 still has subscribers!", source2.hasSubscribers());
        assertFalse("Source 2 still has subscribers!", source3.hasSubscribers());

    }

    @Test
    public void multipleUse() {
        TestSubscriber<Long> ts1 = new TestSubscriber<>();
        TestSubscriber<Long> ts2 = new TestSubscriber<>();

        Flowable<Long> amb = Flowable.timer(100, TimeUnit.MILLISECONDS).ambWith(Flowable.timer(200, TimeUnit.MILLISECONDS));

        amb.subscribe(ts1);
        amb.subscribe(ts2);

        ts1.awaitDone(5, TimeUnit.SECONDS);
        ts2.awaitDone(5, TimeUnit.SECONDS);

        ts1.assertValue(0L);
        ts1.assertComplete();
        ts1.assertNoErrors();

        ts2.assertValue(0L);
        ts2.assertComplete();
        ts2.assertNoErrors();
    }

    @Test
    public void ambIterable() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.amb(Arrays.asList(pp1, pp2)).subscribe(ts);

        ts.assertNoValues();

        pp1.onNext(1);
        pp1.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void ambIterable2() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.amb(Arrays.asList(pp1, pp2)).subscribe(ts);

        ts.assertNoValues();

        pp2.onNext(2);
        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertValue(2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void ambArrayEmpty() {
        assertSame(Flowable.empty(), Flowable.ambArray());
    }

    @Test
    public void ambArraySingleElement() {
        assertSame(Flowable.never(), Flowable.ambArray(Flowable.never()));
    }

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
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            TestSubscriberEx<Integer> ts = Flowable.ambArray(pp1, pp2).to(TestHelper.<Integer>testConsumer());

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp1.onNext(1);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp2.onNext(1);
                }
            };

            TestHelper.race(r1, r2);

            ts.assertSubscribed().assertNoErrors()
            .assertNotComplete().assertValueCount(1);
        }
    }

    @Test
    public void onCompleteRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            TestSubscriber<Integer> ts = Flowable.ambArray(pp1, pp2).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp1.onComplete();
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp2.onComplete();
                }
            };

            TestHelper.race(r1, r2);

            ts.assertResult();
        }
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            TestSubscriber<Integer> ts = Flowable.ambArray(pp1, pp2).test();

            final Throwable ex = new TestException();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp1.onError(ex);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp2.onError(ex);
                }
            };

            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestHelper.race(r1, r2);
            } finally {
                RxJavaPlugins.reset();
            }

            ts.assertFailure(TestException.class);
            if (!errors.isEmpty()) {
                TestHelper.assertUndeliverable(errors, 0, TestException.class);
            }
        }
    }

    @Test
    public void nullIterableElement() {
        Flowable.amb(Arrays.asList(Flowable.never(), null, Flowable.never()))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void iteratorThrows() {
        Flowable.amb(new CrashingMappedIterable<>(1, 100, 100, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                return Flowable.never();
            }
        }))
        .to(TestHelper.<Integer>testConsumer())
        .assertFailureAndMessage(TestException.class, "iterator()");

        Flowable.amb(new CrashingMappedIterable<>(100, 1, 100, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                return Flowable.never();
            }
        }))
        .to(TestHelper.<Integer>testConsumer())
        .assertFailureAndMessage(TestException.class, "hasNext()");

        Flowable.amb(new CrashingMappedIterable<>(100, 100, 1, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                return Flowable.never();
            }
        }))
        .to(TestHelper.<Integer>testConsumer())
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test
    public void ambWithOrder() {
        Flowable<Integer> error = Flowable.error(new RuntimeException());
        Flowable.just(1).ambWith(error).test().assertValue(1).assertComplete();
    }

    @Test
    public void ambIterableOrder() {
        Flowable<Integer> error = Flowable.error(new RuntimeException());
        Flowable.amb(Arrays.asList(Flowable.just(1), error)).test().assertValue(1).assertComplete();
    }

    @Test
    public void ambArrayOrder() {
        Flowable<Integer> error = Flowable.error(new RuntimeException());
        Flowable.ambArray(Flowable.just(1), error).test().assertValue(1).assertComplete();
    }

    @Test
    public void noWinnerSuccessDispose() throws Exception {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final AtomicBoolean interrupted = new AtomicBoolean();
            final CountDownLatch cdl = new CountDownLatch(1);

            Flowable.ambArray(
                Flowable.just(1)
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.computation()),
                Flowable.never()
            )
            .subscribe(new Consumer<Object>() {
                @Override
                public void accept(Object v) throws Exception {
                    interrupted.set(Thread.currentThread().isInterrupted());
                    cdl.countDown();
                }
            });

            assertTrue(cdl.await(500, TimeUnit.SECONDS));
            assertFalse("Interrupted!", interrupted.get());
        }
    }

    @Test
    public void noWinnerErrorDispose() throws Exception {
        final TestException ex = new TestException();
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final AtomicBoolean interrupted = new AtomicBoolean();
            final CountDownLatch cdl = new CountDownLatch(1);

            Flowable.ambArray(
                Flowable.error(ex)
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.computation()),
                Flowable.never()
            )
            .subscribe(Functions.emptyConsumer(), new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    interrupted.set(Thread.currentThread().isInterrupted());
                    cdl.countDown();
                }
            });

            assertTrue(cdl.await(500, TimeUnit.SECONDS));
            assertFalse("Interrupted!", interrupted.get());
        }
    }

    @Test
    public void noWinnerCompleteDispose() throws Exception {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final AtomicBoolean interrupted = new AtomicBoolean();
            final CountDownLatch cdl = new CountDownLatch(1);

            Flowable.ambArray(
                Flowable.empty()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.computation()),
                Flowable.never()
            )
            .subscribe(Functions.emptyConsumer(), Functions.emptyConsumer(), new Action() {
                @Override
                public void run() throws Exception {
                    interrupted.set(Thread.currentThread().isInterrupted());
                    cdl.countDown();
                }
            });

            assertTrue(cdl.await(500, TimeUnit.SECONDS));
            assertFalse("Interrupted!", interrupted.get());
        }
    }

    @Test
    public void publishersInIterable() {
        Publisher<Integer> source = new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> subscriber) {
                Flowable.just(1).subscribe(subscriber);
            }
        };

        Flowable.amb(Arrays.asList(source, source))
        .test()
        .assertResult(1);
    }
}
