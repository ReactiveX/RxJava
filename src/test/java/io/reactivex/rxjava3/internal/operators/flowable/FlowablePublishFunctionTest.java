/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowablePublishFunctionTest extends RxJavaTest {
    @Test
    public void concatTakeFirstLastCompletes() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.range(1, 3)
                .publish(f -> Flowable.concat(f.take(5), f.takeLast(5)))
                .subscribe(ts);

        ts.assertValues(1, 2, 3);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void concatTakeFirstLastBackpressureCompletes() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);

        Flowable.range(1, 6)
                .publish(f -> Flowable.concat(f.take(5), f.takeLast(5)))
                .subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(1); // make sure take() doesn't go unbounded
        ts.request(4);

        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(5);

        ts.assertValues(1, 2, 3, 4, 5, 6);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void canBeCancelled() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.publish(f -> Flowable.concat(f.take(5), f.takeLast(5))).subscribe(ts);

        pp.onNext(1);
        pp.onNext(2);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.cancel();

        Assert.assertFalse("Source has subscribers?", pp.hasSubscribers());
    }

    @Test
    public void invalidPrefetch() {
        try {
            Flowable.<Integer>never().publish(Functions.identity(), -99);
            fail("Didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("prefetch > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void takeCompletes() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.publish(f -> f.take(1)).subscribe(ts);

        pp.onNext(1);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();

        Assert.assertFalse("Source has subscribers?", pp.hasSubscribers());

    }

    @Test
    public void oneStartOnly() {

        final AtomicInteger startCount = new AtomicInteger();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onStart() {
                startCount.incrementAndGet();
            }
        };

        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.publish(f -> f.take(1)).subscribe(ts);

        Assert.assertEquals(1, startCount.get());
    }

    @Test
    public void takeCompletesUnsafe() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.publish(f -> f.take(1)).subscribe(ts);

        pp.onNext(1);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();

        Assert.assertFalse("Source has subscribers?", pp.hasSubscribers());
    }

    @Test
    public void directCompletesUnsafe() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.publish(Functions.identity()).subscribe(ts);

        pp.onNext(1);
        pp.onComplete();

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();

        Assert.assertFalse("Source has subscribers?", pp.hasSubscribers());
    }

    @Test
    public void overflowMissingBackpressureException() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(0);

        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.publish(Functions.identity()).subscribe(ts);

        for (int i = 0; i < Flowable.bufferSize() * 2; i++) {
            pp.onNext(i);
        }

        ts.assertNoValues();
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotComplete();

        Assert.assertEquals("Could not emit value due to lack of requests",
                ts.errors().get(0).getMessage());
        Assert.assertFalse("Source has subscribers?", pp.hasSubscribers());
    }

    @Test
    public void overflowMissingBackpressureExceptionDelayed() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(0);

        PublishProcessor<Integer> pp = PublishProcessor.create();

        new FlowablePublishMulticast<>(pp, Functions.identity(), Flowable.bufferSize(), true).subscribe(ts);

        for (int i = 0; i < Flowable.bufferSize() * 2; i++) {
            pp.onNext(i);
        }

        ts.request(Flowable.bufferSize());

        ts.assertValueCount(Flowable.bufferSize());
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotComplete();

        Assert.assertEquals("Could not emit value due to lack of requests", ts.errors().get(0).getMessage());
        Assert.assertFalse("Source has subscribers?", pp.hasSubscribers());
    }

    @Test
    public void emptyIdentityMapped() {
        Flowable.empty()
        .publish(Functions.identity())
        .test()
        .assertResult();
    }

    @Test
    public void independentlyMapped() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.publish(v -> Flowable.range(1, 5)).test(0);

        assertTrue("pp has no Subscribers?!", pp.hasSubscribers());

        ts.assertNoValues()
        .assertNoErrors()
        .assertNotComplete();

        ts.request(5);

        ts.assertResult(1, 2, 3, 4, 5);

        assertFalse("pp has Subscribers?!", pp.hasSubscribers());
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(f -> f.publish(Functions.identity()), false, 1, 1, 1);
    }

    @Test
    public void frontOverflow() {
        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                for (int i = 0; i < 9; i++) {
                    s.onNext(i);
                }
            }
        }
        .publish(Functions.identity(), 8)
        .test(0)
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void errorResubscribe() {
        Flowable.error(new TestException())
        .publish(f -> f.onErrorResumeWith(f))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedInputCrash() {
        Flowable.just(1)
        .map(v -> {
            throw new TestException();
        })
        .publish(Functions.identity())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void error() {
        new FlowablePublishMulticast<>(Flowable.just(1).concatWith(Flowable.error(new TestException())),
                Functions.identity(), 16, true)
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void backpressuredEmpty() {
        Flowable.<Integer>empty()
        .publish(Functions.identity())
        .test(0L)
        .assertResult();
    }

    @Test
    public void oneByOne() {
        Flowable.range(1, 10)
        .publish(Functions.identity())
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void completeCancelRaceNoRequest() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    cancel();
                    onComplete();
                }
            }
        };

        pp.publish(Functions.identity()).subscribe(ts);

        pp.onNext(1);

        assertFalse(pp.hasSubscribers());

        ts.assertResult(1);
    }

    @Test
    public void inputOutputSubscribeRace() {
        Flowable<Integer> source = Flowable.just(1)
                .publish(f -> f.subscribeOn(Schedulers.single()));

        for (int i = 0; i < 500; i++) {
            source.test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1);
        }
    }

    @Test
    public void inputOutputSubscribeRace2() {
        Flowable<Integer> source = Flowable.just(1).subscribeOn(Schedulers.single())
                .publish(Functions.identity());

        for (int i = 0; i < 500; i++) {
            source.test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1);
        }
    }

    @Test
    public void sourceSubscriptionDelayed() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Integer> ts1 = new TestSubscriber<>(0L);

            Flowable.just(1)
                    .publish(f -> {
                        Runnable r1 = () -> f.subscribe(ts1);

                        Runnable r2 = () -> {
                            for (int j = 0; j < 100; j++) {
                                ts1.request(1);
                            }
                        };

                        TestHelper.race(r1, r2);
                        return f;
                    }).test()
                    .assertResult(1);

            ts1.assertResult(1);
        }
    }

    @Test
    public void longFlow() {
        Flowable.range(1, 1000000)
        .publish(v -> Flowable.mergeArray(
                v.filter(w -> w % 2 == 0),
                v.filter(w -> w % 2 != 0)))
        .takeLast(1)
        .test()
        .assertResult(1000000);
    }

    @Test
    public void longFlow2() {
        Flowable.range(1, 100000)
        .publish(v -> Flowable.mergeArray(
                v.filter(w -> w % 2 == 0),
                v.filter(w -> w % 2 != 0)))
        .test()
        .assertValueCount(100000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void longFlowHidden() {
        Flowable.range(1, 1000000).hide()
        .publish(v -> Flowable.mergeArray(
                v.filter(w -> w % 2 == 0),
                v.filter(w -> w % 2 != 0)))
        .takeLast(1)
        .test()
        .assertResult(1000000);
    }

    @Test
    public void noUpstreamCancelOnCasualChainClose() {
        AtomicBoolean parentUpstreamCancelled = new AtomicBoolean(false);
        Flowable.range(1, 10)
                .doOnCancel(() -> parentUpstreamCancelled.set(true))
                .publish(Functions.identity())
                .test()
                .awaitDone(1, TimeUnit.SECONDS);
        assertFalse("Unnecessary upstream .cancel() call in FlowablePublishMulticast", parentUpstreamCancelled.get());
    }

    @Test
    public void noUpstreamCancelOnCasualChainCloseWithInnerCancels() {
        AtomicBoolean parentUpstreamCancelled = new AtomicBoolean(false);
        Flowable.range(1, 10)
                .doOnCancel(() -> parentUpstreamCancelled.set(true))
                .publish(v -> Flowable.concat(v.take(1), v.skip(5)))
                .test()
                .awaitDone(1, TimeUnit.SECONDS);
        assertFalse("Unnecessary upstream .cancel() call in FlowablePublishMulticast", parentUpstreamCancelled.get());
    }

    @Test
    public void upstreamCancelOnDownstreamCancel() {
        AtomicBoolean parentUpstreamCancelled = new AtomicBoolean(false);
        Flowable.range(1, 10)
                .doOnCancel(() -> parentUpstreamCancelled.set(true))
                .publish(Functions.identity())
                .take(1)
                .test()
                .awaitDone(1, TimeUnit.SECONDS);
        assertTrue("Upstream .cancel() not called in FlowablePublishMulticast", parentUpstreamCancelled.get());
    }
}
