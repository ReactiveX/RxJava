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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableTakeLastTimedTest {

    @Test(expected = IndexOutOfBoundsException.class)
    public void testTakeLastTimedWithNegativeCount() {
        Flowable.just("one").takeLast(-1, 1, TimeUnit.SECONDS);
    }

    @Test
    public void takeLastTimed() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Object> source = PublishProcessor.create();

        // FIXME time unit now matters!
        Flowable<Object> result = source.takeLast(1000, TimeUnit.MILLISECONDS, scheduler);

        Subscriber<Object> o = TestHelper.mockSubscriber();

        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        source.onNext(1); // T: 0ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(2); // T: 250ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(3); // T: 500ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(4); // T: 750ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(5); // T: 1000ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onComplete(); // T: 1250ms

        inOrder.verify(o, times(1)).onNext(2);
        inOrder.verify(o, times(1)).onNext(3);
        inOrder.verify(o, times(1)).onNext(4);
        inOrder.verify(o, times(1)).onNext(5);
        inOrder.verify(o, times(1)).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void takeLastTimedDelayCompletion() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Object> source = PublishProcessor.create();

        // FIXME time unit now matters
        Flowable<Object> result = source.takeLast(1000, TimeUnit.MILLISECONDS, scheduler);

        Subscriber<Object> o = TestHelper.mockSubscriber();

        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        source.onNext(1); // T: 0ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(2); // T: 250ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(3); // T: 500ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(4); // T: 750ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(5); // T: 1000ms
        scheduler.advanceTimeBy(1250, TimeUnit.MILLISECONDS);
        source.onComplete(); // T: 2250ms

        inOrder.verify(o, times(1)).onComplete();

        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void takeLastTimedWithCapacity() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Object> source = PublishProcessor.create();

        // FIXME time unit now matters!
        Flowable<Object> result = source.takeLast(2, 1000, TimeUnit.MILLISECONDS, scheduler);

        Subscriber<Object> o = TestHelper.mockSubscriber();

        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        source.onNext(1); // T: 0ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(2); // T: 250ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(3); // T: 500ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(4); // T: 750ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(5); // T: 1000ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onComplete(); // T: 1250ms

        inOrder.verify(o, times(1)).onNext(4);
        inOrder.verify(o, times(1)).onNext(5);
        inOrder.verify(o, times(1)).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void takeLastTimedThrowingSource() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Object> source = PublishProcessor.create();

        Flowable<Object> result = source.takeLast(1, TimeUnit.SECONDS, scheduler);

        Subscriber<Object> o = TestHelper.mockSubscriber();

        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        source.onNext(1); // T: 0ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(2); // T: 250ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(3); // T: 500ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(4); // T: 750ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(5); // T: 1000ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onError(new TestException()); // T: 1250ms

        inOrder.verify(o, times(1)).onError(any(TestException.class));

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void takeLastTimedWithZeroCapacity() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Object> source = PublishProcessor.create();

        Flowable<Object> result = source.takeLast(0, 1, TimeUnit.SECONDS, scheduler);

        Subscriber<Object> o = TestHelper.mockSubscriber();

        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        source.onNext(1); // T: 0ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(2); // T: 250ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(3); // T: 500ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(4); // T: 750ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(5); // T: 1000ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onComplete(); // T: 1250ms

        inOrder.verify(o, times(1)).onComplete();

        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testContinuousDelivery() {
        TestScheduler scheduler = new TestScheduler();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);
        
        PublishProcessor<Integer> ps = PublishProcessor.create();
        
        ps.takeLast(1000, TimeUnit.MILLISECONDS, scheduler).subscribe(ts);
        
        ps.onNext(1);
        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);
        ps.onNext(2);
        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);
        ps.onNext(3);
        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);
        ps.onNext(4);
        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);
        ps.onComplete();
        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        ts.assertNoValues();
        
        ts.request(1);
        
        ts.assertValue(3);
        
        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);
        ts.request(1);
        
        ts.assertValues(3, 4);
        ts.assertComplete();
        ts.assertNoErrors();
        
    }
}