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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.rxjava3.internal.operators.flowable.FlowablePublish.*;
import io.reactivex.rxjava3.internal.schedulers.ImmediateThinScheduler;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowablePublishTest extends RxJavaTest {

    @Test
    public void publish() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        ConnectableFlowable<String> f = Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(final Subscriber<? super String> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        subscriber.onNext("one");
                        subscriber.onComplete();
                    }
                }).start();
            }
        }).publish();

        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        f.subscribe(new Consumer<String>() {

            @Override
            public void accept(String v) {
                assertEquals("one", v);
                latch.countDown();
            }
        });

        // subscribe again
        f.subscribe(new Consumer<String>() {

            @Override
            public void accept(String v) {
                assertEquals("one", v);
                latch.countDown();
            }
        });

        Disposable connection = f.connect();
        try {
            if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                fail("subscriptions did not receive values");
            }
            assertEquals(1, counter.get());
        } finally {
            connection.dispose();
        }
    }

    @Test
    public void backpressureFastSlow() {
        ConnectableFlowable<Integer> is = Flowable.range(1, Flowable.bufferSize() * 2).publish();
        Flowable<Integer> fast = is.observeOn(Schedulers.computation())
        .doOnComplete(new Action() {
            @Override
            public void run() {
                System.out.println("^^^^^^^^^^^^^ completed FAST");
            }
        });

        Flowable<Integer> slow = is.observeOn(Schedulers.computation()).map(new Function<Integer, Integer>() {
            int c;

            @Override
            public Integer apply(Integer i) {
                if (c == 0) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
                c++;
                return i;
            }

        }).doOnComplete(new Action() {

            @Override
            public void run() {
                System.out.println("^^^^^^^^^^^^^ completed SLOW");
            }

        });

        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Flowable.merge(fast, slow).subscribe(ts);
        is.connect();
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 4, ts.values().size());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void takeUntilWithPublishedStreamUsingSelector() {
        final AtomicInteger emitted = new AtomicInteger();
        Flowable<Integer> xs = Flowable.range(0, Flowable.bufferSize() * 2).doOnNext(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                emitted.incrementAndGet();
            }

        });
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        xs.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Flowable<Integer> xs) {
                return xs.takeUntil(xs.skipWhile(new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer i) {
                        return i <= 3;
                    }

                }));
            }

        }).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertValues(0, 1, 2, 3);
        assertEquals(5, emitted.get());
        System.out.println(ts.values());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void takeUntilWithPublishedStream() {
        Flowable<Integer> xs = Flowable.range(0, Flowable.bufferSize() * 2);
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ConnectableFlowable<Integer> xsp = xs.publish();
        xsp.takeUntil(xsp.skipWhile(new Predicate<Integer>() {

            @Override
            public boolean test(Integer i) {
                return i <= 3;
            }

        })).subscribe(ts);
        xsp.connect();
        System.out.println(ts.values());
    }

    @Test
    public void backpressureTwoConsumers() {
        final AtomicInteger sourceEmission = new AtomicInteger();
        final AtomicBoolean sourceUnsubscribed = new AtomicBoolean();
        final Flowable<Integer> source = Flowable.range(1, 100)
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer t1) {
                        sourceEmission.incrementAndGet();
                    }
                })
                .doOnCancel(new Action() {
                    @Override
                    public void run() {
                        sourceUnsubscribed.set(true);
                    }
                }).share();
        ;

        final AtomicBoolean child1Unsubscribed = new AtomicBoolean();
        final AtomicBoolean child2Unsubscribed = new AtomicBoolean();

        final TestSubscriber<Integer> ts2 = new TestSubscriber<>();

        final TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (values().size() == 2) {
                    source.doOnCancel(new Action() {
                        @Override
                        public void run() {
                            child2Unsubscribed.set(true);
                        }
                    }).take(5).subscribe(ts2);
                }
                super.onNext(t);
            }
        };

        source.doOnCancel(new Action() {
            @Override
            public void run() {
                child1Unsubscribed.set(true);
            }
        }).take(5)
        .subscribe(ts1);

        ts1.awaitDone(5, TimeUnit.SECONDS);
        ts2.awaitDone(5, TimeUnit.SECONDS);

        ts1.assertNoErrors();
        ts2.assertNoErrors();

        assertTrue(sourceUnsubscribed.get());
        assertTrue(child1Unsubscribed.get());
        assertTrue(child2Unsubscribed.get());

        ts1.assertValues(1, 2, 3, 4, 5);
        ts2.assertValues(4, 5, 6, 7, 8);

        assertEquals(8, sourceEmission.get());
    }

    @Test
    public void connectWithNoSubscriber() {
        TestScheduler scheduler = new TestScheduler();
        ConnectableFlowable<Long> cf = Flowable.interval(10, 10, TimeUnit.MILLISECONDS, scheduler).take(3).publish();
        cf.connect();
        // Emit 0
        scheduler.advanceTimeBy(15, TimeUnit.MILLISECONDS);
        TestSubscriber<Long> subscriber = new TestSubscriber<>();
        cf.subscribe(subscriber);
        // Emit 1 and 2
        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
        // 3.x: Flowable.publish no longer drains the input buffer if there are no subscribers
        subscriber.assertResult(0L, 1L, 2L);
    }

    @Test
    public void subscribeAfterDisconnectThenConnect() {
        ConnectableFlowable<Integer> source = Flowable.just(1).publish();

        TestSubscriberEx<Integer> ts1 = new TestSubscriberEx<>();

        source.subscribe(ts1);

        Disposable connection = source.connect();

        ts1.assertValue(1);
        ts1.assertNoErrors();
        ts1.assertTerminated();

        source.reset();

        TestSubscriberEx<Integer> ts2 = new TestSubscriberEx<>();

        source.subscribe(ts2);

        Disposable connection2 = source.connect();

        ts2.assertValue(1);
        ts2.assertNoErrors();
        ts2.assertTerminated();

        System.out.println(connection);
        System.out.println(connection2);
    }

    @Test
    public void noSubscriberRetentionOnCompleted() {
        FlowablePublish<Integer> source = (FlowablePublish<Integer>)Flowable.just(1).publish();

        TestSubscriberEx<Integer> ts1 = new TestSubscriberEx<>();

        source.subscribe(ts1);

        ts1.assertNoValues();
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        source.connect();

        ts1.assertValue(1);
        ts1.assertNoErrors();
        ts1.assertTerminated();

        assertEquals(0, source.current.get().subscribers.get().length);
    }

    @Test
    public void nonNullConnection() {
        ConnectableFlowable<Object> source = Flowable.never().publish();

        assertNotNull(source.connect());
        assertNotNull(source.connect());
    }

    @Test
    public void noDisconnectSomeoneElse() {
        ConnectableFlowable<Object> source = Flowable.never().publish();

        Disposable connection1 = source.connect();
        Disposable connection2 = source.connect();

        connection1.dispose();

        Disposable connection3 = source.connect();

        connection2.dispose();

        assertTrue(checkPublishDisposed(connection1));
        assertTrue(checkPublishDisposed(connection2));
        assertFalse(checkPublishDisposed(connection3));
    }

    @SuppressWarnings("unchecked")
    static boolean checkPublishDisposed(Disposable d) {
        return ((FlowablePublish.PublishConnection<Object>)d).isDisposed();
    }

    @Test
    public void zeroRequested() {
        ConnectableFlowable<Integer> source = Flowable.just(1).publish();

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(0L);

        source.subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        source.connect();

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(5);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertTerminated();
    }

    @Test
    public void connectIsIdempotent() {
        final AtomicInteger calls = new AtomicInteger();
        Flowable<Integer> source = Flowable.unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> t) {
                t.onSubscribe(new BooleanSubscription());
                calls.getAndIncrement();
            }
        });

        ConnectableFlowable<Integer> conn = source.publish();

        assertEquals(0, calls.get());

        conn.connect();
        conn.connect();

        assertEquals(1, calls.get());

        conn.connect().dispose();

        conn.connect();
        conn.connect();

        assertEquals(2, calls.get());
    }

    @Test
    public void syncFusedObserveOn() {
        ConnectableFlowable<Integer> cf = Flowable.range(0, 1000).publish();
        Flowable<Integer> obs = cf.observeOn(Schedulers.computation());
        for (int i = 0; i < 1000; i++) {
            for (int j = 1; j < 6; j++) {
                List<TestSubscriberEx<Integer>> tss = new ArrayList<>();
                for (int k = 1; k < j; k++) {
                    TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
                    tss.add(ts);
                    obs.subscribe(ts);
                }

                Disposable connection = cf.connect();

                for (TestSubscriberEx<Integer> ts : tss) {
                    ts.awaitDone(5, TimeUnit.SECONDS)
                    .assertSubscribed()
                    .assertValueCount(1000)
                    .assertNoErrors()
                    .assertComplete();
                }
                connection.dispose();
            }
        }
    }

    @Test
    public void syncFusedObserveOn2() {
        ConnectableFlowable<Integer> cf = Flowable.range(0, 1000).publish();
        Flowable<Integer> obs = cf.observeOn(ImmediateThinScheduler.INSTANCE);
        for (int i = 0; i < 1000; i++) {
            for (int j = 1; j < 6; j++) {
                List<TestSubscriberEx<Integer>> tss = new ArrayList<>();
                for (int k = 1; k < j; k++) {
                    TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
                    tss.add(ts);
                    obs.subscribe(ts);
                }

                Disposable connection = cf.connect();

                for (TestSubscriberEx<Integer> ts : tss) {
                    ts.awaitDone(5, TimeUnit.SECONDS)
                    .assertSubscribed()
                    .assertValueCount(1000)
                    .assertNoErrors()
                    .assertComplete();
                }
                connection.dispose();
            }
        }
    }

    @Test
    public void asyncFusedObserveOn() {
        ConnectableFlowable<Integer> cf = Flowable.range(0, 1000).observeOn(ImmediateThinScheduler.INSTANCE).publish();
        for (int i = 0; i < 1000; i++) {
            for (int j = 1; j < 6; j++) {
                List<TestSubscriberEx<Integer>> tss = new ArrayList<>();
                for (int k = 1; k < j; k++) {
                    TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
                    tss.add(ts);
                    cf.subscribe(ts);
                }

                Disposable connection = cf.connect();

                for (TestSubscriberEx<Integer> ts : tss) {
                    ts.awaitDone(5, TimeUnit.SECONDS)
                    .assertSubscribed()
                    .assertValueCount(1000)
                    .assertNoErrors()
                    .assertComplete();
                }
                connection.dispose();
            }
        }
    }

    @Test
    public void observeOn() {
        ConnectableFlowable<Integer> cf = Flowable.range(0, 1000).hide().publish();
        Flowable<Integer> obs = cf.observeOn(Schedulers.computation());
        for (int i = 0; i < 1000; i++) {
            for (int j = 1; j < 6; j++) {
                List<TestSubscriberEx<Integer>> tss = new ArrayList<>();
                for (int k = 1; k < j; k++) {
                    TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
                    tss.add(ts);
                    obs.subscribe(ts);
                }

                Disposable connection = cf.connect();

                for (TestSubscriberEx<Integer> ts : tss) {
                    ts.awaitDone(5, TimeUnit.SECONDS)
                    .assertSubscribed()
                    .assertValueCount(1000)
                    .assertNoErrors()
                    .assertComplete();
                }
                connection.dispose();
            }
        }
    }

    @Test
    public void source() {
        Flowable<Integer> f = Flowable.never();

        assertSame(f, (((HasUpstreamPublisher<?>)f.publish()).source()));
    }

    @Test
    public void connectThrows() {
        ConnectableFlowable<Integer> cf = Flowable.<Integer>empty().publish();
        try {
            cf.connect(new Consumer<Disposable>() {
                @Override
                public void accept(Disposable d) throws Exception {
                    throw new TestException();
                }
            });
        } catch (TestException ex) {
            // expected
        }
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final ConnectableFlowable<Integer> cf = Flowable.<Integer>empty().publish();

            final TestSubscriber<Integer> ts = cf.test();

            final TestSubscriber<Integer> ts2 = new TestSubscriber<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    cf.subscribe(ts2);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void disposeOnArrival() {
        ConnectableFlowable<Integer> cf = Flowable.<Integer>empty().publish();

        cf.test(Long.MAX_VALUE, true).assertEmpty();
    }

    @Test
    public void disposeOnArrival2() {
        Flowable<Integer> co = Flowable.<Integer>never().publish().autoConnect();

        co.test(Long.MAX_VALUE, true).assertEmpty();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.never().publish());

        TestHelper.checkDisposed(Flowable.never().publish(Functions.<Flowable<Object>>identity()));
    }

    @Test
    public void empty() {
        ConnectableFlowable<Integer> cf = Flowable.<Integer>empty().publish();

        cf.connect();
    }

    @Test
    public void take() {
        ConnectableFlowable<Integer> cf = Flowable.range(1, 2).publish();

        TestSubscriber<Integer> ts = cf.take(1).test();

        cf.connect();

        ts.assertResult(1);
    }

    @Test
    public void just() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        ConnectableFlowable<Integer> cf = pp.publish();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                pp.onComplete();
            }
        };

        cf.subscribe(ts);
        cf.connect();

        pp.onNext(1);

        ts.assertResult(1);
    }

    @Test
    public void nextCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final ConnectableFlowable<Integer> cf = pp.publish();

            final TestSubscriber<Integer> ts = cf.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                    subscriber.onSubscribe(new BooleanSubscription());
                    subscriber.onNext(1);
                    subscriber.onComplete();
                    subscriber.onNext(2);
                    subscriber.onError(new TestException());
                    subscriber.onComplete();
                }
            }
            .publish()
            .autoConnect()
            .test()
            .assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void noErrorLoss() {
        ConnectableFlowable<Object> cf = Flowable.error(new TestException()).publish();

        cf.connect();

        // 3.x: terminal events are always kept until reset.
        cf.test()
        .assertFailure(TestException.class);
    }

    @Test
    public void subscribeDisconnectRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final ConnectableFlowable<Integer> cf = pp.publish();

            final Disposable d = cf.connect();
            final TestSubscriber<Integer> ts = new TestSubscriber<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    d.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    cf.subscribe(ts);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void selectorDisconnectsIndependentSource() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> v) throws Exception {
                return Flowable.range(1, 2);
            }
        })
        .test()
        .assertResult(1, 2);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void selectorLatecommer() {
        Flowable.range(1, 5)
        .publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> v) throws Exception {
                return v.concatWith(v);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void mainError() {
        Flowable.error(new TestException())
        .publish(Functions.<Flowable<Object>>identity())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void selectorInnerError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> v) throws Exception {
                return Flowable.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void preNextConnect() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final ConnectableFlowable<Integer> cf = Flowable.<Integer>empty().publish();

            cf.connect();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    cf.test();
                }
            };

            TestHelper.race(r1, r1);
        }
    }

    @Test
    public void connectRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final ConnectableFlowable<Integer> cf = Flowable.<Integer>empty().publish();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    cf.connect();
                }
            };

            TestHelper.race(r1, r1);
        }
    }

    @Test
    public void selectorCrash() {
        Flowable.just(1).publish(new Function<Flowable<Integer>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Integer> v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void pollThrows() {
        Flowable.just(1)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .compose(TestHelper.flowableStripBoundary())
        .publish()
        .autoConnect()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void pollThrowsNoSubscribers() {
        ConnectableFlowable<Integer> cf = Flowable.just(1, 2)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                if (v == 2) {
                    throw new TestException();
                }
                return v;
            }
        })
        .compose(TestHelper.<Integer>flowableStripBoundary())
        .publish();

        TestSubscriber<Integer> ts = cf.take(1)
        .test();

        cf.connect();

        ts.assertResult(1);
    }

    @Test
    public void dryRunCrash() {
        final TestSubscriber<Object> ts = new TestSubscriber<Object>(1L) {
            @Override
            public void onNext(Object t) {
                super.onNext(t);
                onComplete();
                cancel();
            }
        };

        Flowable<Object> source = Flowable.range(1, 10)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                if (v == 2) {
                    throw new TestException();
                }
                return v;
            }
        })
        .publish()
        .autoConnect();

        source.subscribe(ts);

        ts
        .assertResult(1);

        // 3.x: terminal events remain observable until reset
        source.test()
        .assertFailure(TestException.class);
    }

    @Test
    public void overflowQueue() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.create(new FlowableOnSubscribe<Object>() {
                @Override
                public void subscribe(FlowableEmitter<Object> s) throws Exception {
                    for (int i = 0; i < 10; i++) {
                        s.onNext(i);
                    }
                }
            }, BackpressureStrategy.MISSING)
            .publish(8)
            .autoConnect()
            .test(0L)
            // 3.x emits errors last, even the full queue errors
            .requestMore(10)
            .assertFailure(MissingBackpressureException.class, 0, 1, 2, 3, 4, 5, 6, 7);

            TestHelper.assertError(errors, 0, MissingBackpressureException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void delayedUpstreamOnSubscribe() {
        final Subscriber<?>[] sub = { null };

        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                sub[0] = s;
            }
        }
        .publish()
        .connect()
        .dispose();

        BooleanSubscription bs = new BooleanSubscription();

        sub[0].onSubscribe(bs);

        assertTrue(bs.isCancelled());
    }

    @Test
    public void disposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final AtomicReference<Disposable> ref = new AtomicReference<>();

            final ConnectableFlowable<Integer> cf = new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    ref.set((Disposable)s);
                }
            }.publish();

            cf.connect();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ref.get().dispose();
                }
            };

            TestHelper.race(r1, r1);
        }
    }

    @Test
    public void removeNotPresent() {
        final AtomicReference<PublishConnection<Integer>> ref = new AtomicReference<>();

        final ConnectableFlowable<Integer> cf = new Flowable<Integer>() {
            @Override
            @SuppressWarnings("unchecked")
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                ref.set((PublishConnection<Integer>)s);
            }
        }.publish();

        cf.connect();

        ref.get().add(new InnerSubscription<>(new TestSubscriber<>(), ref.get()));
        ref.get().remove(null);
    }

    @Test
    public void subscriberSwap() {
        final ConnectableFlowable<Integer> cf = Flowable.range(1, 5).publish();

        cf.connect();

        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        cf.subscribe(ts1);

        ts1.assertResult(1);

        TestSubscriber<Integer> ts2 = new TestSubscriber<>(0);
        cf.subscribe(ts2);

        ts2
        .assertEmpty()
        .requestMore(4)
        .assertResult(2, 3, 4, 5);
    }

    @Test
    public void subscriberLiveSwap() {
        final ConnectableFlowable<Integer> cf = Flowable.range(1, 5).publish();

        final TestSubscriber<Integer> ts2 = new TestSubscriber<>(0);

        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
                cf.subscribe(ts2);
            }
        };

        cf.subscribe(ts1);

        cf.connect();

        ts1.assertResult(1);

        ts2
        .assertEmpty()
        .requestMore(4)
        .assertResult(2, 3, 4, 5);
    }

    @Test
    public void selectorSubscriberSwap() {
        final AtomicReference<Flowable<Integer>> ref = new AtomicReference<>();

        Flowable.range(1, 5).publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> f) throws Exception {
                ref.set(f);
                return Flowable.never();
            }
        }).test();

        ref.get().take(2).test().assertResult(1, 2);

        ref.get()
        .test(0)
        .assertEmpty()
        .requestMore(2)
        .assertValuesOnly(3, 4)
        .requestMore(1)
        .assertResult(3, 4, 5);
    }

    @Test
    public void leavingSubscriberOverrequests() {
        final AtomicReference<Flowable<Integer>> ref = new AtomicReference<>();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> f) throws Exception {
                ref.set(f);
                return Flowable.never();
            }
        }).test();

        TestSubscriber<Integer> ts1 = ref.get().take(2).test();

        pp.onNext(1);
        pp.onNext(2);

        ts1.assertResult(1, 2);

        pp.onNext(3);
        pp.onNext(4);

        TestSubscriber<Integer> ts2 = ref.get().test(0L);

        ts2.assertEmpty();

        ts2.requestMore(2);

        ts2.assertValuesOnly(3, 4);
    }

    // call a transformer only if the input is non-empty
    @Test
    public void composeIfNotEmpty() {
        final FlowableTransformer<Integer, Integer> transformer = new FlowableTransformer<Integer, Integer>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> g) {
                return g.map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) throws Exception {
                        return v + 1;
                    }
                });
            }
        };

        final AtomicInteger calls = new AtomicInteger();
        Flowable.range(1, 5)
        .publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(final Flowable<Integer> shared)
                    throws Exception {
                return shared.take(1).concatMap(new Function<Integer, Publisher<? extends Integer>>() {
                    @Override
                    public Publisher<? extends Integer> apply(Integer first)
                            throws Exception {
                        calls.incrementAndGet();
                        return transformer.apply(Flowable.just(first).concatWith(shared));
                    }
                });
            }
        })
        .test()
        .assertResult(2, 3, 4, 5, 6);

        assertEquals(1, calls.get());
    }

    // call a transformer only if the input is non-empty
    @Test
    public void composeIfNotEmptyNotFused() {
        final FlowableTransformer<Integer, Integer> transformer = new FlowableTransformer<Integer, Integer>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> g) {
                return g.map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) throws Exception {
                        return v + 1;
                    }
                });
            }
        };

        final AtomicInteger calls = new AtomicInteger();
        Flowable.range(1, 5).hide()
        .publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(final Flowable<Integer> shared)
                    throws Exception {
                return shared.take(1).concatMap(new Function<Integer, Publisher<? extends Integer>>() {
                    @Override
                    public Publisher<? extends Integer> apply(Integer first)
                            throws Exception {
                        calls.incrementAndGet();
                        return transformer.apply(Flowable.just(first).concatWith(shared));
                    }
                });
            }
        })
        .test()
        .assertResult(2, 3, 4, 5, 6);

        assertEquals(1, calls.get());
    }

    // call a transformer only if the input is non-empty
    @Test
    public void composeIfNotEmptyIsEmpty() {
        final FlowableTransformer<Integer, Integer> transformer = new FlowableTransformer<Integer, Integer>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> g) {
                return g.map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) throws Exception {
                        return v + 1;
                    }
                });
            }
        };

        final AtomicInteger calls = new AtomicInteger();
        Flowable.<Integer>empty().hide()
        .publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(final Flowable<Integer> shared)
                    throws Exception {
                return shared.take(1).concatMap(new Function<Integer, Publisher<? extends Integer>>() {
                    @Override
                    public Publisher<? extends Integer> apply(Integer first)
                            throws Exception {
                        calls.incrementAndGet();
                        return transformer.apply(Flowable.just(first).concatWith(shared));
                    }
                });
            }
        })
        .test()
        .assertResult();

        assertEquals(0, calls.get());
    }

    @Test
    public void publishFunctionCancelOuterAfterOneInner() {
        final AtomicReference<Flowable<Integer>> ref = new AtomicReference<>();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        final TestSubscriber<Integer> ts = pp.publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> f) throws Exception {
                ref.set(f);
                return Flowable.never();
            }
        }).test();

        ref.get().subscribe(new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                onComplete();
                ts.cancel();
            }
        });

        pp.onNext(1);
    }

    @Test
    public void publishFunctionCancelOuterAfterOneInnerBackpressured() {
        final AtomicReference<Flowable<Integer>> ref = new AtomicReference<>();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        final TestSubscriber<Integer> ts = pp.publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> f) throws Exception {
                ref.set(f);
                return Flowable.never();
            }
        }).test();

        ref.get().subscribe(new TestSubscriber<Integer>(1L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                onComplete();
                ts.cancel();
            }
        });

        pp.onNext(1);
    }

    @Test
    public void publishCancelOneAsync() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final AtomicReference<Flowable<Integer>> ref = new AtomicReference<>();

            pp.publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Flowable<Integer> f) throws Exception {
                    ref.set(f);
                    return Flowable.never();
                }
            }).test();

            final TestSubscriber<Integer> ts1 = ref.get().test();
            TestSubscriber<Integer> ts2 = ref.get().test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            TestHelper.race(r1, r2);

            ts2.assertValuesOnly(1);
        }
    }

    @Test
    public void publishCancelOneAsync2() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        ConnectableFlowable<Integer> cf = pp.publish();

        final TestSubscriber<Integer> ts1 = new TestSubscriber<>();

        final AtomicReference<InnerSubscription<Integer>> ref = new AtomicReference<>();

        cf.subscribe(new FlowableSubscriber<Integer>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onSubscribe(Subscription s) {
                ts1.onSubscribe(new BooleanSubscription());
                // pretend to be cancelled without removing it from the subscriber list
                ref.set((InnerSubscription<Integer>)s);
            }

            @Override
            public void onNext(Integer t) {
                ts1.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                ts1.onError(t);
            }

            @Override
            public void onComplete() {
                ts1.onComplete();
            }
        });
        TestSubscriber<Integer> ts2 = cf.test();

        cf.connect();

        ref.get().set(Long.MIN_VALUE);

        pp.onNext(1);

        ts1.assertEmpty();
        ts2.assertValuesOnly(1);
    }

    @Test
    public void boundaryFusion() {
        Flowable.range(1, 10000)
        .observeOn(Schedulers.single())
        .map(new Function<Integer, String>() {
            @Override
            public String apply(Integer t) throws Exception {
                String name = Thread.currentThread().getName();
                if (name.contains("RxSingleScheduler")) {
                    return "RxSingleScheduler";
                }
                return name;
            }
        })
        .share()
        .observeOn(Schedulers.computation())
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("RxSingleScheduler");
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.range(1, 5).publish());
    }

    @Test
    public void splitCombineSubscriberChangeAfterOnNext() {
        Flowable<Integer> source = Flowable.range(0, 20)
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription v) throws Exception {
                System.out.println("Subscribed");
            }
        })
        .publish(10)
        .refCount()
        ;

        Flowable<Integer> evenNumbers = source.filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        });

        Flowable<Integer> oddNumbers = source.filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 != 0;
            }
        });

        final Single<Integer> getNextOdd = oddNumbers.first(0);

        TestSubscriber<List<Integer>> ts = evenNumbers.concatMap(new Function<Integer, Publisher<List<Integer>>>() {
            @Override
            public Publisher<List<Integer>> apply(Integer v) throws Exception {
                return Single.zip(
                        Single.just(v), getNextOdd,
                        new BiFunction<Integer, Integer, List<Integer>>() {
                            @Override
                            public List<Integer> apply(Integer a, Integer b) throws Exception {
                                return Arrays.asList( a, b );
                            }
                        }
                )
                .toFlowable();
            }
        })
        .takeWhile(new Predicate<List<Integer>>() {
            @Override
            public boolean test(List<Integer> v) throws Exception {
                return v.get(0) < 20;
            }
        })
        .test();

        ts
        .assertResult(
                Arrays.asList(0, 1),
                Arrays.asList(2, 3),
                Arrays.asList(4, 5),
                Arrays.asList(6, 7),
                Arrays.asList(8, 9),
                Arrays.asList(10, 11),
                Arrays.asList(12, 13),
                Arrays.asList(14, 15),
                Arrays.asList(16, 17),
                Arrays.asList(18, 19)
        );
    }

    @Test
    public void splitCombineSubscriberChangeAfterOnNextFused() {
        Flowable<Integer> source = Flowable.range(0, 20)
        .publish(10)
        .refCount()
        ;

        Flowable<Integer> evenNumbers = source.filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        });

        Flowable<Integer> oddNumbers = source.filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 != 0;
            }
        });

        final Single<Integer> getNextOdd = oddNumbers.first(0);

        TestSubscriber<List<Integer>> ts = evenNumbers.concatMap(new Function<Integer, Publisher<List<Integer>>>() {
            @Override
            public Publisher<List<Integer>> apply(Integer v) throws Exception {
                return Single.zip(
                        Single.just(v), getNextOdd,
                        new BiFunction<Integer, Integer, List<Integer>>() {
                            @Override
                            public List<Integer> apply(Integer a, Integer b) throws Exception {
                                return Arrays.asList( a, b );
                            }
                        }
                )
                .toFlowable();
            }
        })
        .takeWhile(new Predicate<List<Integer>>() {
            @Override
            public boolean test(List<Integer> v) throws Exception {
                return v.get(0) < 20;
            }
        })
        .test();

        ts
        .assertResult(
                Arrays.asList(0, 1),
                Arrays.asList(2, 3),
                Arrays.asList(4, 5),
                Arrays.asList(6, 7),
                Arrays.asList(8, 9),
                Arrays.asList(10, 11),
                Arrays.asList(12, 13),
                Arrays.asList(14, 15),
                Arrays.asList(16, 17),
                Arrays.asList(18, 19)
        );
    }

    @Test
    public void altConnectCrash() {
        try {
            new FlowablePublish<>(Flowable.<Integer>empty(), 128)
            .connect(new Consumer<Disposable>() {
                @Override
                public void accept(Disposable t) throws Exception {
                    throw new TestException();
                }
            });
            fail("Should have thrown");
        } catch (TestException expected) {
            // expected
        }
    }

    @Test
    public void altConnectRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final ConnectableFlowable<Integer> cf =
                    new FlowablePublish<>(Flowable.<Integer>never(), 128);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    cf.connect();
                }
            };

            TestHelper.race(r, r);
        }
    }

    @Test
    public void fusedPollCrash() {
        Flowable.range(1, 5)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .compose(TestHelper.flowableStripBoundary())
        .publish()
        .refCount()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void syncFusedNoRequest() {
        Flowable.range(1, 5)
        .publish(1)
        .refCount()
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normalBackpressuredPolls() {
        Flowable.range(1, 5)
        .hide()
        .publish(1)
        .refCount()
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void emptyHidden() {
        Flowable.empty()
        .hide()
        .publish(1)
        .refCount()
        .test()
        .assertResult();
    }

    @Test
    public void emptyFused() {
        Flowable.empty()
        .publish(1)
        .refCount()
        .test()
        .assertResult();
    }

    @Test
    public void overflowQueueRefCount() {
        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                s.onNext(1);
                s.onNext(2);
            }
        }
        .publish(1)
        .refCount()
        .test(0)
        .requestMore(1)
        .assertFailure(MissingBackpressureException.class, 1);
    }

    @Test
    public void doubleErrorRefCount() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onError(new TestException("one"));
                    s.onError(new TestException("two"));
                }
            }
            .publish(1)
            .refCount()
            .to(TestHelper.<Integer>testSubscriber(0L))
            .assertFailureAndMessage(TestException.class, "one");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "two");
            assertEquals(1, errors.size());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteAvailableUntilReset() {
        ConnectableFlowable<Integer> cf = Flowable.just(1).publish();

        TestSubscriber<Integer> ts = cf.test();
        ts.assertEmpty();

        cf.connect();

        ts.assertResult(1);

        cf.test().assertResult();

        cf.reset();

        ts = cf.test();
        ts.assertEmpty();

        cf.connect();

        ts.assertResult(1);
    }

    @Test
    public void onErrorAvailableUntilReset() {
        ConnectableFlowable<Integer> cf = Flowable.just(1)
                .concatWith(Flowable.<Integer>error(new TestException()))
                .publish();

        TestSubscriber<Integer> ts = cf.test();
        ts.assertEmpty();

        cf.connect();

        ts.assertFailure(TestException.class, 1);

        cf.test().assertFailure(TestException.class);

        cf.reset();

        ts = cf.test();
        ts.assertEmpty();

        cf.connect();

        ts.assertFailure(TestException.class, 1);
    }

    @Test
    public void disposeResets() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        ConnectableFlowable<Integer> cf = pp.publish();

        assertFalse(pp.hasSubscribers());

        Disposable d = cf.connect();

        assertTrue(pp.hasSubscribers());

        d.dispose();

        assertFalse(pp.hasSubscribers());

        TestSubscriber<Integer> ts = cf.test();

        cf.connect();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts.assertValuesOnly(1);
    }
}
