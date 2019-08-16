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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableSkipLastTimedTest extends RxJavaTest {

    @Test
    public void skipLastTimed() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> source = PublishProcessor.create();

        // FIXME the timeunit now matters due to rounding
        Flowable<Integer> result = source.skipLast(1000, TimeUnit.MILLISECONDS, scheduler);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        result.subscribe(subscriber);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);

        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        source.onNext(4);
        source.onNext(5);
        source.onNext(6);

        scheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS);
        source.onComplete();

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onNext(1);
        inOrder.verify(subscriber).onNext(2);
        inOrder.verify(subscriber).onNext(3);
        inOrder.verify(subscriber, never()).onNext(4);
        inOrder.verify(subscriber, never()).onNext(5);
        inOrder.verify(subscriber, never()).onNext(6);
        inOrder.verify(subscriber).onComplete();
        inOrder.verifyNoMoreInteractions();

        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void skipLastTimedErrorBeforeTime() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> source = PublishProcessor.create();

        Flowable<Integer> result = source.skipLast(1, TimeUnit.SECONDS, scheduler);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        result.subscribe(subscriber);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        source.onError(new TestException());

        scheduler.advanceTimeBy(1050, TimeUnit.MILLISECONDS);

        verify(subscriber).onError(any(TestException.class));

        verify(subscriber, never()).onComplete();
        verify(subscriber, never()).onNext(any());
    }

    @Test
    public void skipLastTimedCompleteBeforeTime() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> source = PublishProcessor.create();

        Flowable<Integer> result = source.skipLast(1, TimeUnit.SECONDS, scheduler);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        result.subscribe(subscriber);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);

        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        source.onComplete();

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onComplete();
        inOrder.verifyNoMoreInteractions();

        verify(subscriber, never()).onNext(any());
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void skipLastTimedWhenAllElementsAreValid() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> source = PublishProcessor.create();

        Flowable<Integer> result = source.skipLast(1, TimeUnit.MILLISECONDS, scheduler);

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        result.subscribe(subscriber);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);

        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        source.onComplete();

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber).onNext(1);
        inOrder.verify(subscriber).onNext(2);
        inOrder.verify(subscriber).onNext(3);
        inOrder.verify(subscriber).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void skipLastTimedDefaultScheduler() {
        Flowable.just(1).concatWith(Flowable.just(2).delay(500, TimeUnit.MILLISECONDS))
        .skipLast(300, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void skipLastTimedDefaultSchedulerDelayError() {
        Flowable.just(1).concatWith(Flowable.just(2).delay(500, TimeUnit.MILLISECONDS))
        .skipLast(300, TimeUnit.MILLISECONDS, true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void skipLastTimedCustomSchedulerDelayError() {
        Flowable.just(1).concatWith(Flowable.just(2).delay(500, TimeUnit.MILLISECONDS))
        .skipLast(300, TimeUnit.MILLISECONDS, Schedulers.io(), true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishProcessor.create().skipLast(1, TimeUnit.DAYS));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f) throws Exception {
                return f.skipLast(1, TimeUnit.DAYS);
            }
        });
    }

    @Test
    public void onNextDisposeRace() {
        TestScheduler scheduler = new TestScheduler();
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = pp.skipLast(1, TimeUnit.DAYS, scheduler).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void errorDelayed() {
        Flowable.error(new TestException())
        .skipLast(1, TimeUnit.DAYS, new TestScheduler(), true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void take() {
        Flowable.just(1)
        .skipLast(0, TimeUnit.SECONDS)
        .take(1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void observeOn() {
        Flowable.range(1, 1000)
        .skipLast(0, TimeUnit.SECONDS)
        .observeOn(Schedulers.single(), false, 16)
        .to(TestHelper.<Integer>testConsumer())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(1000)
        .assertComplete()
        .assertNoErrors();
    }
}
