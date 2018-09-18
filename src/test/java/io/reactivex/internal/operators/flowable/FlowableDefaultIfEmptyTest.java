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

import static org.mockito.Mockito.*;

import org.junit.*;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.subscribers.*;

public class FlowableDefaultIfEmptyTest {

    @Test
    public void testDefaultIfEmpty() {
        Flowable<Integer> source = Flowable.just(1, 2, 3);
        Flowable<Integer> flowable = source.defaultIfEmpty(10);

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, never()).onNext(10);
        verify(subscriber).onNext(1);
        verify(subscriber).onNext(2);
        verify(subscriber).onNext(3);
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDefaultIfEmptyWithEmpty() {
        Flowable<Integer> source = Flowable.empty();
        Flowable<Integer> flowable = source.defaultIfEmpty(10);

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber).onNext(10);
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    @Ignore("Subscribers should not throw")
    public void testEmptyButClientThrows() {
        final Subscriber<Integer> subscriber = TestHelper.mockSubscriber();

        Flowable.<Integer>empty().defaultIfEmpty(1).subscribe(new DefaultSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
            }
        });

        verify(subscriber).onError(any(TestException.class));
        verify(subscriber, never()).onNext(any(Integer.class));
        verify(subscriber, never()).onComplete();
    }

    @Test
    public void testBackpressureEmpty() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);
        Flowable.<Integer>empty().defaultIfEmpty(1).subscribe(ts);
        ts.assertNoValues();
        ts.assertNotTerminated();
        ts.request(1);
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void testBackpressureNonEmpty() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);
        Flowable.just(1,2,3).defaultIfEmpty(1).subscribe(ts);
        ts.assertNoValues();
        ts.assertNotTerminated();
        ts.request(2);
        ts.assertValues(1, 2);
        ts.request(1);
        ts.assertValues(1, 2, 3);
        ts.assertNoErrors();
        ts.assertComplete();
    }
}
