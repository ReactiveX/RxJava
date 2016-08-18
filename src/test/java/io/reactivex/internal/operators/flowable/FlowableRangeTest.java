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
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.functions.Consumer;
import io.reactivex.subscribers.*;

public class FlowableRangeTest {

    @Test
    public void testRangeStartAt2Count3() {
        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        
        Flowable.range(2, 3).subscribe(observer);

        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onNext(4);
        verify(observer, never()).onNext(5);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testRangeUnsubscribe() {
        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        
        final AtomicInteger count = new AtomicInteger();
        
        Flowable.range(1, 1000).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t1) {
                count.incrementAndGet();
            }
        })
        .take(3).subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, never()).onNext(4);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
        assertEquals(3, count.get());
    }

    @Test
    public void testRangeWithZero() {
        Flowable.range(1, 0);
    }

    @Test
    public void testRangeWithOverflow2() {
        Flowable.range(Integer.MAX_VALUE, 0);
    }

    @Test
    public void testRangeWithOverflow3() {
        Flowable.range(1, Integer.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeWithOverflow4() {
        Flowable.range(2, Integer.MAX_VALUE);
    }

    @Test
    public void testRangeWithOverflow5() {
        assertFalse(Flowable.range(Integer.MIN_VALUE, 0).blockingIterable().iterator().hasNext());
    }

    @Test
    public void testBackpressureViaRequest() {
        Flowable<Integer> o = Flowable.range(1, Flowable.bufferSize());
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);
        
        ts.assertNoValues();
        ts.request(1);
        
        o.subscribe(ts);
        
        ts.assertValue(1);
        
        ts.request(2);
        ts.assertValues(1, 2, 3);
        
        ts.request(3);
        ts.assertValues(1, 2, 3, 4, 5, 6);
        
        ts.request(Flowable.bufferSize());
        ts.assertTerminated();
    }

    @Test
    public void testNoBackpressure() {
        ArrayList<Integer> list = new ArrayList<Integer>(Flowable.bufferSize() * 2);
        for (int i = 1; i <= Flowable.bufferSize() * 2 + 1; i++) {
            list.add(i);
        }

        Flowable<Integer> o = Flowable.range(1, list.size());
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);
        
        ts.assertNoValues();
        ts.request(Long.MAX_VALUE); // infinite
        
        o.subscribe(ts);
        
        ts.assertValueSequence(list);
        ts.assertTerminated();
    }
    void testWithBackpressureOneByOne(int start) {
        Flowable<Integer> source = Flowable.range(start, 100);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);
        ts.request(1);
        source.subscribe(ts);
        
        List<Integer> list = new ArrayList<Integer>(100);
        for (int i = 0; i < 100; i++) {
            list.add(i + start);
            ts.request(1);
        }
        ts.assertValueSequence(list);
        ts.assertTerminated();
    }
    void testWithBackpressureAllAtOnce(int start) {
        Flowable<Integer> source = Flowable.range(start, 100);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);
        ts.request(100);
        source.subscribe(ts);
        
        List<Integer> list = new ArrayList<Integer>(100);
        for (int i = 0; i < 100; i++) {
            list.add(i + start);
        }
        ts.assertValueSequence(list);
        ts.assertTerminated();
    }
    @Test
    public void testWithBackpressure1() {
        for (int i = 0; i < 100; i++) {
            testWithBackpressureOneByOne(i);
        }
    }
    @Test
    public void testWithBackpressureAllAtOnce() {
        for (int i = 0; i < 100; i++) {
            testWithBackpressureAllAtOnce(i);
        }
    }
    @Test
    public void testWithBackpressureRequestWayMore() {
        Flowable<Integer> source = Flowable.range(50, 100);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);
        ts.request(150);
        source.subscribe(ts);
        
        List<Integer> list = new ArrayList<Integer>(100);
        for (int i = 0; i < 100; i++) {
            list.add(i + 50);
        }
        
        ts.request(50); // and then some
        
        ts.assertValueSequence(list);
        ts.assertTerminated();
    }
    
    @Test
    public void testRequestOverflow() {
        final AtomicInteger count = new AtomicInteger();
        int n = 10;
        Flowable.range(1, n).subscribe(new DefaultSubscriber<Integer>() {

            @Override
            public void onStart() {
                request(2);
            }
            
            @Override
            public void onComplete() {
                //do nothing
            }

            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }

            @Override
            public void onNext(Integer t) {
                count.incrementAndGet();
                request(Long.MAX_VALUE - 1);
            }});
        assertEquals(n, count.get());
    }
    
    @Test
    public void testEmptyRangeSendsOnCompleteEagerlyWithRequestZero() {
        final AtomicBoolean completed = new AtomicBoolean(false);
        Flowable.range(1, 0).subscribe(new DefaultSubscriber<Integer>() {

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
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.range(Integer.MAX_VALUE - 1, 2).subscribe(ts);
        
        ts.assertComplete();
        ts.assertNoErrors();
        ts.assertValues(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
    }
    @Test(timeout = 1000)
    public void testNearMaxValueWithBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(3L);
        Flowable.range(Integer.MAX_VALUE - 1, 2).subscribe(ts);
        
        ts.assertComplete();
        ts.assertNoErrors();
        ts.assertValues(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
    }
    

    @Test
    public void negativeCount() {
        try {
            Flowable.range(1, -1);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("count >= 0 required but it was -1", ex.getMessage());
        }
    }
}