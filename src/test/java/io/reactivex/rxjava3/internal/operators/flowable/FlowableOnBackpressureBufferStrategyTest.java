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

import static io.reactivex.rxjava3.core.BackpressureOverflowStrategy.*;
import static io.reactivex.rxjava3.internal.functions.Functions.EMPTY_ACTION;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableOnBackpressureBufferStrategyTest extends RxJavaTest {

    @Test
    public void backpressureWithBufferDropOldestIncrementOnAction() throws InterruptedException {
        int bufferSize = 3;
        final AtomicInteger droppedCount = new AtomicInteger(0);
        Action incrementOnDrop = new Action() {
            @Override
            public void run() throws Exception {
                droppedCount.incrementAndGet();
            }
        };
        TestSubscriber<Long> ts = createTestSubscriber();
        Flowable.fromPublisher(send500ValuesAndComplete.onBackpressureBuffer(bufferSize, incrementOnDrop, DROP_OLDEST))
                .subscribe(ts);
        // we request 10 but only 3 should come from the buffer
        ts.request(10);
        ts.awaitDone(5, TimeUnit.SECONDS);
        assertEquals(bufferSize, ts.values().size());
        ts.assertNoErrors();
        assertEquals(497, ts.values().get(0).intValue());
        assertEquals(498, ts.values().get(1).intValue());
        assertEquals(499, ts.values().get(2).intValue());
        assertEquals(droppedCount.get(), 500 - bufferSize);
    }

    @Test
    public void backpressureWithBufferDropOldestIncrementOnConsumer() throws InterruptedException {
        int bufferSize = 3;
        final AtomicLong droppedSum = new AtomicLong(0);
        Consumer<Long> incrementOnDrop = new Consumer<Long>() {
            @Override
            public void accept(Long aLong) throws Throwable {
                droppedSum.addAndGet(aLong);
            }
        };
        TestSubscriber<Long> ts = createTestSubscriber();
        Flowable.fromPublisher(send500ValuesAndComplete.onBackpressureBuffer(bufferSize, DROP_OLDEST, incrementOnDrop))
                .subscribe(ts);
        // we request 10 but only 3 should come from the buffer
        ts.request(10);
        ts.awaitDone(5, TimeUnit.SECONDS);
        assertEquals(bufferSize, ts.values().size());
        ts.assertNoErrors();
        assertEquals(497, ts.values().get(0).intValue());
        assertEquals(498, ts.values().get(1).intValue());
        assertEquals(499, ts.values().get(2).intValue());
        // sum should be the sum of all 500 values produced minus the values received
        long expectedSum = LongStream.range(0, 500)
                .sum() - ts.values().stream()
                .reduce(0L, Long::sum);
        assertEquals(expectedSum, droppedSum.get());
    }

    private TestSubscriber<Long> createTestSubscriber() {
        return new TestSubscriber<>(new DefaultSubscriber<Long>() {

            @Override
            protected void onStart() {
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Long t) {
            }

        }, 0L);
    }

    @Test
    public void backpressureWithBufferDropLatestIncrementOnAction() throws InterruptedException {
        int bufferSize = 3;
        final AtomicInteger droppedCount = new AtomicInteger(0);
        Action incrementOnDrop = new Action() {
            @Override
            public void run() throws Exception {
                droppedCount.incrementAndGet();
            }
        };
        TestSubscriber<Long> ts = createTestSubscriber();
        Flowable.fromPublisher(send500ValuesAndComplete.onBackpressureBuffer(bufferSize, incrementOnDrop, DROP_LATEST))
                .subscribe(ts);
        // we request 10 but only 3 should come from the buffer
        ts.request(10);
        ts.awaitDone(5, TimeUnit.SECONDS);
        assertEquals(bufferSize, ts.values().size());
        ts.assertNoErrors();
        assertEquals(0, ts.values().get(0).intValue());
        assertEquals(1, ts.values().get(1).intValue());
        assertEquals(499, ts.values().get(2).intValue());
        assertEquals(droppedCount.get(), 500 - bufferSize);
    }

    @Test
    public void backpressureWithBufferDropLatestIncrementOnConsumer() throws InterruptedException {
        int bufferSize = 3;
        final AtomicLong droppedSum = new AtomicLong(0);
        Consumer<Long> incrementOnDrop = new Consumer<Long>() {
            @Override
            public void accept(Long aLong) throws Throwable {
                droppedSum.addAndGet(aLong);
            }
        };
        TestSubscriber<Long> ts = createTestSubscriber();
        Flowable.fromPublisher(send500ValuesAndComplete.onBackpressureBuffer(bufferSize, DROP_LATEST, incrementOnDrop))
                .subscribe(ts);
        // we request 10 but only 3 should come from the buffer
        ts.request(10);
        ts.awaitDone(5, TimeUnit.SECONDS);
        assertEquals(bufferSize, ts.values().size());
        ts.assertNoErrors();
        assertEquals(0, ts.values().get(0).intValue());
        assertEquals(1, ts.values().get(1).intValue());
        assertEquals(499, ts.values().get(2).intValue());
        // sum should be the sum of all 500 values produced minus the values received
        long expectedSum = LongStream.range(0, 500)
                .sum() - ts.values().stream()
                .reduce(0L, Long::sum);
        assertEquals(expectedSum, droppedSum.get());
    }

    private static final Flowable<Long> send500ValuesAndComplete = Flowable.unsafeCreate(new Publisher<Long>() {
        @Override
        public void subscribe(Subscriber<? super Long> s) {
            BooleanSubscription bs = new BooleanSubscription();
            s.onSubscribe(bs);
            long i = 0;
            while (!bs.isCancelled() && i < 500) {
                s.onNext(i++);
            }
            if (!bs.isCancelled()) {
                s.onComplete();
            }
        }
    });

    @Test(expected = IllegalArgumentException.class)
    public void backpressureBufferNegativeCapacity() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(-1, EMPTY_ACTION , DROP_OLDEST);
    }

    @Test(expected = IllegalArgumentException.class)
    public void backpressureBufferZeroCapacity() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(0, EMPTY_ACTION , DROP_OLDEST);
    }

    @Test(expected = IllegalArgumentException.class)
    public void backpressureBufferNegativeCapacityUsingConsumer() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(-1, DROP_OLDEST, r -> {});
    }

    @Test(expected = IllegalArgumentException.class)
    public void backpressureBufferZeroCapacityUsingConsumer() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(0, DROP_OLDEST, r -> {});
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1)
                .onBackpressureBuffer(16, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR));
    }

    @Test
    public void disposeUsingConsumer() {
        TestHelper.checkDisposed(Flowable.just(1)
                .onBackpressureBuffer(16, BackpressureOverflowStrategy.ERROR, r -> {}));
    }

    @Test
    public void error() {
        Flowable
        .error(new TestException())
        .onBackpressureBuffer(16, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorUsingConsumer() {
        Flowable
                .error(new TestException())
                .onBackpressureBuffer(16, BackpressureOverflowStrategy.ERROR, r -> {})
                .test()
                .assertFailure(TestException.class);
    }

    @Test
    public void overflowError() {
        AtomicInteger invokes = new AtomicInteger(0);
        Action action = new Action() {
            @Override
            public void run() throws Throwable {
                invokes.incrementAndGet();
            }
        };
        Flowable.range(1, 20)
        .onBackpressureBuffer(8, action, BackpressureOverflowStrategy.ERROR)
        .test(0L)
        .assertFailure(MissingBackpressureException.class);
        assertEquals(1, invokes.get());
    }

    @Test
    public void overflowErrorUsingConsumer() {
        AtomicInteger rejectedValue = new AtomicInteger(0);
        Consumer<Integer> consumer = new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Throwable {
                rejectedValue.set(integer);
            }
        };
        Flowable.range(1, 20)
                .onBackpressureBuffer(8, BackpressureOverflowStrategy.ERROR, consumer)
                .test(0L)
                .assertFailure(MissingBackpressureException.class);
        // The 9th value (9) is rejected
        assertEquals(9, rejectedValue.get());
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Object>, Object>() {
            @Override
            public Object apply(Flowable<Object> f) throws Exception {
                return f.onBackpressureBuffer(8, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR);
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void badSourceUsingConsumer() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Object>, Object>() {
            @Override
            public Object apply(Flowable<Object> f) throws Exception {
                return f.onBackpressureBuffer(8, BackpressureOverflowStrategy.ERROR, r -> {});
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f) throws Exception {
                return f.onBackpressureBuffer(8, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR);
            }
        });
    }

    @Test
    public void doubleOnSubscribeUsingConsumer() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f) throws Exception {
                return f.onBackpressureBuffer(8, BackpressureOverflowStrategy.ERROR, r -> {});
            }
        });
    }

    @Test
    public void overflowCrashes() {
        Flowable.range(1, 20)
        .onBackpressureBuffer(8, new Action() {
            @Override
            public void run() throws Exception {
                throw new TestException();
            }
        }, BackpressureOverflowStrategy.DROP_OLDEST)
        .test(0L)
        .assertFailure(TestException.class);
    }

    @Test
    public void overflowCrashesUsingConsumer() {
        Flowable.range(1, 20)
                .onBackpressureBuffer(8, BackpressureOverflowStrategy.DROP_OLDEST, new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Throwable {
                        throw new TestException();
                    }
                })
                .test(0L)
                .assertFailure(TestException.class);
    }

    @Test
    public void propagatesActionExceptionOnOverflow() {
        Flowable.range(1, 20)
                .onBackpressureBuffer(8, ERROR, new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Throwable {
                        throw new TestException();
                    }
                })
                .test(0L)
                .assertFailure(TestException.class);
    }

    @Test
    public void propagatesConsumerExceptionOnOverflow() {
        Flowable.range(1, 20)
                .onBackpressureBuffer(8, ERROR, new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Throwable {
                        throw new TestException();
                    }
                })
                .test(0L)
                .assertFailure(TestException.class);
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.just(1)
                .onBackpressureBuffer(16, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR));
    }

    @Test
    public void badRequestUsingConsume() {
        TestHelper.assertBadRequestReported(Flowable.just(1)
                .onBackpressureBuffer(16, BackpressureOverflowStrategy.ERROR, r -> {}));
    }

    @Test
    public void empty() {
        Flowable.empty()
        .onBackpressureBuffer(16, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR)
        .test(0L)
        .assertResult();
    }

    @Test
    public void emptyUsingConsumer() {
        Flowable.empty()
                .onBackpressureBuffer(16, BackpressureOverflowStrategy.ERROR, r -> {})
                .test(0L)
                .assertResult();
    }

    @Test
    public void justTake() {
        Flowable.just(1)
        .onBackpressureBuffer(16, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR)
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void justTakeUsingConsumer() {
        Flowable.just(1)
                .onBackpressureBuffer(16, BackpressureOverflowStrategy.ERROR, r -> {})
                .take(1)
                .test()
                .assertResult(1);
    }

    @Test
    public void overflowNullAction() {
        Flowable.range(1, 5)
        .onBackpressureBuffer(1, null, BackpressureOverflowStrategy.DROP_OLDEST)
        .test(0L)
        .assertEmpty();
    }

    @Test
    public void overflowEmptyConsumer() {
        Flowable.range(1, 5)
                .onBackpressureBuffer(1, BackpressureOverflowStrategy.DROP_OLDEST, r -> {})
                .test(0L)
                .assertEmpty();
    }

    @Test
    public void cancelOnDrain() {
        Flowable.range(1, 5)
        .onBackpressureBuffer(10, null, BackpressureOverflowStrategy.DROP_OLDEST)
        .takeUntil(v -> true)
        .test(0L)
        .assertEmpty()
        .requestMore(10)
        .assertResult(1);
    }

    @Test
    public void cancelOnDrainUsingConsumer() {
        Flowable.range(1, 5)
                .onBackpressureBuffer(10, BackpressureOverflowStrategy.DROP_OLDEST, r -> {})
                .takeUntil(v -> true)
                .test(0L)
                .assertEmpty()
                .requestMore(10)
                .assertResult(1);
    }
}
