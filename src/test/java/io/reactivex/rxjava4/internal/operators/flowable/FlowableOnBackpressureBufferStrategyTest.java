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

import static io.reactivex.rxjava4.core.BackpressureOverflowStrategy.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.functions.Consumer;
import io.reactivex.rxjava4.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.subscribers.*;
import io.reactivex.rxjava4.testsupport.*;

public class FlowableOnBackpressureBufferStrategyTest extends RxJavaTest {

    @Test
    public void backpressureWithBufferDropOldest() throws InterruptedException {
        int bufferSize = 3;
        final AtomicInteger droppedCount = new AtomicInteger(0);
        TestSubscriber<Long> ts = createTestSubscriber();
        Flowable.fromPublisher(send500ValuesAndComplete.onBackpressureBuffer(bufferSize, DROP_OLDEST,
                _ -> droppedCount.getAndIncrement()))
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

    private TestSubscriber<Long> createTestSubscriber() {
        return new TestSubscriber<>(new DefaultSubscriber<>() /* NFI */ {

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
    public void backpressureWithBufferDropLatest() throws InterruptedException {
        int bufferSize = 3;
        final AtomicInteger droppedCount = new AtomicInteger(0);
        TestSubscriber<Long> ts = createTestSubscriber();
        Flowable.fromPublisher(send500ValuesAndComplete.onBackpressureBuffer(bufferSize, DROP_LATEST,
                _ -> droppedCount.getAndIncrement()))
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

    private static final Flowable<Long> send500ValuesAndComplete = Flowable.unsafeCreate(s -> {
        BooleanSubscription bs = new BooleanSubscription();
        s.onSubscribe(bs);
        long i = 0;
        while (!bs.isCancelled() && i < 500) {
            s.onNext(i++);
        }
        if (!bs.isCancelled()) {
            s.onComplete();
        }
    });

    @Test
    public void backpressureBufferNegativeCapacity() throws InterruptedException {
        assertThrows(IllegalArgumentException.class, () -> {
            Flowable.empty().onBackpressureBuffer(-1, DROP_OLDEST);
        });
    }

    @Test
    public void backpressureBufferZeroCapacity() throws InterruptedException {
        assertThrows(IllegalArgumentException.class, () -> {
            Flowable.empty().onBackpressureBuffer(0, DROP_OLDEST);
        });
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1)
                .onBackpressureBuffer(16, BackpressureOverflowStrategy.ERROR));
    }

    @Test
    public void error() {
        Flowable
        .error(new TestException())
        .onBackpressureBuffer(16, BackpressureOverflowStrategy.ERROR)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void overflowError() {
        Flowable.range(1, 20)
        .onBackpressureBuffer(8, BackpressureOverflowStrategy.ERROR)
        .test(0L)
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(f -> f.onBackpressureBuffer(8, BackpressureOverflowStrategy.ERROR), false, 1, 1, 1);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(f -> f.onBackpressureBuffer(8, BackpressureOverflowStrategy.ERROR));
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.just(1)
                .onBackpressureBuffer(16, BackpressureOverflowStrategy.ERROR));
    }

    @Test
    public void empty() {
        Flowable.empty()
        .onBackpressureBuffer(16, BackpressureOverflowStrategy.ERROR)
        .test(0L)
        .assertResult();
    }

    @Test
    public void justTake() {
        Flowable.just(1)
        .onBackpressureBuffer(16, BackpressureOverflowStrategy.ERROR)
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void cancelOnDrain() {
        Flowable.range(1, 5)
        .onBackpressureBuffer(10, BackpressureOverflowStrategy.DROP_OLDEST)
        .takeUntil(_ -> true)
        .test(0L)
        .assertEmpty()
        .requestMore(10)
        .assertResult(1);
    }

    @Test
    public void onDroppedNormalDropOldest() throws Throwable {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        @SuppressWarnings("unchecked")
        Consumer<Integer> onDropped = mock(Consumer.class);

        TestSubscriber<Integer> ts = pp.onBackpressureBuffer(1, BackpressureOverflowStrategy.DROP_OLDEST, onDropped)
        .test(0L);

        ts.assertEmpty();

        pp.onNext(1);

        ts.assertEmpty();
        verify(onDropped, never()).accept(any());

        pp.onNext(2);

        ts.assertEmpty();

        verify(onDropped).accept(1);
    }

    @Test
    public void onDroppedNormalDropLatest() throws Throwable {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        @SuppressWarnings("unchecked")
        Consumer<Integer> onDropped = mock(Consumer.class);

        TestSubscriber<Integer> ts = pp.onBackpressureBuffer(2, BackpressureOverflowStrategy.DROP_LATEST, onDropped)
        .test(0L);

        ts.assertEmpty();

        pp.onNext(1);

        pp.onNext(2);

        ts.assertEmpty();
        verify(onDropped, never()).accept(any());

        pp.onNext(3);

        ts.assertEmpty();

        verify(onDropped).accept(2);
    }

    @Test
    public void onDroppedNormalError() throws Throwable {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        @SuppressWarnings("unchecked")
        Consumer<Integer> onDropped = mock(Consumer.class);

        TestSubscriber<Integer> ts = pp.onBackpressureBuffer(1, BackpressureOverflowStrategy.ERROR, onDropped)
        .test(0L);

        ts.assertEmpty();

        pp.onNext(1);

        ts.assertEmpty();
        verify(onDropped, never()).accept(any());

        pp.onNext(2);

        ts.assertFailure(MissingBackpressureException.class);

        verify(onDropped).accept(2);
    }

    @Test
    public void onDroppedCrash() throws Throwable {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        Consumer<Integer> onDropped = _ -> { throw new TestException(); };

        TestSubscriberEx<Integer> ts = pp.onBackpressureBuffer(1, BackpressureOverflowStrategy.DROP_OLDEST, onDropped)
        .subscribeWith(new TestSubscriberEx<>(0L));

        ts.assertEmpty();

        pp.onNext(1);

        ts.assertEmpty();

        pp.onNext(2);

        ts.assertFailure(TestException.class);
    }
}
