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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorTakeLastTest {

    @Test
    public void testTakeLastEmpty() {
        NbpObservable<String> w = NbpObservable.empty();
        NbpObservable<String> take = w.takeLast(2);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        take.subscribe(NbpObserver);
        verify(NbpObserver, never()).onNext(any(String.class));
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testTakeLast1() {
        NbpObservable<String> w = NbpObservable.just("one", "two", "three");
        NbpObservable<String> take = w.takeLast(2);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(NbpObserver);
        take.subscribe(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext("two");
        inOrder.verify(NbpObserver, times(1)).onNext("three");
        verify(NbpObserver, never()).onNext("one");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testTakeLast2() {
        NbpObservable<String> w = NbpObservable.just("one");
        NbpObservable<String> take = w.takeLast(10);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        take.subscribe(NbpObserver);
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testTakeLastWithZeroCount() {
        NbpObservable<String> w = NbpObservable.just("one");
        NbpObservable<String> take = w.takeLast(0);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        take.subscribe(NbpObserver);
        verify(NbpObserver, never()).onNext("one");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    @Ignore("Null values no longer allowed")
    public void testTakeLastWithNull() {
        NbpObservable<String> w = NbpObservable.just("one", null, "three");
        NbpObservable<String> take = w.takeLast(2);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        take.subscribe(NbpObserver);
        verify(NbpObserver, never()).onNext("one");
        verify(NbpObserver, times(1)).onNext(null);
        verify(NbpObserver, times(1)).onNext("three");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testTakeLastWithNegativeCount() {
        NbpObservable.just("one").takeLast(-1);
    }

    @Test
    public void testBackpressure1() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable.range(1, 100000).takeLast(1)
        .observeOn(Schedulers.newThread())
        .map(newSlowProcessor()).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        ts.assertValue(100000);
    }

    @Test
    public void testBackpressure2() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable.range(1, 100000).takeLast(Observable.bufferSize() * 4)
        .observeOn(Schedulers.newThread()).map(newSlowProcessor()).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Observable.bufferSize() * 4, ts.valueCount());
    }

    private Function<Integer, Integer> newSlowProcessor() {
        return new Function<Integer, Integer>() {
            int c = 0;

            @Override
            public Integer apply(Integer i) {
                if (c++ < 100) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                    }
                }
                return i;
            }

        };
    }

    @Test
    public void testIssue1522() {
        // https://github.com/ReactiveX/RxJava/issues/1522
        assertEquals(0, NbpObservable
                .empty()
                .count()
                .filter(v -> false)
                .toList()
                .toBlocking().single().size());
    }

    @Test
    public void testUnsubscribeTakesEffectEarlyOnFastPath() {
        final AtomicInteger count = new AtomicInteger();
        NbpObservable.range(0, 100000).takeLast(100000).subscribe(new NbpObserver<Integer>() {

            @Override
            public void onStart() {
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Integer integer) {
                count.incrementAndGet();
                cancel();
            }
        });
        assertEquals(1,count.get());
    }
}