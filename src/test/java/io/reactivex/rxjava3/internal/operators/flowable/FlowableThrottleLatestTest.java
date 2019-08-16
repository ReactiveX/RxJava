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

import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableThrottleLatestTest extends RxJavaTest {

    @Test
    public void just() {
        Flowable.just(1)
        .throttleLatest(1, TimeUnit.MINUTES)
        .test()
        .assertResult(1);
    }

    @Test
    public void range() {
        Flowable.range(1, 5)
        .throttleLatest(1, TimeUnit.MINUTES)
        .test()
        .assertResult(1);
    }

    @Test
    public void rangeEmitLatest() {
        Flowable.range(1, 5)
        .throttleLatest(1, TimeUnit.MINUTES, true)
        .test()
        .assertResult(1, 5);
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
        .throttleLatest(1, TimeUnit.MINUTES)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f) throws Exception {
                return f.throttleLatest(1, TimeUnit.MINUTES);
            }
        });
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(
                Flowable.never()
                .throttleLatest(1, TimeUnit.MINUTES)
        );
    }

    @Test
    public void normal() {
        TestScheduler sch = new TestScheduler();
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.throttleLatest(1, TimeUnit.SECONDS, sch).test();

        pp.onNext(1);

        ts.assertValuesOnly(1);

        pp.onNext(2);

        ts.assertValuesOnly(1);

        pp.onNext(3);

        ts.assertValuesOnly(1);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValuesOnly(1, 3);

        pp.onNext(4);

        ts.assertValuesOnly(1, 3);

        pp.onNext(5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValuesOnly(1, 3, 5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValuesOnly(1, 3, 5);

        pp.onNext(6);

        ts.assertValuesOnly(1, 3, 5, 6);

        pp.onNext(7);
        pp.onComplete();

        ts.assertResult(1, 3, 5, 6);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertResult(1, 3, 5, 6);
    }

    @Test
    public void normalEmitLast() {
        TestScheduler sch = new TestScheduler();
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.throttleLatest(1, TimeUnit.SECONDS, sch, true).test();

        pp.onNext(1);

        ts.assertValuesOnly(1);

        pp.onNext(2);

        ts.assertValuesOnly(1);

        pp.onNext(3);

        ts.assertValuesOnly(1);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValuesOnly(1, 3);

        pp.onNext(4);

        ts.assertValuesOnly(1, 3);

        pp.onNext(5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValuesOnly(1, 3, 5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValuesOnly(1, 3, 5);

        pp.onNext(6);

        ts.assertValuesOnly(1, 3, 5, 6);

        pp.onNext(7);
        pp.onComplete();

        ts.assertResult(1, 3, 5, 6, 7);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertResult(1, 3, 5, 6, 7);
    }

    @Test
    public void missingBackpressureExceptionFirst() throws Throwable {
        TestScheduler sch = new TestScheduler();
        Action onCancel = mock(Action.class);

        Flowable.just(1, 2)
        .doOnCancel(onCancel)
        .throttleLatest(1, TimeUnit.MINUTES, sch)
        .test(0)
        .assertFailure(MissingBackpressureException.class);

        verify(onCancel).run();
    }

    @Test
    public void missingBackpressureExceptionLatest() throws Throwable {
        TestScheduler sch = new TestScheduler();
        Action onCancel = mock(Action.class);

        TestSubscriber<Integer> ts = Flowable.just(1, 2)
        .concatWith(Flowable.<Integer>never())
        .doOnCancel(onCancel)
        .throttleLatest(1, TimeUnit.SECONDS, sch, true)
        .test(1);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertFailure(MissingBackpressureException.class, 1);

        verify(onCancel).run();
    }

    @Test
    public void missingBackpressureExceptionLatestComplete() throws Throwable {
        TestScheduler sch = new TestScheduler();
        Action onCancel = mock(Action.class);

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
        .doOnCancel(onCancel)
        .throttleLatest(1, TimeUnit.SECONDS, sch, true)
        .test(1);

        pp.onNext(1);
        pp.onNext(2);

        ts.assertValuesOnly(1);

        pp.onComplete();

        ts.assertFailure(MissingBackpressureException.class, 1);

        verify(onCancel, never()).run();
    }

    @Test
    public void take() throws Throwable {
        Action onCancel = mock(Action.class);

        Flowable.range(1, 5)
        .doOnCancel(onCancel)
        .throttleLatest(1, TimeUnit.MINUTES)
        .take(1)
        .test()
        .assertResult(1);

        verify(onCancel).run();
    }

    @Test
    public void reentrantComplete() {
        TestScheduler sch = new TestScheduler();
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    pp.onNext(2);
                }
                if (t == 2) {
                    pp.onComplete();
                }
            }
        };

        pp.throttleLatest(1, TimeUnit.SECONDS, sch).subscribe(ts);

        pp.onNext(1);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertResult(1, 2);
    }
}
