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

import io.reactivex.Flowable;
import io.reactivex.TestHelper;
import io.reactivex.functions.Consumer;
import io.reactivex.subscribers.DefaultSubscriber;
import io.reactivex.subscribers.TestSubscriber;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.reactivestreams.Subscriber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FlowableRangeLongTest {

    @Test
    public void testRangeStartAt2Count3() {
        Subscriber<Long> observer = TestHelper.mockSubscriber();

        Flowable.rangeLong(2, 3).subscribe(observer);

        verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onNext(3L);
        verify(observer, times(1)).onNext(4L);
        verify(observer, never()).onNext(5L);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testRangeUnsubscribe() {
        Subscriber<Long> observer = TestHelper.mockSubscriber();

        final AtomicInteger count = new AtomicInteger();

        Flowable.rangeLong(1, 1000).doOnNext(new Consumer<Long>() {
            @Override
            public void accept(Long t1) {
                count.incrementAndGet();
            }
        })
        .take(3).subscribe(observer);

        verify(observer, times(1)).onNext(1L);
        verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onNext(3L);
        verify(observer, never()).onNext(4L);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        assertEquals(3, count.get());
    }

    @Test
    public void testRangeWithZero() {
        Flowable.rangeLong(1, 0);
    }

    @Test
    public void testRangeWithOverflow2() {
        Flowable.rangeLong(Long.MAX_VALUE, 0);
    }

    @Test
    public void testRangeWithOverflow3() {
        Flowable.rangeLong(1, Long.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeWithOverflow4() {
        Flowable.rangeLong(2, Long.MAX_VALUE);
    }

    @Test
    public void testRangeWithOverflow5() {
        assertFalse(Flowable.rangeLong(Long.MIN_VALUE, 0).blockingIterable().iterator().hasNext());
    }

    @Test
    public void testBackpressureViaRequest() {
        Flowable<Long> o = Flowable.rangeLong(1, Flowable.bufferSize());

        TestSubscriber<Long> ts = new TestSubscriber<Long>(0L);

        ts.assertNoValues();
        ts.request(1);

        o.subscribe(ts);

        ts.assertValue(1L);

        ts.request(2);
        ts.assertValues(1L, 2L, 3L);

        ts.request(3);
        ts.assertValues(1L, 2L, 3L, 4L, 5L, 6L);

        ts.request(Flowable.bufferSize());
        ts.assertTerminated();
    }

    @Test
    public void testNoBackpressure() {
        ArrayList<Long> list = new ArrayList<Long>(Flowable.bufferSize() * 2);
        for (long i = 1; i <= Flowable.bufferSize() * 2 + 1; i++) {
            list.add(i);
        }

        Flowable<Long> o = Flowable.rangeLong(1, list.size());

        TestSubscriber<Long> ts = new TestSubscriber<Long>(0L);

        ts.assertNoValues();
        ts.request(Long.MAX_VALUE); // infinite

        o.subscribe(ts);

        ts.assertValueSequence(list);
        ts.assertTerminated();
    }
    void testWithBackpressureOneByOne(long start) {
        Flowable<Long> source = Flowable.rangeLong(start, 100);

        TestSubscriber<Long> ts = new TestSubscriber<Long>(0L);
        ts.request(1);
        source.subscribe(ts);

        List<Long> list = new ArrayList<Long>(100);
        for (long i = 0; i < 100; i++) {
            list.add(i + start);
            ts.request(1);
        }
        ts.assertValueSequence(list);
        ts.assertTerminated();
    }
    void testWithBackpressureAllAtOnce(long start) {
        Flowable<Long> source = Flowable.rangeLong(start, 100);

        TestSubscriber<Long> ts = new TestSubscriber<Long>(0L);
        ts.request(100);
        source.subscribe(ts);

        List<Long> list = new ArrayList<Long>(100);
        for (long i = 0; i < 100; i++) {
            list.add(i + start);
        }
        ts.assertValueSequence(list);
        ts.assertTerminated();
    }
    @Test
    public void testWithBackpressure1() {
        for (long i = 0; i < 100; i++) {
            testWithBackpressureOneByOne(i);
        }
    }
    @Test
    public void testWithBackpressureAllAtOnce() {
        for (long i = 0; i < 100; i++) {
            testWithBackpressureAllAtOnce(i);
        }
    }
    @Test
    public void testWithBackpressureRequestWayMore() {
        Flowable<Long> source = Flowable.rangeLong(50, 100);

        TestSubscriber<Long> ts = new TestSubscriber<Long>(0L);
        ts.request(150);
        source.subscribe(ts);

        List<Long> list = new ArrayList<Long>(100);
        for (long i = 0; i < 100; i++) {
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
        Flowable.rangeLong(1, n).subscribe(new DefaultSubscriber<Long>() {

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
            public void onNext(Long t) {
                count.incrementAndGet();
                request(Long.MAX_VALUE - 1);
            }});
        assertEquals(n, count.get());
    }

    @Test
    public void testEmptyRangeSendsOnCompleteEagerlyWithRequestZero() {
        final AtomicBoolean completed = new AtomicBoolean(false);
        Flowable.rangeLong(1, 0).subscribe(new DefaultSubscriber<Long>() {

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
            public void onNext(Long t) {

            }});
        assertTrue(completed.get());
    }

    @Test(timeout = 1000)
    public void testNearMaxValueWithoutBackpressure() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>();
        Flowable.rangeLong(Long.MAX_VALUE - 1L, 2L).subscribe(ts);

        ts.assertComplete();
        ts.assertNoErrors();
        ts.assertValues(Long.MAX_VALUE - 1L, Long.MAX_VALUE);
    }

    @Test(timeout = 1000)
    public void testNearMaxValueWithBackpressure() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>(3L);
        Flowable.rangeLong(Long.MAX_VALUE - 1L, 2L).subscribe(ts);

        ts.assertComplete();
        ts.assertNoErrors();
        ts.assertValues(Long.MAX_VALUE - 1L, Long.MAX_VALUE);
    }

    @Test
    public void negativeCount() {
        try {
            Flowable.rangeLong(1L, -1L);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("count >= 0 required but it was -1", ex.getMessage());
        }
    }
}
