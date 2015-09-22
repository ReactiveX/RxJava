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

import java.util.ArrayList;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOnSubscribeRangeTest {

    @Test
    public void testRangeStartAt2Count3() {
        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        
        NbpObservable.range(2, 3).subscribe(NbpObserver);

        verify(NbpObserver, times(1)).onNext(2);
        verify(NbpObserver, times(1)).onNext(3);
        verify(NbpObserver, times(1)).onNext(4);
        verify(NbpObserver, never()).onNext(5);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testRangeUnsubscribe() {
        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        
        final AtomicInteger count = new AtomicInteger();
        
        NbpObservable.range(1, 1000).doOnNext(t1 -> count.incrementAndGet())
        .take(3).subscribe(NbpObserver);

        verify(NbpObserver, times(1)).onNext(1);
        verify(NbpObserver, times(1)).onNext(2);
        verify(NbpObserver, times(1)).onNext(3);
        verify(NbpObserver, never()).onNext(4);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        assertEquals(3, count.get());
    }

    @Test
    public void testRangeWithZero() {
        NbpObservable.range(1, 0);
    }

    @Test
    public void testRangeWithOverflow2() {
        NbpObservable.range(Integer.MAX_VALUE, 0);
    }

    @Test
    public void testRangeWithOverflow3() {
        NbpObservable.range(1, Integer.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeWithOverflow4() {
        NbpObservable.range(2, Integer.MAX_VALUE);
    }

    @Test
    public void testRangeWithOverflow5() {
        assertFalse(NbpObservable.range(Integer.MIN_VALUE, 0).toBlocking().iterator().hasNext());
    }

    @Test
    public void testNoBackpressure() {
        ArrayList<Integer> list = new ArrayList<>(Observable.bufferSize() * 2);
        for (int i = 1; i <= Observable.bufferSize() * 2 + 1; i++) {
            list.add(i);
        }

        NbpObservable<Integer> o = NbpObservable.range(1, list.size());
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
        o.subscribe(ts);
        
        ts.assertValueSequence(list);
        ts.assertTerminated();
    }
    
    @Test
    public void testEmptyRangeSendsOnCompleteEagerlyWithRequestZero() {
        final AtomicBoolean completed = new AtomicBoolean(false);
        NbpObservable.range(1, 0).subscribe(new NbpObserver<Integer>() {

            @Override
            public void onStart() {
//                request(0);
            }
            
            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable e) {
                
            }

            @Override
            public void onNext(Integer t) {
                
            }});
        assertTrue(completed.get());
    }
    
    @Test(timeout = 1000)
    public void testNearMaxValueWithoutBackpressure() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable.range(Integer.MAX_VALUE - 1, 2).subscribe(ts);
        
        ts.assertComplete();
        ts.assertNoErrors();
        ts.assertValues(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
    }
}