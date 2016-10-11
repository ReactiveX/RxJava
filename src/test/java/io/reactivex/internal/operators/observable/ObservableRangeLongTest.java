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

package io.reactivex.internal.operators.observable;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.TestHelper;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DefaultObserver;
import io.reactivex.observers.TestObserver;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObservableRangeLongTest {
    @Test
    public void testRangeStartAt2Count3() {
        Observer<Long> observer = TestHelper.mockObserver();

        Observable.rangeLong(2, 3).subscribe(observer);

        verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onNext(3L);
        verify(observer, times(1)).onNext(4L);
        verify(observer, never()).onNext(5L);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testRangeUnsubscribe() {
        Observer<Long> observer = TestHelper.mockObserver();

        final AtomicInteger count = new AtomicInteger();

        Observable.rangeLong(1, 1000).doOnNext(new Consumer<Long>() {
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
        Observable.rangeLong(1L, 0L);
    }

    @Test
    public void testRangeWithOverflow2() {
        Observable.rangeLong(Long.MAX_VALUE, 0L);
    }

    @Test
    public void testRangeWithOverflow3() {
        Observable.rangeLong(1L, Long.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeWithOverflow4() {
        Observable.rangeLong(2L, Long.MAX_VALUE);
    }

    @Test
    public void testRangeWithOverflow5() {
        assertFalse(Observable.rangeLong(Integer.MIN_VALUE, 0).blockingIterable().iterator().hasNext());
    }

    @Test
    public void testNoBackpressure() {
        ArrayList<Long> list = new ArrayList<Long>(Flowable.bufferSize() * 2);
        for (long i = 1; i <= Flowable.bufferSize() * 2 + 1; i++) {
            list.add(i);
        }

        Observable<Long> o = Observable.rangeLong(1, list.size());

        TestObserver<Long> ts = new TestObserver<Long>();

        o.subscribe(ts);

        ts.assertValueSequence(list);
        ts.assertTerminated();
    }

    @Test
    public void testEmptyRangeSendsOnCompleteEagerlyWithRequestZero() {
        final AtomicBoolean completed = new AtomicBoolean(false);
        Observable.rangeLong(1L, 0L).subscribe(new DefaultObserver<Long>() {

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
        TestObserver<Long> ts = new TestObserver<Long>();
        Observable.rangeLong(Long.MAX_VALUE - 1L, 2L).subscribe(ts);

        ts.assertComplete();
        ts.assertNoErrors();
        ts.assertValues(Long.MAX_VALUE - 1, Long.MAX_VALUE);
    }

    @Test
    public void negativeCount() {
        try {
            Observable.rangeLong(1L, -1L);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("count >= 0 required but it was -1", ex.getMessage());
        }
    }

    @Test
    public void countOne() {
        Observable.rangeLong(5495454L, 1L)
            .test()
            .assertResult(5495454L);
    }
}
