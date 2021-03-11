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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import io.reactivex.rxjava3.annotations.NonNull;
import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.internal.util.CrashingMappedIterable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableWithLatestFromTest extends RxJavaTest {
    static final BiFunction<Integer, Integer, Integer> COMBINER = (t1, t2) -> (t1 << 8) + t2;
    static final BiFunction<Integer, Integer, Integer> COMBINER_ERROR = (t1, t2) -> {
        throw new TestException("Forced failure");
    };
    @Test
    public void simple() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(subscriber);

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        result.subscribe(subscriber);

        source.onNext(1);
        inOrder.verify(subscriber, never()).onNext(anyInt());

        other.onNext(1);
        inOrder.verify(subscriber, never()).onNext(anyInt());

        source.onNext(2);
        inOrder.verify(subscriber).onNext((2 << 8) + 1);

        other.onNext(2);
        inOrder.verify(subscriber, never()).onNext(anyInt());

        other.onComplete();
        inOrder.verify(subscriber, never()).onComplete();

        source.onNext(3);
        inOrder.verify(subscriber).onNext((3 << 8) + 2);

        source.onComplete();
        inOrder.verify(subscriber).onComplete();

        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void emptySource() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        result.subscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(other.hasSubscribers());

        other.onNext(1);

        source.onComplete();

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertNoValues();

        assertFalse(source.hasSubscribers());
        assertFalse(other.hasSubscribers());
    }

    @Test
    public void emptyOther() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        result.subscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(other.hasSubscribers());

        source.onNext(1);

        source.onComplete();

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertNoValues();

        assertFalse(source.hasSubscribers());
        assertFalse(other.hasSubscribers());
    }

    @Test
    public void unsubscription() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        result.subscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(other.hasSubscribers());

        other.onNext(1);
        source.onNext(1);

        ts.cancel();

        ts.assertValue((1 << 8) + 1);
        ts.assertNoErrors();
        ts.assertNotComplete();

        assertFalse(source.hasSubscribers());
        assertFalse(other.hasSubscribers());
    }

    @Test
    public void sourceThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        result.subscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(other.hasSubscribers());

        other.onNext(1);
        source.onNext(1);

        source.onError(new TestException());

        ts.assertTerminated();
        ts.assertValue((1 << 8) + 1);
        ts.assertError(TestException.class);
        ts.assertNotComplete();

        assertFalse(source.hasSubscribers());
        assertFalse(other.hasSubscribers());
    }

    @Test
    public void otherThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        result.subscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(other.hasSubscribers());

        other.onNext(1);
        source.onNext(1);

        other.onError(new TestException());

        ts.assertTerminated();
        ts.assertValue((1 << 8) + 1);
        ts.assertNotComplete();
        ts.assertError(TestException.class);

        assertFalse(source.hasSubscribers());
        assertFalse(other.hasSubscribers());
    }

    @Test
    public void functionThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER_ERROR);

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();

        result.subscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(other.hasSubscribers());

        other.onNext(1);
        source.onNext(1);

        ts.assertTerminated();
        ts.assertNotComplete();
        ts.assertNoValues();
        ts.assertError(TestException.class);

        assertFalse(source.hasSubscribers());
        assertFalse(other.hasSubscribers());
    }

    @Test
    public void noDownstreamUnsubscribe() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        result.subscribe(ts);

        source.onComplete();

        assertFalse(ts.isCancelled());
    }

    @Test
    public void backpressure() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

        result.subscribe(ts);

        assertTrue("Other has no observers!", other.hasSubscribers());

        ts.request(1);

        source.onNext(1);

        assertTrue("Other has no observers!", other.hasSubscribers());

        ts.assertNoValues();

        other.onNext(1);

        source.onNext(2);

        ts.assertValue((2 << 8) + 1);

        ts.request(5);
        source.onNext(3);
        source.onNext(4);
        source.onNext(5);
        source.onNext(6);
        source.onNext(7);
        ts.assertValues(
                (2 << 8) + 1, (3 << 8) + 1, (4 << 8) + 1, (5 << 8) + 1,
                (6 << 8) + 1, (7 << 8) + 1
        );

        ts.cancel();

        assertFalse("Other has observers!", other.hasSubscribers());

        ts.assertNoErrors();
    }

    static final Function<Object[], String> toArray = Arrays::toString;

    @Test
    public void manySources() {
        PublishProcessor<String> pp1 = PublishProcessor.create();
        PublishProcessor<String> pp2 = PublishProcessor.create();
        PublishProcessor<String> pp3 = PublishProcessor.create();
        PublishProcessor<String> main = PublishProcessor.create();

        TestSubscriber<String> ts = new TestSubscriber<>();

        main.withLatestFrom(new Flowable[] { pp1, pp2, pp3 }, toArray)
        .subscribe(ts);

        main.onNext("1");
        ts.assertNoValues();
        pp1.onNext("a");
        ts.assertNoValues();
        pp2.onNext("A");
        ts.assertNoValues();
        pp3.onNext("=");
        ts.assertNoValues();

        main.onNext("2");
        ts.assertValues("[2, a, A, =]");

        pp2.onNext("B");

        ts.assertValues("[2, a, A, =]");

        pp3.onComplete();
        ts.assertValues("[2, a, A, =]");

        pp1.onNext("b");

        main.onNext("3");

        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");

        main.onComplete();
        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");
        ts.assertNoErrors();
        ts.assertComplete();

        assertFalse("ps1 has subscribers?", pp1.hasSubscribers());
        assertFalse("ps2 has subscribers?", pp2.hasSubscribers());
        assertFalse("ps3 has subscribers?", pp3.hasSubscribers());
    }

    @Test
    public void manySourcesIterable() {
        PublishProcessor<String> pp1 = PublishProcessor.create();
        PublishProcessor<String> pp2 = PublishProcessor.create();
        PublishProcessor<String> pp3 = PublishProcessor.create();
        PublishProcessor<String> main = PublishProcessor.create();

        TestSubscriber<String> ts = new TestSubscriber<>();

        main.withLatestFrom(Arrays.<Flowable<?>>asList(pp1, pp2, pp3), toArray)
        .subscribe(ts);

        main.onNext("1");
        ts.assertNoValues();
        pp1.onNext("a");
        ts.assertNoValues();
        pp2.onNext("A");
        ts.assertNoValues();
        pp3.onNext("=");
        ts.assertNoValues();

        main.onNext("2");
        ts.assertValues("[2, a, A, =]");

        pp2.onNext("B");

        ts.assertValues("[2, a, A, =]");

        pp3.onComplete();
        ts.assertValues("[2, a, A, =]");

        pp1.onNext("b");

        main.onNext("3");

        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");

        main.onComplete();
        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");
        ts.assertNoErrors();
        ts.assertComplete();

        assertFalse("ps1 has subscribers?", pp1.hasSubscribers());
        assertFalse("ps2 has subscribers?", pp2.hasSubscribers());
        assertFalse("ps3 has subscribers?", pp3.hasSubscribers());
    }

    @Test
    public void manySourcesIterableSweep() {
        for (String val : new String[] { "1" /*, null*/ }) {
            int n = 35;
            for (int i = 0; i < n; i++) {
                List<Flowable<?>> sources = new ArrayList<>();
                List<String> expected = new ArrayList<>();
                expected.add(val);

                for (int j = 0; j < i; j++) {
                    sources.add(Flowable.just(val));
                    expected.add(val);
                }

                TestSubscriber<String> ts = new TestSubscriber<>();

                PublishProcessor<String> main = PublishProcessor.create();

                main.withLatestFrom(sources, toArray).subscribe(ts);

                ts.assertNoValues();

                main.onNext(val);
                main.onComplete();

                ts.assertValue(expected.toString());
                ts.assertNoErrors();
                ts.assertComplete();
            }
        }
    }

    @Test
    public void backpressureNoSignal() {
        PublishProcessor<String> pp1 = PublishProcessor.create();
        PublishProcessor<String> pp2 = PublishProcessor.create();

        TestSubscriber<String> ts = new TestSubscriber<>(0);

        Flowable.range(1, 10).withLatestFrom(new Flowable<?>[] { pp1, pp2 }, toArray)
        .subscribe(ts);

        ts.assertNoValues();

        ts.request(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertComplete();

        assertFalse("ps1 has subscribers?", pp1.hasSubscribers());
        assertFalse("ps2 has subscribers?", pp2.hasSubscribers());
    }

    @Test
    public void backpressureWithSignal() {
        PublishProcessor<String> pp1 = PublishProcessor.create();
        PublishProcessor<String> pp2 = PublishProcessor.create();

        TestSubscriber<String> ts = new TestSubscriber<>(0);

        Flowable.range(1, 3).withLatestFrom(new Flowable<?>[] { pp1, pp2 }, toArray)
        .subscribe(ts);

        ts.assertNoValues();

        pp1.onNext("1");
        pp2.onNext("1");

        ts.request(1);

        ts.assertValue("[1, 1, 1]");

        ts.request(1);

        ts.assertValues("[1, 1, 1]", "[2, 1, 1]");

        ts.request(1);

        ts.assertValues("[1, 1, 1]", "[2, 1, 1]", "[3, 1, 1]");
        ts.assertNoErrors();
        ts.assertComplete();

        assertFalse("ps1 has subscribers?", pp1.hasSubscribers());
        assertFalse("ps2 has subscribers?", pp2.hasSubscribers());
    }

    @Test
    public void withEmpty() {
        TestSubscriber<String> ts = new TestSubscriber<>(0);

        Flowable.range(1, 3).withLatestFrom(
                new Flowable<?>[] { Flowable.just(1), Flowable.empty() }, toArray)
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void withError() {
        TestSubscriber<String> ts = new TestSubscriber<>(0);

        Flowable.range(1, 3).withLatestFrom(
                new Flowable<?>[] { Flowable.just(1), Flowable.error(new TestException()) }, toArray)
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void withMainError() {
        TestSubscriber<String> ts = new TestSubscriber<>(0);

        Flowable.error(new TestException()).withLatestFrom(
                new Flowable<?>[] { Flowable.just(1), Flowable.just(1) }, toArray)
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void with2Others() {
        Flowable<Integer> just = Flowable.just(1);

        TestSubscriber<List<Integer>> ts = new TestSubscriber<>();

        just.withLatestFrom(just, just, Arrays::asList)
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1));
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void with3Others() {
        Flowable<Integer> just = Flowable.just(1);

        TestSubscriber<List<Integer>> ts = new TestSubscriber<>();

        just.withLatestFrom(just, just, just, Arrays::asList)
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1, 1));
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void with4Others() {
        Flowable<Integer> just = Flowable.just(1);

        TestSubscriber<List<Integer>> ts = new TestSubscriber<>();

        just.withLatestFrom(just, just, just, just, Arrays::asList)
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1, 1, 1));
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).withLatestFrom(Flowable.just(2), (BiFunction<Integer, Integer, Object>) (a, b) -> a));

        TestHelper.checkDisposed(Flowable.just(1).withLatestFrom(Flowable.just(2), Flowable.just(3), (Function3<Integer, Integer, Integer, Object>) (a, b, c) -> a));
    }

    @Test
    public void manyIteratorThrows() {
        Flowable.just(1)
        .withLatestFrom(new CrashingMappedIterable<>(1, 100, 100, v -> Flowable.just(2)), (Function<Object[], Object>) a -> a)
        .to(TestHelper.testConsumer())
        .assertFailureAndMessage(TestException.class, "iterator()");
    }

    @Test
    public void manyCombinerThrows() {
        Flowable.just(1).withLatestFrom(Flowable.just(2), Flowable.just(3), (a, b, c) -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void manyErrors() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(@NonNull Subscriber<? super Integer> subscriber) {
                    subscriber.onSubscribe(new BooleanSubscription());
                    subscriber.onError(new TestException("First"));
                    subscriber.onNext(1);
                    subscriber.onError(new TestException("Second"));
                    subscriber.onComplete();
                }
            }.withLatestFrom(Flowable.just(2), Flowable.just(3), (Function3<Integer, Integer, Integer, Object>) (a, b, c) -> a)
            .to(TestHelper.testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void otherErrors() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.just(1)
            .withLatestFrom(new Flowable<Integer>() {
                @Override
                protected void subscribeActual(@NonNull Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onError(new TestException("First"));
                    s.onError(new TestException("Second"));
                }
            }, Integer::sum)
            .to(TestHelper.testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void combineToNull1() {
        Flowable.just(1)
        .withLatestFrom(Flowable.just(2), (a, b) -> null)
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void combineToNull2() {
        Flowable.just(1)
        .withLatestFrom(Arrays.asList(Flowable.just(2), Flowable.just(3)), o -> null)
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void zeroOtherCombinerReturnsNull() {
        Flowable.just(1)
        .withLatestFrom(new Flowable[0], Functions.justFunction(null))
        .to(TestHelper.testConsumer())
        .assertFailureAndMessage(NullPointerException.class, "The combiner returned a null value");
    }

    @Test
    public void singleRequestNotForgottenWhenNoData() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

        result.subscribe(ts);

        ts.request(1);

        source.onNext(1);

        ts.assertNoValues();

        other.onNext(1);

        ts.assertNoValues();

        source.onNext(2);

        ts.assertValue((2 << 8) + 1);
    }

    @Test
    public void coldSourceConsumedWithoutOther() {
        Flowable.range(1, 10).withLatestFrom(Flowable.never(),
                (BiFunction<Integer, Object, Object>) (a, b) -> a)
        .test(1)
        .assertResult();
    }

    @Test
    public void coldSourceConsumedWithoutManyOthers() {
        Flowable.range(1, 10).withLatestFrom(Flowable.never(), Flowable.never(), Flowable.never(),
                (Function4<Integer, Object, Object, Object, Object>) (a, b, c, d) -> a)
        .test(1)
        .assertResult();
    }

    @Test
    public void otherOnSubscribeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp0 = PublishProcessor.create();
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();
            final PublishProcessor<Integer> pp3 = PublishProcessor.create();

            final Flowable<Object> source = pp0.withLatestFrom(pp1, pp2, pp3, (Function4<Object, Integer, Integer, Integer, Object>) (a, b, c, d) -> a);

            final TestSubscriber<Object> ts = new TestSubscriber<>();

            Runnable r1 = () -> source.subscribe(ts);

            Runnable r2 = ts::cancel;

            TestHelper.race(r1, r2);

            ts.assertEmpty();

            assertFalse(pp0.hasSubscribers());
            assertFalse(pp1.hasSubscribers());
            assertFalse(pp2.hasSubscribers());
            assertFalse(pp3.hasSubscribers());
        }
    }

    @Test
    public void otherCompleteRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp0 = PublishProcessor.create();
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();
            final PublishProcessor<Integer> pp3 = PublishProcessor.create();

            final Flowable<Object> source = pp0.withLatestFrom(pp1, pp2, pp3, (Function4<Object, Integer, Integer, Integer, Object>) (a, b, c, d) -> a);

            final TestSubscriber<Object> ts = new TestSubscriber<>();

            Runnable r1 = () -> source.subscribe(ts);

            Runnable r2 = pp1::onComplete;

            TestHelper.race(r1, r2);

            ts.assertResult();

            assertFalse(pp0.hasSubscribers());
            assertFalse(pp1.hasSubscribers());
            assertFalse(pp2.hasSubscribers());
            assertFalse(pp3.hasSubscribers());
        }
    }
}
