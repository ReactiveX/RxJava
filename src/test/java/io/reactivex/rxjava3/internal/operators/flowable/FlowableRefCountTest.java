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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableRefCount.RefConnection;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableRefCountTest extends RxJavaTest {

    @Test
    public void refCountAsync() {
        final AtomicInteger subscribeCount = new AtomicInteger();
        final AtomicInteger nextCount = new AtomicInteger();
        Flowable<Long> r = Flowable.interval(0, 20, TimeUnit.MILLISECONDS)
                .doOnSubscribe(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription s) {
                        subscribeCount.incrementAndGet();
                    }
                })
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long l) {
                        nextCount.incrementAndGet();
                    }
                })
                .publish().refCount();

        final AtomicInteger receivedCount = new AtomicInteger();
        Disposable d1 = r.subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long l) {
                receivedCount.incrementAndGet();
            }
        });

        Disposable d2 = r.subscribe();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }

        for (;;) {
            int a = nextCount.get();
            int b = receivedCount.get();
            if (a > 10 && a < 20 && a == b) {
                break;
            }
            if (a >= 20) {
                break;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
            }
        }
        // give time to emit

        // now unsubscribe
        d2.dispose(); // unsubscribe s2 first as we're counting in 1 and there can be a race between unsubscribe and one subscriber getting a value but not the other
        d1.dispose();

        System.out.println("onNext: " + nextCount.get());

        // should emit once for both subscribers
        assertEquals(nextCount.get(), receivedCount.get());
        // only 1 subscribe
        assertEquals(1, subscribeCount.get());
    }

    @Test
    public void refCountSynchronous() {
        final AtomicInteger subscribeCount = new AtomicInteger();
        final AtomicInteger nextCount = new AtomicInteger();
        Flowable<Integer> r = Flowable.just(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .doOnSubscribe(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription s) {
                        subscribeCount.incrementAndGet();
                    }
                })
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer l) {
                        nextCount.incrementAndGet();
                    }
                })
                .publish().refCount();

        final AtomicInteger receivedCount = new AtomicInteger();
        Disposable d1 = r.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer l) {
                receivedCount.incrementAndGet();
            }
        });

        Disposable d2 = r.subscribe();

        // give time to emit
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }

        // now unsubscribe
        d2.dispose(); // unsubscribe s2 first as we're counting in 1 and there can be a race between unsubscribe and one subscriber getting a value but not the other
        d1.dispose();

        System.out.println("onNext Count: " + nextCount.get());

        // it will emit twice because it is synchronous
        assertEquals(nextCount.get(), receivedCount.get() * 2);
        // it will subscribe twice because it is synchronous
        assertEquals(2, subscribeCount.get());
    }

    @Test
    public void refCountSynchronousTake() {
        final AtomicInteger nextCount = new AtomicInteger();
        Flowable<Integer> r = Flowable.just(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer l) {
                            System.out.println("onNext --------> " + l);
                            nextCount.incrementAndGet();
                    }
                })
                .take(4)
                .publish().refCount();

        final AtomicInteger receivedCount = new AtomicInteger();
        r.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer l) {
                receivedCount.incrementAndGet();
            }
        });

        System.out.println("onNext: " + nextCount.get());

        assertEquals(4, receivedCount.get());
        assertEquals(4, receivedCount.get());
    }

    @Test
    public void repeat() {
        final AtomicInteger subscribeCount = new AtomicInteger();
        final AtomicInteger unsubscribeCount = new AtomicInteger();
        Flowable<Long> r = Flowable.interval(0, 1, TimeUnit.MILLISECONDS)
                .doOnSubscribe(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription s) {
                            System.out.println("******************************* Subscribe received");
                            // when we are subscribed
                            subscribeCount.incrementAndGet();
                    }
                })
                .doOnCancel(new Action() {
                    @Override
                    public void run() {
                            System.out.println("******************************* Unsubscribe received");
                            // when we are unsubscribed
                            unsubscribeCount.incrementAndGet();
                    }
                })
                .publish().refCount();

        for (int i = 0; i < 10; i++) {
            TestSubscriber<Long> ts1 = new TestSubscriber<>();
            TestSubscriber<Long> ts2 = new TestSubscriber<>();
            r.subscribe(ts1);
            r.subscribe(ts2);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            ts1.cancel();
            ts2.cancel();
            ts1.assertNoErrors();
            ts2.assertNoErrors();
            assertTrue(ts1.values().size() > 0);
            assertTrue(ts2.values().size() > 0);
        }

        assertEquals(10, subscribeCount.get());
        assertEquals(10, unsubscribeCount.get());
    }

    @Test
    public void connectUnsubscribe() throws InterruptedException {
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        Flowable<Long> f = synchronousInterval()
                .doOnSubscribe(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription s) {
                            System.out.println("******************************* Subscribe received");
                            // when we are subscribed
                            subscribeLatch.countDown();
                    }
                })
                .doOnCancel(new Action() {
                    @Override
                    public void run() {
                            System.out.println("******************************* Unsubscribe received");
                            // when we are unsubscribed
                            unsubscribeLatch.countDown();
                    }
                });

        TestSubscriberEx<Long> s = new TestSubscriberEx<>();
        f.publish().refCount().subscribeOn(Schedulers.newThread()).subscribe(s);
        System.out.println("send unsubscribe");
        // wait until connected
        subscribeLatch.await();
        // now unsubscribe
        s.cancel();
        System.out.println("DONE sending unsubscribe ... now waiting");
        if (!unsubscribeLatch.await(3000, TimeUnit.MILLISECONDS)) {
            System.out.println("Errors: " + s.errors());
            if (s.errors().size() > 0) {
                s.errors().get(0).printStackTrace();
            }
            fail("timed out waiting for unsubscribe");
        }
        s.assertNoErrors();
    }

    @Test
    public void connectUnsubscribeRaceConditionLoop() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            connectUnsubscribeRaceCondition();
        }
    }

    @Test
    public void connectUnsubscribeRaceCondition() throws InterruptedException {
        final AtomicInteger subUnsubCount = new AtomicInteger();
        Flowable<Long> f = synchronousInterval()
                .doOnCancel(new Action() {
                    @Override
                    public void run() {
                            System.out.println("******************************* Unsubscribe received");
                            // when we are unsubscribed
                            subUnsubCount.decrementAndGet();
                    }
                })
                .doOnSubscribe(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription s) {
                            System.out.println("******************************* SUBSCRIBE received");
                            subUnsubCount.incrementAndGet();
                    }
                });

        TestSubscriberEx<Long> s = new TestSubscriberEx<>();

        f.publish().refCount().subscribeOn(Schedulers.computation()).subscribe(s);
        System.out.println("send unsubscribe");
        // now immediately unsubscribe while subscribeOn is racing to subscribe
        s.cancel();
        // this generally will mean it won't even subscribe as it is already unsubscribed by the time connect() gets scheduled
        // give time to the counter to update
        Thread.sleep(10);
        // either we subscribed and then unsubscribed, or we didn't ever even subscribe
        assertEquals(0, subUnsubCount.get());

        System.out.println("DONE sending unsubscribe ... now waiting");
        System.out.println("Errors: " + s.errors());
        if (s.errors().size() > 0) {
            s.errors().get(0).printStackTrace();
        }
        s.assertNoErrors();
    }

    private Flowable<Long> synchronousInterval() {
        return Flowable.unsafeCreate(new Publisher<Long>() {
            @Override
            public void subscribe(Subscriber<? super Long> subscriber) {
                final AtomicBoolean cancel = new AtomicBoolean();
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {

                    }

                    @Override
                    public void cancel() {
                        cancel.set(true);
                    }

                });
                for (;;) {
                    if (cancel.get()) {
                        break;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    subscriber.onNext(1L);
                }
            }
        });
    }

    @Test
    public void onlyFirstShouldSubscribeAndLastUnsubscribe() {
        final AtomicInteger subscriptionCount = new AtomicInteger();
        final AtomicInteger unsubscriptionCount = new AtomicInteger();
        Flowable<Integer> flowable = Flowable.unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> subscriber) {
                subscriptionCount.incrementAndGet();
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {

                    }

                    @Override
                    public void cancel() {
                        unsubscriptionCount.incrementAndGet();
                    }
                });
            }
        });
        Flowable<Integer> refCounted = flowable.publish().refCount();

        Disposable first = refCounted.subscribe();
        assertEquals(1, subscriptionCount.get());

        Disposable second = refCounted.subscribe();
        assertEquals(1, subscriptionCount.get());

        first.dispose();
        assertEquals(0, unsubscriptionCount.get());

        second.dispose();
        assertEquals(1, unsubscriptionCount.get());
    }

    @Test
    public void refCount() {
        TestScheduler s = new TestScheduler();
        Flowable<Long> interval = Flowable.interval(100, TimeUnit.MILLISECONDS, s).publish().refCount();

        // subscribe list1
        final List<Long> list1 = new ArrayList<>();
        Disposable d1 = interval.subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long t1) {
                list1.add(t1);
            }
        });

        s.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        assertEquals(2, list1.size());
        assertEquals(0L, list1.get(0).longValue());
        assertEquals(1L, list1.get(1).longValue());

        // subscribe list2
        final List<Long> list2 = new ArrayList<>();
        Disposable d2 = interval.subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long t1) {
                list2.add(t1);
            }
        });

        s.advanceTimeBy(300, TimeUnit.MILLISECONDS);

        // list 1 should have 5 items
        assertEquals(5, list1.size());
        assertEquals(2L, list1.get(2).longValue());
        assertEquals(3L, list1.get(3).longValue());
        assertEquals(4L, list1.get(4).longValue());

        // list 2 should only have 3 items
        assertEquals(3, list2.size());
        assertEquals(2L, list2.get(0).longValue());
        assertEquals(3L, list2.get(1).longValue());
        assertEquals(4L, list2.get(2).longValue());

        // unsubscribe list1
        d1.dispose();

        // advance further
        s.advanceTimeBy(300, TimeUnit.MILLISECONDS);

        // list 1 should still have 5 items
        assertEquals(5, list1.size());

        // list 2 should have 6 items
        assertEquals(6, list2.size());
        assertEquals(5L, list2.get(3).longValue());
        assertEquals(6L, list2.get(4).longValue());
        assertEquals(7L, list2.get(5).longValue());

        // unsubscribe list2
        d2.dispose();

        // advance further
        s.advanceTimeBy(1000, TimeUnit.MILLISECONDS);

        // subscribing a new one should start over because the source should have been unsubscribed
        // subscribe list3
        final List<Long> list3 = new ArrayList<>();
        interval.subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long t1) {
                list3.add(t1);
            }
        });

        s.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        assertEquals(2, list3.size());
        assertEquals(0L, list3.get(0).longValue());
        assertEquals(1L, list3.get(1).longValue());

    }

    @Test
    public void alreadyUnsubscribedClient() {
        Subscriber<Integer> done = CancelledSubscriber.INSTANCE;

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();

        Flowable<Integer> result = Flowable.just(1).publish().refCount();

        result.subscribe(done);

        result.subscribe(subscriber);

        verify(subscriber).onNext(1);
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void alreadyUnsubscribedInterleavesWithClient() {
        ReplayProcessor<Integer> source = ReplayProcessor.create();

        Subscriber<Integer> done = CancelledSubscriber.INSTANCE;

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(subscriber);

        Flowable<Integer> result = source.publish().refCount();

        result.subscribe(subscriber);

        source.onNext(1);

        result.subscribe(done);

        source.onNext(2);
        source.onComplete();

        inOrder.verify(subscriber).onNext(1);
        inOrder.verify(subscriber).onNext(2);
        inOrder.verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void connectDisconnectConnectAndSubjectState() {
        Flowable<Integer> f1 = Flowable.just(10);
        Flowable<Integer> f2 = Flowable.just(20);
        Flowable<Integer> combined = Flowable.combineLatest(f1, f2, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }
        })
        .publish().refCount();

        TestSubscriberEx<Integer> ts1 = new TestSubscriberEx<>();
        TestSubscriberEx<Integer> ts2 = new TestSubscriberEx<>();

        combined.subscribe(ts1);
        combined.subscribe(ts2);

        ts1.assertTerminated();
        ts1.assertNoErrors();
        ts1.assertValue(30);

        ts2.assertTerminated();
        ts2.assertNoErrors();
        ts2.assertValue(30);
    }

    @Test
    public void upstreamErrorAllowsRetry() throws InterruptedException {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final AtomicInteger intervalSubscribed = new AtomicInteger();
            Flowable<String> interval =
                    Flowable.interval(200, TimeUnit.MILLISECONDS)
                            .doOnSubscribe(new Consumer<Subscription>() {
                                @Override
                                public void accept(Subscription s) {
                                                System.out.println("Subscribing to interval " + intervalSubscribed.incrementAndGet());
                                        }
                            }
                             )
                            .flatMap(new Function<Long, Publisher<String>>() {
                                @Override
                                public Publisher<String> apply(Long t1) {
                                        return Flowable.defer(new Supplier<Publisher<String>>() {
                                            @Override
                                            public Publisher<String> get() {
                                                    return Flowable.<String>error(new TestException("Some exception"));
                                            }
                                        });
                                }
                            })
                            .onErrorResumeNext(new Function<Throwable, Publisher<String>>() {
                                @Override
                                public Publisher<String> apply(Throwable t1) {
                                        return Flowable.error(t1);
                                }
                            })
                            .publish()
                            .refCount();

            interval
                    .doOnError(new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable t1) {
                                System.out.println("Subscriber 1 onError: " + t1);
                        }
                    })
                    .retry(5)
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String t1) {
                                System.out.println("Subscriber 1: " + t1);
                        }
                    });
            Thread.sleep(100);
            interval
            .doOnError(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable t1) {
                        System.out.println("Subscriber 2 onError: " + t1);
                }
            })
            .retry(5)
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String t1) {
                                System.out.println("Subscriber 2: " + t1);
                        }
                    });

            Thread.sleep(1300);

            System.out.println(intervalSubscribed.get());
            assertEquals(6, intervalSubscribed.get());

            TestHelper.assertError(errors, 0, OnErrorNotImplementedException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    private enum CancelledSubscriber implements FlowableSubscriber<Integer> {
        INSTANCE;

        @Override public void onSubscribe(Subscription s) {
            s.cancel();
        }

        @Override public void onNext(Integer o) {
        }

        @Override public void onError(Throwable t) {
        }

        @Override public void onComplete() {
        }
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Flowable.just(1).publish().refCount());
    }

    @Test
    public void noOpConnect() {
        final int[] calls = { 0 };
        Flowable<Integer> f = new ConnectableFlowable<Integer>() {
            @Override
            public void connect(Consumer<? super Disposable> connection) {
                calls[0]++;
            }

            @Override
            public void reset() {
                // nothing to do in this test
            }

            @Override
            protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
            }
        }.refCount();

        f.test();
        f.test();

        assertEquals(1, calls[0]);
    }

    Flowable<Object> source;

    @Test
    public void replayNoLeak() throws Exception {
        Thread.sleep(100);
        System.gc();
        Thread.sleep(100);

        long start = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = Flowable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new byte[100 * 1000 * 1000];
            }
        })
        .replay(1)
        .refCount();

        source.subscribe();

        long after = TestHelper.awaitGC(GC_SLEEP_TIME, 20, start + 20 * 1000 * 1000);

        source = null;
        assertTrue(String.format("%,3d -> %,3d%n", start, after), start + 20 * 1000 * 1000 > after);
    }

    @Test
    public void replayNoLeak2() throws Exception {
        Thread.sleep(100);
        System.gc();
        Thread.sleep(100);

        long start = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = Flowable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new byte[100 * 1000 * 1000];
            }
        }).concatWith(Flowable.never())
        .replay(1)
        .refCount();

        Disposable d1 = source.subscribe();
        Disposable d2 = source.subscribe();

        d1.dispose();
        d2.dispose();

        d1 = null;
        d2 = null;

        long after = TestHelper.awaitGC(GC_SLEEP_TIME, 20, start + 20 * 1000 * 1000);

        source = null;
        assertTrue(String.format("%,3d -> %,3d%n", start, after), start + 20 * 1000 * 1000 > after);
    }

    static final class ExceptionData extends Exception {
        private static final long serialVersionUID = -6763898015338136119L;

        public final Object data;

        ExceptionData(Object data) {
            this.data = data;
        }
    }

    static final int GC_SLEEP_TIME = 250;

    @Test
    public void publishNoLeak() throws Exception {
        System.gc();
        Thread.sleep(GC_SLEEP_TIME);

        source = Flowable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new ExceptionData(new byte[100 * 1000 * 1000]);
            }
        })
        .publish()
        .refCount();

        long start = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer());

        long after = TestHelper.awaitGC(GC_SLEEP_TIME, 20, start + 20 * 1000 * 1000);

        source = null;

        assertTrue(String.format("%,3d -> %,3d%n", start, after), start + 20 * 1000 * 1000 > after);
    }

    @Test
    public void publishNoLeak2() throws Exception {
        System.gc();
        Thread.sleep(GC_SLEEP_TIME);

        long start = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = Flowable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new byte[100 * 1000 * 1000];
            }
        }).concatWith(Flowable.never())
        .publish()
        .refCount();

        TestSubscriber<Object> d1 = source.test();
        TestSubscriber<Object> d2 = source.test();

        d1.cancel();
        d2.cancel();

        d1 = null;
        d2 = null;

        long after = TestHelper.awaitGC(GC_SLEEP_TIME, 20, start + 20 * 1000 * 1000);

        source = null;
        assertTrue(String.format("%,3d -> %,3d%n", start, after), start + 20 * 1000 * 1000 > after);
    }

    @Test
    public void replayIsUnsubscribed() {
        ConnectableFlowable<Integer> cf = Flowable.just(1)
        .replay();

        if (cf instanceof Disposable) {
            assertTrue(((Disposable)cf).isDisposed());

            Disposable connection = cf.connect();

            assertFalse(((Disposable)cf).isDisposed());

            connection.dispose();

            assertTrue(((Disposable)cf).isDisposed());
        }
    }

    static final class BadFlowableSubscribe extends ConnectableFlowable<Object> {

        @Override
        public void connect(Consumer<? super Disposable> connection) {
            try {
                connection.accept(Disposable.empty());
            } catch (Throwable ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }

        @Override
        public void reset() {
            // nothing to do in this test
        }

        @Override
        protected void subscribeActual(Subscriber<? super Object> subscriber) {
            throw new TestException("subscribeActual");
        }
    }

    static final class BadFlowableDispose extends ConnectableFlowable<Object> {

        @Override
        public void connect(Consumer<? super Disposable> connection) {
            try {
                connection.accept(Disposable.empty());
            } catch (Throwable ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }

        @Override
        public void reset() {
            throw new TestException("dispose");
        }

        @Override
        protected void subscribeActual(Subscriber<? super Object> subscriber) {
            subscriber.onSubscribe(new BooleanSubscription());
        }
    }

    static final class BadFlowableConnect extends ConnectableFlowable<Object> {

        @Override
        public void connect(Consumer<? super Disposable> connection) {
            throw new TestException("connect");
        }

        @Override
        public void reset() {
            // nothing to do in this test
        }

        @Override
        protected void subscribeActual(Subscriber<? super Object> subscriber) {
            subscriber.onSubscribe(new BooleanSubscription());
        }
    }

    @Test
    public void badSourceSubscribe() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            BadFlowableSubscribe bo = new BadFlowableSubscribe();

            try {
                bo.refCount()
                .test();
                fail("Should have thrown");
            } catch (NullPointerException ex) {
                assertTrue(ex.getCause() instanceof TestException);
            }

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSourceDispose() {
        BadFlowableDispose bf = new BadFlowableDispose();

        try {
            bf.refCount()
            .test()
            .cancel();
            fail("Should have thrown");
        } catch (TestException expected) {
        }
    }

    @Test
    public void badSourceConnect() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            BadFlowableConnect bf = new BadFlowableConnect();

            try {
                bf.refCount()
                .test();
                fail("Should have thrown");
            } catch (NullPointerException ex) {
                assertTrue(ex.getCause() instanceof TestException);
            }

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    static final class BadFlowableSubscribe2 extends ConnectableFlowable<Object> {

        int count;

        @Override
        public void connect(Consumer<? super Disposable> connection) {
            try {
                connection.accept(Disposable.empty());
            } catch (Throwable ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }

        @Override
        public void reset() {
            // nothing to do in this test
        }

        @Override
        protected void subscribeActual(Subscriber<? super Object> subscriber) {
            if (++count == 1) {
                subscriber.onSubscribe(new BooleanSubscription());
            } else {
                throw new TestException("subscribeActual");
            }
        }
    }

    @Test
    public void badSourceSubscribe2() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            BadFlowableSubscribe2 bf = new BadFlowableSubscribe2();

            Flowable<Object> f = bf.refCount();
            f.test();
            try {
                f.test();
                fail("Should have thrown");
            } catch (NullPointerException ex) {
                assertTrue(ex.getCause() instanceof TestException);
            }

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    static final class BadFlowableConnect2 extends ConnectableFlowable<Object> {

        @Override
        public void connect(Consumer<? super Disposable> connection) {
            try {
                connection.accept(Disposable.empty());
            } catch (Throwable ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }

        @Override
        public void reset() {
            throw new TestException("dispose");
        }

        @Override
        protected void subscribeActual(Subscriber<? super Object> subscriber) {
            subscriber.onSubscribe(new BooleanSubscription());
            subscriber.onComplete();
        }
    }

    @Test
    public void badSourceCompleteDisconnect() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            BadFlowableConnect2 bf = new BadFlowableConnect2();

            try {
                bf.refCount()
                .test();
                fail("Should have thrown");
            } catch (NullPointerException ex) {
                assertTrue(ex.getCause() instanceof TestException);
            }

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void blockingSourceAsnycCancel() throws Exception {
        BehaviorProcessor<Integer> bp = BehaviorProcessor.createDefault(1);

        Flowable<Integer> f = bp
        .replay(1)
        .refCount();

        f.subscribe();

        final AtomicBoolean interrupted = new AtomicBoolean();

        f.switchMap(new Function<Integer, Publisher<? extends Object>>() {
            @Override
            public Publisher<? extends Object> apply(Integer v) throws Exception {
                return Flowable.create(new FlowableOnSubscribe<Object>() {
                    @Override
                    public void subscribe(FlowableEmitter<Object> emitter) throws Exception {
                        while (!emitter.isCancelled()) {
                            Thread.sleep(100);
                        }
                        interrupted.set(true);
                    }
                }, BackpressureStrategy.MISSING);
            }
        })
        .takeUntil(Flowable.timer(500, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();

        assertTrue(interrupted.get());
    }

    @Test
    public void byCount() {
        final int[] subscriptions = { 0 };

        Flowable<Integer> source = Flowable.range(1, 5)
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                subscriptions[0]++;
            }
        })
        .publish()
        .refCount(2);

        for (int i = 0; i < 3; i++) {
            TestSubscriber<Integer> ts1 = source.test();

            ts1.assertEmpty();

            TestSubscriber<Integer> ts2 = source.test();

            ts1.assertResult(1, 2, 3, 4, 5);
            ts2.assertResult(1, 2, 3, 4, 5);
        }

        assertEquals(3, subscriptions[0]);
    }

    @Test
    public void resubscribeBeforeTimeout() throws Exception {
        final int[] subscriptions = { 0 };

        PublishProcessor<Integer> pp = PublishProcessor.create();

        Flowable<Integer> source = pp
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                subscriptions[0]++;
            }
        })
        .publish()
        .refCount(500, TimeUnit.MILLISECONDS);

        TestSubscriber<Integer> ts1 = source.test(0);

        assertEquals(1, subscriptions[0]);

        ts1.cancel();

        Thread.sleep(100);

        ts1 = source.test(0);

        assertEquals(1, subscriptions[0]);

        Thread.sleep(500);

        assertEquals(1, subscriptions[0]);

        pp.onNext(1);
        pp.onNext(2);
        pp.onNext(3);
        pp.onNext(4);
        pp.onNext(5);
        pp.onComplete();

        ts1.requestMore(5)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void letitTimeout() throws Exception {
        final int[] subscriptions = { 0 };

        PublishProcessor<Integer> pp = PublishProcessor.create();

        Flowable<Integer> source = pp
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                subscriptions[0]++;
            }
        })
        .publish()
        .refCount(1, 100, TimeUnit.MILLISECONDS);

        TestSubscriber<Integer> ts1 = source.test(0);

        assertEquals(1, subscriptions[0]);

        ts1.cancel();

        assertTrue(pp.hasSubscribers());

        Thread.sleep(200);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void error() {
        Flowable.<Integer>error(new IOException())
        .publish()
        .refCount(500, TimeUnit.MILLISECONDS)
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void comeAndGo() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        Flowable<Integer> source = pp
        .publish()
        .refCount(1);

        TestSubscriber<Integer> ts1 = source.test(0);

        assertTrue(pp.hasSubscribers());

        for (int i = 0; i < 3; i++) {
            TestSubscriber<Integer> ts2 = source.test();
            ts1.cancel();
            ts1 = ts2;
        }

        ts1.cancel();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void unsubscribeSubscribeRace() {
        for (int i = 0; i < 1000; i++) {

            final Flowable<Integer> source = Flowable.range(1, 5)
                    .replay()
                    .refCount(1)
                    ;

            final TestSubscriber<Integer> ts1 = source.test(0);

            final TestSubscriber<Integer> ts2 = new TestSubscriber<>(0);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    source.subscribe(ts2);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            ts2.requestMore(6) // FIXME RxJava replay() doesn't issue onComplete without request
            .withTag("Round: " + i)
            .assertResult(1, 2, 3, 4, 5);
        }
    }

    static final class BadFlowableDoubleOnX extends ConnectableFlowable<Object>
    implements Disposable {

        @Override
        public void connect(Consumer<? super Disposable> connection) {
            try {
                connection.accept(Disposable.empty());
            } catch (Throwable ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }

        @Override
        public void reset() {
            // nothing to do in this test
        }

        @Override
        protected void subscribeActual(Subscriber<? super Object> subscriber) {
            subscriber.onSubscribe(new BooleanSubscription());
            subscriber.onSubscribe(new BooleanSubscription());
            subscriber.onComplete();
            subscriber.onComplete();
            subscriber.onError(new TestException());
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean isDisposed() {
            return false;
        }
    }

    @Test
    public void doubleOnX() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new BadFlowableDoubleOnX()
            .refCount()
            .test()
            .assertResult();

            TestHelper.assertError(errors, 0, ProtocolViolationException.class);
            TestHelper.assertUndeliverable(errors, 1, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void doubleOnXCount() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new BadFlowableDoubleOnX()
            .refCount(1)
            .test()
            .assertResult();

            TestHelper.assertError(errors, 0, ProtocolViolationException.class);
            TestHelper.assertUndeliverable(errors, 1, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void doubleOnXTime() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new BadFlowableDoubleOnX()
            .refCount(5, TimeUnit.SECONDS, Schedulers.single())
            .test()
            .assertResult();

            TestHelper.assertError(errors, 0, ProtocolViolationException.class);
            TestHelper.assertUndeliverable(errors, 1, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cancelTerminateStateExclusion() {
        FlowableRefCount<Object> o = (FlowableRefCount<Object>)PublishProcessor.create()
        .publish()
        .refCount();

        o.cancel(null);

        RefConnection rc = new RefConnection(o);
        o.connection = null;
        rc.subscriberCount = 0;
        o.timeout(rc);

        rc.subscriberCount = 1;
        o.timeout(rc);

        o.connection = rc;
        o.timeout(rc);

        rc.subscriberCount = 0;
        o.timeout(rc);

        // -------------------

        rc.subscriberCount = 2;
        rc.connected = false;
        o.connection = rc;
        o.cancel(rc);

        rc.subscriberCount = 1;
        rc.connected = false;
        o.connection = rc;
        o.cancel(rc);

        rc.subscriberCount = 2;
        rc.connected = true;
        o.connection = rc;
        o.cancel(rc);

        rc.subscriberCount = 1;
        rc.connected = true;
        o.connection = rc;
        rc.set(null);
        o.cancel(rc);

        o.connection = rc;
        o.cancel(new RefConnection(o));
    }

    @Test
    public void replayRefCountShallBeThreadSafe() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            Flowable<Integer> flowable = Flowable.just(1).replay(1).refCount();

            TestSubscriber<Integer> ts1 = flowable
                    .subscribeOn(Schedulers.io())
                    .test();

            TestSubscriber<Integer> ts2 = flowable
                    .subscribeOn(Schedulers.io())
                    .test();

            ts1
            .withTag("" + i)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1);

            ts2
            .withTag("" + i)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1);
        }
    }

    static final class TestConnectableFlowable<T> extends ConnectableFlowable<T> {

        volatile boolean reset;

        @Override
        public void reset() {
            reset = true;
        }

        @Override
        public void connect(Consumer<? super Disposable> connection) {
            // not relevant
        }

        @Override
        protected void subscribeActual(Subscriber<? super T> subscriber) {
            // not relevant
        }
    }

    @Test
    public void timeoutResetsSource() {
        TestConnectableFlowable<Object> tcf = new TestConnectableFlowable<>();
        FlowableRefCount<Object> o = (FlowableRefCount<Object>)tcf.refCount();

        RefConnection rc = new RefConnection(o);
        rc.set(Disposable.empty());
        o.connection = rc;

        o.timeout(rc);

        assertTrue(tcf.reset);
    }

    @Test
    public void disconnectBeforeConnect() {
        BehaviorProcessor<Integer> processor = BehaviorProcessor.create();

        Flowable<Integer> flowable = processor
                .replay(1)
                .refCount();

        flowable.takeUntil(Flowable.just(1)).test();

        processor.onNext(2);

        flowable.take(1).test().assertResult(2);
    }

    @Test
    public void publishRefCountShallBeThreadSafe() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            Flowable<Integer> flowable = Flowable.just(1).publish().refCount();

            TestSubscriber<Integer> subscriber1 = flowable
                    .subscribeOn(Schedulers.io())
                    .test();

            TestSubscriber<Integer> subscriber2 = flowable
                    .subscribeOn(Schedulers.io())
                    .test();

            subscriber1
            .withTag("subscriber1 " + i)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertNoErrors()
            .assertComplete();

            subscriber2
            .withTag("subscriber2 " + i)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test
    public void upstreamTerminationTriggersAnotherCancel() throws Exception {
        ReplayProcessor<Integer> rp = ReplayProcessor.create();
        rp.onNext(1);
        rp.onComplete();

        Flowable<Integer> shared = rp.share();

        shared
        .buffer(shared.debounce(5, TimeUnit.SECONDS))
        .test()
        .assertValueCount(2);

        shared
        .buffer(shared.debounce(5, TimeUnit.SECONDS))
        .test()
        .assertValueCount(2);
    }}
