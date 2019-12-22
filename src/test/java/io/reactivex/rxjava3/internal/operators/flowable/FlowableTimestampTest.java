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

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableTimestampTest extends RxJavaTest {
    Subscriber<Object> subscriber;

    @Before
    public void before() {
        subscriber = TestHelper.mockSubscriber();
    }

    @Test
    public void timestampWithScheduler() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> source = PublishProcessor.create();
        Flowable<Timed<Integer>> m = source.timestamp(scheduler);
        m.subscribe(subscriber);

        source.onNext(1);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        source.onNext(2);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        source.onNext(3);

        InOrder inOrder = inOrder(subscriber);

        inOrder.verify(subscriber, times(1)).onNext(new Timed<>(1, 0, TimeUnit.MILLISECONDS));
        inOrder.verify(subscriber, times(1)).onNext(new Timed<>(2, 100, TimeUnit.MILLISECONDS));
        inOrder.verify(subscriber, times(1)).onNext(new Timed<>(3, 200, TimeUnit.MILLISECONDS));

        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, never()).onComplete();
    }

    @Test
    public void timestampWithScheduler2() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> source = PublishProcessor.create();
        Flowable<Timed<Integer>> m = source.timestamp(scheduler);
        m.subscribe(subscriber);

        source.onNext(1);
        source.onNext(2);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        source.onNext(3);

        InOrder inOrder = inOrder(subscriber);

        inOrder.verify(subscriber, times(1)).onNext(new Timed<>(1, 0, TimeUnit.MILLISECONDS));
        inOrder.verify(subscriber, times(1)).onNext(new Timed<>(2, 0, TimeUnit.MILLISECONDS));
        inOrder.verify(subscriber, times(1)).onNext(new Timed<>(3, 200, TimeUnit.MILLISECONDS));

        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, never()).onComplete();
    }

    @Test
    public void timeIntervalDefault() {
        final TestScheduler scheduler = new TestScheduler();

        RxJavaPlugins.setComputationSchedulerHandler(new Function<Scheduler, Scheduler>() {
            @Override
            public Scheduler apply(Scheduler v) throws Exception {
                return scheduler;
            }
        });

        try {
            Flowable.range(1, 5)
            .timestamp()
            .map(new Function<Timed<Integer>, Long>() {
                @Override
                public Long apply(Timed<Integer> v) throws Exception {
                    return v.time();
                }
            })
            .test()
            .assertResult(0L, 0L, 0L, 0L, 0L);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void timeIntervalDefaultSchedulerCustomUnit() {
        final TestScheduler scheduler = new TestScheduler();

        RxJavaPlugins.setComputationSchedulerHandler(new Function<Scheduler, Scheduler>() {
            @Override
            public Scheduler apply(Scheduler v) throws Exception {
                return scheduler;
            }
        });

        try {
            Flowable.range(1, 5)
            .timestamp(TimeUnit.SECONDS)
            .map(new Function<Timed<Integer>, Long>() {
                @Override
                public Long apply(Timed<Integer> v) throws Exception {
                    return v.time();
                }
            })
            .test()
            .assertResult(0L, 0L, 0L, 0L, 0L);
        } finally {
            RxJavaPlugins.reset();
        }
    }

}
