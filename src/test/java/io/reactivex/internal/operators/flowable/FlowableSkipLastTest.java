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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableSkipLastTest {

    @Test
    public void testSkipLastEmpty() {
        Flowable<String> observable = Flowable.<String> empty().skipLast(2);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);
        verify(observer, never()).onNext(any(String.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSkipLast1() {
        Flowable<String> observable = Flowable.fromIterable(Arrays.asList("one", "two", "three")).skipLast(2);

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
        Flowable<String> observable = Flowable.fromIterable(Arrays.asList("one", "two")).skipLast(2);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);
        verify(observer, never()).onNext(any(String.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSkipLastWithZeroCount() {
        Flowable<String> w = Flowable.just("one", "two");
        Flowable<String> observable = w.skipLast(0);

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
        Flowable<String> observable = Flowable.fromIterable(Arrays.asList("one", null, "two")).skipLast(1);

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
        Flowable<Integer> o = Flowable.range(0, Flowable.bufferSize() * 2).skipLast(Flowable.bufferSize() + 10);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        o.observeOn(Schedulers.computation()).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals((Flowable.bufferSize()) - 10, ts.valueCount());

    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSkipLastWithNegativeCount() {
        Flowable.just("one").skipLast(-1);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).skipLast(1));
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
        .skipLast(1)
        .test()
        .assertFailure(TestException.class);
    }

}
