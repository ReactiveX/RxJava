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
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorAnyTest {

    @Test
    public void testAnyWithTwoItems() {
        NbpObservable<Integer> w = NbpObservable.just(1, 2);
        NbpObservable<Boolean> NbpObservable = w.any(v -> true);

        NbpSubscriber<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();
        
        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onNext(false);
        verify(NbpObserver, times(1)).onNext(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithTwoItems() {
        NbpObservable<Integer> w = NbpObservable.just(1, 2);
        NbpObservable<Boolean> NbpObservable = w.isEmpty();

        NbpSubscriber<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onNext(true);
        verify(NbpObserver, times(1)).onNext(false);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testAnyWithOneItem() {
        NbpObservable<Integer> w = NbpObservable.just(1);
        NbpObservable<Boolean> NbpObservable = w.any(v -> true);

        NbpSubscriber<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onNext(false);
        verify(NbpObserver, times(1)).onNext(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithOneItem() {
        NbpObservable<Integer> w = NbpObservable.just(1);
        NbpObservable<Boolean> NbpObservable = w.isEmpty();

        NbpSubscriber<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onNext(true);
        verify(NbpObserver, times(1)).onNext(false);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testAnyWithEmpty() {
        NbpObservable<Integer> w = NbpObservable.empty();
        NbpObservable<Boolean> NbpObservable = w.any(v -> true);

        NbpSubscriber<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, times(1)).onNext(false);
        verify(NbpObserver, never()).onNext(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithEmpty() {
        NbpObservable<Integer> w = NbpObservable.empty();
        NbpObservable<Boolean> NbpObservable = w.isEmpty();

        NbpSubscriber<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, times(1)).onNext(true);
        verify(NbpObserver, never()).onNext(false);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testAnyWithPredicate1() {
        NbpObservable<Integer> w = NbpObservable.just(1, 2, 3);
        NbpObservable<Boolean> NbpObservable = w.any(t1 -> t1 < 2);

        NbpSubscriber<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onNext(false);
        verify(NbpObserver, times(1)).onNext(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testExists1() {
        NbpObservable<Integer> w = NbpObservable.just(1, 2, 3);
        NbpObservable<Boolean> NbpObservable = w.any(t1 -> t1 < 2);

        NbpSubscriber<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onNext(false);
        verify(NbpObserver, times(1)).onNext(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testAnyWithPredicate2() {
        NbpObservable<Integer> w = NbpObservable.just(1, 2, 3);
        NbpObservable<Boolean> NbpObservable = w.any(t1 -> t1 < 1);

        NbpSubscriber<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, times(1)).onNext(false);
        verify(NbpObserver, never()).onNext(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testAnyWithEmptyAndPredicate() {
        // If the source is empty, always output false.
        NbpObservable<Integer> w = NbpObservable.empty();
        NbpObservable<Boolean> NbpObservable = w.any(t -> true);

        NbpSubscriber<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, times(1)).onNext(false);
        verify(NbpObserver, never()).onNext(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testWithFollowingFirst() {
        NbpObservable<Integer> o = NbpObservable.fromArray(1, 3, 5, 6);
        NbpObservable<Boolean> anyEven = o.any(i -> i % 2 == 0);
        
        assertTrue(anyEven.toBlocking().first());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstream() {
        NbpObservable<Integer> source = NbpObservable.just(1).isEmpty()
            .flatMap(t1 -> NbpObservable.just(2).delay(500, TimeUnit.MILLISECONDS));
        
        assertEquals((Object)2, source.toBlocking().first());
    }
    
    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessage() {
        NbpTestSubscriber<Boolean> ts = new NbpTestSubscriber<>();
        final IllegalArgumentException ex = new IllegalArgumentException();
        
        NbpObservable.just("Boo!").any(v -> {
            throw ex;
        }).subscribe(ts);
        
        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(ex);
        // FIXME value as last cause?
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }
}