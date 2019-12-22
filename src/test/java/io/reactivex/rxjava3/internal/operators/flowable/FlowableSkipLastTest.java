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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableSkipLastTest extends RxJavaTest {

    @Test
    public void skipLastEmpty() {
        Flowable<String> flowable = Flowable.<String> empty().skipLast(2);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        verify(subscriber, never()).onNext(any(String.class));
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void skipLast1() {
        Flowable<String> flowable = Flowable.fromIterable(Arrays.asList("one", "two", "three")).skipLast(2);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(subscriber);
        flowable.subscribe(subscriber);

        inOrder.verify(subscriber, never()).onNext("two");
        inOrder.verify(subscriber, never()).onNext("three");
        verify(subscriber, times(1)).onNext("one");
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void skipLast2() {
        Flowable<String> flowable = Flowable.fromIterable(Arrays.asList("one", "two")).skipLast(2);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        verify(subscriber, never()).onNext(any(String.class));
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void skipLastWithZeroCount() {
        Flowable<String> w = Flowable.just("one", "two");
        Flowable<String> flowable = w.skipLast(0);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        verify(subscriber, times(1)).onNext("one");
        verify(subscriber, times(1)).onNext("two");
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void skipLastWithBackpressure() {
        Flowable<Integer> f = Flowable.range(0, Flowable.bufferSize() * 2).skipLast(Flowable.bufferSize() + 10);
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        f.observeOn(Schedulers.computation()).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        assertEquals((Flowable.bufferSize()) - 10, ts.values().size());

    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void skipLastWithNegativeCount() {
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

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f)
                    throws Exception {
                return f.skipLast(1);
            }
        });
    }

}
