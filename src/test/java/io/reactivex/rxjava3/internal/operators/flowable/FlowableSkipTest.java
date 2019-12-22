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
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableSkipTest extends RxJavaTest {

    @Test
    public void skipNegativeElements() {

        Flowable<String> skip = Flowable.just("one", "two", "three").skip(-99);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        skip.subscribe(subscriber);
        verify(subscriber, times(1)).onNext("one");
        verify(subscriber, times(1)).onNext("two");
        verify(subscriber, times(1)).onNext("three");
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void skipZeroElements() {

        Flowable<String> skip = Flowable.just("one", "two", "three").skip(0);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        skip.subscribe(subscriber);
        verify(subscriber, times(1)).onNext("one");
        verify(subscriber, times(1)).onNext("two");
        verify(subscriber, times(1)).onNext("three");
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void skipOneElement() {

        Flowable<String> skip = Flowable.just("one", "two", "three").skip(1);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        skip.subscribe(subscriber);
        verify(subscriber, never()).onNext("one");
        verify(subscriber, times(1)).onNext("two");
        verify(subscriber, times(1)).onNext("three");
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void skipTwoElements() {

        Flowable<String> skip = Flowable.just("one", "two", "three").skip(2);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        skip.subscribe(subscriber);
        verify(subscriber, never()).onNext("one");
        verify(subscriber, never()).onNext("two");
        verify(subscriber, times(1)).onNext("three");
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void skipEmptyStream() {

        Flowable<String> w = Flowable.empty();
        Flowable<String> skip = w.skip(1);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        skip.subscribe(subscriber);
        verify(subscriber, never()).onNext(any(String.class));
        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void skipMultipleObservers() {

        Flowable<String> skip = Flowable.just("one", "two", "three")
                .skip(2);

        Subscriber<String> subscriber1 = TestHelper.mockSubscriber();
        skip.subscribe(subscriber1);

        Subscriber<String> subscriber2 = TestHelper.mockSubscriber();
        skip.subscribe(subscriber2);

        verify(subscriber1, times(1)).onNext(any(String.class));
        verify(subscriber1, never()).onError(any(Throwable.class));
        verify(subscriber1, times(1)).onComplete();

        verify(subscriber2, times(1)).onNext(any(String.class));
        verify(subscriber2, never()).onError(any(Throwable.class));
        verify(subscriber2, times(1)).onComplete();
    }

    @Test
    public void skipError() {

        Exception e = new Exception();

        Flowable<String> ok = Flowable.just("one");
        Flowable<String> error = Flowable.error(e);

        Flowable<String> skip = Flowable.concat(ok, error).skip(100);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        skip.subscribe(subscriber);

        verify(subscriber, never()).onNext(any(String.class));
        verify(subscriber, times(1)).onError(e);
        verify(subscriber, never()).onComplete();

    }

    @Test
    public void backpressureMultipleSmallAsyncRequests() throws InterruptedException {
        final AtomicLong requests = new AtomicLong(0);
        TestSubscriber<Long> ts = new TestSubscriber<>(0L);
        Flowable.interval(100, TimeUnit.MILLISECONDS)
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        requests.addAndGet(n);
                    }
                }).skip(4).subscribe(ts);
        Thread.sleep(100);
        ts.request(1);
        ts.request(1);
        Thread.sleep(100);
        ts.cancel();
        ts.assertNoErrors();
        assertEquals(6, requests.get());
    }

    @Test
    public void requestOverflowDoesNotOccur() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(Long.MAX_VALUE - 1);
        Flowable.range(1, 10).skip(5).subscribe(ts);
        ts.assertTerminated();
        ts.assertComplete();
        ts.assertNoErrors();
        assertEquals(Arrays.asList(6, 7, 8, 9, 10), ts.values());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).skip(2));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f)
                    throws Exception {
                return f.skip(1);
            }
        });
    }

}
