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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.internal.schedulers.ImmediateThinScheduler;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.TestSubscriber;

public class FlowablePublishTest {

    @Test
    public void testPublish() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        ConnectableFlowable<String> o = Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(final Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        observer.onNext("one");
                        observer.onComplete();
                    }
                }).start();
            }
        }).publish();

        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(new Consumer<String>() {

            @Override
            public void accept(String v) {
                assertEquals("one", v);
                latch.countDown();
            }
        });

        // subscribe again
        o.subscribe(new Consumer<String>() {

            @Override
            public void accept(String v) {
                assertEquals("one", v);
                latch.countDown();
            }
        });

        Disposable s = o.connect();
        try {
            if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                fail("subscriptions did not receive values");
            }
            assertEquals(1, counter.get());
        } finally {
            s.dispose();
        }
    }

    @Test
    public void testBackpressureFastSlow() {
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

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.merge(fast, slow).subscribe(ts);
        is.connect();
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 4, ts.valueCount());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void testTakeUntilWithPublishedStreamUsingSelector() {
        final AtomicInteger emitted = new AtomicInteger();
        Flowable<Integer> xs = Flowable.range(0, Flowable.bufferSize() * 2).doOnNext(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                emitted.incrementAndGet();
            }

        });
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
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
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        ts.assertValues(0, 1, 2, 3);
        assertEquals(5, emitted.get());
        System.out.println(ts.values());
    }

    // use case from https://github.com/ReactiveX/RxJava/issues/1732
    @Test
    public void testTakeUntilWithPublishedStream() {
        Flowable<Integer> xs = Flowable.range(0, Flowable.bufferSize() * 2);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
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

    @Test(timeout = 10000)
    public void testBackpressureTwoConsumers() {
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

        final TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>();

        final TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                if (valueCount() == 2) {
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

        ts1.awaitTerminalEvent();
        ts2.awaitTerminalEvent();

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
    public void testConnectWithNoSubscriber() {
        TestScheduler scheduler = new TestScheduler();
        ConnectableFlowable<Long> co = Flowable.interval(10, 10, TimeUnit.MILLISECONDS, scheduler).take(3).publish();
        co.connect();
        // Emit 0
        scheduler.advanceTimeBy(15, TimeUnit.MILLISECONDS);
        TestSubscriber<Long> subscriber = new TestSubscriber<Long>();
        co.subscribe(subscriber);
        // Emit 1 and 2
        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
        subscriber.assertValues(1L, 2L);
        subscriber.assertNoErrors();
        subscriber.assertTerminated();
    }

    @Test
    public void testSubscribeAfterDisconnectThenConnect() {
        ConnectableFlowable<Integer> source = Flowable.just(1).publish();

        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>();

        source.subscribe(ts1);

        Disposable s = source.connect();

        ts1.assertValue(1);
        ts1.assertNoErrors();
        ts1.assertTerminated();

        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>();

        source.subscribe(ts2);

        Disposable s2 = source.connect();

        ts2.assertValue(1);
        ts2.assertNoErrors();
        ts2.assertTerminated();

        System.out.println(s);
        System.out.println(s2);
    }

    @Test
    public void testNoSubscriberRetentionOnCompleted() {
        FlowablePublish<Integer> source = (FlowablePublish<Integer>)Flowable.just(1).publish();

        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>();

        source.subscribe(ts1);

        ts1.assertNoValues();
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        source.connect();

        ts1.assertValue(1);
        ts1.assertNoErrors();
        ts1.assertTerminated();

        assertNull(source.current.get());
    }

    @Test
    public void testNonNullConnection() {
        ConnectableFlowable<Object> source = Flowable.never().publish();

        assertNotNull(source.connect());
        assertNotNull(source.connect());
    }

    @Test
    public void testNoDisconnectSomeoneElse() {
        ConnectableFlowable<Object> source = Flowable.never().publish();

        Disposable s1 = source.connect();
        Disposable s2 = source.connect();

        s1.dispose();

        Disposable s3 = source.connect();

        s2.dispose();

        assertTrue(checkPublishDisposed(s1));
        assertTrue(checkPublishDisposed(s2));
        assertFalse(checkPublishDisposed(s3));
    }

    @SuppressWarnings("unchecked")
    static boolean checkPublishDisposed(Disposable d) {
        return ((FlowablePublish.PublishSubscriber<Object>)d).isDisposed();
    }

    @Test
    public void testZeroRequested() {
        ConnectableFlowable<Integer> source = Flowable.just(1).publish();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

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
    public void testConnectIsIdempotent() {
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
        ConnectableFlowable<Integer> co = Flowable.range(0, 1000).publish();
        Flowable<Integer> obs = co.observeOn(Schedulers.computation());
        for (int i = 0; i < 1000; i++) {
            for (int j = 1; j < 6; j++) {
                List<TestSubscriber<Integer>> tss = new ArrayList<TestSubscriber<Integer>>();
                for (int k = 1; k < j; k++) {
                    TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
                    tss.add(ts);
                    obs.subscribe(ts);
                }

                Disposable s = co.connect();

                for (TestSubscriber<Integer> ts : tss) {
                    ts.awaitDone(5, TimeUnit.SECONDS)
                    .assertSubscribed()
                    .assertValueCount(1000)
                    .assertNoErrors()
                    .assertComplete();
                }
                s.dispose();
            }
        }
    }

    @Test
    public void syncFusedObserveOn2() {
        ConnectableFlowable<Integer> co = Flowable.range(0, 1000).publish();
        Flowable<Integer> obs = co.observeOn(ImmediateThinScheduler.INSTANCE);
        for (int i = 0; i < 1000; i++) {
            for (int j = 1; j < 6; j++) {
                List<TestSubscriber<Integer>> tss = new ArrayList<TestSubscriber<Integer>>();
                for (int k = 1; k < j; k++) {
                    TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
                    tss.add(ts);
                    obs.subscribe(ts);
                }

                Disposable s = co.connect();

                for (TestSubscriber<Integer> ts : tss) {
                    ts.awaitDone(5, TimeUnit.SECONDS)
                    .assertSubscribed()
                    .assertValueCount(1000)
                    .assertNoErrors()
                    .assertComplete();
                }
                s.dispose();
            }
        }
    }

    @Test
    public void asyncFusedObserveOn() {
        ConnectableFlowable<Integer> co = Flowable.range(0, 1000).observeOn(ImmediateThinScheduler.INSTANCE).publish();
        for (int i = 0; i < 1000; i++) {
            for (int j = 1; j < 6; j++) {
                List<TestSubscriber<Integer>> tss = new ArrayList<TestSubscriber<Integer>>();
                for (int k = 1; k < j; k++) {
                    TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
                    tss.add(ts);
                    co.subscribe(ts);
                }

                Disposable s = co.connect();

                for (TestSubscriber<Integer> ts : tss) {
                    ts.awaitDone(5, TimeUnit.SECONDS)
                    .assertSubscribed()
                    .assertValueCount(1000)
                    .assertNoErrors()
                    .assertComplete();
                }
                s.dispose();
            }
        }
    }

    @Test
    public void testObserveOn() {
        ConnectableFlowable<Integer> co = Flowable.range(0, 1000).hide().publish();
        Flowable<Integer> obs = co.observeOn(Schedulers.computation());
        for (int i = 0; i < 1000; i++) {
            for (int j = 1; j < 6; j++) {
                List<TestSubscriber<Integer>> tss = new ArrayList<TestSubscriber<Integer>>();
                for (int k = 1; k < j; k++) {
                    TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
                    tss.add(ts);
                    obs.subscribe(ts);
                }

                Disposable s = co.connect();

                for (TestSubscriber<Integer> ts : tss) {
                    ts.awaitDone(5, TimeUnit.SECONDS)
                    .assertSubscribed()
                    .assertValueCount(1000)
                    .assertNoErrors()
                    .assertComplete();
                }
                s.dispose();
            }
        }
    }

    @Test
    public void source() {
        Flowable<Integer> o = Flowable.never();

        assertSame(o, (((HasUpstreamPublisher<?>)o.publish()).source()));
    }

    @Test
    public void connectThrows() {
        ConnectableFlowable<Integer> co = Flowable.<Integer>empty().publish();
        try {
            co.connect(new Consumer<Disposable>() {
                @Override
                public void accept(Disposable s) throws Exception {
                    throw new TestException();
                }
            });
        } catch (TestException ex) {
            // expected
        }
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < 500; i++) {

            final ConnectableFlowable<Integer> co = Flowable.<Integer>empty().publish();

            final TestSubscriber<Integer> to = co.test();

            final TestSubscriber<Integer> to2 = new TestSubscriber<Integer>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to2);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void disposeOnArrival() {
        ConnectableFlowable<Integer> co = Flowable.<Integer>empty().publish();

        co.test(Long.MAX_VALUE, true).assertEmpty();
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
        ConnectableFlowable<Integer> co = Flowable.<Integer>empty().publish();

        co.connect();
    }

    @Test
    public void take() {
        ConnectableFlowable<Integer> co = Flowable.range(1, 2).publish();

        TestSubscriber<Integer> to = co.take(1).test();

        co.connect();

        to.assertResult(1);
    }

    @Test
    public void just() {
        final PublishProcessor<Integer> ps = PublishProcessor.create();

        ConnectableFlowable<Integer> co = ps.publish();

        TestSubscriber<Integer> to = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                ps.onComplete();
            }
        };

        co.subscribe(to);
        co.connect();

        ps.onNext(1);

        to.assertResult(1);
    }

    @Test
    public void nextCancelRace() {
        for (int i = 0; i < 500; i++) {

            final PublishProcessor<Integer> ps = PublishProcessor.create();

            final ConnectableFlowable<Integer> co = ps.publish();

            final TestSubscriber<Integer> to = co.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
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
                protected void subscribeActual(Subscriber<? super Integer> observer) {
                    observer.onSubscribe(new BooleanSubscription());
                    observer.onNext(1);
                    observer.onComplete();
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
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
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ConnectableFlowable<Object> co = Flowable.error(new TestException()).publish();

            co.connect();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void subscribeDisconnectRace() {
        for (int i = 0; i < 500; i++) {

            final PublishProcessor<Integer> ps = PublishProcessor.create();

            final ConnectableFlowable<Integer> co = ps.publish();

            final Disposable d = co.connect();
            final TestSubscriber<Integer> to = new TestSubscriber<Integer>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    d.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    co.subscribe(to);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void selectorDisconnectsIndependentSource() {
        PublishProcessor<Integer> ps = PublishProcessor.create();

        ps.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> v) throws Exception {
                return Flowable.range(1, 2);
            }
        })
        .test()
        .assertResult(1, 2);

        assertFalse(ps.hasSubscribers());
    }

    @Test(timeout = 5000)
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
        PublishProcessor<Integer> ps = PublishProcessor.create();

        ps.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> v) throws Exception {
                return Flowable.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertFalse(ps.hasSubscribers());
    }

    @Test
    public void preNextConnect() {
        for (int i = 0; i < 500; i++) {

            final ConnectableFlowable<Integer> co = Flowable.<Integer>empty().publish();

            co.connect();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.test();
                }
            };

            TestHelper.race(r1, r1);
        }
    }

    @Test
    public void connectRace() {
        for (int i = 0; i < 500; i++) {

            final ConnectableFlowable<Integer> co = Flowable.<Integer>empty().publish();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    co.connect();
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
        .publish()
        .autoConnect()
        .test()
        .assertFailure(TestException.class);
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

        Flowable.range(1, 10)
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
        .autoConnect()
        .subscribe(ts);

        ts
        .assertResult(1);
    }

    @Test
    public void overflowQueue() {
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
        .assertFailure(MissingBackpressureException.class);
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
}
