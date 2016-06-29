/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.flowable;

import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.*;

public class FlowableTimeIntervalTest {

    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

    private Subscriber<Timed<Integer>> observer;

    private TestScheduler testScheduler;
    private PublishProcessor<Integer> subject;
    private Flowable<Timed<Integer>> observable;

    @Before
    public void setUp() {
        observer = TestHelper.mockSubscriber();
        testScheduler = new TestScheduler();
        subject = PublishProcessor.create();
        observable = subject.timeInterval(testScheduler);
    }

    @Test
    public void testTimeInterval() {
        InOrder inOrder = inOrder(observer);
        observable.subscribe(observer);

        testScheduler.advanceTimeBy(1000, TIME_UNIT);
        subject.onNext(1);
        testScheduler.advanceTimeBy(2000, TIME_UNIT);
        subject.onNext(2);
        testScheduler.advanceTimeBy(3000, TIME_UNIT);
        subject.onNext(3);
        subject.onComplete();

        inOrder.verify(observer, times(1)).onNext(
                new Timed<Integer>(1, 1000, TIME_UNIT));
        inOrder.verify(observer, times(1)).onNext(
                new Timed<Integer>(2, 2000, TIME_UNIT));
        inOrder.verify(observer, times(1)).onNext(
                new Timed<Integer>(3, 3000, TIME_UNIT));
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }
}