/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.function.BiFunction;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.exceptions.TestException;
import io.reactivex.subjects.nbp.NbpPublishSubject;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorWithLatestFromTest {
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> other = NbpPublishSubject.create();
        
        NbpSubscriber<Integer> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);
        
        NbpObservable<Integer> result = source.withLatestFrom(other, COMBINER);
        
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> other = NbpPublishSubject.create();
        
        NbpObservable<Integer> result = source.withLatestFrom(other, COMBINER);
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> other = NbpPublishSubject.create();
        
        NbpObservable<Integer> result = source.withLatestFrom(other, COMBINER);
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> other = NbpPublishSubject.create();
        
        NbpObservable<Integer> result = source.withLatestFrom(other, COMBINER);
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> other = NbpPublishSubject.create();
        
        NbpObservable<Integer> result = source.withLatestFrom(other, COMBINER);
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> other = NbpPublishSubject.create();
        
        NbpObservable<Integer> result = source.withLatestFrom(other, COMBINER);
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> other = NbpPublishSubject.create();
        
        NbpObservable<Integer> result = source.withLatestFrom(other, COMBINER_ERROR);
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> other = NbpPublishSubject.create();
        
        NbpObservable<Integer> result = source.withLatestFrom(other, COMBINER);
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
        result.unsafeSubscribe(ts);
        
        source.onComplete();
        
        assertFalse(ts.isCancelled());
    }
}