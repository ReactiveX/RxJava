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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableCacheTest extends RxJavaTest {
    @Test
    public void coldReplayNoBackpressure() {
        FlowableCache<Integer> source = new FlowableCache<>(Flowable.range(0, 1000), 16);

        assertFalse("Source is connected!", source.isConnected());

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        source.subscribe(ts);

        assertTrue("Source is not connected!", source.isConnected());
        assertFalse("Subscribers retained!", source.hasSubscribers());

        ts.assertNoErrors();
        ts.assertTerminated();
        List<Integer> onNextEvents = ts.values();
        assertEquals(1000, onNextEvents.size());

        for (int i = 0; i < 1000; i++) {
            assertEquals((Integer)i, onNextEvents.get(i));
        }
    }

    @Test
    public void coldReplayBackpressure() {
        FlowableCache<Integer> source = new FlowableCache<>(Flowable.range(0, 1000), 16);

        assertFalse("Source is connected!", source.isConnected());

        TestSubscriber<Integer> ts = new TestSubscriber<>(0L);
        ts.request(10);

        source.subscribe(ts);

        assertTrue("Source is not connected!", source.isConnected());
        assertFalse("Subscribers retained!", source.hasSubscribers());

        ts.assertNoErrors();
        ts.assertNotComplete();
        List<Integer> onNextEvents = ts.values();
        assertEquals(10, onNextEvents.size());

        for (int i = 0; i < 10; i++) {
            assertEquals((Integer)i, onNextEvents.get(i));
        }

        ts.cancel();
        assertFalse("Subscribers retained!", source.hasSubscribers());
    }

    @Test
    public void cache() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        Flowable<String> f = Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(final Subscriber<? super String> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        System.out.println("published observable being executed");
                        subscriber.onNext("one");
                        subscriber.onComplete();
                    }
                }).start();
            }
        }).cache();

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        f.subscribe(new Consumer<String>() {
            @Override
            public void accept(String v) {
                    assertEquals("one", v);
                    System.out.println("v: " + v);
                    latch.countDown();
            }
        });

        // subscribe again
        f.subscribe(new Consumer<String>() {
            @Override
            public void accept(String v) {
                    assertEquals("one", v);
                    System.out.println("v: " + v);
                    latch.countDown();
            }
        });

        if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("subscriptions did not receive values");
        }
        assertEquals(1, counter.get());
    }

    @Test
    public void unsubscribeSource() throws Throwable {
        Action unsubscribe = mock(Action.class);
        Flowable<Integer> f = Flowable.just(1).doOnCancel(unsubscribe).cache();
        f.subscribe();
        f.subscribe();
        f.subscribe();
        verify(unsubscribe, never()).run();
    }

    @Test
    public void take() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        FlowableCache<Integer> cached = new FlowableCache<>(Flowable.range(1, 100), 16);
        cached.take(10).subscribe(ts);

        ts.assertNoErrors();
        ts.assertComplete();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertFalse(cached.hasSubscribers());
    }

    @Test
    public void async() {
        Flowable<Integer> source = Flowable.range(1, 10000);
        for (int i = 0; i < 100; i++) {
            TestSubscriber<Integer> ts1 = new TestSubscriber<>();

            FlowableCache<Integer> cached = new FlowableCache<>(source, 16);

            cached.observeOn(Schedulers.computation()).subscribe(ts1);

            ts1.awaitDone(2, TimeUnit.SECONDS);
            ts1.assertNoErrors();
            ts1.assertComplete();
            assertEquals(10000, ts1.values().size());

            TestSubscriber<Integer> ts2 = new TestSubscriber<>();
            cached.observeOn(Schedulers.computation()).subscribe(ts2);

            ts2.awaitDone(2, TimeUnit.SECONDS);
            ts2.assertNoErrors();
            ts2.assertComplete();
            assertEquals(10000, ts2.values().size());
        }
    }

    @Test
    public void asyncComeAndGo() {
        Flowable<Long> source = Flowable.interval(1, 1, TimeUnit.MILLISECONDS)
                .take(1000)
                .subscribeOn(Schedulers.io());
        FlowableCache<Long> cached = new FlowableCache<>(source, 16);

        Flowable<Long> output = cached.observeOn(Schedulers.computation());

        List<TestSubscriber<Long>> list = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            TestSubscriber<Long> ts = new TestSubscriber<>();
            list.add(ts);
            output.skip(i * 10).take(10).subscribe(ts);
        }

        List<Long> expected = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            expected.add((long)(i - 10));
        }
        int j = 0;
        for (TestSubscriber<Long> ts : list) {
            ts.awaitDone(3, TimeUnit.SECONDS);
            ts.assertNoErrors();
            ts.assertComplete();

            for (int i = j * 10; i < j * 10 + 10; i++) {
                expected.set(i - j * 10, (long)i);
            }

            ts.assertValueSequence(expected);

            j++;
        }
    }

    @Test
    public void noMissingBackpressureException() {
        final int m = 4 * 1000 * 1000;
        Flowable<Integer> firehose = Flowable.unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> t) {
                t.onSubscribe(new BooleanSubscription());
                for (int i = 0; i < m; i++) {
                    t.onNext(i);
                }
                t.onComplete();
            }
        });

        TestSubscriber<Integer> ts = new TestSubscriber<>();
        firehose.cache().observeOn(Schedulers.computation()).takeLast(100).subscribe(ts);

        ts.awaitDone(3, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertComplete();

        assertEquals(100, ts.values().size());
    }

    @Test
    public void valuesAndThenError() {
        Flowable<Integer> source = Flowable.range(1, 10)
                .concatWith(Flowable.<Integer>error(new TestException()))
                .cache();

        TestSubscriber<Integer> ts = new TestSubscriber<>();
        source.subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts.assertNotComplete();
        ts.assertError(TestException.class);

        TestSubscriber<Integer> ts2 = new TestSubscriber<>();
        source.subscribe(ts2);

        ts2.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts2.assertNotComplete();
        ts2.assertError(TestException.class);
    }

    @Test
    public void take2() {
        Flowable<Integer> cache = Flowable.range(1, 5).cache();

        cache.take(2).test().assertResult(1, 2);
        cache.take(3).test().assertResult(1, 2, 3);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.range(1, 5).cache());
    }

    @Test
    public void disposeOnArrival2() {
        Flowable<Integer> f = PublishProcessor.<Integer>create().cache();

        f.test();

        f.test(0L, true)
        .assertEmpty();
    }

    @Test
    public void subscribeEmitRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.<Integer>create();

            final Flowable<Integer> cache = pp.cache();

            cache.test();

            final TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    cache.subscribe(ts);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 500; j++) {
                        pp.onNext(j);
                    }
                    pp.onComplete();
                }
            };

            TestHelper.race(r1, r2);

            ts
            .awaitDone(5, TimeUnit.SECONDS)
            .assertSubscribed().assertValueCount(500).assertComplete().assertNoErrors();
        }
    }

    @Test
    public void observers() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        FlowableCache<Integer> cache = (FlowableCache<Integer>)Flowable.range(1, 5).concatWith(pp).cache();

        assertFalse(cache.hasSubscribers());

        assertEquals(0, cache.cachedEventCount());

        TestSubscriber<Integer> ts = cache.test();

        assertTrue(cache.hasSubscribers());

        assertEquals(5, cache.cachedEventCount());

        pp.onComplete();

        ts.assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void disposeOnArrival() {
        Flowable.range(1, 5).cache()
        .test(0L, true)
        .assertEmpty();
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Object>, Object>() {
            @Override
            public Object apply(Flowable<Object> f) throws Exception {
                return f.cache();
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.never().cache());
    }

    @Test
    public void take1() {
        Flowable<Integer> cache = Flowable.just(1, 2)
        .cache();

        cache.test();

        cache
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void empty() {
        Flowable.empty()
        .cache()
        .test(0L)
        .assertResult();
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
        .cache()
        .test(0L)
        .assertFailure(TestException.class);
    }

    @Test
    public void cancelledUpFrontConnectAnyway() {
        final AtomicInteger call = new AtomicInteger();
        Flowable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return call.incrementAndGet();
            }
        })
        .cache()
        .test(1L, true)
        .assertNoValues();

        assertEquals(1, call.get());
    }

    @Test
    public void cancelledUpFront() {
        final AtomicInteger call = new AtomicInteger();
        Flowable<Object> f = Flowable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return call.incrementAndGet();
            }
        }).concatWith(Flowable.never())
        .cache();

        f.test().assertValuesOnly(1);

        f.test(1L, true)
        .assertEmpty();

        assertEquals(1, call.get());
    }

    @Test
    public void subscribeSubscribeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final Flowable<Integer> cache = Flowable.range(1, 500).cache();

            final TestSubscriberEx<Integer> ts1 = new TestSubscriberEx<>();
            final TestSubscriberEx<Integer> ts2 = new TestSubscriberEx<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    cache.subscribe(ts1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    cache.subscribe(ts2);
                }
            };

            TestHelper.race(r1, r2);

            ts1
            .awaitDone(5, TimeUnit.SECONDS)
            .assertSubscribed()
            .assertValueCount(500)
            .assertComplete()
            .assertNoErrors();

            ts2
            .awaitDone(5, TimeUnit.SECONDS)
            .assertSubscribed()
            .assertValueCount(500)
            .assertComplete()
            .assertNoErrors();
        }
    }

    @Test
    public void subscribeCompleteRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.<Integer>create();

            final Flowable<Integer> cache = pp.cache();

            cache.test();

            final TestSubscriber<Integer> ts = new TestSubscriber<>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    cache.subscribe(ts);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };

            TestHelper.race(r1, r2);

            ts
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult();
        }
    }

    @Test
    public void backpressure() {
        Flowable.range(1, 5)
        .cache()
        .test(0)
        .assertEmpty()
        .requestMore(2)
        .assertValuesOnly(1, 2)
        .requestMore(3)
        .assertResult(1, 2, 3, 4, 5);
    }
}
