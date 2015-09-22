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

import java.util.Arrays;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorSkipLastTest {

    @Test
    public void testSkipLastEmpty() {
        NbpObservable<String> o = NbpObservable.<String> empty().skipLast(2);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);
        verify(NbpObserver, never()).onNext(any(String.class));
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testSkipLast1() {
        NbpObservable<String> o = NbpObservable.fromIterable(Arrays.asList("one", "two", "three")).skipLast(2);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(NbpObserver);
        o.subscribe(NbpObserver);
        inOrder.verify(NbpObserver, never()).onNext("two");
        inOrder.verify(NbpObserver, never()).onNext("three");
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testSkipLast2() {
        NbpObservable<String> o = NbpObservable.fromIterable(Arrays.asList("one", "two")).skipLast(2);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);
        verify(NbpObserver, never()).onNext(any(String.class));
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testSkipLastWithZeroCount() {
        NbpObservable<String> w = NbpObservable.just("one", "two");
        NbpObservable<String> NbpObservable = w.skipLast(0);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.subscribe(NbpObserver);
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, times(1)).onNext("two");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    @Ignore("Null values not allowed")
    public void testSkipLastWithNull() {
        NbpObservable<String> o = NbpObservable.fromIterable(Arrays.asList("one", null, "two")).skipLast(1);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, times(1)).onNext(null);
        verify(NbpObserver, never()).onNext("two");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testSkipLastWithBackpressure() {
        NbpObservable<Integer> o = NbpObservable.range(0, Observable.bufferSize() * 2).skipLast(Observable.bufferSize() + 10);
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        o.observeOn(Schedulers.computation()).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals((Observable.bufferSize()) - 10, ts.valueCount());

    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSkipLastWithNegativeCount() {
        NbpObservable.just("one").skipLast(-1);
    }

}