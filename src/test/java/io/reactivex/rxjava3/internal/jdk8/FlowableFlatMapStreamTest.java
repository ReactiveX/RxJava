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
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableFlatMapStreamTest extends RxJavaTest {

    @Test
    public void empty() {
        Flowable.empty()
        .flatMapStream(v -> Stream.of(1, 2, 3, 4, 5))
        .test()
        .assertResult();
    }

    @Test
    public void emptyHidden() {
        Flowable.empty()
        .hide()
        .flatMapStream(v -> Stream.of(1, 2, 3, 4, 5))
        .test()
        .assertResult();
    }

    @Test
    public void just() {
        Flowable.just(1)
        .flatMapStream(v -> Stream.of(v + 1, v + 2, v + 3, v + 4, v + 5))
        .test()
        .assertResult(2, 3, 4, 5, 6);
    }

    @Test
    public void justHidden() {
        Flowable.just(1).hide()
        .flatMapStream(v -> Stream.of(v + 1, v + 2, v + 3, v + 4, v + 5))
        .test()
        .assertResult(2, 3, 4, 5, 6);
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
        .flatMapStream(v -> Stream.of(1, 2, 3, 4, 5))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void supplierFusedError() {
        Flowable.fromCallable(() -> { throw new TestException(); })
        .flatMapStream(v -> Stream.of(1, 2, 3, 4, 5))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorHidden() {
        Flowable.error(new TestException())
        .hide()
        .flatMapStream(v -> Stream.of(1, 2, 3, 4, 5))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void range() {
        Flowable.range(1, 5)
        .flatMapStream(v -> IntStream.range(v * 10, v * 10 + 5).boxed())
        .test()
        .assertResult(
                10, 11, 12, 13, 14,
                20, 21, 22, 23, 24,
                30, 31, 32, 33, 34,
                40, 41, 42, 43, 44,
                50, 51, 52, 53, 54
        );
    }

    @Test
    public void rangeHidden() {
        Flowable.range(1, 5)
        .hide()
        .flatMapStream(v -> IntStream.range(v * 10, v * 10 + 5).boxed())
        .test()
        .assertResult(
                10, 11, 12, 13, 14,
                20, 21, 22, 23, 24,
                30, 31, 32, 33, 34,
                40, 41, 42, 43, 44,
                50, 51, 52, 53, 54
        );
    }

    @Test
    public void rangeToEmpty() {
        Flowable.range(1, 5)
        .flatMapStream(v -> Stream.of())
        .test()
        .assertResult();
    }

    @Test
    public void rangeTake() {
        Flowable.range(1, 5)
        .flatMapStream(v -> IntStream.range(v * 10, v * 10 + 5).boxed())
        .take(12)
        .test()
        .assertResult(
                10, 11, 12, 13, 14,
                20, 21, 22, 23, 24,
                30, 31
        );
    }

    @Test
    public void rangeTakeHidden() {
        Flowable.range(1, 5)
        .hide()
        .flatMapStream(v -> IntStream.range(v * 10, v * 10 + 5).boxed())
        .take(12)
        .test()
        .assertResult(
                10, 11, 12, 13, 14,
                20, 21, 22, 23, 24,
                30, 31
        );
    }

    @Test
    public void upstreamCancelled() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        AtomicInteger calls = new AtomicInteger();

        TestSubscriber<Integer> ts = pp
                .flatMapStream(v -> Stream.of(v + 1, v + 2).onClose(() -> calls.getAndIncrement()))
                .test(1);

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts.assertValuesOnly(2);
        ts.cancel();

        assertFalse(pp.hasSubscribers());

        assertEquals(1, calls.get());
    }

    @Test
    public void upstreamCancelledCloseCrash() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            PublishProcessor<Integer> pp = PublishProcessor.create();

            TestSubscriber<Integer> ts = pp
                    .flatMapStream(v -> Stream.of(v + 1, v + 2).onClose(() -> { throw new TestException(); }))
                    .test(1);

            assertTrue(pp.hasSubscribers());

            pp.onNext(1);

            ts.assertValuesOnly(2);
            ts.cancel();

            assertFalse(pp.hasSubscribers());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void crossMap() {
        Flowable.range(1, 1000)
        .flatMapStream(v -> IntStream.range(v * 1000, v * 1000 + 1000).boxed())
        .test()
        .assertValueCount(1_000_000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void crossMapHidden() {
        Flowable.range(1, 1000)
        .hide()
        .flatMapStream(v -> IntStream.range(v * 1000, v * 1000 + 1000).boxed())
        .test()
        .assertValueCount(1_000_000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void crossMapBackpressured() {
        for (int n = 1; n < 2048; n *= 2) {
            Flowable.range(1, 1000)
            .flatMapStream(v -> IntStream.range(v * 1000, v * 1000 + 1000).boxed())
            .rebatchRequests(n)
            .test()
            .withTag("rebatch: " + n)
            .assertValueCount(1_000_000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test
    public void crossMapBackpressuredHidden() {
        for (int n = 1; n < 2048; n *= 2) {
            Flowable.range(1, 1000)
            .hide()
            .flatMapStream(v -> IntStream.range(v * 1000, v * 1000 + 1000).boxed())
            .rebatchRequests(n)
            .test()
            .withTag("rebatch: " + n)
            .assertValueCount(1_000_000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test
    public void onSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(f -> f.flatMapStream(v -> Stream.of(1, 2)));
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(UnicastProcessor.create().flatMapStream(v -> Stream.of(1, 2)));
    }

    @Test
    public void queueOverflow() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onNext(2);
                    s.onNext(3);
                    s.onError(new TestException());
                }
            }
            .flatMapStream(v -> Stream.of(1, 2), 1)
            .test(0)
            .assertFailure(MissingBackpressureException.class);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void mapperThrows() {
        Flowable.just(1).hide()
        .concatMapStream(v -> { throw new TestException(); })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperNull() {
        Flowable.just(1).hide()
        .concatMapStream(v -> null)
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void streamNull() {
        Flowable.just(1).hide()
        .concatMapStream(v -> Stream.of(1, null))
        .test()
        .assertFailure(NullPointerException.class, 1);
    }

    @Test
    public void hasNextThrows() {
        Flowable.just(1).hide()
        .concatMapStream(v -> Stream.generate(() -> { throw new TestException(); }))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void hasNextThrowsLater() {
        AtomicInteger counter = new AtomicInteger();
        Flowable.just(1).hide()
        .concatMapStream(v -> Stream.generate(() -> {
            if (counter.getAndIncrement() == 0) {
                return 1;
            }
            throw new TestException();
        }))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void mapperThrowsWhenUpstreamErrors() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            PublishProcessor<Integer> pp = PublishProcessor.create();

            AtomicInteger counter = new AtomicInteger();

            TestSubscriber<Integer> ts = pp.hide()
            .concatMapStream(v -> {
                if (counter.getAndIncrement() == 0) {
                    return Stream.of(1, 2);
                }
                pp.onError(new IOException());
                throw new TestException();
            })
            .test();

            pp.onNext(1);
            pp.onNext(2);

            ts
            .assertFailure(IOException.class, 1, 2);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void rangeBackpressured() {
        Flowable.range(1, 5)
        .hide()
        .concatMapStream(v -> Stream.of(v), 1)
        .test(0)
        .assertEmpty()
        .requestMore(5)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void cancelAfterIteratorNext() throws Exception {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        @SuppressWarnings("unchecked")
        Stream<Integer> stream = mock(Stream.class);
        when(stream.iterator()).thenReturn(new Iterator<Integer>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                ts.cancel();
                return 1;
            }
        });

        Flowable.just(1)
        .hide()
        .concatMapStream(v -> stream)
        .subscribe(ts);

        ts.assertEmpty();
    }

    @Test
    public void asyncUpstreamFused() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();

        TestSubscriber<Integer> ts = up.flatMapStream(v -> Stream.of(1, 2))
        .test();

        assertTrue(up.hasSubscribers());

        up.onNext(1);

        ts.assertValuesOnly(1, 2);

        up.onComplete();

        ts.assertResult(1, 2);
    }

    @Test
    public void asyncUpstreamFusionBoundary() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();

        TestSubscriber<Integer> ts = up
                .map(v -> v + 1)
                .flatMapStream(v -> Stream.of(1, 2))
        .test();

        assertTrue(up.hasSubscribers());

        up.onNext(1);

        ts.assertValuesOnly(1, 2);

        up.onComplete();

        ts.assertResult(1, 2);
    }

    @Test
    public void fusedPollCrash() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();

        TestSubscriber<Integer> ts = up
                .map(v -> { throw new TestException(); })
                .compose(TestHelper.flowableStripBoundary())
                .flatMapStream(v -> Stream.of(1, 2))
        .test();

        assertTrue(up.hasSubscribers());

        up.onNext(1);

        assertFalse(up.hasSubscribers());

        ts.assertFailure(TestException.class);
    }
}
