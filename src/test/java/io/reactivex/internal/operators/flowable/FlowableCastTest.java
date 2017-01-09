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
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableCastTest {

    @Test
    public void testCast() {
        Flowable<?> source = Flowable.just(1, 2);
        Flowable<Integer> observable = source.cast(Integer.class);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(1);
        verify(observer, never()).onError(
                any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testCastWithWrongType() {
        Flowable<?> source = Flowable.just(1, 2);
        Flowable<Boolean> observable = source.cast(Boolean.class);

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, times(1)).onError(
                any(ClassCastException.class));
    }

    @Test
    public void castCrashUnsubscribes() {

        PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<String> ts = TestSubscriber.create();

        ps.cast(String.class).subscribe(ts);

        Assert.assertTrue("Not subscribed?", ps.hasSubscribers());

        ps.onNext(1);

        Assert.assertFalse("Subscribed?", ps.hasSubscribers());

        ts.assertError(ClassCastException.class);
    }
}
