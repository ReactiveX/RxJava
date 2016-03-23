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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.exceptions.TestException;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.BiFunction;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorWithLatestFromTest {
    static final BiFunction<Integer, Integer, Integer> COMBINER = new BiFunction<Integer, Integer, Integer>() {
        @Override
        public Integer apply(Integer t1, Integer t2) {
            return (t1 << 8) + t2;
        }
    };
    static final BiFunction<Integer, Integer, Integer> COMBINER_ERROR = new BiFunction<Integer, Integer, Integer>() {
        @Override
        public Integer apply(Integer t1, Integer t2) {
            throw new TestException("Forced failure");
        }
    };
    @Test
    public void testSimple() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();
        
        Subscriber<Integer> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);
        
        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);
        
        result.subscribe(o);
        
        source.onNext(1);
        inOrder.verify(o, never()).onNext(anyInt());
        
        other.onNext(1);
        inOrder.verify(o, never()).onNext(anyInt());
        
        source.onNext(2);
        inOrder.verify(o).onNext((2 << 8) + 1);
        
        other.onNext(2);
        inOrder.verify(o, never()).onNext(anyInt());
        
        other.onComplete();
        inOrder.verify(o, never()).onComplete();
        
        source.onNext(3);
        inOrder.verify(o).onNext((3 << 8) + 2);
        
        source.onComplete();
        inOrder.verify(o).onComplete();
        
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testEmptySource() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();
        
        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
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
    public void testEmptyOther() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();
        
        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
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
    public void testUnsubscription() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();
        
        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        result.subscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(other.hasSubscribers());

        other.onNext(1);
        source.onNext(1);
        
        ts.dispose();
        
        ts.assertValue((1 << 8) + 1);
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        assertFalse(source.hasSubscribers());
        assertFalse(other.hasSubscribers());
    }

    @Test
    public void testSourceThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();
        
        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
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
    public void testOtherThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();
        
        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
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
    public void testFunctionThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();
        
        Flowable<Integer> result = source.withLatestFrom(other, COMBINER_ERROR);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
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
    public void testNoDownstreamUnsubscribe() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();
        
        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        result.unsafeSubscribe(ts);
        
        source.onComplete();
        
        assertFalse(ts.isCancelled());
    }
    
    @Test
    public void testBackpressure() {
        Flowable<Integer> source = Flowable.range(1, 10);
        PublishProcessor<Integer> other = PublishProcessor.create();
        
        Flowable<Integer> result = source.withLatestFrom(other, COMBINER);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>((Long)null);
        
        result.subscribe(ts);

        assertTrue("Other has no observers!", other.hasSubscribers());
        
        ts.request(1);

        assertTrue("Other has no observers!", other.hasSubscribers());

        ts.assertNoValues();
        
        other.onNext(1);
        
        ts.request(1);
        
        ts.assertValue((2 << 8) + 1);
        
        ts.request(5);
        ts.assertValues(
                (2 << 8) + 1, (3 << 8) + 1, (4 << 8) + 1, (5 << 8) + 1, 
                (6 << 8) + 1, (7 << 8) + 1 
        );
        
        ts.dispose();
        
        assertFalse("Other has observers!", other.hasSubscribers());

        ts.assertNoErrors();
    }
}