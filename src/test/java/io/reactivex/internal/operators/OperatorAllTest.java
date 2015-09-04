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

public class OperatorAllTest {

    @Test
    public void testAll() {
        Observable<String> obs = Observable.just("one", "two", "six");

        Subscriber <Boolean> observer = TestHelper.mockSubscriber();
        
        obs.all(s -> s.length() == 3)
        .subscribe(observer);

        verify(observer).onSubscribe(any());
        verify(observer).onNext(true);
        verify(observer).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testNotAll() {
        Observable<String> obs = Observable.just("one", "two", "three", "six");

        Subscriber <Boolean> observer = TestHelper.mockSubscriber();

        obs.all(s -> s.length() == 3)
        .subscribe(observer);

        verify(observer).onSubscribe(any());
        verify(observer).onNext(false);
        verify(observer).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testEmpty() {
        Observable<String> obs = Observable.empty();

        Subscriber <Boolean> observer = TestHelper.mockSubscriber();

        obs.all(s -> s.length() == 3)
        .subscribe(observer);

        verify(observer).onSubscribe(any());
        verify(observer).onNext(true);
        verify(observer).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testError() {
        Throwable error = new Throwable();
        Observable<String> obs = Observable.error(error);

        Subscriber <Boolean> observer = TestHelper.mockSubscriber();

        obs.all(s -> s.length() == 3)
        .subscribe(observer);

        verify(observer).onSubscribe(any());
        verify(observer).onError(error);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testFollowingFirst() {
        Observable<Integer> o = Observable.fromArray(1, 3, 5, 6);
        Observable<Boolean> allOdd = o.all(i -> i % 2 == 1);
        
        assertFalse(allOdd.toBlocking().first());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstream() {
        Observable<Integer> source = Observable.just(1)
            .all(t1 -> false)
            .flatMap(t1 -> Observable.just(2).delay(500, TimeUnit.MILLISECONDS));
        
        assertEquals((Object)2, source.toBlocking().first());
    }
    
    @Test
    public void testBackpressureIfNoneRequestedNoneShouldBeDelivered() {
        TestSubscriber<Boolean> ts = new TestSubscriber<>((Long)null);
        Observable.empty().all(t1 -> false).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }
    
    @Test
    public void testBackpressureIfOneRequestedOneShouldBeDelivered() {
        TestSubscriber<Boolean> ts = new TestSubscriber<>(1L);
        
        Observable.empty().all(t -> false).subscribe(ts);
        
        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertComplete();
        
        ts.assertValue(true);
    }
    
    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessage() {
        TestSubscriber<Boolean> ts = new TestSubscriber<>();
        
        final IllegalArgumentException ex = new IllegalArgumentException();
        
        Observable.just("Boo!").all(v -> {
            throw ex;
        })
        .subscribe(ts);
        
        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(ex);
        // FIXME need to decide about adding the value that probably caused the crash in some way
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }
}