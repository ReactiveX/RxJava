/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.observers.*;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableRangeTest extends RxJavaTest {

    @Test
    public void rangeStartAt2Count3() {
        Observer<Integer> observer = TestHelper.mockObserver();

        Observable.range(2, 3).subscribe(observer);

        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onNext(4);
        verify(observer, never()).onNext(5);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void rangeUnsubscribe() {
        Observer<Integer> observer = TestHelper.mockObserver();

        final AtomicInteger count = new AtomicInteger();

        Observable.range(1, 1000).doOnNext(new Consumer<Integer>() {
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
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        assertEquals(3, count.get());
    }

    @Test
    public void rangeWithZero() {
        Observable.range(1, 0);
    }

    @Test
    public void rangeWithOverflow2() {
        Observable.range(Integer.MAX_VALUE, 0);
    }

    @Test
    public void rangeWithOverflow3() {
        Observable.range(1, Integer.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rangeWithOverflow4() {
        Observable.range(2, Integer.MAX_VALUE);
    }

    @Test
    public void rangeWithOverflow5() {
        assertFalse(Observable.range(Integer.MIN_VALUE, 0).blockingIterable().iterator().hasNext());
    }

    @Test
    public void noBackpressure() {
        ArrayList<Integer> list = new ArrayList<>(Flowable.bufferSize() * 2);
        for (int i = 1; i <= Flowable.bufferSize() * 2 + 1; i++) {
            list.add(i);
        }

        Observable<Integer> o = Observable.range(1, list.size());

        TestObserverEx<Integer> to = new TestObserverEx<>();

        o.subscribe(to);

        to.assertValueSequence(list);
        to.assertTerminated();
    }

    @Test
    public void emptyRangeSendsOnCompleteEagerlyWithRequestZero() {
        final AtomicBoolean completed = new AtomicBoolean(false);
        Observable.range(1, 0).subscribe(new DefaultObserver<Integer>() {

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

    @Test
    public void nearMaxValueWithoutBackpressure() {
        TestObserver<Integer> to = new TestObserver<>();
        Observable.range(Integer.MAX_VALUE - 1, 2).subscribe(to);

        to.assertComplete();
        to.assertNoErrors();
        to.assertValues(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
    }

    @Test
    public void negativeCount() {
        try {
            Observable.range(1, -1);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("count >= 0 required but it was -1", ex.getMessage());
        }
    }

    @Test
    public void requestWrongFusion() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ASYNC);

        Observable.range(1, 5)
        .subscribe(to);

        to.assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3, 4, 5);
    }
}
