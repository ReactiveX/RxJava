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

package io.reactivex.rxjava4.internal.operators.flowable;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.Flow.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class FlowableBlockingTest extends RxJavaTest {

    @Test
    public void blockingFirst() {
        assertEquals(1, Flowable.range(1, 10)
                .subscribeOn(Schedulers.computation()).blockingFirst().intValue());
    }

    @Test
    public void blockingFirstDefault() {
        assertEquals(1, Flowable.<Integer>empty()
                .subscribeOn(Schedulers.computation()).blockingFirst(1).intValue());
    }

    @Test
    public void blockingSubscribeConsumer() {
        final List<Integer> list = new ArrayList<>();

        Flowable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(list::add);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void boundedBlockingSubscribeConsumer() {
        final List<Integer> list = new ArrayList<>();

        Flowable.range(1, 5)
                .subscribeOn(Schedulers.computation())
                .blockingSubscribe(list::add, 128);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void boundedBlockingSubscribeConsumerBufferExceed() {
        final List<Integer> list = new ArrayList<>();

        Flowable.range(1, 5)
                .subscribeOn(Schedulers.computation())
                .blockingSubscribe(list::add, 3);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumer() {
        final List<Object> list = new ArrayList<>();

        Flowable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(list::add, Functions.emptyConsumer());

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void boundedBlockingSubscribeConsumerConsumer() {
        final List<Object> list = new ArrayList<>();

        Flowable.range(1, 5)
                .subscribeOn(Schedulers.computation())
                .blockingSubscribe(list::add, Functions.emptyConsumer(), 128);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void boundedBlockingSubscribeConsumerConsumerBufferExceed() {
        final List<Object> list = new ArrayList<>();

        Flowable.range(1, 5)
                .subscribeOn(Schedulers.computation())
                .blockingSubscribe(list::add, Functions.emptyConsumer(), 3);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumerError() {
        final List<Object> list = new ArrayList<>();

        TestException ex = new TestException();

        Consumer<Object> cons = list::add;

        Flowable.range(1, 5).concatWith(Flowable.<Integer>error(ex))
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(cons, cons);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, ex), list);
    }

    @Test
    public void boundedBlockingSubscribeConsumerConsumerError() {
        final List<Object> list = new ArrayList<>();

        TestException ex = new TestException();

        Consumer<Object> cons = list::add;

        Flowable.range(1, 5).concatWith(Flowable.<Integer>error(ex))
                .subscribeOn(Schedulers.computation())
                .blockingSubscribe(cons, cons, 128);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, ex), list);
    }

    @Test
    public void blockingSubscribeConsumerConsumerAction() {
        final List<Object> list = new ArrayList<>();

        Consumer<Object> cons = list::add;

        Flowable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(cons, cons, () -> list.add(100));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void boundedBlockingSubscribeConsumerConsumerAction() {
        final List<Object> list = new ArrayList<>();

        Consumer<Object> cons = list::add;

        Action action = () -> list.add(100);

        Flowable.range(1, 5)
                .subscribeOn(Schedulers.computation())
                .blockingSubscribe(cons, cons, action, 128);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void boundedBlockingSubscribeConsumerConsumerActionBufferExceed() {
        final List<Object> list = new ArrayList<>();

        Consumer<Object> cons = list::add;

        Action action = () -> list.add(100);

        Flowable.range(1, 5)
                .subscribeOn(Schedulers.computation())
                .blockingSubscribe(cons, cons, action, 3);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void boundedBlockingSubscribeConsumerConsumerActionBufferExceedMillionItem() {
        final List<Object> list = new ArrayList<>();

        Consumer<Object> cons = list::add;

        Action action = () -> list.add(1000001);

        Flowable.range(1, 1000000)
                .subscribeOn(Schedulers.computation())
                .blockingSubscribe(cons, cons, action, 128);

        assertEquals(1000000 + 1, list.size());
    }

    @Test
    public void blockingSubscribeObserver() {
        final List<Object> list = new ArrayList<>();

        Flowable.range(1, 5)
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new FlowableSubscriber<Object>() /* NFI */ {

            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object value) {
                list.add(value);
            }

            @Override
            public void onError(Throwable e) {
                list.add(e);
            }

            @Override
            public void onComplete() {
                list.add(100);
            }

        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void blockingSubscribeObserverError() {
        final List<Object> list = new ArrayList<>();

        final TestException ex = new TestException();

        Flowable.range(1, 5).concatWith(Flowable.<Integer>error(ex))
        .subscribeOn(Schedulers.computation())
        .blockingSubscribe(new FlowableSubscriber<Object>() /* NFI */ {

            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object value) {
                list.add(value);
            }

            @Override
            public void onError(Throwable e) {
                list.add(e);
            }

            @Override
            public void onComplete() {
                list.add(100);
            }

        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, ex), list);
    }

    @Test
    public void blockingForEachThrows() {
        assertThrows(TestException.class, () -> {
            Flowable.just(1)
            .blockingForEach(_ -> {
                throw new TestException();
            });
        });
    }

    @Test
    public void blockingFirstEmpty() {
        assertThrows(NoSuchElementException.class, () -> {
            Flowable.empty().blockingFirst();
        });
    }

    @Test
    public void blockingLastEmpty() {
        assertThrows(NoSuchElementException.class, () -> {
            Flowable.empty().blockingLast();
        });
    }

    @Test
    public void blockingFirstNormal() {
        assertEquals(1, Flowable.just(1, 2).blockingFirst(3).intValue());
    }

    @Test
    public void blockingLastNormal() {
        assertEquals(2, Flowable.just(1, 2).blockingLast(3).intValue());
    }

    @Test
    public void firstFgnoredCancelAndOnNext() {
        Flowable<Integer> source = Flowable.fromPublisher(s -> {
            s.onSubscribe(new BooleanSubscription());
            s.onNext(1);
            s.onNext(2);
        });

        assertEquals(1, source.blockingFirst().intValue());
    }

    @Test
    public void firstIgnoredCancelAndOnError() {
        List<Throwable> list = TestHelper.trackPluginErrors();
        try {
            Flowable<Integer> source = Flowable.fromPublisher(s -> {
                s.onSubscribe(new BooleanSubscription());
                s.onNext(1);
                s.onError(new TestException());
            });

            assertEquals(1, source.blockingFirst().intValue());

            TestHelper.assertUndeliverable(list, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void firstOnError() {
        assertThrows(TestException.class, () -> {
            Flowable<Integer> source = Flowable.fromPublisher(s -> {
                s.onSubscribe(new BooleanSubscription());
                s.onError(new TestException());
            });

            source.blockingFirst();
        });
    }

    @Test
    public void interrupt() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

        Thread.currentThread().interrupt();

        try {
            Flowable.just(1)
            .blockingSubscribe(ts);

            ts.assertFailure(InterruptedException.class);
        } finally {
            Thread.interrupted(); // clear interrupted status just in case
        }
    }

    @Test
    public void blockingSingleEmpty() {
        assertThrows(NoSuchElementException.class, () -> {
            Flowable.empty().blockingSingle();
        });
    }

    @Test
    public void onCompleteDelayed() {
        TestSubscriber<Object> ts = new TestSubscriber<>();

        Flowable.empty().delay(100, TimeUnit.MILLISECONDS)
        .blockingSubscribe(ts);

        ts.assertResult();
    }

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(FlowableBlockingSubscribe.class);
    }

    @Test
    public void disposeUpFront() {
        TestSubscriber<Object> ts = new TestSubscriber<>();
        ts.cancel();
        Flowable.just(1).blockingSubscribe(ts);

        ts.assertEmpty();
    }

    @Test
    public void delayed() throws Exception {
        final TestSubscriber<Object> ts = new TestSubscriber<>();
        var s = new AtomicReference<Subscriber<? super Integer>>();

        Schedulers.single().scheduleDirect(() -> {
            ts.cancel();
            s.get().onNext(1);
        }, 200, TimeUnit.MILLISECONDS);

        new Flowable<Integer>() /* NFI */ {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                s.set(subscriber);
            }
        }.blockingSubscribe(ts);

        while (!ts.isCancelled()) {
            Thread.sleep(100);
        }

        ts.assertEmpty();
    }

    @Test
    public void blockinsSubscribeCancelAsync() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Integer> ts = new TestSubscriber<>();

            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final Runnable r1 = ts::cancel;

            final Runnable r2 = () -> pp.onNext(1);

            final AtomicInteger c = new AtomicInteger(2);

            Schedulers.computation().scheduleDirect(() -> {
                c.decrementAndGet();
                while (c.get() != 0 && !pp.hasSubscribers()) { }

                TestHelper.race(r1, r2);
            });

            c.decrementAndGet();
            while (c.get() != 0) { }

            pp
            .blockingSubscribe(ts);
        }
    }
}
