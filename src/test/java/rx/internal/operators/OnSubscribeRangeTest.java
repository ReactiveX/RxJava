/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.internal.util.RxRingBuffer;
import rx.observers.TestSubscriber;

public class OnSubscribeRangeTest {

    @Test
    public void testRangeStartAt2Count3() {
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        Observable.range(2, 3).subscribe(observer);

        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onNext(4);
        verify(observer, never()).onNext(5);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testRangeUnsubscribe() {
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        final AtomicInteger count = new AtomicInteger();
        Observable.range(1, 1000).doOnNext(new Action1<Integer>() {

            @Override
            public void call(Integer t1) {
                count.incrementAndGet();
            }

        }).take(3).subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, never()).onNext(4);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        assertEquals(3, count.get());
    }

    @Test
    public void testRangeWithZero() {
        Observable.range(1, 0);
    }

    @Test
    public void testRangeWithOverflow2() {
        Observable.range(Integer.MAX_VALUE, 0);
    }

    @Test
    public void testRangeWithOverflow3() {
        Observable.range(1, Integer.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeWithOverflow4() {
        Observable.range(2, Integer.MAX_VALUE);
    }

    @Test
    public void testRangeWithOverflow5() {
        assertFalse(Observable.range(Integer.MIN_VALUE, 0).toBlocking().getIterator().hasNext());
    }

    @Test
    public void testBackpressureViaRequest() {
        OnSubscribeRange o = new OnSubscribeRange(1, RxRingBuffer.SIZE);
        TestSubscriber<Integer> ts = TestSubscriber.create();
        ts.assertReceivedOnNext(Collections.<Integer> emptyList());
        ts.requestMore(1);
        o.call(ts);
        ts.assertReceivedOnNext(Arrays.asList(1));
        ts.requestMore(2);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3));
        ts.requestMore(3);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6));
        ts.requestMore(RxRingBuffer.SIZE);
        ts.assertTerminalEvent();
    }

    @Test
    public void testNoBackpressure() {
        ArrayList<Integer> list = new ArrayList<Integer>(RxRingBuffer.SIZE * 2);
        for (int i = 1; i <= RxRingBuffer.SIZE * 2 + 1; i++) {
            list.add(i);
        }

        OnSubscribeRange o = new OnSubscribeRange(1, list.size());
        TestSubscriber<Integer> ts = TestSubscriber.create();
        ts.assertReceivedOnNext(Collections.<Integer> emptyList());
        ts.requestMore(Long.MAX_VALUE); // infinite
        o.call(ts);
        ts.assertReceivedOnNext(list);
        ts.assertTerminalEvent();
    }
    void testWithBackpressureOneByOne(int start) {
        Observable<Integer> source = Observable.range(start, 100);
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        ts.requestMore(1);
        source.subscribe(ts);
        
        List<Integer> list = new ArrayList<Integer>(100);
        for (int i = 0; i < 100; i++) {
            list.add(i + start);
            ts.requestMore(1);
        }
        ts.assertReceivedOnNext(list);
        ts.assertTerminalEvent();
    }
    void testWithBackpressureAllAtOnce(int start) {
        Observable<Integer> source = Observable.range(start, 100);
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        ts.requestMore(100);
        source.subscribe(ts);
        
        List<Integer> list = new ArrayList<Integer>(100);
        for (int i = 0; i < 100; i++) {
            list.add(i + start);
        }
        ts.assertReceivedOnNext(list);
        ts.assertTerminalEvent();
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
        Observable<Integer> source = Observable.range(50, 100);
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        ts.requestMore(150);
        source.subscribe(ts);
        
        List<Integer> list = new ArrayList<Integer>(100);
        for (int i = 0; i < 100; i++) {
            list.add(i + 50);
        }
        
        ts.requestMore(50); // and then some
        
        ts.assertReceivedOnNext(list);
        ts.assertTerminalEvent();
    }
    
    @Test
    public void testRequestOverflow() {
        final AtomicInteger count = new AtomicInteger();
        int n = 10;
        Observable.range(1, n).subscribe(new Subscriber<Integer>() {

            @Override
            public void onStart() {
                request(2);
            }
            
            @Override
            public void onCompleted() {
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
        Observable.range(1, 0).subscribe(new Subscriber<Integer>() {

            @Override
            public void onStart() {
                request(0);
            }
            
            @Override
            public void onCompleted() {
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
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable.range(Integer.MAX_VALUE - 1, 2).subscribe(ts);
        
        ts.assertCompleted();
        ts.assertNoErrors();
        ts.assertValues(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
    }
    @Test(timeout = 1000)
    public void testNearMaxValueWithBackpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(3);
        Observable.range(Integer.MAX_VALUE - 1, 2).subscribe(ts);
        
        ts.assertCompleted();
        ts.assertNoErrors();
        ts.assertValues(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
    }
}
