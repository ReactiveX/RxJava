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

package io.reactivex.internal.operators.observable;

import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.flowable.TestHelper;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.PublishSubject;

public class NbpOperatorTimeIntervalTest {

    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

    private Observer<Timed<Integer>> NbpObserver;

    private TestScheduler testScheduler;
    private PublishSubject<Integer> subject;
    private Observable<Timed<Integer>> NbpObservable;

    @Before
    public void setUp() {
        NbpObserver = TestHelper.mockNbpSubscriber();
        testScheduler = new TestScheduler();
        subject = PublishSubject.create();
        NbpObservable = subject.timeInterval(testScheduler);
    }

    @Test
    public void testTimeInterval() {
        InOrder inOrder = inOrder(NbpObserver);
        NbpObservable.subscribe(NbpObserver);

        testScheduler.advanceTimeBy(1000, TIME_UNIT);
        subject.onNext(1);
        testScheduler.advanceTimeBy(2000, TIME_UNIT);
        subject.onNext(2);
        testScheduler.advanceTimeBy(3000, TIME_UNIT);
        subject.onNext(3);
        subject.onComplete();

        inOrder.verify(NbpObserver, times(1)).onNext(
                new Timed<Integer>(1, 1000, TIME_UNIT));
        inOrder.verify(NbpObserver, times(1)).onNext(
                new Timed<Integer>(2, 2000, TIME_UNIT));
        inOrder.verify(NbpObserver, times(1)).onNext(
                new Timed<Integer>(3, 3000, TIME_UNIT));
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }
}