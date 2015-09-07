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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorSkipLastTest {

    @Test
    public void testSkipLastEmpty() {
        Observable<String> observable = Observable.<String> empty().skipLast(2);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);
        verify(observer, never()).onNext(any(String.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSkipLast1() {
        Observable<String> observable = Observable.fromIterable(Arrays.asList("one", "two", "three")).skipLast(2);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(observer);
        observable.subscribe(observer);
        inOrder.verify(observer, never()).onNext("two");
        inOrder.verify(observer, never()).onNext("three");
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSkipLast2() {
        Observable<String> observable = Observable.fromIterable(Arrays.asList("one", "two")).skipLast(2);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);
        verify(observer, never()).onNext(any(String.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSkipLastWithZeroCount() {
        Observable<String> w = Observable.just("one", "two");
        Observable<String> observable = w.skipLast(0);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    @Ignore("Null values not allowed")
    public void testSkipLastWithNull() {
        Observable<String> observable = Observable.fromIterable(Arrays.asList("one", null, "two")).skipLast(1);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext(null);
        verify(observer, never()).onNext("two");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSkipLastWithBackpressure() {
        Observable<Integer> o = Observable.range(0, Observable.bufferSize() * 2).skipLast(Observable.bufferSize() + 10);
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        o.observeOn(Schedulers.computation()).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals((Observable.bufferSize()) - 10, ts.valueCount());

    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSkipLastWithNegativeCount() {
        Observable.just("one").skipLast(-1);
    }

}