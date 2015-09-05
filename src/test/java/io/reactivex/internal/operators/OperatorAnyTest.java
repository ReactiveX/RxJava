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

package io.reactivex.internal.operators;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorAnyTest {

    @Test
    public void testAnyWithTwoItems() {
        Observable<Integer> w = Observable.just(1, 2);
        Observable<Boolean> observable = w.any(v -> true);

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();
        
        observable.subscribe(observer);
        
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithTwoItems() {
        Observable<Integer> w = Observable.just(1, 2);
        Observable<Boolean> observable = w.isEmpty();

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, never()).onNext(true);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithOneItem() {
        Observable<Integer> w = Observable.just(1);
        Observable<Boolean> observable = w.any(v -> true);

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithOneItem() {
        Observable<Integer> w = Observable.just(1);
        Observable<Boolean> observable = w.isEmpty();

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, never()).onNext(true);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.any(v -> true);

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.isEmpty();

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onNext(false);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithPredicate1() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> observable = w.any(t1 -> t1 < 2);

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testExists1() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> observable = w.any(t1 -> t1 < 2);

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithPredicate2() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> observable = w.any(t1 -> t1 < 1);

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithEmptyAndPredicate() {
        // If the source is empty, always output false.
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.any(t -> true);

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testWithFollowingFirst() {
        Observable<Integer> o = Observable.fromArray(1, 3, 5, 6);
        Observable<Boolean> anyEven = o.any(i -> i % 2 == 0);
        
        assertTrue(anyEven.toBlocking().first());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstream() {
        Observable<Integer> source = Observable.just(1).isEmpty()
            .flatMap(t1 -> Observable.just(2).delay(500, TimeUnit.MILLISECONDS));
        
        assertEquals((Object)2, source.toBlocking().first());
    }
    
    @Test
    public void testBackpressureIfNoneRequestedNoneShouldBeDelivered() {
        TestSubscriber<Boolean> ts = new TestSubscriber<>((Long)null);
        
        Observable.just(1).any(t -> true)
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }
    
    @Test
    public void testBackpressureIfOneRequestedOneShouldBeDelivered() {
        TestSubscriber<Boolean> ts = new TestSubscriber<>(1L);
        Observable.just(1).any(v -> true).subscribe(ts);
        
        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertComplete();
        ts.assertValue(true);
    }
    
    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessage() {
        TestSubscriber<Boolean> ts = new TestSubscriber<>();
        final IllegalArgumentException ex = new IllegalArgumentException();
        
        Observable.just("Boo!").any(v -> {
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