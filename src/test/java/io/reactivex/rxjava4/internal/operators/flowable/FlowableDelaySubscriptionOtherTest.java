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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class FlowableDelaySubscriptionOtherTest extends RxJavaTest {
    @Test
    public void noPrematureSubscription() {
        PublishProcessor<Object> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        final AtomicInteger subscribed = new AtomicInteger();

        Flowable.just(1)
        .doOnSubscribe(_ -> subscribed.getAndIncrement())
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        assertEquals(0, subscribed.get(), "Premature subscription");

        other.onNext(1);

        assertEquals(1, subscribed.get(), "No subscription");

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void noMultipleSubscriptions() {
        PublishProcessor<Object> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        final AtomicInteger subscribed = new AtomicInteger();

        Flowable.just(1)
        .doOnSubscribe(_ -> subscribed.getAndIncrement())
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        assertEquals(0, subscribed.get(), "Premature subscription");

        other.onNext(1);
        other.onNext(2);

        assertEquals(1, subscribed.get(), "No subscription");

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void completeTriggersSubscription() {
        PublishProcessor<Object> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        final AtomicInteger subscribed = new AtomicInteger();

        Flowable.just(1)
        .doOnSubscribe(_ -> subscribed.getAndIncrement())
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        assertEquals(0, subscribed.get(), "Premature subscription");

        other.onComplete();

        assertEquals(1, subscribed.get(), "No subscription");

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void noPrematureSubscriptionToError() {
        PublishProcessor<Object> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        final AtomicInteger subscribed = new AtomicInteger();

        Flowable.<Integer>error(new TestException())
        .doOnSubscribe(_ -> subscribed.getAndIncrement())
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        assertEquals(0, subscribed.get(), "Premature subscription");

        other.onComplete();

        assertEquals(1, subscribed.get(), "No subscription");

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void noSubscriptionIfOtherErrors() {
        PublishProcessor<Object> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        final AtomicInteger subscribed = new AtomicInteger();

        Flowable.<Integer>error(new TestException())
        .doOnSubscribe(_ -> subscribed.getAndIncrement())
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        assertEquals(0, subscribed.get(), "Premature subscription");

        other.onError(new TestException());

        assertEquals(0, subscribed.get(), "Premature subscription");

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void backpressurePassesThrough() {

        PublishProcessor<Object> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

        final AtomicInteger subscribed = new AtomicInteger();

        Flowable.just(1, 2, 3, 4, 5)
        .doOnSubscribe(_ -> subscribed.getAndIncrement())
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        assertEquals(0, subscribed.get(), "Premature subscription");

        other.onNext(1);

        assertEquals(1, subscribed.get(), "No subscription");

        assertFalse(other.hasSubscribers(), "Not unsubscribed from other");

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        ts.request(1);
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(2);
        ts.assertValues(1, 2, 3);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(10);
        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void unsubscriptionPropagatesBeforeSubscribe() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        source.delaySubscription(other).subscribe(ts);

        assertFalse(source.hasSubscribers(), "source subscribed?");
        assertTrue(other.hasSubscribers(), "other not subscribed?");

        ts.cancel();

        assertFalse(source.hasSubscribers(), "source subscribed?");
        assertFalse(other.hasSubscribers(), "other still subscribed?");
    }

    @Test
    public void unsubscriptionPropagatesAfterSubscribe() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        source.delaySubscription(other).subscribe(ts);

        assertFalse(source.hasSubscribers(), "source subscribed?");
        assertTrue(other.hasSubscribers(), "other not subscribed?");

        other.onComplete();

        assertTrue(source.hasSubscribers(), "source not subscribed?");
        assertFalse(other.hasSubscribers(), "other still subscribed?");

        ts.cancel();

        assertFalse(source.hasSubscribers(), "source subscribed?");
        assertFalse(other.hasSubscribers(), "other still subscribed?");
    }

    @Test
    public void delayAndTakeUntilNeverSubscribeToSource() {
        PublishProcessor<Integer> delayUntil = PublishProcessor.create();
        PublishProcessor<Integer> interrupt = PublishProcessor.create();
        final AtomicBoolean subscribed = new AtomicBoolean(false);

        Flowable.just(1)
        .doOnSubscribe(_ -> subscribed.set(true))
        .delaySubscription(delayUntil)
        .takeUntil(interrupt)
        .subscribe();

        interrupt.onNext(9000);
        delayUntil.onNext(1);

        assertFalse(subscribed.get());
    }

    @Test
    public void badSourceOther() {
        TestHelper.checkBadSourceFlowable(f -> Flowable.just(1).delaySubscription(f), false, 1, 1, 1);
    }

    @Test
    public void afterDelayNoInterrupt() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            for (Scheduler s : new Scheduler[] {
                    Schedulers.single(), Schedulers.computation(), Schedulers.newThread(), Schedulers.cached(),
                    Schedulers.from(exec)
                }) {
                final TestSubscriber<Boolean> ts = TestSubscriber.create();
                ts.withTag(s.getClass().getSimpleName());

                Flowable.<Boolean>create(emitter -> {
                  emitter.onNext(Thread.interrupted());
                  emitter.onComplete();
                }, BackpressureStrategy.MISSING)
                .delaySubscription(100, TimeUnit.MILLISECONDS, s)
                .subscribe(ts);

                ts.awaitDone(5, TimeUnit.SECONDS);
                ts.assertValue(false);
            }
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void doubleOnSubscribeMain() {
        TestHelper.checkDoubleOnSubscribeFlowable(f -> f.delaySubscription(Flowable.empty()));
    }

    @Test
    public void doubleOnSubscribeOther() {
        TestHelper.checkDoubleOnSubscribeFlowable(f -> PublishProcessor.create().delaySubscription(f));
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(PublishProcessor.create().delaySubscription(Flowable.empty()));
    }
}
