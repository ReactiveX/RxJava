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

package io.reactivex.rxjava3.internal.jdk8;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableFromStreamTest extends RxJavaTest {

    @Test
    public void empty() {
        Flowable.fromStream(Stream.<Integer>of())
        .test()
        .assertResult();
    }

    @Test
    public void just() {
        Flowable.fromStream(Stream.<Integer>of(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void many() {
        Flowable.fromStream(Stream.<Integer>of(1, 2, 3, 4, 5))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void manyBackpressured() {
        Flowable.fromStream(Stream.<Integer>of(1, 2, 3, 4, 5))
        .test(0L)
        .assertEmpty()
        .requestMore(1)
        .assertValuesOnly(1)
        .requestMore(2)
        .assertValuesOnly(1, 2, 3)
        .requestMore(2)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void noReuse() {
        Flowable<Integer> source = Flowable.fromStream(Stream.<Integer>of(1, 2, 3, 4, 5));

        source
        .test()
        .assertResult(1, 2, 3, 4, 5);

        source
        .test()
        .assertFailure(IllegalStateException.class);
    }

    @Test
    public void take() {
        Flowable.fromStream(IntStream.rangeClosed(1, 10).boxed())
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void emptyConditional() {
        Flowable.fromStream(Stream.<Integer>of())
        .filter(v -> true)
        .test()
        .assertResult();
    }

    @Test
    public void justConditional() {
        Flowable.fromStream(Stream.<Integer>of(1))
        .filter(v -> true)
        .test()
        .assertResult(1);
    }

    @Test
    public void manyConditional() {
        Flowable.fromStream(Stream.<Integer>of(1, 2, 3, 4, 5))
        .filter(v -> true)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void manyBackpressuredConditional() {
        Flowable.fromStream(Stream.<Integer>of(1, 2, 3, 4, 5))
        .filter(v -> true)
        .test(0L)
        .assertEmpty()
        .requestMore(1)
        .assertValuesOnly(1)
        .requestMore(2)
        .assertValuesOnly(1, 2, 3)
        .requestMore(2)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void manyConditionalSkip() {
        Flowable.fromStream(IntStream.rangeClosed(1, 10).boxed())
        .filter(v -> v % 2 == 0)
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void takeConditional() {
        Flowable.fromStream(IntStream.rangeClosed(1, 10).boxed())
        .filter(v -> true)
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void noOfferNoCrashAfterClear() throws Throwable {
        AtomicReference<SimpleQueue<?>> queue = new AtomicReference<>();

        Flowable.fromStream(IntStream.rangeClosed(1, 10).boxed())
        .subscribe(new FlowableSubscriber<Integer>() {
            @Override
            public void onSubscribe(@NonNull Subscription s) {
                queue.set((SimpleQueue<?>)s);
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

        SimpleQueue<?> q = queue.get();
        TestHelper.assertNoOffer(q);

        assertFalse(q.isEmpty());

        q.clear();

        assertNull(q.poll());

        assertTrue(q.isEmpty());

        q.clear();

        assertNull(q.poll());

        assertTrue(q.isEmpty());
    }

    @Test
    public void streamOfNull() {
        Flowable.fromStream(Stream.of((Integer)null))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void streamOfNullConditional() {
        Flowable.fromStream(Stream.of((Integer)null))
        .filter(v -> true)
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void syncFusionSupport() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.ANY);

        Flowable.fromStream(IntStream.rangeClosed(1, 10).boxed())
        .subscribeWith(ts)
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void asyncFusionNotSupported() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        ts.setInitialFusionMode(QueueFuseable.ASYNC);

        Flowable.fromStream(IntStream.rangeClosed(1, 10).boxed())
        .subscribeWith(ts)
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void fusedForParallel() {
        Flowable.fromStream(IntStream.rangeClosed(1, 1000).boxed())
        .parallel()
        .runOn(Schedulers.computation(), 1)
        .map(v -> v + 1)
        .sequential()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void runToEndCloseCrash() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).onClose(() -> { throw new TestException(); });

            Flowable.fromStream(stream)
            .test()
            .assertResult(1, 2, 3, 4, 5);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void takeCloseCrash() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5).onClose(() -> { throw new TestException(); });

            Flowable.fromStream(stream)
            .take(3)
            .test()
            .assertResult(1, 2, 3);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void hasNextCrash() {
        AtomicInteger v = new AtomicInteger();
        Flowable.fromStream(Stream.<Integer>generate(() -> {
            int value = v.getAndIncrement();
            if (value == 1) {
                throw new TestException();
            }
            return value;
        }))
        .test()
        .assertFailure(TestException.class, 0);
    }

    @Test
    public void hasNextCrashConditional() {
        AtomicInteger counter = new AtomicInteger();
        Flowable.fromStream(Stream.<Integer>generate(() -> {
            int value = counter.getAndIncrement();
            if (value == 1) {
                throw new TestException();
            }
            return value;
        }))
        .filter(v -> true)
        .test()
        .assertFailure(TestException.class, 0);
    }

    void requestOneByOneBase(boolean conditional) {
        List<Object> list = new ArrayList<>();

        Flowable<Integer> source = Flowable.fromStream(IntStream.rangeClosed(1, 10).boxed());
        if (conditional) {
            source = source.filter(v -> true);
        }

        source.subscribe(new FlowableSubscriber<Integer>() {

            @NonNull Subscription upstream;

            @Override
            public void onSubscribe(@NonNull Subscription s) {
                this.upstream = s;
                s.request(1);
            }

            @Override
            public void onNext(Integer t) {
                list.add(t);
                upstream.request(1);
            }

            @Override
            public void onError(Throwable t) {
                list.add(t);
            }

            @Override
            public void onComplete() {
                list.add(100);
            }
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 100), list);
    }

    @Test
    public void requestOneByOne() {
        requestOneByOneBase(false);
    }

    @Test
    public void requestOneByOneConditional() {
        requestOneByOneBase(true);
    }

    void requestRaceBase(boolean conditional) throws Exception {
        ExecutorService exec = Executors.newCachedThreadPool();
        try {
            for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

                AtomicInteger counter = new AtomicInteger();

                int max = 100;

                Flowable<Integer> source = Flowable.fromStream(IntStream.rangeClosed(1, max).boxed());
                if (conditional) {
                    source = source.filter(v -> true);
                }

                CountDownLatch cdl = new CountDownLatch(1);

                source
                .subscribe(new FlowableSubscriber<Integer>() {

                    @NonNull Subscription upstream;

                    @Override
                    public void onSubscribe(@NonNull Subscription s) {

                        this.upstream = s;
                        s.request(1);

                    }

                    @Override
                    public void onNext(Integer t) {
                        counter.getAndIncrement();

                        AtomicInteger sync = new AtomicInteger(2);
                        exec.submit(() -> {
                            if (sync.decrementAndGet() != 0) {
                                while (sync.get() != 0) { }
                            }
                            upstream.request(1);
                        });

                        if (sync.decrementAndGet() != 0) {
                            while (sync.get() != 0) { }
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                        cdl.countDown();
                    }

                    @Override
                    public void onComplete() {
                        counter.getAndIncrement();
                        cdl.countDown();
                    }
                });

                assertTrue(cdl.await(60, TimeUnit.SECONDS));

                assertEquals(max + 1, counter.get());
            }
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void requestRace() throws Exception {
        requestRaceBase(false);
    }

    @Test
    public void requestRaceConditional() throws Exception {
        requestRaceBase(true);
    }

    @Test
    public void closeCalledOnEmpty() {
        AtomicInteger calls = new AtomicInteger();

        Flowable.fromStream(Stream.of().onClose(() -> calls.getAndIncrement()))
        .test()
        .assertResult();

        assertEquals(1, calls.get());
    }

    @Test
    public void closeCalledAfterItems() {
        AtomicInteger calls = new AtomicInteger();

        Flowable.fromStream(Stream.of(1, 2, 3, 4, 5).onClose(() -> calls.getAndIncrement()))
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls.get());
    }

    @Test
    public void closeCalledOnCancel() {
        AtomicInteger calls = new AtomicInteger();

        Flowable.fromStream(Stream.of(1, 2, 3, 4, 5).onClose(() -> calls.getAndIncrement()))
        .take(3)
        .test()
        .assertResult(1, 2, 3);

        assertEquals(1, calls.get());
    }

    @Test
    public void closeCalledOnItemCrash() {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger counter = new AtomicInteger();
        Flowable.fromStream(Stream.<Integer>generate(() -> {
            int value = counter.getAndIncrement();
            if (value == 1) {
                throw new TestException();
            }
            return value;
        }).onClose(() -> calls.getAndIncrement()))
        .test()
        .assertFailure(TestException.class, 0);

        assertEquals(1, calls.get());
    }

    @Test
    public void closeCalledAfterItemsConditional() {
        AtomicInteger calls = new AtomicInteger();

        Flowable.fromStream(Stream.of(1, 2, 3, 4, 5).onClose(() -> calls.getAndIncrement()))
        .filter(v -> true)
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls.get());
    }

    @Test
    public void closeCalledOnCancelConditional() {
        AtomicInteger calls = new AtomicInteger();

        Flowable.fromStream(Stream.of(1, 2, 3, 4, 5).onClose(() -> calls.getAndIncrement()))
        .filter(v -> true)
        .take(3)
        .test()
        .assertResult(1, 2, 3);

        assertEquals(1, calls.get());
    }

    @Test
    public void closeCalledOnItemCrashConditional() {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger counter = new AtomicInteger();
        Flowable.fromStream(Stream.<Integer>generate(() -> {
            int value = counter.getAndIncrement();
            if (value == 1) {
                throw new TestException();
            }
            return value;
        }).onClose(() -> calls.getAndIncrement()))
        .filter(v -> true)
        .test()
        .assertFailure(TestException.class, 0);

        assertEquals(1, calls.get());
    }
}
